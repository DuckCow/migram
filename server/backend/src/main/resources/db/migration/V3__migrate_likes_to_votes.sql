-- V3: Migrate existing likes to votes (idempotent)
-- Converts legacy 'likes' to 'votes' with vote_type='UP'
--
-- Behavior:
--   - Fresh DB: likes table is empty, no rows migrated (safe)
--   - Existing DB: migrates likes -> votes, skips duplicates
--
-- TODO (Manual verification for existing DB):
--   1. Before migration: SELECT COUNT(*) FROM likes;
--   2. After migration:  SELECT COUNT(*) FROM votes WHERE vote_type='UP';
--   3. Verify counts match (or check for expected duplicates)
--   4. Once confirmed, consider: DROP TABLE likes; (optional cleanup)

INSERT INTO votes (user_id, post_id, comment_id, vote_type, created_at)
SELECT l.user_id, l.post_id, l.comment_id, 'UP', l.created_at
FROM likes l
WHERE NOT EXISTS (
    SELECT 1 FROM votes v
    WHERE v.user_id = l.user_id
    AND (
        (v.post_id = l.post_id AND l.post_id IS NOT NULL)
        OR (v.comment_id = l.comment_id AND l.comment_id IS NOT NULL)
    )
);
