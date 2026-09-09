-- What sort of piece it is: feature, report, literature, column, interview,
-- travel, photography. Stored as a slug so the label can be translated.
-- Existing rows fall back to 'feature', the most common kind.
ALTER TABLE press_clippings ADD COLUMN kind VARCHAR(32) NOT NULL DEFAULT 'feature';
CREATE INDEX idx_press_clippings_kind ON press_clippings (kind);
