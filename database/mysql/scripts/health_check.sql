-- Description: Perform a lightweight connection and selected-database check.
-- Date: 2026-09-28
-- Author: Sherry Cards Shop project team
-- Objective: Confirm that the SQL client can execute a query and identify the selected database.
-- Comments: Read-only; select the intended database before running.

SELECT 1 AS connection_status, DATABASE() AS selected_database;