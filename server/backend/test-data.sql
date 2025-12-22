-- Test Data for Vote System Testing
-- Run this in H2 console: http://localhost:8080/h2-console
-- JDBC URL: jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE

-- Insert test users with nicknames
INSERT INTO users (id, name, email, profile_picture, nickname, origin_country, current_country) VALUES
(1, 'Test User 1', 'test1@example.com', null, '빠른고양이', 'JP', 'JP'),
(2, 'Test User 2', 'test2@example.com', null, '느린토끼', 'CA', 'CA'),
(3, 'Test User 3', 'test3@example.com', null, '행복한판다', 'KR', 'KR');

-- Insert ROLE_USER for each user
INSERT INTO user_roles (user_id, roles) VALUES
(1, 'USER'),
(2, 'USER'),
(3, 'USER');

-- Insert test posts with different countries and timestamps
INSERT INTO post (id, title, content, media_url, country, created_at, modified_at, user_id) VALUES
(1, 'Tokyo Life', 'Life in Tokyo is amazing!', null, 'JP', DATEADD('HOUR', -2, CURRENT_TIMESTAMP), DATEADD('HOUR', -2, CURRENT_TIMESTAMP), 1),
(2, 'Vancouver Views', 'Beautiful Vancouver scenery', null, 'CA', DATEADD('HOUR', -5, CURRENT_TIMESTAMP), DATEADD('HOUR', -5, CURRENT_TIMESTAMP), 2),
(3, 'Seoul Food', 'Korean BBQ experience', null, 'KR', DATEADD('HOUR', -1, CURRENT_TIMESTAMP), DATEADD('HOUR', -1, CURRENT_TIMESTAMP), 3),
(4, 'Mount Fuji Trip', 'Visited Mount Fuji yesterday', null, 'JP', DATEADD('HOUR', -10, CURRENT_TIMESTAMP), DATEADD('HOUR', -10, CURRENT_TIMESTAMP), 1),
(5, 'Ottawa Winter', 'Cold but beautiful winter in Canada', null, 'CA', DATEADD('DAY', -1, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP), 2);

-- Insert test comments
INSERT INTO comment (id, content, created_at, modified_at, post_id, user_id) VALUES
(1, 'Great post!', DATEADD('HOUR', -1, CURRENT_TIMESTAMP), DATEADD('HOUR', -1, CURRENT_TIMESTAMP), 1, 2),
(2, 'Thanks for sharing!', DATEADD('HOUR', -3, CURRENT_TIMESTAMP), DATEADD('HOUR', -3, CURRENT_TIMESTAMP), 1, 3),
(3, 'Amazing views!', DATEADD('HOUR', -4, CURRENT_TIMESTAMP), DATEADD('HOUR', -4, CURRENT_TIMESTAMP), 2, 1);

-- Insert test votes for posts
-- Post 1 (Tokyo Life): 5 upvotes, 1 downvote (net: +4, recent)
INSERT INTO vote (id, user_id, post_id, comment_id, vote_type, created_at) VALUES
(1, 2, 1, null, 'UP', DATEADD('HOUR', -1, CURRENT_TIMESTAMP)),
(2, 3, 1, null, 'UP', DATEADD('HOUR', -1, CURRENT_TIMESTAMP));

-- Post 2 (Vancouver): 3 upvotes, 0 downvotes (net: +3, older)
INSERT INTO vote (id, user_id, post_id, comment_id, vote_type, created_at) VALUES
(3, 1, 2, null, 'UP', DATEADD('HOUR', -4, CURRENT_TIMESTAMP)),
(4, 3, 2, null, 'UP', DATEADD('HOUR', -4, CURRENT_TIMESTAMP));

-- Post 3 (Seoul): 8 upvotes, 2 downvotes (net: +6, very recent) - Should rank HIGH with Hot algorithm
INSERT INTO vote (id, user_id, post_id, comment_id, vote_type, created_at) VALUES
(5, 1, 3, null, 'UP', DATEADD('MINUTE', -30, CURRENT_TIMESTAMP)),
(6, 2, 3, null, 'UP', DATEADD('MINUTE', -30, CURRENT_TIMESTAMP));

-- Post 4 (Mt Fuji): 2 upvotes, 0 downvotes (net: +2, very old)
INSERT INTO vote (id, user_id, post_id, comment_id, vote_type, created_at) VALUES
(7, 2, 4, null, 'UP', DATEADD('HOUR', -9, CURRENT_TIMESTAMP)),
(8, 3, 4, null, 'UP', DATEADD('HOUR', -9, CURRENT_TIMESTAMP));

-- Post 5 (Ottawa): 1 upvote, 0 downvotes (net: +1, old)
INSERT INTO vote (id, user_id, post_id, comment_id, vote_type, created_at) VALUES
(9, 1, 5, null, 'UP', DATEADD('DAY', -1, CURRENT_TIMESTAMP));

-- Insert votes for comments
INSERT INTO vote (id, user_id, post_id, comment_id, vote_type, created_at) VALUES
(10, 1, null, 1, 'UP', DATEADD('MINUTE', -30, CURRENT_TIMESTAMP)),
(11, 3, null, 1, 'UP', DATEADD('MINUTE', -30, CURRENT_TIMESTAMP)),
(12, 2, null, 2, 'UP', DATEADD('MINUTE', -30, CURRENT_TIMESTAMP));

-- Expected Hot Algorithm Ranking (approximately):
-- 1. Post 3 (Seoul Food): +6 votes, 1 hour old - HOT SCORE HIGH
-- 2. Post 1 (Tokyo Life): +4 votes, 2 hours old
-- 3. Post 2 (Vancouver): +3 votes, 5 hours old
-- 4. Post 4 (Mt Fuji): +2 votes, 10 hours old
-- 5. Post 5 (Ottawa): +1 vote, 24 hours old
