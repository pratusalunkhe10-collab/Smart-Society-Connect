-- Run this once in the smart_society_connect_db database before restarting the backend.
ALTER TABLE users ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';
-- Existing accounts remain usable. All registrations after this change start as PENDING.
