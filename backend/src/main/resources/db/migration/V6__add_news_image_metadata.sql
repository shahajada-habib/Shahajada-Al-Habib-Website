ALTER TABLE news
    ADD COLUMN image_caption TEXT NULL,
    ADD COLUMN image_source VARCHAR(255) NULL,
    ADD COLUMN image_alt VARCHAR(255) NULL;
