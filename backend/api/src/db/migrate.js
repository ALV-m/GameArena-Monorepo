const { pool } = require('./pool');

const migrations = `
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE ga_user_role AS ENUM ('user', 'admin', 'moderator');
CREATE TYPE ga_tournament_status AS ENUM ('draft', 'registration', 'in_progress', 'completed', 'cancelled');
CREATE TYPE ga_tournament_format AS ENUM ('single_elimination', 'double_elimination', 'round_robin', 'swiss', 'best_of_3', 'best_of_5');
CREATE TYPE ga_challenge_status AS ENUM ('open', 'accepted', 'in_progress', 'completed', 'cancelled', 'disputed');
CREATE TYPE ga_match_status AS ENUM ('pending', 'in_progress', 'completed', 'disputed', 'cancelled');
CREATE TYPE ga_transaction_type AS ENUM ('deposit', 'withdrawal', 'tournament_entry', 'tournament_prize', 'challenge_entry', 'challenge_prize', 'refund', 'bonus', 'referral');
CREATE TYPE ga_payment_status AS ENUM ('pending', 'completed', 'failed', 'cancelled');
CREATE TYPE ga_payment_gateway AS ENUM ('stripe', 'payhero', 'paypal');
CREATE TYPE ga_dispute_status AS ENUM ('open', 'reviewing', 'resolved', 'dismissed');
CREATE TYPE ga_game_type AS ENUM ('efootball', 'ea_fc_mobile', 'pubg', 'cod_mobile', 'mobile_legends', 'free_fire_max', 'free_fire', 'clash_royale', 'chess', 'clash_of_clans');

CREATE TABLE ga_users (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  username VARCHAR(30) UNIQUE NOT NULL,
  email VARCHAR(255) UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  avatar_url TEXT,
  country VARCHAR(2),
  rank_tier VARCHAR(20) DEFAULT 'bronze',
  elo_rating INTEGER DEFAULT 1000,
  is_admin BOOLEAN DEFAULT FALSE,
  is_moderator BOOLEAN DEFAULT FALSE,
  is_banned BOOLEAN DEFAULT FALSE,
  ban_reason TEXT,
  total_wins INTEGER DEFAULT 0,
  total_losses INTEGER DEFAULT 0,
  total_matches INTEGER DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE ga_wallets (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID UNIQUE NOT NULL REFERENCES ga_users(id) ON DELETE CASCADE,
  balance DECIMAL(12,2) DEFAULT 0.00 CHECK (balance >= 0),
  currency VARCHAR(3) DEFAULT 'KES',
  total_deposited DECIMAL(12,2) DEFAULT 0.00,
  total_withdrawn DECIMAL(12,2) DEFAULT 0.00,
  total_earned DECIMAL(12,2) DEFAULT 0.00,
  total_spent DECIMAL(12,2) DEFAULT 0.00,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE ga_transactions (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  wallet_id UUID NOT NULL REFERENCES ga_wallets(id) ON DELETE CASCADE,
  type ga_transaction_type NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) DEFAULT 'KES',
  status ga_payment_status DEFAULT 'pending',
  payment_gateway ga_payment_gateway,
  gateway_reference_id VARCHAR(255),
  description TEXT,
  metadata JSONB DEFAULT '{}',
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE ga_tournaments (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  creator_id UUID NOT NULL REFERENCES ga_users(id),
  name VARCHAR(100) NOT NULL,
  game ga_game_type NOT NULL,
  description TEXT,
  format ga_tournament_format NOT NULL DEFAULT 'single_elimination',
  rules TEXT,
  entry_fee DECIMAL(12,2) DEFAULT 0.00 CHECK (entry_fee >= 0),
  prize_pool DECIMAL(12,2) DEFAULT 0.00,
  max_players INTEGER NOT NULL CHECK (max_players >= 2),
  current_players INTEGER DEFAULT 0,
  status ga_tournament_status DEFAULT 'draft',
  winner_id UUID REFERENCES ga_users(id),
  start_time TIMESTAMPTZ,
  registration_deadline TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE ga_tournament_participants (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tournament_id UUID NOT NULL REFERENCES ga_tournaments(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES ga_users(id) ON DELETE CASCADE,
  seed_number INTEGER,
  eliminated BOOLEAN DEFAULT FALSE,
  eliminated_at TIMESTAMPTZ,
  final_position INTEGER,
  joined_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(tournament_id, user_id)
);

CREATE TABLE ga_challenges (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  challenger_id UUID NOT NULL REFERENCES ga_users(id),
  opponent_id UUID REFERENCES ga_users(id),
  game ga_game_type NOT NULL,
  stake_amount DECIMAL(12,2) NOT NULL CHECK (stake_amount > 0),
  currency VARCHAR(3) DEFAULT 'KES',
  status ga_challenge_status DEFAULT 'open',
  winner_id UUID REFERENCES ga_users(id),
  room_code VARCHAR(20),
  room_password VARCHAR(20),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  accepted_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ
);

CREATE TABLE ga_matches (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tournament_id UUID REFERENCES ga_tournaments(id) ON DELETE SET NULL,
  challenge_id UUID REFERENCES ga_challenges(id) ON DELETE SET NULL,
  round_number INTEGER,
  bracket_position INTEGER,
  player1_id UUID NOT NULL REFERENCES ga_users(id),
  player2_id UUID REFERENCES ga_users(id),
  player1_score INTEGER DEFAULT 0,
  player2_score INTEGER DEFAULT 0,
  winner_id UUID REFERENCES ga_users(id),
  room_code VARCHAR(20),
  room_password VARCHAR(20),
  status ga_match_status DEFAULT 'pending',
  stake_amount DECIMAL(12,2) DEFAULT 0.00,
  scheduled_time TIMESTAMPTZ,
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  screenshot_url TEXT,
  screenshot_metadata JSONB DEFAULT '{}',
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE ga_disputes (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  match_id UUID NOT NULL REFERENCES ga_matches(id),
  reporter_id UUID NOT NULL REFERENCES ga_users(id),
  reason TEXT NOT NULL,
  evidence_url TEXT,
  status ga_dispute_status DEFAULT 'open',
  admin_id UUID REFERENCES ga_users(id),
  resolution TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  resolved_at TIMESTAMPTZ
);

CREATE TABLE ga_payments (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES ga_users(id),
  gateway ga_payment_gateway NOT NULL,
  gateway_reference_id VARCHAR(255),
  gateway_session_id VARCHAR(255),
  amount DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) DEFAULT 'KES',
  type VARCHAR(20) NOT NULL CHECK (type IN ('deposit', 'withdrawal')),
  status ga_payment_status DEFAULT 'pending',
  phone_number VARCHAR(20),
  email VARCHAR(255),
  metadata JSONB DEFAULT '{}',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_ga_wallets_user ON ga_wallets(user_id);
CREATE INDEX idx_ga_transactions_wallet ON ga_transactions(wallet_id);
CREATE INDEX idx_ga_transactions_status ON ga_transactions(status);
CREATE INDEX idx_ga_tournaments_status ON ga_tournaments(status);
CREATE INDEX idx_ga_tournaments_creator ON ga_tournaments(creator_id);
CREATE INDEX idx_ga_tournament_participants_tournament ON ga_tournament_participants(tournament_id);
CREATE INDEX idx_ga_tournament_participants_user ON ga_tournament_participants(user_id);
CREATE INDEX idx_ga_challenges_status ON ga_challenges(status);
CREATE INDEX idx_ga_challenges_challenger ON ga_challenges(challenger_id);
CREATE INDEX idx_ga_challenges_opponent ON ga_challenges(opponent_id);
CREATE INDEX idx_ga_matches_tournament ON ga_matches(tournament_id);
CREATE INDEX idx_ga_matches_challenge ON ga_matches(challenge_id);
CREATE INDEX idx_ga_matches_status ON ga_matches(status);
CREATE INDEX idx_ga_disputes_status ON ga_disputes(status);
CREATE INDEX idx_ga_payments_user ON ga_payments(user_id);
CREATE INDEX idx_ga_payments_status ON ga_payments(status);
`;

async function migrate() {
  try {
    await pool.query(migrations);
    console.log('Database migration completed successfully');
    process.exit(0);
  } catch (err) {
    console.error('Migration failed:', err);
    process.exit(1);
  }
}

migrate();
