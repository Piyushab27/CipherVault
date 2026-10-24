-- CipherVault schema. Runs on first start (DBConnection.initialise) and can also be run by hand:
--   mysql -u root -p < src/main/resources/schema.sql
-- Safe to run again: every statement is idempotent.

CREATE DATABASE IF NOT EXISTS ciphervault;
USE ciphervault;

CREATE TABLE IF NOT EXISTS users (
  user_id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) UNIQUE NOT NULL,
  master_hash VARCHAR(64) NOT NULL,
  failed_attempts INT DEFAULT 0,
  security_question VARCHAR(150),
  security_answer_hash VARCHAR(64),
  reset_attempts INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS categories (
  category_id INT PRIMARY KEY,
  name VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS credentials (
  cred_id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  category_id INT,
  site_name VARCHAR(100) NOT NULL,
  username VARCHAR(100) NOT NULL,
  password VARCHAR(255) NOT NULL,
  strength VARCHAR(10),
  updated_at DATE,
  FOREIGN KEY (user_id) REFERENCES users(user_id),
  FOREIGN KEY (category_id) REFERENCES categories(category_id),
  INDEX idx_site_name (site_name)
);

INSERT IGNORE INTO categories (category_id, name) VALUES
  (1, 'Social'), (2, 'College'), (3, 'Banking'), (4, 'Shopping'),
  (5, 'Entertainment'), (6, 'Work'), (7, 'Other'), (8, 'Email');

CREATE OR REPLACE VIEW weak_passwords AS
  SELECT cred_id, user_id, site_name, username, updated_at
  FROM credentials
  WHERE strength = 'Weak';

-- updated_at is set by the database on every insert and update
DROP TRIGGER IF EXISTS trg_credentials_insert;
CREATE TRIGGER trg_credentials_insert BEFORE INSERT ON credentials
  FOR EACH ROW SET NEW.updated_at = CURDATE();

DROP TRIGGER IF EXISTS trg_credentials_update;
CREATE TRIGGER trg_credentials_update BEFORE UPDATE ON credentials
  FOR EACH ROW SET NEW.updated_at = CURDATE();

DROP PROCEDURE IF EXISTS add_credential;
DELIMITER $$
CREATE PROCEDURE add_credential(
  IN p_user INT, IN p_category INT, IN p_site VARCHAR(100), IN p_username VARCHAR(100),
  IN p_password VARCHAR(255), IN p_strength VARCHAR(10), OUT p_id INT)
BEGIN
  INSERT INTO credentials (user_id, category_id, site_name, username, password, strength)
  VALUES (p_user, p_category, p_site, p_username, p_password, p_strength);
  SET p_id = LAST_INSERT_ID();
END$$
DELIMITER ;
