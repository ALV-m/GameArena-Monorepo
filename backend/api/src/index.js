require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');
const { pool } = require('./db/pool');

const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/users');
const walletRoutes = require('./routes/wallet');
const tournamentRoutes = require('./routes/tournaments');
const challengeRoutes = require('./routes/challenges');
const matchRoutes = require('./routes/matches');
const paymentRoutes = require('./routes/payments');
const adminRoutes = require('./routes/admin');
const screenshotRoutes = require('./routes/screenshots');

const app = express();
const PORT = process.env.PORT || 3001;

app.use(helmet());
const allowedOrigins = [
  process.env.ADMIN_URL,
  process.env.WEB_URL,
  'https://gamearena-admin.onrender.com',
  'https://gamearena-web.onrender.com',
  'http://localhost:3000',
  'http://localhost:3001',
  'http://localhost:5173',
].filter(Boolean);
app.use(cors({ origin: (origin, cb) => { if (!origin || allowedOrigins.includes(origin)) cb(null, true); else cb(new Error('Not allowed by CORS')); }, credentials: true }));
app.use(morgan('combined'));
app.use(express.json());

const limiter = rateLimit({ windowMs: 15 * 60 * 1000, max: 200 });
app.use('/api/', limiter);

app.get('/api/health', async (req, res) => {
  try {
    await pool.query('SELECT NOW()');
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
  } catch (err) {
    res.status(500).json({ status: 'error', message: 'Database connection failed' });
  }
});

app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);
app.use('/api/wallet', walletRoutes);
app.use('/api/tournaments', tournamentRoutes);
app.use('/api/challenges', challengeRoutes);
app.use('/api/matches', matchRoutes);
app.use('/api/payments', paymentRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/matches', screenshotRoutes);
app.use('/uploads', express.static(require('path').join(__dirname, '../uploads')));

app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(err.status || 500).json({
    error: err.message || 'Internal server error',
  });
});

app.listen(PORT, () => {
  console.log(`GameArena API running on port ${PORT}`);
});

module.exports = app;
