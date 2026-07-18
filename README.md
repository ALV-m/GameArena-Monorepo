# GameArena Monorepo

The ultimate mobile gaming platform. Boost performance, compete in tournaments, win real money.

## Structure

```
GameArena-Monorepo/
├── app/          # Android app (Kotlin / Jetpack Compose)
├── backend/      # REST API (Express.js) + Admin Dashboard (Next.js)
├── web/          # Marketing landing page (React / Vite / Tailwind)
└── FrameX-Base/  # FPS overlay engine (embedded in app/)
```

## Projects

### app/ — Android Client

- **Stack:** Kotlin, Jetpack Compose, Hilt, Retrofit, ML Kit OCR
- **Features:** Game booster overlay, real-time FPS/CPU/GPU monitoring, tournaments, wallet, chat, audio/screen tools
- **Min SDK:** 26 (Android 8.0) | **Target SDK:** 34

### backend/ — API + Admin

- **api/** — Express.js REST API with PostgreSQL
  - Auth (JWT + bcrypt), tournaments, challenges, matches, wallet, payments (Stripe / PayHero M-Pesa / PayPal), screenshot uploads
- **dashboard/** — Next.js admin panel for managing tournaments, users, and payments
- **Deploy:** Render.com (see `render.yaml`)

### web/ — Landing Page

- **Stack:** React 18, Vite 5, Tailwind CSS 3, Framer Motion
- **Features:** Hero, features showcase, leaderboard, download CTA, responsive design

## Getting Started

### Android App

```bash
cd app
./gradlew assembleDebug
```

### Backend API

```bash
cd backend/api
cp .env.example .env   # fill in your credentials
npm install
npm run migrate
npm run dev
```

### Admin Dashboard

```bash
cd backend/dashboard
npm install
npm run dev            # runs on port 3000
```

### Web Frontend

```bash
cd web
npm install
npm run dev            # runs on port 3000
```

## Environment Variables

See `backend/api/.env.example` for required API keys:

| Variable | Description |
|---|---|
| `DATABASE_URL` | PostgreSQL connection string |
| `JWT_SECRET` | Secret for JWT signing |
| `STRIPE_SECRET_KEY` | Stripe payment processing |
| `PAYHERO_API_KEY` | PayHero (M-Pesa) integration |
| `PAYPAL_CLIENT_ID` | PayPal integration |

## License

Private — All rights reserved.
