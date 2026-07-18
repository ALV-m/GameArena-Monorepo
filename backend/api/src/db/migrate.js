const { pool } = require('./pool');

const migrations = `
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE user_role AS ENUM ('user', 'admin', 'moderator');
CREATE TYPE tournament_status AS ENUM ('draft', 'registration', 'in_progress', 'completed', 'cancelled');
CREATE TYPE tournament_format AS ENUM ('single_elimination', 'double_elimination', 'round_robin', 'swiss', 'best_of_3', 'best_of_5');
CREATE TYPE challenge_status AS ENUM ('open', 'accepted', 'in_progress', 'completed', 'cancelled', 'disputed');
CREATE TYPE match_status AS ENUM ('pending', 'in_progress', 'completed', 'disputed', 'cancelled');
CREATE TYPE transaction_type AS ENUM ('deposit', 'withdrawal', 'tournament_entry', 'tournament_prize', 'challenge_entry', 'challenge_prize', 'refund', 'bonus', 'referral');
CREATE TYPE payment_status AS ENUM ('pending', 'completed', 'failed', 'cancelled');
CREATE TYPE payment_gateway AS ENUM ('stripe', 'payhero', 'paypal');
CREATE TYPE dispute_status AS ENUM ('open', 'reviewing', 'resolved', 'dismissed');
CREATE TYPE game_type AS ENUM ('efootball', 'ea_fc_mobile', 'pubg', 'cod_mobile', 'mobile_legends', 'free_fire_max', 'free_fire', 'clash_royale', 'chess', 'clash_of_clans');

CREATE TABLE users (
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

CREATE TABLE wallets (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  balance DECIMAL(12,2) DEFAULT 0.00 CHECK (balance >= 0),
  currency VARCHAR(3) DEFAULT 'KES',
  total_deposited DECIMAL(12,2) DEFAULT 0.00,
  total_withdrawn DECIMAL(12,2) DEFAULT 0.00,
  total_earned DECIMAL(12,2) DEFAULT 0.00,
  total_spent DECIMAL(12,2) DEFAULT 0.00,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE transactions (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  wallet_id UUID NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
  type transaction_type NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) DEFAULT 'KES',
  status payment_status DEFAULT 'pending',
  payment_gateway payment_gateway,
  gateway_reference_id VARCHAR(255),
  description TEXT,
  metadata JSONB DEFAULT '{}',
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE tournaments (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  creator_id UUID NOT NULL REFERENCES users(id),
  name VARCHAR(100) NOT NULL,
  game game_type NOT NULL,
  description TEXT,
  format tournament_format NOT NULL DEFAULT 'single_elimination',
  rules TEXT,
  entry_fee DECIMAL(12,2) DEFAULT 0.00 CHECK (entry_fee >= 0),
  prize_pool DECIMAL(12,2) DEFAULT 0.00,
  max_players INTEGER NOT NULL CHECK (max_players >= 2),
  current_players INTEGER DEFAULT 0,
  status tournament_status DEFAULT 'draft',
  winner_id UUID REFERENCES users(id),
  start_time TIMESTAMPTZ,
  registration_deadline TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE tournament_participants (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  seed_number INTEGER,
  eliminated BOOLEAN DEFAULT FALSE,
  eliminated_at TIMESTAMPTZ,
  final_position INTEGER,
  joined_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(tournament_id, user_id)
);

CREATE TABLE challenges (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  challenger_id UUID NOT NULL REFERENCES users(id),
  opponent_id UUID REFERENCES users(id),
  game game_type NOT NULL,
  stake_amount DECIMAL(12,2) NOT NULL CHECK (stake_amount > 0),
  currency VARCHAR(3) DEFAULT 'KES',
  status challenge_status DEFAULT 'open',
  winner_id UUID REFERENCES users(id),
  room_code VARCHAR(20),
  room_password VARCHAR(20),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  accepted_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ
);

CREATE TABLE matches (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tournament_id UUID REFERENCES tournaments(id) ON DELETE SET NULL,
  challenge_id UUID REFERENCES challenges(id) ON DELETE SET NULL,
  round_number INTEGER,
  bracket_position INTEGER,
  player1_id UUID NOT NULL REFERENCES users(id),
  player2_id UUID REFERENCES users(id),
  player1_score INTEGER DEFAULT 0,
  player2_score INTEGER DEFAULT 0,
  winner_id UUID REFERENCES users(id),
  room_code VARCHAR(20),
  room_password VARCHAR(20),
  status match_status DEFAULT 'pending',
  stake_amount DECIMAL(12,2) DEFAULT 0.00,
  scheduled_time TIMESTAMPTZ,
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  screenshot_url TEXT,
  screenshot_metadata JSONB DEFAULT '{}',
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE disputes (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  match_id UUID NOT NULL REFERENCES matches(id),
  reporter_id UUID NOT NULL REFERENCES users(id),
  reason TEXT NOT NULL,
  evidence_url TEXT,
  status dispute_status DEFAULT 'open',
  admin_id UUID REFERENCES users(id),
  resolution TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  resolved_at TIMESTAMPTZ
);

CREATE TABLE payments (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES users(id),
  gateway payment_gateway NOT NULL,
  gateway_reference_id VARCHAR(255),
  gateway_session_id VARCHAR(255),
  amount DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) DEFAULT 'KES',
  type VARCHAR(20) NOT NULL CHECK (type IN ('deposit', 'withdrawal')),
  status payment_status DEFAULT 'pending',
  phone_number VARCHAR(20),
  email VARCHAR(255),
  metadata JSONB DEFAULT '{}',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_wallets_user ON wallets(user_id);
CREATE INDEX idx_transactions_wallet ON transactions(wallet_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_tournaments_status ON tournaments(status);
CREATE INDEX idx_tournaments_creator ON tournaments(creator_id);
CREATE INDEX idx_tournament_participants_tournament ON tournament_participants(tournament_id);
CREATE INDEX idx_tournament_participants_user ON tournament_participants(user_id);
CREATE INDEX idx_challenges_status ON challenges(status);
CREATE INDEX idx_challenges_challenger ON challenges(challenger_id);
CREATE INDEX idx_challenges_opponent ON challenges(opponent_id);
CREATE INDEX idx_matches_tournament ON matches(tournament_id);
CREATE INDEX idx_matches_challenge ON matches(challenge_id);
CREATE INDEX idx_matches_status ON matches(status);
CREATE INDEX idx_disputes_status ON disputes(status);
CREATE INDEX idx_payments_user ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
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
