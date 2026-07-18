require('dotenv').config();
const bcrypt = require('bcryptjs');
const { pool } = require('./pool');

async function seed() {
  try {
    const passwordHash = await bcrypt.hash('admin123', 12);
    await pool.query(
      `INSERT INTO users (username, email, password_hash, is_admin)
       VALUES ('admin', 'admin@gamearena.com', $1, TRUE)
       ON CONFLICT (username) DO NOTHING`,
      [passwordHash]
    );

    const admin = await pool.query("SELECT id FROM users WHERE username = 'admin'");
    if (admin.rows.length > 0) {
      await pool.query(
        'INSERT INTO wallets (user_id) VALUES ($1) ON CONFLICT (user_id) DO NOTHING',
        [admin.rows[0].id]
      );
    }

    console.log('Seed completed: admin user created (username: admin, password: admin123)');
    process.exit(0);
  } catch (err) {
    console.error('Seed failed:', err);
    process.exit(1);
  }
}

seed();
