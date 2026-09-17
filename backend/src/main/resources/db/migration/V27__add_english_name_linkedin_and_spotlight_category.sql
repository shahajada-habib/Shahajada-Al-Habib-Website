-- English display name (the Bangla name was the only one and stuck around even
-- when the reader switched the site to English), a LinkedIn link alongside the
-- existing socials, and a no-code "spotlight one category on the homepage"
-- slot (e.g. কবিতা, once there are enough poems to deserve their own section).
ALTER TABLE site_settings ADD COLUMN author_name_en VARCHAR(160);
ALTER TABLE site_settings ADD COLUMN linkedin_url VARCHAR(500);
ALTER TABLE site_settings ADD COLUMN home_spotlight_category_slug VARCHAR(120);

UPDATE site_settings SET author_name_en = 'Shahajada Al Habib' WHERE id = 1;
