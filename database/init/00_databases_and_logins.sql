-- Shuttle Tracker - SQL Server setup: two databases and their SQL logins.
--   shuttle : the app schema (routes, stops, vehicles), see 01_schema.sql
--   traccar : Traccar's own tables (tc_devices, tc_positions, ...). Traccar creates them on first start.
--
-- Dev-only passwords. CHECK_POLICY = OFF lets SQL Server accept these short passwords.
-- Change them (and the matching config) before deploying anywhere real.

IF DB_ID('shuttle') IS NULL CREATE DATABASE shuttle;
GO
IF DB_ID('traccar') IS NULL CREATE DATABASE traccar;
GO

IF SUSER_ID('shuttle') IS NULL
    CREATE LOGIN shuttle WITH PASSWORD = 'shuttle', CHECK_POLICY = OFF, DEFAULT_DATABASE = shuttle;
GO
IF SUSER_ID('traccar') IS NULL
    CREATE LOGIN traccar WITH PASSWORD = 'traccar', CHECK_POLICY = OFF, DEFAULT_DATABASE = traccar;
GO

USE shuttle;
IF USER_ID('shuttle') IS NULL CREATE USER shuttle FOR LOGIN shuttle;
ALTER ROLE db_owner ADD MEMBER shuttle;
GO

USE traccar;
IF USER_ID('traccar') IS NULL CREATE USER traccar FOR LOGIN traccar;
ALTER ROLE db_owner ADD MEMBER traccar;
GO
