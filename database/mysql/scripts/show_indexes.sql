-- Description: List indexes in the currently selected database.
-- Date: 2026-09-28
-- Author: Sherry Cards Shop project team
-- Objective: Inspect index names, columns, and uniqueness.
-- Comments: Read-only; requires a selected database.

SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME, NON_UNIQUE, SEQ_IN_INDEX
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;