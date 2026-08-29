CREATE TABLE IF NOT EXISTS cv_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(200) NOT NULL,
    purpose TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    created_at DATETIME(6),
    handled_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_cv_requests_status_created (status, created_at)
);
