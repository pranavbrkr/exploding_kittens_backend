-- One-time migration: add email verification columns to existing users table.
-- Run this once if you get "column email_verified contains null values" or "email_verified does not exist".
-- Connect to your explodingkittens DB (e.g. psql -U kitten -d explodingkittens) and run this file.

-- Add new columns as nullable first
ALTER TABLE users ADD COLUMN IF NOT EXISTS email varchar(255) UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified boolean;
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_code varchar(6);
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_code_expires_at timestamp with time zone;

-- Backfill: existing rows (legacy users) are treated as verified
UPDATE users SET email_verified = true WHERE email_verified IS NULL;

-- Now enforce NOT NULL and set default for new rows
ALTER TABLE users ALTER COLUMN email_verified SET DEFAULT false;
ALTER TABLE users ALTER COLUMN email_verified SET NOT NULL;

-- Allow username to be null (new users set it at complete-registration)
ALTER TABLE users ALTER COLUMN username DROP NOT NULL;
