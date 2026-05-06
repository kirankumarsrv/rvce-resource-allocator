-- ============================================================
-- V10__fix_seeded_user_passwords.sql
-- Fix seeded user password hashes for known test accounts.
-- This migration corrects the stored bcrypt hash for the seed users
-- so the documented password `Test@1234` works for login.
-- ============================================================

UPDATE users
SET password_hash = '$2b$12$0dsNhVCAEAcS3nMYQnoc0.WvWNfo27XJWooMCvyL.B2bquUuJOkpO'
WHERE email IN (
    'admin@rvce.edu.in',
    'tto@rvce.edu.in',
    'priya.sharma@rvce.edu.in',
    'ramesh.kumar@rvce.edu.in',
    'kiran@rvce.edu.in'
);
