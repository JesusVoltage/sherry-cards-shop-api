-- Description: List foreign-key column mappings in the currently selected database.
-- Date: 2026-09-28
-- Author: Sherry Cards Shop project team
-- Objective: Inspect relationships and referenced columns.
-- Comments: Read-only; requires a selected database.

SELECT TABLE_NAME, COLUMN_NAME, CONSTRAINT_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, CONSTRAINT_NAME, ORDINAL_POSITION;