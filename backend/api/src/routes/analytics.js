const express = require('express');
const { pool } = require('../db/pool');
const { authenticate, adminOnly } = require('../middleware/auth');

const router = express.Router();

router.post('/track', async (req, res) => {
  try {
    const { event_type, device_id, app_version, platform, country, metadata } = req.body;
    if (!event_type || !device_id) {
      return res.status(400).json({ error: 'event_type and device_id required' });
    }
    await pool.query(
      `INSERT INTO ga_app_analytics (event_type, device_id, app_version, platform, country, metadata)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [event_type, device_id, app_version || null, platform || 'android', country || null, metadata || '{}']
    );
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: 'Failed to track event' });
  }
});

router.get('/stats', authenticate, adminOnly, async (req, res) => {
  try {
    const [totalDownloads, totalInstalls, activeToday, activeWeek, activeMonth, dailyEvents, uniqueDevices, byVersion] = await Promise.all([
      pool.query(`SELECT COUNT(DISTINCT device_id) as count FROM ga_app_analytics WHERE event_type = 'download'`),
      pool.query(`SELECT COUNT(DISTINCT device_id) as count FROM ga_app_analytics WHERE event_type = 'install'`),
      pool.query(`SELECT COUNT(DISTINCT device_id) as count FROM ga_app_analytics WHERE event_type = 'active' AND created_at > NOW() - INTERVAL '24 hours'`),
      pool.query(`SELECT COUNT(DISTINCT device_id) as count FROM ga_app_analytics WHERE event_type = 'active' AND created_at > NOW() - INTERVAL '7 days'`),
      pool.query(`SELECT COUNT(DISTINCT device_id) as count FROM ga_app_analytics WHERE event_type = 'active' AND created_at > NOW() - INTERVAL '30 days'`),
      pool.query(`SELECT DATE(created_at) as date, event_type, COUNT(*) as count FROM ga_app_analytics WHERE created_at > NOW() - INTERVAL '30 days' GROUP BY DATE(created_at), event_type ORDER BY date`),
      pool.query(`SELECT COUNT(DISTINCT device_id) as total, COUNT(DISTINCT device_id) FILTER (WHERE created_at > NOW() - INTERVAL '24 hours') as today FROM ga_app_analytics`),
      pool.query(`SELECT app_version, COUNT(DISTINCT device_id) as devices FROM ga_app_analytics WHERE app_version IS NOT NULL GROUP BY app_version ORDER BY devices DESC LIMIT 10`),
    ]);

    res.json({
      downloads: { total: parseInt(totalDownloads.rows[0].count) },
      installs: { total: parseInt(totalInstalls.rows[0].count) },
      active: {
        today: parseInt(activeToday.rows[0].count),
        thisWeek: parseInt(activeWeek.rows[0].count),
        thisMonth: parseInt(activeMonth.rows[0].count),
      },
      devices: {
        total: parseInt(uniqueDevices.rows[0].total),
        today: parseInt(uniqueDevices.rows[0].today),
      },
      daily: dailyEvents.rows,
      byVersion: byVersion.rows,
    });
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch analytics' });
  }
});

module.exports = router;
