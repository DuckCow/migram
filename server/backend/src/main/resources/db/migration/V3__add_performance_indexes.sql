-- V3: Add performance optimization indexes
-- Created: 2025-12-23
-- Purpose: Improve query performance for vote counts and feed sorting

-- Add index on post.created_at for feed sorting by time
CREATE INDEX IF NOT EXISTS idx_post_created_at ON post(created_at);

-- Add index on comment.created_at for comment sorting
CREATE INDEX IF NOT EXISTS idx_comment_created_at ON comment(created_at);

-- Note: Indexes on votes(post_id, vote_type) and votes(comment_id, vote_type)
-- are created automatically by JPA @Index annotations in Vote.kt
