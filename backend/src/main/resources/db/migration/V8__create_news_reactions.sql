CREATE TABLE IF NOT EXISTS reactions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  news_id BIGINT NOT NULL,
  reaction_type ENUM('like', 'dislike') NOT NULL,
  ip_hash VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_reactions_news FOREIGN KEY (news_id) REFERENCES news(id) ON DELETE CASCADE,
  CONSTRAINT uk_reactions_news_ip UNIQUE (news_id, ip_hash),
  INDEX idx_reactions_news_type (news_id, reaction_type)
);
