-- No-code control over the homepage's optional sections, plus one tasteful,
-- togglable ad/sponsor slot. Defaults keep today's page exactly as it is:
-- the category row and social section are new (default on), ads are off
-- until the author fills in an image and turns the switch on.
ALTER TABLE site_settings ADD COLUMN home_show_category_row BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE site_settings ADD COLUMN home_show_social_section BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE site_settings ADD COLUMN ads_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE site_settings ADD COLUMN ad_image_url VARCHAR(1000);
ALTER TABLE site_settings ADD COLUMN ad_link_url VARCHAR(1000);
ALTER TABLE site_settings ADD COLUMN ad_label VARCHAR(80);
