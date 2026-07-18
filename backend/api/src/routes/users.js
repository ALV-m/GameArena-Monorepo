const express = require('express');
const { pool } = require('../db/pool');
const { authenticate } = require('../middleware/auth');

const router = express.Router();

router.get('/leaderboard', async (req, res) => {
  try {
    const { game, limit = 50 } = req.query;
    const result = await pool.query(
      `SELECT id, username, avatar_url, country, rank_tier, elo_rating, total_wins, total_losses, total_matches
       FROM users WHERE is_banned = FALSE
       ORDER BY elo_rating DESC NULLS LAST
       LIMIT $1`,
      [Math.min(parseInt(limit) || 50, 100)]
    );
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch leaderboard' });
  }
});

router.get('/:id', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT id, username, avatar_url, country, rank_tier, elo_rating,
              total_wins, total_losses, total_matches, created_at
       FROM users WHERE id = $1 AND is_banned = FALSE`,
      [req.params.id]
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'User not found' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch user' });
  }
});

router.put('/profile', authenticate, async (req, res) => {
  try {
    const { avatar_url, country } = req.body;
    const result = await pool.query(
      `UPDATE users SET avatar_url = COALESCE($1, avatar_url), country = COALESCE($2, country), updated_at = NOW()
       WHERE id = $3 RETURNING id, username, avatar_url, country`,
      [avatar_url, country, req.user.id]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Failed to update profile' });
  }
});

module.exports = router;
