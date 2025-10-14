CREATE TABLE IF NOT EXISTS likes (
    slug TEXT PRIMARY KEY,
    count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT current_timestamp
);

CREATE INDEX IF NOT EXISTS idx_likes_updated_at ON likes(updated_at DESC);
