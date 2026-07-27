DELIMITER //

CREATE PROCEDURE add_column_if_missing(
    IN target_table VARCHAR(64),
    IN target_column VARCHAR(64),
    IN column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND column_name = target_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', target_table, '` ADD COLUMN `', target_column, '` ', column_definition);
        PREPARE statement FROM @sql;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END//

DELIMITER ;

CALL add_column_if_missing('news', 'created_by', 'VARCHAR(255) NULL');
CALL add_column_if_missing('news', 'updated_by', 'VARCHAR(255) NULL');
CALL add_column_if_missing('news', 'published_by', 'VARCHAR(255) NULL');

DROP PROCEDURE add_column_if_missing;
