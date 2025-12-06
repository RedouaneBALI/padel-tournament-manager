-- Add users table for storing user profiles
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  name VARCHAR(255),
  locale VARCHAR(10),
  profile_type VARCHAR(20) DEFAULT 'SPECTATOR',
  city VARCHAR(255),
  country VARCHAR(255)
);
