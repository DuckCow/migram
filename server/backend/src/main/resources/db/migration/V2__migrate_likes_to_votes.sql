-- ================================================================
-- Migram Vote System Migration
-- Version 2: Migrate from Like system to Vote system
-- ================================================================
-- IMPORTANT: This is a manual migration script
-- Run this AFTER deploying the new Vote entity and ensuring Hibernate has created the tables
-- ================================================================

-- Step 1: Verify votes table exists (created by Hibernate)
-- If not, you need to start the application first to let Hibernate create it

-- Step 2: Migrate existing likes to votes
-- All existing likes are converted to UP votes
INSERT INTO vote (user_id, post_id, comment_id, vote_type, created_at)
SELECT
    l.user_id,
    l.post_id,
    l.comment_id,
    'UP',
    l.created_at
FROM likes l
WHERE NOT EXISTS (
    SELECT 1 FROM vote v
    WHERE v.user_id = l.user_id
    AND (
        (v.post_id = l.post_id AND l.post_id IS NOT NULL)
        OR (v.comment_id = l.comment_id AND l.comment_id IS NOT NULL)
    )
);

-- Step 4: Create indexes for performance
-- These indexes speed up vote count queries and user vote lookups
CREATE INDEX IF NOT EXISTS idx_vote_post_type ON vote(post_id, vote_type);
CREATE INDEX IF NOT EXISTS idx_vote_comment_type ON vote(comment_id, vote_type);
CREATE INDEX IF NOT EXISTS idx_vote_user_post ON vote(user_id, post_id);
CREATE INDEX IF NOT EXISTS idx_vote_user_comment ON vote(user_id, comment_id);
CREATE INDEX IF NOT EXISTS idx_vote_created_at ON vote(created_at);

-- Step 5: Verify migration results
-- Run these queries to check the migration was successful:
/*
SELECT
    'Total likes' as description,
    COUNT(*) as count
FROM likes
UNION ALL
SELECT
    'Post likes' as description,
    COUNT(*) as count
FROM likes WHERE post_id IS NOT NULL
UNION ALL
SELECT
    'Comment likes' as description,
    COUNT(*) as count
FROM likes WHERE comment_id IS NOT NULL
UNION ALL
SELECT
    'Total votes' as description,
    COUNT(*) as count
FROM vote
UNION ALL
SELECT
    'Post votes' as description,
    COUNT(*) as count
FROM vote WHERE post_id IS NOT NULL
UNION ALL
SELECT
    'Comment votes' as description,
    COUNT(*) as count
FROM vote WHERE comment_id IS NOT NULL;
*/

-- ================================================================
-- BACKUP INSTRUCTIONS:
-- ================================================================
-- Before running this migration:
-- 1. CREATE TABLE likes_backup AS SELECT * FROM likes;
--
-- After verifying the migration (wait 24-48 hours in production):
-- 2. Test the application thoroughly with the new vote system
-- 3. Verify vote counts match like counts from backup
-- 4. Once confirmed stable, you can drop the old table:
--    DROP TABLE likes;
--    DROP TABLE likes_backup;
-- ================================================================

-- ================================================================
-- NOTES:
-- ================================================================
-- 1. The nickname column is automatically added to users table by Hibernate
-- 2. All existing users will have nickname = NULL until first login
-- 3. The Vote entity unique constraints are handled by Hibernate/JPA
-- 4. Run this script manually using MySQL client or database tool
-- 5. Monitor application logs after migration for any errors
-- ================================================================
