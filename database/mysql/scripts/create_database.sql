-- Description: Create the local application database if it does not exist.
-- Date: 2026-09-28
-- Author: Sherry Cards Shop project team
-- Objective: Prepare an empty MySQL 8 database for local development.
-- Comments: This does not create application tables; Flyway owns the application schema.

CREATE DATABASE IF NOT EXISTS sherry_cards_shop
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;