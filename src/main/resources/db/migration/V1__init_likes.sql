CREATE TABLE IF NOT EXISTS likes (
    slug TEXT PRIMARY KEY,
    like_count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_likes_updated_at ON likes(updated_at DESC);
