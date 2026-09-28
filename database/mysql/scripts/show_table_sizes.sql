-- Description: Show approximate data and index sizes per table.
-- Date: 2026-09-28
-- Author: Sherry Cards Shop project team
-- Objective: Help inspect storage usage by table.
-- Comments: Read-only estimates from INFORMATION_SCHEMA; requires a selected database.

SELECT TABLE_NAME,
       TABLE_ROWS AS approximate_rows,
       ROUND(DATA_LENGTH / 1024 / 1024, 2) AS data_size_mb,
       ROUND(INDEX_LENGTH / 1024 / 1024, 2) AS index_size_mb
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY DATA_LENGTH + INDEX_LENGTH DESC, TABLE_NAME;