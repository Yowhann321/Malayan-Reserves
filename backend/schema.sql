-- Malayan Reserves database (MySQL / MariaDB).
-- Import with: mysql -u root -p < schema.sql   (or the Import tab in phpMyAdmin)

CREATE DATABASE IF NOT EXISTS malayan_reserves CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE malayan_reserves;

CREATE TABLE IF NOT EXISTS users (
    id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL,
    email       VARCHAR(120) NOT NULL UNIQUE,
    type        ENUM('STUDENT', 'FACULTY', 'EMPLOYEE') NOT NULL DEFAULT 'STUDENT',
    v_type      VARCHAR(20)  NOT NULL,
    v_color     VARCHAR(20)  NOT NULL,
    plate       VARCHAR(12)  NOT NULL UNIQUE,
    pw_hash     VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Only a SHA-256 hash of each session token is stored.
CREATE TABLE IF NOT EXISTS sessions (
    token_hash  CHAR(64)     NOT NULL PRIMARY KEY,
    user_id     INT UNSIGNED NOT NULL,
    expires_at  DATETIME     NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS slots (
    id          VARCHAR(4)   NOT NULL PRIMARY KEY,
    row_no      TINYINT      NOT NULL,
    staff_only  TINYINT(1)   NOT NULL DEFAULT 0,
    note        VARCHAR(80)  NOT NULL DEFAULT ''
) ENGINE=InnoDB;

-- Times are Unix epoch milliseconds, the same values the app uses.
CREATE TABLE IF NOT EXISTS reservations (
    id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    slot_id     VARCHAR(4)   NOT NULL,
    user_id     INT UNSIGNED NOT NULL,
    start_ms    BIGINT       NOT NULL,
    end_ms      BIGINT       NOT NULL,
    status      ENUM('UPCOMING', 'HELD', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'NO_SHOW') NOT NULL,
    arrived_at  BIGINT       NULL,
    ended_at    BIGINT       NULL,
    extended    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (slot_id) REFERENCES slots(id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_slot_status (slot_id, status),
    INDEX idx_user_status (user_id, status)
) ENGINE=InnoDB;

INSERT IGNORE INTO slots (id, row_no, staff_only, note) VALUES
    ('P01', 0, 1, 'Faculty and staff priority, nearest the gate'),
    ('P02', 0, 1, 'Faculty and staff priority, nearest the gate'),
    ('P03', 0, 1, 'Faculty and staff priority, nearest the gate'),
    ('P04', 1, 0, 'Near the gate'),
    ('P05', 1, 0, 'Near the gate'),
    ('P06', 1, 0, 'Near the gate'),
    ('P07', 2, 0, 'Middle of the lot'),
    ('P08', 2, 0, 'Middle of the lot'),
    ('P09', 2, 0, 'Middle of the lot'),
    ('P10', 3, 0, 'Covered parking'),
    ('P11', 3, 0, 'Covered parking'),
    ('P12', 3, 0, 'Covered parking');
