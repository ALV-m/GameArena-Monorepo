const express = require('express');
const { pool } = require('../db/pool');
const { authenticate } = require('../middleware/auth');

const router = express.Router();

router.get('/', async (req, res) => {
  try {
    const { status, game, limit = 20, offset = 0 } = req.query;
    let query = `
      SELECT t.*, u.username as creator_name,
        (SELECT COUNT(*) FROM tournament_participants tp WHERE tp.tournament_id = t.id) as registered_players
      FROM tournaments t JOIN users u ON t.creator_id = u.id
      WHERE 1=1`;
    const params = [];
    let paramCount = 0;

    if (status) { paramCount++; query += ` AND t.status = $${paramCount}`; params.push(status); }
    if (game) { paramCount++; query += ` AND t.game = $${paramCount}`; params.push(game); }

    query += ` ORDER BY t.created_at DESC LIMIT $${++paramCount} OFFSET $${++paramCount}`;
    params.push(Math.min(parseInt(limit) || 20, 100), parseInt(offset) || 0);

    const result = await pool.query(query, params);
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch tournaments' });
  }
});

router.get('/:id', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT t.*, u.username as creator_name,
        (SELECT COUNT(*) FROM tournament_participants tp WHERE tp.tournament_id = t.id) as registered_players
       FROM tournaments t JOIN users u ON t.creator_id = u.id WHERE t.id = $1`,
      [req.params.id]
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'Tournament not found' });

    const participants = await pool.query(
      `SELECT u.id, u.username, u.avatar_url, u.elo_rating, tp.seed_number, tp.eliminated, tp.final_position
       FROM tournament_participants tp JOIN users u ON tp.user_id = u.id
       WHERE tp.tournament_id = $1 ORDER BY tp.joined_at`,
      [req.params.id]
    );

    const matches = await pool.query(
      `SELECT m.*, u1.username as player1_name, u2.username as player2_name, w.username as winner_name
       FROM matches m
       LEFT JOIN users u1 ON m.player1_id = u1.id
       LEFT JOIN users u2 ON m.player2_id = u2.id
       LEFT JOIN users w ON m.winner_id = w.id
       WHERE m.tournament_id = $1 ORDER BY m.round_number, m.bracket_position`,
      [req.params.id]
    );

    res.json({ ...result.rows[0], participants: participants.rows, matches: matches.rows });
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch tournament' });
  }
});

router.post('/', authenticate, async (req, res) => {
  try {
    const { name, game, description, format, rules, entry_fee, max_players, start_time, registration_deadline } = req.body;

    if (!name || !game || !format || !max_players) {
      return res.status(400).json({ error: 'Name, game, format, and max_players are required' });
    }
    if (max_players < 2 || max_players > 128) {
      return res.status(400).json({ error: 'Max players must be 2-128' });
    }

    const result = await pool.query(
      `INSERT INTO tournaments (creator_id, name, game, description, format, rules, entry_fee, max_players, start_time, registration_deadline, status)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, 'registration') RETURNING *`,
      [req.user.id, name, game, description, format, rules, entry_fee || 0, max_players, start_time, registration_deadline]
    );

    res.status(201).json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Failed to create tournament' });
  }
});

router.post('/:id/join', authenticate, async (req, res) => {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const tournament = await client.query(
      'SELECT * FROM tournaments WHERE id = $1 FOR UPDATE',
      [req.params.id]
    );
    if (tournament.rows.length === 0) throw new Error('Tournament not found');

    const t = tournament.rows[0];
    if (t.status !== 'registration') throw new Error('Tournament is not accepting registrations');
    if (t.current_players >= t.max_players) throw new Error('Tournament is full');
    if (t.creator_id === req.user.id) throw new Error('Cannot join your own tournament');

    const existing = await client.query(
      'SELECT id FROM tournament_participants WHERE tournament_id = $1 AND user_id = $2',
      [req.params.id, req.user.id]
    );
    if (existing.rows.length > 0) throw new Error('Already registered');

    if (parseFloat(t.entry_fee) > 0) {
      const wallet = await client.query(
        'SELECT * FROM wallets WHERE user_id = $1 FOR UPDATE',
        [req.user.id]
      );
      if (parseFloat(wallet.rows[0].balance) < parseFloat(t.entry_fee)) {
        throw new Error('Insufficient balance');
      }
      await client.query(
        'UPDATE wallets SET balance = balance - $1, total_spent = total_spent + $1, updated_at = NOW() WHERE id = $2',
        [t.entry_fee, wallet.rows[0].id]
      );
      await client.query(
        `INSERT INTO transactions (wallet_id, type, amount, status, description)
         VALUES ($1, 'tournament_entry', $2, 'completed', $3)`,
        [wallet.rows[0].id, t.entry_fee, `Entry fee for ${t.name}`]
      );
    }

    await client.query(
      'INSERT INTO tournament_participants (tournament_id, user_id) VALUES ($1, $2)',
      [req.params.id, req.user.id]
    );
    await client.query(
      'UPDATE tournaments SET current_players = current_players + 1, updated_at = NOW() WHERE id = $1',
      [req.params.id]
    );

    await client.query('COMMIT');
    res.json({ message: 'Joined tournament successfully' });
  } catch (err) {
    await client.query('ROLLBACK');
    res.status(400).json({ error: err.message });
  } finally {
    client.release();
  }
});

router.post('/:id/leave', authenticate, async (req, res) => {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const tournament = await client.query(
      'SELECT * FROM tournaments WHERE id = $1 FOR UPDATE',
      [req.params.id]
    );
    if (tournament.rows.length === 0) throw new Error('Tournament not found');

    const t = tournament.rows[0];
    if (t.status !== 'registration') throw new Error('Cannot leave after tournament starts');

    const participant = await client.query(
      'SELECT id FROM tournament_participants WHERE tournament_id = $1 AND user_id = $2',
      [req.params.id, req.user.id]
    );
    if (participant.rows.length === 0) throw new Error('Not registered');

    await client.query(
      'DELETE FROM tournament_participants WHERE tournament_id = $1 AND user_id = $2',
      [req.params.id, req.user.id]
    );
    await client.query(
      'UPDATE tournaments SET current_players = current_players - 1, updated_at = NOW() WHERE id = $1',
      [req.params.id]
    );

    if (parseFloat(t.entry_fee) > 0) {
      const wallet = await client.query(
        'SELECT * FROM wallets WHERE user_id = $1 FOR UPDATE',
        [req.user.id]
      );
      await client.query(
        'UPDATE wallets SET balance = balance + $1, updated_at = NOW() WHERE id = $2',
        [t.entry_fee, wallet.rows[0].id]
      );
      await client.query(
        `INSERT INTO transactions (wallet_id, type, amount, status, description)
         VALUES ($1, 'refund', $2, 'completed', $3)`,
        [wallet.rows[0].id, t.entry_fee, `Refund for leaving ${t.name}`]
      );
    }

    await client.query('COMMIT');
    res.json({ message: 'Left tournament successfully' });
  } catch (err) {
    await client.query('ROLLBACK');
    res.status(400).json({ error: err.message });
  } finally {
    client.release();
  }
});

router.post('/:id/start', authenticate, async (req, res) => {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const tournament = await client.query(
      'SELECT * FROM tournaments WHERE id = $1 FOR UPDATE',
      [req.params.id]
    );
    if (tournament.rows.length === 0) throw new Error('Tournament not found');

    const t = tournament.rows[0];
    if (t.creator_id !== req.user.id && !req.user.is_admin) throw new Error('Only creator can start');
    if (t.status !== 'registration') throw new Error('Tournament not in registration');
    if (t.current_players < 2) throw new Error('Need at least 2 players');

    const participants = await client.query(
      'SELECT user_id FROM tournament_participants WHERE tournament_id = $1 ORDER BY RANDOM()',
      [req.params.id]
    );

    const players = participants.rows.map(p => p.user_id);
    const matches = [];
    let round = 1;

    if (t.format === 'single_elimination') {
      for (let i = 0; i < players.length; i += 2) {
        const m = await client.query(
          `INSERT INTO matches (tournament_id, round_number, bracket_position, player1_id, player2_id, stake_amount, status)
           VALUES ($1, $2, $3, $4, $5, $6, 'pending') RETURNING *`,
          [req.params.id, 1, i / 2, players[i], players[i + 1] || null, t.entry_fee]
        );
        matches.push(m.rows[0]);

        if (!players[i + 1]) {
          await client.query(
            `UPDATE matches SET winner_id = $1, player2_id = NULL, status = 'completed', completed_at = NOW() WHERE id = $2`,
            [players[i], m.rows[0].id]
          );
        }
      }
    }

    await client.query(
      "UPDATE tournaments SET status = 'in_progress', updated_at = NOW() WHERE id = $1",
      [req.params.id]
    );

    await client.query('COMMIT');
    res.json({ message: 'Tournament started', matches });
  } catch (err) {
    await client.query('ROLLBACK');
    res.status(400).json({ error: err.message });
  } finally {
    client.release();
  }
});

module.exports = router;
