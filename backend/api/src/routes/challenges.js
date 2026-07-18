const express = require('express');
const { pool } = require('../db/pool');
const { authenticate } = require('../middleware/auth');

const router = express.Router();

router.get('/', async (req, res) => {
  try {
    const { status, game, limit = 20, offset = 0 } = req.query;
    let query = `
      SELECT c.*, u1.username as challenger_name, u2.username as opponent_name,
             w.username as winner_name
      FROM challenges c
      JOIN users u1 ON c.challenger_id = u1.id
      LEFT JOIN users u2 ON c.opponent_id = u2.id
      LEFT JOIN users w ON c.winner_id = w.id
      WHERE 1=1`;
    const params = [];
    let p = 0;

    if (status) { query += ` AND c.status = $${++p}`; params.push(status); }
    if (game) { query += ` AND c.game = $${++p}`; params.push(game); }

    query += ` ORDER BY c.created_at DESC LIMIT $${++p} OFFSET $${++p}`;
    params.push(Math.min(parseInt(limit) || 20, 100), parseInt(offset) || 0);

    const result = await pool.query(query, params);
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch challenges' });
  }
});

router.post('/', authenticate, async (req, res) => {
  const client = await pool.connect();
  try {
    const { game, stake_amount, opponent_username } = req.body;

    if (!game || !stake_amount || stake_amount <= 0) {
      return res.status(400).json({ error: 'Game and positive stake amount required' });
    }

    await client.query('BEGIN');
    const wallet = await client.query(
      'SELECT * FROM wallets WHERE user_id = $1 FOR UPDATE',
      [req.user.id]
    );
    if (parseFloat(wallet.rows[0].balance) < parseFloat(stake_amount)) {
      throw new Error('Insufficient balance');
    }

    await client.query(
      'UPDATE wallets SET balance = balance - $1, total_spent = total_spent + $1, updated_at = NOW() WHERE id = $2',
      [stake_amount, wallet.rows[0].id]
    );

    let opponentId = null;
    if (opponent_username) {
      const opp = await client.query('SELECT id FROM users WHERE username = $1', [opponent_username]);
      if (opp.rows.length === 0) throw new Error('Opponent not found');
      if (opp.rows[0].id === req.user.id) throw new Error('Cannot challenge yourself');
      opponentId = opp.rows[0].id;

      const oppWallet = await client.query(
        'SELECT * FROM wallets WHERE user_id = $1 FOR UPDATE',
        [opponentId]
      );
      if (oppWallet.rows.length > 0 && parseFloat(oppWallet.rows[0].balance) < parseFloat(stake_amount)) {
        throw new Error('Opponent has insufficient balance');
      }
    }

    const challenge = await client.query(
      `INSERT INTO challenges (challenger_id, opponent_id, game, stake_amount, status)
       VALUES ($1, $2, $3, $4, $5) RETURNING *`,
      [req.user.id, opponentId, game, stake_amount, opponentId ? 'open' : 'open']
    );

    await client.query(
      `INSERT INTO transactions (wallet_id, type, amount, status, description)
       VALUES ($1, 'challenge_entry', $2, 'pending', $3)`,
      [wallet.rows[0].id, stake_amount, `Challenge stake for ${game}`]
    );

    await client.query('COMMIT');
    res.status(201).json(challenge.rows[0]);
  } catch (err) {
    await client.query('ROLLBACK');
    res.status(400).json({ error: err.message });
  } finally {
    client.release();
  }
});

router.post('/:id/accept', authenticate, async (req, res) => {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const challenge = await client.query(
      'SELECT * FROM challenges WHERE id = $1 FOR UPDATE',
      [req.params.id]
    );
    if (challenge.rows.length === 0) throw new Error('Challenge not found');

    const c = challenge.rows[0];
    if (c.status !== 'open') throw new Error('Challenge is no longer open');
    if (c.challenger_id === req.user.id) throw new Error('Cannot accept your own challenge');
    if (c.opponent_id && c.opponent_id !== req.user.id) throw new Error('Challenge is for a specific opponent');

    const wallet = await client.query(
      'SELECT * FROM wallets WHERE user_id = $1 FOR UPDATE',
      [req.user.id]
    );
    if (parseFloat(wallet.rows[0].balance) < parseFloat(c.stake_amount)) {
      throw new Error('Insufficient balance');
    }

    await client.query(
      'UPDATE wallets SET balance = balance - $1, total_spent = total_spent + $1, updated_at = NOW() WHERE id = $2',
      [c.stake_amount, wallet.rows[0].id]
    );
    await client.query(
      `INSERT INTO transactions (wallet_id, type, amount, status, description)
       VALUES ($1, 'challenge_entry', $2, 'pending', $3)`,
      [wallet.rows[0].id, c.stake_amount, `Challenge stake accepted`]
    );

    await client.query(
      "UPDATE challenges SET opponent_id = $1, status = 'accepted', accepted_at = NOW() WHERE id = $2",
      [req.user.id, req.params.id]
    );

    const match = await client.query(
      `INSERT INTO matches (challenge_id, player1_id, player2_id, stake_amount, status)
       VALUES ($1, $2, $3, $4, 'pending') RETURNING *`,
      [req.params.id, c.challenger_id, req.user.id, parseFloat(c.stake_amount) * 2]
    );

    await client.query('COMMIT');
    res.json({ challenge: challenge.rows[0], match: match.rows[0] });
  } catch (err) {
    await client.query('ROLLBACK');
    res.status(400).json({ error: err.message });
  } finally {
    client.release();
  }
});

router.post('/:id/cancel', authenticate, async (req, res) => {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const challenge = await client.query(
      'SELECT * FROM challenges WHERE id = $1 FOR UPDATE',
      [req.params.id]
    );
    if (challenge.rows.length === 0) throw new Error('Challenge not found');
    if (challenge.rows[0].challenger_id !== req.user.id) throw new Error('Only creator can cancel');
    if (!['open'].includes(challenge.rows[0].status)) throw new Error('Cannot cancel');

    const wallet = await client.query(
      'SELECT * FROM wallets WHERE user_id = $1 FOR UPDATE',
      [req.user.id]
    );
    await client.query(
      'UPDATE wallets SET balance = balance + $1, total_spent = total_spent - $1, updated_at = NOW() WHERE id = $2',
      [challenge.rows[0].stake_amount, wallet.rows[0].id]
    );

    await client.query(
      "UPDATE challenges SET status = 'cancelled' WHERE id = $1",
      [req.params.id]
    );

    await client.query('COMMIT');
    res.json({ message: 'Challenge cancelled' });
  } catch (err) {
    await client.query('ROLLBACK');
    res.status(400).json({ error: err.message });
  } finally {
    client.release();
  }
});

module.exports = router;
