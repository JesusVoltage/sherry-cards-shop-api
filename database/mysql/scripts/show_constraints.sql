-- Description: List table constraints in the currently selected database.
-- Date: 2026-09-28
-- Author: Sherry Cards Shop project team
-- Objective: Inspect primary-key, unique, foreign-key, and check constraints.
-- Comments: Read-only; requires a selected database.

SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
ORDER BY TABLE_NAME, CONSTRAINT_TYPE, CONSTRAINT_NAME;