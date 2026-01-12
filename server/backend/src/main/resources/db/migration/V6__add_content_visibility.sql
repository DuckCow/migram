-- V6: Add visibility column to posts and comments for content moderation
-- ContentVisibility: PUBLIC, LIMITED, HIDDEN, REMOVED

-- Add visibility column to posts
ALTER TABLE post ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';

-- Add visibility column to comments
ALTER TABLE comment ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';

-- Add indexes for visibility filtering (frequently queried)
CREATE INDEX idx_post_visibility ON post(visibility);
CREATE INDEX idx_comment_visibility ON comment(visibility);

-- Composite indexes for common queries (feed, board posts)
CREATE INDEX idx_post_board_visibility ON post(board_id, visibility, created_at DESC);
