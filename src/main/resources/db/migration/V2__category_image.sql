-- Add optional image URL for categories (used in shop browse carousel)

ALTER TABLE categories
    ADD COLUMN image_url VARCHAR(512);
