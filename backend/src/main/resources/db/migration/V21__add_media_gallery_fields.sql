-- Lets the admin mark uploaded photos to appear on the public /gallery page, with an optional caption.
ALTER TABLE media_assets ADD COLUMN caption VARCHAR(255);
ALTER TABLE media_assets ADD COLUMN show_in_gallery BOOLEAN NOT NULL DEFAULT FALSE;
