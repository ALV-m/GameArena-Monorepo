const express = require('express');
const { pool } = require('../db/pool');
const path = require('path');
const fs = require('fs');

const router = express.Router();

router.get('/apk', async (req, res) => {
  try {
    const deviceId = req.query.device || req.ip;
    const appVersion = req.query.v || 'unknown';

    await pool.query(
      `INSERT INTO ga_app_analytics (event_type, device_id, app_version, platform, country, metadata)
       VALUES ('download', $1, $2, 'android', $3, $4)`,
      [deviceId, appVersion, req.headers['cf-ipcountry'] || null, JSON.stringify({ ip: req.ip, ua: req.headers['user-agent'] })]
    );
  } catch (err) {
    console.error('Download track failed:', err.message);
  }

  const apkPath = path.join(__dirname, '../../uploads/GameArena.apk');
  if (fs.existsSync(apkPath)) {
    res.download(apkPath, 'GameArena.apk');
  } else {
    res.status(404).json({ error: 'APK not available yet. Upload it to /uploads/GameArena.apk' });
  }
});

module.exports = router;
