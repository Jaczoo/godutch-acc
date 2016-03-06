# --- !Ups
CREATE TABLE bookings (
  id SERIAL PRIMARY KEY,
  listing_id VARCHAR(100) NOT NULL,
  unit_id VARCHAR(100) NOT NULL,
  checkin VARCHAR(100) NOT NULL,
  checkout VARCHAR(100) NOT NULL,
  headline VARCHAR(255) NOT NULL,
  initial_price DECIMAL NOT NULL,
  sleeps INTEGER NOT NULL
);

CREATE TABLE user_bookings (
  id SERIAL PRIMARY KEY,
  booking_id INTEGER NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
  user_email VARCHAR(100) NOT NULL,
  committed BOOLEAN NOT NULL,
  seen BOOLEAN NOT NULL
);

# --- !Downs
DROP TABLE IF EXISTS user_bookings;
DROP TABLE IF EXISTS bookings;