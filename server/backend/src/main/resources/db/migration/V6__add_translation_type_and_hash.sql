-- V6: Add translation type (FULL/PREVIEW) and source text hash for cache invalidation
-- This supports separate caching for feed preview vs full post body translation

-- Add new columns
ALTER TABLE content_translation ADD COLUMN IF NOT EXISTS translation_type VARCHAR(20) DEFAULT 'FULL';
ALTER TABLE content_translation ADD COLUMN IF NOT EXISTS source_text_hash VARCHAR(64);

-- Set default values for existing rows
UPDATE content_translation SET translation_type = 'FULL' WHERE translation_type IS NULL;
UPDATE content_translation SET source_text_hash = 'legacy' WHERE source_text_hash IS NULL;

-- Make columns NOT NULL
ALTER TABLE content_translation ALTER COLUMN translation_type SET NOT NULL;
ALTER TABLE content_translation ALTER COLUMN source_text_hash SET NOT NULL;

-- Drop old unique constraint and add new one including translation_type
ALTER TABLE content_translation DROP CONSTRAINT IF EXISTS unique_translation;
ALTER TABLE content_translation ADD CONSTRAINT unique_translation
    UNIQUE (content_type, content_id, target_lang, translation_type);

-- Update index for lookups
DROP INDEX IF EXISTS idx_content_translation_lookup;
CREATE INDEX IF NOT EXISTS idx_content_translation_lookup
    ON content_translation(content_type, content_id, target_lang, translation_type);
