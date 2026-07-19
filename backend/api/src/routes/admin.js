const express = require('express');
const { pool } = require('../db/pool');
const { authenticate, adminOnly } = require('../middleware/auth');

const router = express.Router();

router.use(authenticate, adminOnly);

router.get('/dashboard', async (req, res) => {
  try {
    const [users, tournaments, matches, transactions, disputes, revenue] = await Promise.all([
      pool.query('SELECT COUNT(*) as total, COUNT(*) FILTER (WHERE created_at > NOW() - INTERVAL \'24 hours\') as today FROM ga_users'),
      pool.query('SELECT COUNT(*) as total, COUNT(*) FILTER (WHERE status = \'in_progress\') as active, COUNT(*) FILTER (WHERE status = \'completed\') as completed FROM ga_tournaments'),
      pool.query('SELECT COUNT(*) as total, COUNT(*) FILTER (WHERE status = \'completed\') as completed, COUNT(*) FILTER (WHERE status = \'disputed\') as disputed FROM ga_matches'),
      pool.query('SELECT SUM(amount) as total_deposited, SUM(amount) as total_withdrawn FROM ga_transactions WHERE status = \'completed\''),
      pool.query('SELECT COUNT(*) as total, COUNT(*) FILTER (WHERE status = \'open\') as pending FROM ga_disputes'),
      pool.query("SELECT SUM(amount) as platform_fees FROM ga_transactions WHERE type IN ('tournament_entry', 'challenge_entry') AND status = 'completed'"),
    ]);

    res.json({
      users: users.rows[0],
      tournaments: tournaments.rows[0],
      matches: matches.rows[0],
      transactions: transactions.rows[0],
      disputes: disputes.rows[0],
      revenue: revenue.rows[0],
    });
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch dashboard' });
  }
});

router.get('/users', async (req, res) => {
  try {
    const { search, banned, limit = 50, offset = 0 } = req.query;
    let query = `SELECT u.id, u.username, u.email, u.is_admin, u.is_banned, u.elo_rating,
                        u.total_wins, u.total_losses, u.created_at,
                        w.balance
                 FROM ga_users u LEFT JOIN ga_wallets w ON w.user_id = u.id WHERE 1=1`;
    const params = [];
    let p = 0;

    if (search) { query += ` AND (u.username ILIKE $${++p} OR u.email ILIKE $${p})`; params.push(`%${search}%`); }
    if (banned !== undefined) { query += ` AND u.is_banned = $${++p}`; params.push(banned === 'true'); }

    query += ` ORDER BY u.created_at DESC LIMIT $${++p} OFFSET $${++p}`;
    params.push(Math.min(parseInt(limit) || 50, 100), parseInt(offset) || 0);

    const result = await pool.query(query, params);
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch users' });
  }
});

router.put('/users/:id/ban', async (req, res) => {
  try {
    const { ban, reason } = req.body;
    const result = await pool.query(
      `UPDATE ga_users SET is_banned = $1, ban_reason = $2, updated_at = NOW() WHERE id = $3
       RETURNING id, username, is_banned, ban_reason`,
      [ban, reason, req.params.id]
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'User not found' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Failed to update user' });
  }
});

router.put('/users/:id/admin', async (req, res) => {
  try {
    const { admin } = req.body;
    const result = await pool.query(
      'UPDATE ga_users SET is_admin = $1, updated_at = NOW() WHERE id = $2 RETURNING id, username, is_admin',
      [admin, req.params.id]
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'User not found' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Failed to update admin status' });
  }
});

router.get('/tournaments', async (req, res) => {
  try {
    const { status, limit = 50, offset = 0 } = req.query;
    let query = `SELECT t.*, u.username as creator_name FROM ga_tournaments t JOIN ga_users u ON t.creator_id = u.id WHERE 1=1`;
    const params = [];
    let p = 0;

    if (status) { query += ` AND t.status = $${++p}`; params.push(status); }
    query += ` ORDER BY t.created_at DESC LIMIT $${++p} OFFSET $${++p}`;
    params.push(Math.min(parseInt(limit) || 50, 100), parseInt(offset) || 0);

    const result = await pool.query(query, params);
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch tournaments' });
  }
});

router.put('/tournaments/:id/status', async (req, res) => {
  try {
    const { status } = req.body;
    const result = await pool.query(
      "UPDATE ga_tournaments SET status = $1, updated_at = NOW() WHERE id = $2 RETURNING *",
      [status, req.params.id]
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'Tournament not found' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Failed to update tournament' });
  }
});

router.get('/transactions', async (req, res) => {
  try {
    const { type, status, limit = 50, offset = 0 } = req.query;
    let query = `SELECT t.*, u.username FROM ga_transactions t JOIN ga_wallets w ON t.wallet_id = w.id JOIN ga_users u ON w.user_id = u.id WHERE 1=1`;
    const params = [];
    let p = 0;

    if (type) { query += ` AND t.type = $${++p}`; params.push(type); }
    if (status) { query += ` AND t.status = $${++p}`; params.push(status); }
    query += ` ORDER BY t.created_at DESC LIMIT $${++p} OFFSET $${++p}`;
    params.push(Math.min(parseInt(limit) || 50, 100), parseInt(offset) || 0);

    const result = await pool.query(query, params);
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch transactions' });
  }
});

router.get('/disputes', async (req, res) => {
  try {
    const { status, limit = 50, offset = 0 } = req.query;
    let query = `SELECT d.*, u.username as reporter_name, m.player1_id, m.player2_id,
                        u1.username as player1_name, u2.username as player2_name
                 FROM ga_disputes d
                 JOIN ga_users u ON d.reporter_id = u.id
                 JOIN ga_matches m ON d.match_id = m.id
                 LEFT JOIN ga_users u1 ON m.player1_id = u1.id
                 LEFT JOIN ga_users u2 ON m.player2_id = u2.id WHERE 1=1`;
    const params = [];
    let p = 0;

    if (status) { query += ` AND d.status = $${++p}`; params.push(status); }
    query += ` ORDER BY d.created_at DESC LIMIT $${++p} OFFSET $${++p}`;
    params.push(Math.min(parseInt(limit) || 50, 100), parseInt(offset) || 0);

    const result = await pool.query(query, params);
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch disputes' });
  }
});

router.put('/disputes/:id/resolve', async (req, res) => {
  const client = await pool.connect();
  try {
    const { resolution, winner_id, status = 'resolved' } = req.body;
    await client.query('BEGIN');

    const dispute = await client.query(
      'SELECT * FROM ga_disputes WHERE id = $1 FOR UPDATE',
      [req.params.id]
    );
    if (dispute.rows.length === 0) throw new Error('Dispute not found');

    const d = dispute.rows[0];
    await client.query(
      "UPDATE ga_disputes SET status = $1, admin_id = $2, resolution = $3, resolved_at = NOW() WHERE id = $4",
      [status, req.user.id, resolution, req.params.id]
    );

    if (winner_id) {
      const match = await client.query('SELECT * FROM ga_matches WHERE id = $1 FOR UPDATE', [d.match_id]);
      const m = match.rows[0];
      if (m && parseFloat(m.stake_amount) > 0) {
        const loser_id = winner_id === m.player1_id ? m.player2_id : m.player1_id;
        const wallet = await client.query('SELECT * FROM ga_wallets WHERE user_id = $1 FOR UPDATE', [winner_id]);
        if (wallet.rows.length > 0) {
          await client.query(
            'UPDATE ga_wallets SET balance = balance + $1, total_earned = total_earned + $1, updated_at = NOW() WHERE id = $2',
            [m.stake_amount, wallet.rows[0].id]
          );
          await client.query(
            `INSERT INTO ga_transactions (wallet_id, type, amount, status, description)
             VALUES ($1, 'challenge_prize', $2, 'completed', 'Dispute resolved - payout')`,
            [wallet.rows[0].id, m.stake_amount]
          );
        }
      }
      await client.query("UPDATE ga_matches SET winner_id = $1, status = 'completed', completed_at = NOW() WHERE id = $2", [winner_id, d.match_id]);
    }

    await client.query('COMMIT');
    res.json({ message: 'Dispute resolved' });
  } catch (err) {
    await client.query('ROLLBACK');
    res.status(400).json({ error: err.message });
  } finally {
    client.release();
  }
});

router.get('/payments', async (req, res) => {
  try {
    const { gateway, status, limit = 50, offset = 0 } = req.query;
    let query = `SELECT p.*, u.username FROM ga_payments p JOIN ga_users u ON p.user_id = u.id WHERE 1=1`;
    const params = [];
    let p = 0;

    if (gateway) { query += ` AND p.gateway = $${++p}`; params.push(gateway); }
    if (status) { query += ` AND p.status = $${++p}`; params.push(status); }
    query += ` ORDER BY p.created_at DESC LIMIT $${++p} OFFSET $${++p}`;
    params.push(Math.min(parseInt(limit) || 50, 100), parseInt(offset) || 0);

    const result = await pool.query(query, params);
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch payments' });
  }
});

module.exports = router;
