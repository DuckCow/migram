-- V8: Add Full-Text Search support for PostgreSQL
-- This migration adds tsvector columns and GIN indexes for fast text search
-- Note: H2 does not support PostgreSQL FTS features (ts_vector, to_tsvector, GIN)
--       This is expected - dev profile will use DatabaseSearchService (LIKE queries)

-- Add tsvector columns for full-text search
-- Separate columns for title and content allow different weighting
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'post' AND column_name = 'title_search') THEN
        ALTER TABLE post ADD COLUMN title_search tsvector;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'post' AND column_name = 'content_search') THEN
        ALTER TABLE post ADD COLUMN content_search tsvector;
    END IF;
END $$;

-- Create function to update search vectors
-- Uses 'simple' configuration for language-agnostic search
-- This supports all languages without language-specific stemming
CREATE OR REPLACE FUNCTION update_post_search_vectors()
RETURNS TRIGGER AS $$
BEGIN
    -- Convert title and content to tsvector using 'simple' configuration
    -- COALESCE handles NULL values gracefully
    NEW.title_search := to_tsvector('simple', COALESCE(NEW.title, ''));
    NEW.content_search := to_tsvector('simple', COALESCE(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update tsvector columns
-- Fires before INSERT or UPDATE of title/content columns
DROP TRIGGER IF EXISTS post_search_vector_update ON post;
CREATE TRIGGER post_search_vector_update
    BEFORE INSERT OR UPDATE OF title, content
    ON post
    FOR EACH ROW
    EXECUTE FUNCTION update_post_search_vectors();

-- Backfill existing posts with search vectors
-- This ensures all existing data is searchable
UPDATE post
SET title_search = to_tsvector('simple', COALESCE(title, '')),
    content_search = to_tsvector('simple', COALESCE(content, ''))
WHERE title_search IS NULL OR content_search IS NULL;

-- Create GIN indexes for fast full-text search
-- GIN (Generalized Inverted Index) is optimal for tsvector columns
CREATE INDEX IF NOT EXISTS idx_post_title_search ON post USING GIN(title_search);
CREATE INDEX IF NOT EXISTS idx_post_content_search ON post USING GIN(content_search);

-- Create combined index for searches across both title and content
-- Uses weighted combination: title (A weight = 1.0) + content (B weight = 0.4)
-- This allows title matches to rank higher than content matches
CREATE INDEX IF NOT EXISTS idx_post_combined_search ON post USING GIN(
    (setweight(title_search, 'A') || setweight(content_search, 'B'))
);
