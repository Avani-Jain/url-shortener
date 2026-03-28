CREATE DATABASE IF NOT EXISTS url_shortener;
USE url_shortener;

CREATE TABLE IF NOT EXISTS shortened_urls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL UNIQUE,
    original_url LONGTEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expires_at DATETIME,
    click_count BIGINT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    creator_ip VARCHAR(45),
    
    INDEX idx_short_code (short_code),
    INDEX idx_original_url (original_url(255)),
    INDEX idx_created_at (created_at),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS url_click_analytics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL,
    click_timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_agent VARCHAR(255),
    referrer VARCHAR(255),
    client_ip VARCHAR(45),
    
    FOREIGN KEY (short_code) REFERENCES shortened_urls(short_code) ON DELETE CASCADE,
    INDEX idx_short_code (short_code),
    INDEX idx_click_timestamp (click_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    short_code VARCHAR(10),
    original_url LONGTEXT,
    client_ip VARCHAR(45),
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_action (action),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON url_shortener.* TO 'shortener'@'%';
FLUSH PRIVILEGES;