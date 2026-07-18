const { pool } = require('./pool');

async function migrate() {
  try {
    await pool.query(`
      ALTER TABLE matches ADD COLUMN IF NOT EXISTS screenshot_url TEXT;
      ALTER TABLE matches ADD COLUMN IF NOT EXISTS screenshot_metadata JSONB DEFAULT '{}';
    `);
    console.log('Screenshot columns added successfully');
    process.exit(0);
  } catch (err) {
    if (err.code === '42701') {
      console.log('Columns already exist, skipping...');
      process.exit(0);
    }
    console.error('Migration failed:', err);
    process.exit(1);
  }
}

migrate();
