-- Description: Show approximate total data and index size for the selected database.
-- Date: 2026-09-28
-- Author: Sherry Cards Shop project team
-- Objective: Summarize database storage usage.
-- Comments: Read-only estimates from INFORMATION_SCHEMA; requires a selected database.

SELECT DATABASE() AS database_name,
       ROUND(COALESCE(SUM(DATA_LENGTH), 0) / 1024 / 1024, 2) AS data_size_mb,
       ROUND(COALESCE(SUM(INDEX_LENGTH), 0) / 1024 / 1024, 2) AS index_size_mb,
       ROUND(COALESCE(SUM(DATA_LENGTH + INDEX_LENGTH), 0) / 1024 / 1024, 2) AS total_size_mb
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE();