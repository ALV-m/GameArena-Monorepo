const express = require('express');
const { pool } = require('../db/pool');
const { authenticate } = require('../middleware/auth');

const router = express.Router();

router.get('/:id', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT m.*, u1.username as player1_name, u2.username as player2_name,
              w.username as winner_name, t.name as tournament_name
       FROM matches m
       LEFT JOIN users u1 ON m.player1_id = u1.id
       LEFT JOIN users u2 ON m.player2_id = u2.id
       LEFT JOIN users w ON m.winner_id = w.id
       LEFT JOIN tournaments t ON m.tournament_id = t.id
       WHERE m.id = $1`,
      [req.params.id]
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'Match not found' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch match' });
  }
});

router.post('/:id/result', authenticate, async (req, res) => {
  const client = await pool.connect();
  try {
    const { player1_score, player2_score, winner_id } = req.body;

    await client.query('BEGIN');
    const match = await client.query(
      'SELECT * FROM matches WHERE id = $1 FOR UPDATE',
      [req.params.id]
    );
    if (match.rows.length === 0) throw new Error('Match not found');

    const m = match.rows[0];
    if (m.player1_id !== req.user.id && m.player2_id !== req.user.id) {
      throw new Error('Not a participant');
    }
    if (m.status === 'completed') throw new Error('Match already completed');
    if (!winner_id) throw new Error('Winner must be specified');
    if (winner_id !== m.player1_id && winner_id !== m.player2_id) {
      throw new Error('Winner must be a match participant');
    }

    await client.query(
      `UPDATE matches SET player1_score = $1, player2_score = $2, winner_id = $3,
       status = 'completed', completed_at = NOW() WHERE id = $4`,
      [player1_score || 0, player2_score || 0, winner_id, req.params.id]
    );

    const loser_id = winner_id === m.player1_id ? m.player2_id : m.player1_id;
    const totalPrize = parseFloat(m.stake_amount);

    if (totalPrize > 0 && loser_id) {
      const winnerWallet = await client.query(
        'SELECT * FROM wallets WHERE user_id = $1 FOR UPDATE',
        [winner_id]
      );
      if (winnerWallet.rows.length > 0) {
        await client.query(
          'UPDATE wallets SET balance = balance + $1, total_earned = total_earned + $1, updated_at = NOW() WHERE id = $2',
          [totalPrize, winnerWallet.rows[0].id]
        );
        await client.query(
          `INSERT INTO transactions (wallet_id, type, amount, status, description)
           VALUES ($1, 'challenge_prize', $2, 'completed', $3)`,
          [winnerWallet.rows[0].id, totalPrize, 'Match prize payout']
        );
      }

      await client.query(
        "UPDATE transactions SET status = 'completed' WHERE wallet_id = (SELECT id FROM wallets WHERE user_id = $1) AND type = 'challenge_entry' AND status = 'pending'",
        [winner_id]
      );
      await client.query(
        "UPDATE transactions SET status = 'completed' WHERE wallet_id = (SELECT id FROM wallets WHERE user_id = $1) AND type = 'challenge_entry' AND status = 'pending'",
        [loser_id]
      );
    }

    await client.query(
      "UPDATE users SET total_matches = total_matches + 1 WHERE id IN ($1, $2)",
      [m.player1_id, m.player2_id]
    );
    await client.query(
      "UPDATE users SET total_wins = total_wins + 1 WHERE id = $1",
      [winner_id]
    );
    await client.query(
      "UPDATE users SET total_losses = total_losses + 1 WHERE id = $1",
      [loser_id]
    );

    await client.query('COMMIT');
    res.json({ message: 'Result submitted and payout processed' });
  } catch (err) {
    await client.query('ROLLBACK');
    res.status(400).json({ error: err.message });
  } finally {
    client.release();
  }
});

router.post('/:id/dispute', authenticate, async (req, res) => {
  try {
    const { reason, evidence_url } = req.body;
    if (!reason) return res.status(400).json({ error: 'Reason required' });

    const match = await pool.query('SELECT * FROM matches WHERE id = $1', [req.params.id]);
    if (match.rows.length === 0) return res.status(404).json({ error: 'Match not found' });

    const m = match.rows[0];
    if (m.player1_id !== req.user.id && m.player2_id !== req.user.id) {
      return res.status(403).json({ error: 'Not a participant' });
    }

    await pool.query(
      "UPDATE matches SET status = 'disputed' WHERE id = $1",
      [req.params.id]
    );

    const dispute = await pool.query(
      `INSERT INTO disputes (match_id, reporter_id, reason, evidence_url)
       VALUES ($1, $2, $3, $4) RETURNING *`,
      [req.params.id, req.user.id, reason, evidence_url]
    );

    res.status(201).json(dispute.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Failed to create dispute' });
  }
});

module.exports = router;
