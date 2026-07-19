const express = require('express');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const { pool } = require('../db/pool');
const { authenticate } = require('../middleware/auth');

const router = express.Router();

const uploadDir = path.join(__dirname, '../../uploads');
if (!fs.existsSync(uploadDir)) fs.mkdirSync(uploadDir, { recursive: true });

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadDir),
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname);
    cb(null, `screenshot-${req.user.id}-${Date.now()}${ext}`);
  },
});

const upload = multer({
  storage,
  limits: { fileSize: 10 * 1024 * 1024 },
  fileFilter: (req, file, cb) => {
    const allowed = /jpeg|jpg|png|webp/;
    const ext = allowed.test(path.extname(file.originalname).toLowerCase());
    const mime = allowed.test(file.mimetype);
    cb(null, ext && mime);
  },
});

router.post('/:id/screenshot', authenticate, upload.single('screenshot'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ error: 'No valid image uploaded' });

    const match = await pool.query('SELECT * FROM ga_matches WHERE id = $1', [req.params.id]);
    if (match.rows.length === 0) {
      fs.unlinkSync(req.file.path);
      return res.status(404).json({ error: 'Match not found' });
    }

    const m = match.rows[0];
    if (m.player1_id !== req.user.id && m.player2_id !== req.user.id) {
      fs.unlinkSync(req.file.path);
      return res.status(403).json({ error: 'Not a match participant' });
    }

    const screenshotUrl = `/uploads/${req.file.filename}`;
    const metadata = JSON.stringify({
      originalName: req.file.originalname,
      size: req.file.size,
      uploadedBy: req.user.id,
      uploadedAt: new Date().toISOString(),
    });

    await pool.query(
      `UPDATE ga_matches SET screenshot_url = $1, screenshot_metadata = $2, updated_at = NOW() WHERE id = $3`,
      [screenshotUrl, metadata, req.params.id]
    );

    const ocrResult = await performOCR(req.file.path);

    res.json({
      message: 'Screenshot uploaded successfully',
      screenshot_url: screenshotUrl,
      ocr_result: ocrResult,
    });
  } catch (err) {
    if (req.file) fs.unlinkSync(req.file.path);
    res.status(500).json({ error: 'Failed to upload screenshot' });
  }
});

router.get('/:id/screenshots', authenticate, async (req, res) => {
  try {
    const match = await pool.query(
      'SELECT screenshot_url, screenshot_metadata FROM ga_matches WHERE id = $1',
      [req.params.id]
    );
    if (match.rows.length === 0) return res.status(404).json({ error: 'Match not found' });

    const m = match.rows[0];
    res.json({
      screenshot_url: m.screenshot_url || null,
      metadata: m.screenshot_metadata ? JSON.parse(m.screenshot_metadata) : null,
    });
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch screenshots' });
  }
});

async function performOCR(imagePath) {
  try {
    return {
      scores: null,
      detected_text: null,
      confidence: 0,
      note: 'OCR processing available with Tesseract.js integration',
    };
  } catch (err) {
    return { scores: null, error: 'OCR failed' };
  }
}

module.exports = router;
