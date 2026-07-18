const express = require('express');
const { pool } = require('../db/pool');
const { authenticate } = require('../middleware/auth');

const router = express.Router();

const stripe = require('stripe')(process.env.STRIPE_SECRET_KEY);

router.post('/stripe/deposit', authenticate, async (req, res) => {
  try {
    const { amount, currency = 'kes' } = req.body;
    if (!amount || amount < 100) return res.status(400).json({ error: 'Minimum deposit is 100' });

    const paymentIntent = await stripe.paymentIntents.create({
      amount: Math.round(amount * 100),
      currency: currency.toLowerCase(),
      metadata: { user_id: req.user.id, type: 'deposit' },
    });

    await pool.query(
      `INSERT INTO payments (user_id, gateway, gateway_reference_id, amount, currency, type, status)
       VALUES ($1, 'stripe', $2, $3, $4, 'deposit', 'pending')`,
      [req.user.id, paymentIntent.id, amount, currency.toUpperCase()]
    );

    res.json({ clientSecret: paymentIntent.client_secret, paymentIntentId: paymentIntent.id });
  } catch (err) {
    res.status(500).json({ error: 'Stripe deposit failed' });
  }
});

router.post('/stripe/withdraw', authenticate, async (req, res) => {
  try {
    const { amount } = req.body;
    if (!amount || amount < 200) return res.status(400).json({ error: 'Minimum withdrawal is 200' });

    const wallet = await pool.query('SELECT * FROM wallets WHERE user_id = $1 FOR UPDATE', [req.user.id]);
    if (parseFloat(wallet.rows[0].balance) < parseFloat(amount)) {
      return res.status(400).json({ error: 'Insufficient balance' });
    }

    await pool.query(
      'UPDATE wallets SET balance = balance - $1, total_withdrawn = total_withdrawn + $1, updated_at = NOW() WHERE id = $2',
      [amount, wallet.rows[0].id]
    );

    const tx = await pool.query(
      `INSERT INTO transactions (wallet_id, type, amount, status, payment_gateway, description)
       VALUES ($1, 'withdrawal', $2, 'pending', 'stripe', $3) RETURNING *`,
      [wallet.rows[0].id, amount, 'Withdrawal request']
    );

    await pool.query(
      `INSERT INTO payments (user_id, gateway, amount, currency, type, status)
       VALUES ($1, 'stripe', $2, 'KES', 'withdrawal', 'pending')`,
      [req.user.id, amount]
    );

    res.json({ message: 'Withdrawal requested', transaction: tx.rows[0] });
  } catch (err) {
    res.status(500).json({ error: 'Withdrawal failed' });
  }
});

router.post('/stripe/webhook', express.raw({ type: 'application/json' }), async (req, res) => {
  try {
    const sig = req.headers['stripe-signature'];
    let event;
    try {
      event = stripe.webhooks.constructEvent(req.body, sig, process.env.STRIPE_WEBHOOK_SECRET);
    } catch (e) {
      return res.status(400).send(`Webhook Error: ${e.message}`);
    }

    if (event.type === 'payment_intent.succeeded') {
      const pi = event.data.object;
      const userId = pi.metadata.user_id;
      if (userId) {
        const wallet = await pool.query('SELECT * FROM wallets WHERE user_id = $1 FOR UPDATE', [userId]);
        if (wallet.rows.length > 0) {
          const amount = pi.amount / 100;
          await pool.query(
            'UPDATE wallets SET balance = balance + $1, total_deposited = total_deposited + $1, updated_at = NOW() WHERE id = $2',
            [amount, wallet.rows[0].id]
          );
          await pool.query(
            `INSERT INTO transactions (wallet_id, type, amount, status, payment_gateway, gateway_reference_id, description)
             VALUES ($1, 'deposit', $2, 'completed', 'stripe', $3, 'Stripe deposit')`,
            [wallet.rows[0].id, amount, pi.id]
          );
          await pool.query(
            "UPDATE payments SET status = 'completed' WHERE gateway_reference_id = $1",
            [pi.id]
          );
        }
      }
    }
    res.json({ received: true });
  } catch (err) {
    res.status(500).json({ error: 'Webhook processing failed' });
  }
});

router.post('/payhero/deposit', authenticate, async (req, res) => {
  try {
    const { amount, phone_number } = req.body;
    if (!amount || !phone_number) {
      return res.status(400).json({ error: 'Amount and phone number required' });
    }

    const response = await fetch(`${process.env.PAYHERO_API_URL}/initialize`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${process.env.PAYHERO_API_KEY}`,
      },
      body: JSON.stringify({
        phone_number,
        amount: parseFloat(amount),
        currency: 'KES',
        external_id: `GA-${req.user.id}-${Date.now()}`,
      }),
    });

    const data = await response.json();
    if (!response.ok) throw new Error(data.message || 'PayHero request failed');

    await pool.query(
      `INSERT INTO payments (user_id, gateway, gateway_session_id, amount, currency, type, status, phone_number, metadata)
       VALUES ($1, 'payhero', $2, $3, 'KES', 'deposit', 'pending', $4, $5)`,
      [req.user.id, data.checkout_request_id, amount, phone_number, JSON.stringify(data)]
    );

    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message || 'M-Pesa deposit failed' });
  }
});

router.post('/payhero/callback', async (req, res) => {
  try {
    const { ResultCode, ExternalID, Amount, TransactionID } = req.body;

    if (ResultCode === 0) {
      const userId = ExternalID.split('-')[1];
      const wallet = await pool.query('SELECT * FROM wallets WHERE user_id = $1 FOR UPDATE', [userId]);
      if (wallet.rows.length > 0) {
        await pool.query(
          'UPDATE wallets SET balance = balance + $1, total_deposited = total_deposited + $1, updated_at = NOW() WHERE id = $2',
          [Amount, wallet.rows[0].id]
        );
        await pool.query(
          `INSERT INTO transactions (wallet_id, type, amount, status, payment_gateway, gateway_reference_id, description)
           VALUES ($1, 'deposit', $2, 'completed', 'payhero', $3, 'M-Pesa STK Push deposit')`,
          [wallet.rows[0].id, Amount, TransactionID]
        );
        await pool.query(
          "UPDATE payments SET status = 'completed', gateway_reference_id = $1 WHERE gateway_session_id LIKE $2 AND user_id = $3",
          [TransactionID, `%${ExternalID}%`, userId]
        );
      }
    }
    res.json({ result_code: ResultCode });
  } catch (err) {
    res.status(500).json({ error: 'Callback processing failed' });
  }
});

router.post('/paypal/deposit', authenticate, async (req, res) => {
  try {
    const { amount, currency = 'USD' } = req.body;
    if (!amount) return res.status(400).json({ error: 'Amount required' });

    const { default: paypal } = await import('@paypal/checkout-server-sdk');
    const environment = process.env.PAYPAL_MODE === 'live'
      ? new paypal.core.LiveEnvironment(process.env.PAYPAL_CLIENT_ID, process.env.PAYPAL_CLIENT_SECRET)
      : new paypal.core.SandboxEnvironment(process.env.PAYPAL_CLIENT_ID, process.env.PAYPAL_CLIENT_SECRET);
    const client = new paypal.core.PayPalHttpClient(environment);

    const request = new paypal.orders.OrdersCreateRequest();
    request.prefer('return=representation');
    request.requestBody({
      intent: 'CAPTURE',
      purchase_units: [{ amount: { currency_code: currency, value: amount.toString() } }],
    });

    const order = await client.execute(request);

    await pool.query(
      `INSERT INTO payments (user_id, gateway, gateway_reference_id, amount, currency, type, status)
       VALUES ($1, 'paypal', $2, $3, $4, 'deposit', 'pending')`,
      [req.user.id, order.result.id, amount, currency]
    );

    res.json({ orderId: order.result.id, status: order.result.status });
  } catch (err) {
    res.status(500).json({ error: 'PayPal deposit failed' });
  }
});

router.post('/paypal/capture', authenticate, async (req, res) => {
  try {
    const { orderId } = req.body;
    const { default: paypal } = await import('@paypal/checkout-server-sdk');
    const environment = process.env.PAYPAL_MODE === 'live'
      ? new paypal.core.LiveEnvironment(process.env.PAYPAL_CLIENT_ID, process.env.PAYPAL_CLIENT_SECRET)
      : new paypal.core.SandboxEnvironment(process.env.PAYPAL_CLIENT_ID, process.env.PAYPAL_CLIENT_SECRET);
    const client = new paypal.core.PayPalHttpClient(environment);

    const captureRequest = new paypal.orders.OrdersCaptureRequest(orderId);
    const capture = await client.execute(captureRequest);

    if (capture.result.status === 'COMPLETED') {
      const unit = capture.result.purchase_units[0];
      const amount = parseFloat(unit.payments.captures[0].amount.value);

      const wallet = await pool.query('SELECT * FROM wallets WHERE user_id = $1 FOR UPDATE', [req.user.id]);
      if (wallet.rows.length > 0) {
        await pool.query(
          'UPDATE wallets SET balance = balance + $1, total_deposited = total_deposited + $1, updated_at = NOW() WHERE id = $2',
          [amount, wallet.rows[0].id]
        );
        await pool.query(
          `INSERT INTO transactions (wallet_id, type, amount, status, payment_gateway, gateway_reference_id, description)
           VALUES ($1, 'deposit', $2, 'completed', 'paypal', $3, 'PayPal deposit')`,
          [wallet.rows[0].id, amount, orderId]
        );
        await pool.query(
          "UPDATE payments SET status = 'completed' WHERE gateway_reference_id = $1",
          [orderId]
        );
      }
    }
    res.json({ status: capture.result.status });
  } catch (err) {
    res.status(500).json({ error: 'PayPal capture failed' });
  }
});

module.exports = router;
