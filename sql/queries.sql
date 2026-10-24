-- CipherVault: example SQL for the DBMS part. Run after the app (or schema.sql) has created the database.
USE ciphervault;

-- CRUD (the app runs these through PreparedStatement; ? marks a parameter)
-- INSERT: CALL add_credential(1, 1, 'Instagram', 'poojitha_07', '<encoded>', 'Strong', @new_id);
-- UPDATE: UPDATE credentials SET password = ?, strength = ? WHERE cred_id = ? AND user_id = ?;
-- DELETE: DELETE FROM credentials WHERE cred_id = ? AND user_id = ?;
-- SELECT: SELECT * FROM credentials WHERE user_id = ? AND site_name LIKE CONCAT('%', ?, '%');

-- JOIN with categories
SELECT c.site_name, c.username, cat.name AS category, c.strength, c.updated_at
FROM credentials c JOIN categories cat ON c.category_id = cat.category_id
WHERE c.user_id = 1
ORDER BY c.site_name;

-- COUNT per category with GROUP BY
SELECT cat.name AS category, COUNT(*) AS entries
FROM credentials c JOIN categories cat ON c.category_id = cat.category_id
WHERE c.user_id = 1
GROUP BY cat.name;

-- Passwords used for more than one site by the same user (subquery)
SELECT site_name FROM credentials c
WHERE password IN (SELECT password FROM credentials
                   WHERE user_id = c.user_id AND cred_id <> c.cred_id);

-- The weak_passwords view
SELECT * FROM weak_passwords;

-- RANK() by strength: Weak first
SELECT site_name, strength,
       RANK() OVER (ORDER BY FIELD(strength, 'Weak', 'Medium', 'Strong')) AS strength_rank
FROM credentials
WHERE user_id = 1;

-- Edit entry as a transaction (the app does this with setAutoCommit(false), commit, rollback)
START TRANSACTION;
UPDATE credentials SET strength = 'Strong' WHERE cred_id = 1 AND user_id = 1;
COMMIT;          -- or ROLLBACK; to undo

-- The index on site_name is used by searches that start with the typed text
EXPLAIN SELECT * FROM credentials WHERE site_name LIKE 'Insta%';

-- Check the trigger: updated_at is today after an update
SELECT cred_id, updated_at FROM credentials WHERE cred_id = 1;
