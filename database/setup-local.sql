-- One-time setup for a locally installed PostgreSQL (no Docker).
-- Run from the database/ folder as the postgres superuser:
--   & "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -f setup-local.sql
-- Matches the defaults in backend/src/main/resources/application.properties
-- and traccar/traccar-local.xml.

\set ON_ERROR_STOP on

-- Traccar's own database (it creates its tables on first start)
\ir init/00_traccar_database.sql

-- App database
CREATE ROLE shuttle LOGIN PASSWORD 'shuttle';
CREATE DATABASE shuttle OWNER shuttle;

\connect shuttle
SET ROLE shuttle;
\ir init/01_schema.sql
\ir init/02_sample_data.sql

\echo 'shuttle and traccar databases ready'
