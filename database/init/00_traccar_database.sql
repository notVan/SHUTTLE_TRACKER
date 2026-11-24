-- Traccar keeps its own tables (tc_devices, tc_positions, ...) in a separate database.
-- Traccar creates those tables itself on first start; this file only creates the empty database.
CREATE ROLE traccar LOGIN PASSWORD 'traccar';
CREATE DATABASE traccar OWNER traccar;
