-- A locked account (3 wrong tries) stays locked until failed_attempts is reset in the database.
-- Replace the username, then run in the mysql client.
USE ciphervault;
-- failed_attempts: 3 wrong master passwords; reset_attempts: 3 wrong security answers in Forgot password.
UPDATE users SET failed_attempts = 0, reset_attempts = 0 WHERE username = 'poojitha';
