CREATE DATABASE IF NOT EXISTS `stepwong_web`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `stepwong_web`;

CREATE TABLE IF NOT EXISTS `app_users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(64) NOT NULL,
    `password_hash` VARCHAR(120) NOT NULL,
    `nickname` VARCHAR(64) NOT NULL,
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `step_accounts` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `owner_id` BIGINT NOT NULL,
    `display_name` VARCHAR(64) NOT NULL,
    `account_no` VARCHAR(128) NOT NULL,
    `encrypted_password` VARCHAR(1024) NOT NULL,
    `encrypted_login_token` VARCHAR(4096) DEFAULT NULL,
    `encrypted_app_token` VARCHAR(4096) DEFAULT NULL,
    `zepp_user_id` VARCHAR(128) DEFAULT NULL,
    `zepp_device_id` VARCHAR(64) DEFAULT NULL,
    `login_token_updated_at` DATETIME(6) DEFAULT NULL,
    `app_token_updated_at` DATETIME(6) DEFAULT NULL,
    `min_step` INT NOT NULL DEFAULT 18000,
    `max_step` INT NOT NULL DEFAULT 25000,
    `auto_enabled` TINYINT(1) NOT NULL DEFAULT 0,
    `run_hour` INT NOT NULL DEFAULT 8,
    `run_minute` INT NOT NULL DEFAULT 35,
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `last_run_date` DATE DEFAULT NULL,
    `last_run_at` DATETIME(6) DEFAULT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_owner_account_no` (`owner_id`, `account_no`),
    KEY `idx_step_accounts_owner` (`owner_id`),
    KEY `idx_step_accounts_schedule` (`auto_enabled`, `run_hour`, `run_minute`),
    CONSTRAINT `fk_step_accounts_owner` FOREIGN KEY (`owner_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `execution_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `owner_id` BIGINT NOT NULL,
    `account_id` BIGINT DEFAULT NULL,
    `account_no_snapshot` VARCHAR(128) NOT NULL,
    `trigger_type` VARCHAR(32) NOT NULL,
    `success` TINYINT(1) NOT NULL,
    `step_count` INT DEFAULT NULL,
    `message` VARCHAR(2048) NOT NULL,
    `started_at` DATETIME(6) NOT NULL,
    `finished_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_execution_logs_owner` (`owner_id`),
    KEY `idx_execution_logs_account` (`account_id`),
    KEY `idx_execution_logs_started_at` (`started_at`),
    CONSTRAINT `fk_execution_logs_owner` FOREIGN KEY (`owner_id`) REFERENCES `app_users` (`id`),
    CONSTRAINT `fk_execution_logs_account` FOREIGN KEY (`account_id`) REFERENCES `step_accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
