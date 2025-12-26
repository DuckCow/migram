-- Add original_lang column to posts
ALTER TABLE posts ADD COLUMN original_lang VARCHAR(10);

-- Add original_lang column to comment (not comments)
ALTER TABLE comment ADD COLUMN original_lang VARCHAR(10);

-- Set default to 'en' for existing records
UPDATE posts SET original_lang = 'en' WHERE original_lang IS NULL;
UPDATE comment SET original_lang = 'en' WHERE original_lang IS NULL;

-- Make columns NOT NULL after setting defaults
ALTER TABLE posts ALTER COLUMN original_lang SET NOT NULL;
ALTER TABLE posts ALTER COLUMN original_lang SET DEFAULT 'en';

ALTER TABLE comment ALTER COLUMN original_lang SET NOT NULL;
ALTER TABLE comment ALTER COLUMN original_lang SET DEFAULT 'en';

-- Create content_translation table for caching translations
CREATE TABLE content_translation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_type VARCHAR(20) NOT NULL,
    content_id BIGINT NOT NULL,
    target_lang VARCHAR(10) NOT NULL,
    translated_text TEXT NOT NULL,
    model VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_translation UNIQUE (content_type, content_id, target_lang)
);

-- Create index for faster lookups
CREATE INDEX idx_content_translation_lookup ON content_translation(content_type, content_id, target_lang);
CREATE INDEX idx_content_translation_created ON content_translation(created_at);
