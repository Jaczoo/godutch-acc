# --- !Ups
CREATE TABLE offers (
  id SERIAL PRIMARY KEY,
  listing_id VARCHAR(100) NOT NULL,
  unit_id VARCHAR(100) NOT NULL,
  checkin VARCHAR(100) NOT NULL,
  checkout VARCHAR(100) NOT NULL
);

CREATE TABLE user_offers (
  id SERIAL PRIMARY KEY,
  offer_id INTEGER NOT NULL REFERENCES offers (id) ON DELETE CASCADE,
  user_email VARCHAR(100) NOT NULL,
  committed BOOLEAN NOT NULL,
  seen BOOLEAN NOT NULL
);

# --- !Downs
DROP TABLE IF EXISTS user_offers;
DROP TABLE IF EXISTS offers;