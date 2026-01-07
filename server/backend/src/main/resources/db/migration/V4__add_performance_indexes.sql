-- V4: Add performance optimization indexes
-- Purpose: Improve query performance for vote counts and feed sorting
-- Note: post/comment tables created by Hibernate, not Flyway
-- These indexes should be added via @Index annotations in entities or manually

-- Indexes on votes already created in V2
-- Indexes on post/comment can be added via JPA @Index annotations:
--   Post.kt: @Index(name = "idx_post_created_at", columnList = "created_at")
--   Comment.kt: @Index(name = "idx_comment_created_at", columnList = "created_at")

SELECT 1;
