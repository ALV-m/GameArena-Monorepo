const express = require('express');
const { pool } = require('../db/pool');
const { authenticate } = require('../middleware/auth');

const router = express.Router();

router.get('/', authenticate, async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT * FROM wallets WHERE user_id = $1`,
      [req.user.id]
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'Wallet not found' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch wallet' });
  }
});

router.get('/transactions', authenticate, async (req, res) => {
  try {
    const { limit = 50, offset = 0 } = req.query;
    const wallet = await pool.query('SELECT id FROM wallets WHERE user_id = $1', [req.user.id]);
    if (wallet.rows.length === 0) return res.status(404).json({ error: 'Wallet not found' });

    const result = await pool.query(
      `SELECT * FROM transactions WHERE wallet_id = $1 ORDER BY created_at DESC LIMIT $2 OFFSET $3`,
      [wallet.rows[0].id, Math.min(parseInt(limit) || 50, 100), parseInt(offset) || 0]
    );
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch transactions' });
  }
});

router.post('/deduct', authenticate, async (req, res) => {
  const client = await pool.connect();
  try {
    const { amount, type, description } = req.body;
    if (!amount || amount <= 0) return res.status(400).json({ error: 'Invalid amount' });

    await client.query('BEGIN');
    const wallet = await client.query(
      'SELECT * FROM wallets WHERE user_id = $1 FOR UPDATE',
      [req.user.id]
    );
    if (wallet.rows.length === 0) throw new Error('Wallet not found');

    const balance = parseFloat(wallet.rows[0].balance);
    if (balance < parseFloat(amount)) {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Insufficient balance' });
    }

    await client.query(
      'UPDATE wallets SET balance = balance - $1, total_spent = total_spent + $1, updated_at = NOW() WHERE id = $2',
      [amount, wallet.rows[0].id]
    );

    const tx = await client.query(
      `INSERT INTO transactions (wallet_id, type, amount, status, description)
       VALUES ($1, $2, $3, 'completed', $4) RETURNING *`,
      [wallet.rows[0].id, type, amount, description]
    );

    await client.query('COMMIT');
    res.json(tx.rows[0]);
  } catch (err) {
    await client.query('ROLLBACK');
    res.status(500).json({ error: 'Transaction failed' });
  } finally {
    client.release();
  }
});

router.post('/credit', authenticate, async (req, res) => {
  const client = await pool.connect();
  try {
    const { amount, type, description } = req.body;
    if (!amount || amount <= 0) return res.status(400).json({ error: 'Invalid amount' });

    await client.query('BEGIN');
    const wallet = await client.query(
      'SELECT * FROM wallets WHERE user_id = $1 FOR UPDATE',
      [req.user.id]
    );
    if (wallet.rows.length === 0) throw new Error('Wallet not found');

    const isPrize = ['tournament_prize', 'challenge_prize'].includes(type);
    await client.query(
      `UPDATE wallets SET balance = balance + $1,
       ${isPrize ? 'total_earned = total_earned + $1' : 'total_deposited = total_deposited + $1'},
       updated_at = NOW() WHERE id = $2`,
      [amount, wallet.rows[0].id]
    );

    const tx = await client.query(
      `INSERT INTO transactions (wallet_id, type, amount, status, description)
       VALUES ($1, $2, $3, 'completed', $4) RETURNING *`,
      [wallet.rows[0].id, type, amount, description]
    );

    await client.query('COMMIT');
    res.json(tx.rows[0]);
  } catch (err) {
    await client.query('ROLLBACK');
    res.status(500).json({ error: 'Transaction failed' });
  } finally {
    client.release();
  }
});

module.exports = router;
