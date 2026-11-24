-- Shuttle Tracker - SQL Server schema (database `shuttle`)
--
-- Raw GPS positions are NOT stored here: Traccar owns them (traccar.dbo.tc_positions).
-- This database holds what Traccar doesn't know about: shuttle routes, stops,
-- and which 15-digit IMEI is fitted to which vehicle.

USE shuttle;
GO
-- Filtered indexes (below) need these on; sqlcmd turns QUOTED_IDENTIFIER off by default.
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- 1. Shuttle routes
IF OBJECT_ID('dbo.routes') IS NULL
CREATE TABLE dbo.routes (
    id        INT IDENTITY(1,1) PRIMARY KEY,
    name      VARCHAR(100) NOT NULL UNIQUE,
    color     VARCHAR(7)   NOT NULL DEFAULT '#3e8ef7',   -- hex colour used on the map
    is_active BIT          NOT NULL DEFAULT 1
);
GO

-- 2. Stops along a route, in driving order
IF OBJECT_ID('dbo.stops') IS NULL
CREATE TABLE dbo.stops (
    id        INT IDENTITY(1,1) PRIMARY KEY,
    route_id  INT          NOT NULL REFERENCES dbo.routes(id) ON DELETE CASCADE,
    name      VARCHAR(100) NOT NULL,
    sequence  INT          NOT NULL,
    latitude  FLOAT        NOT NULL,
    longitude FLOAT        NOT NULL,
    CONSTRAINT uq_stops_route_sequence UNIQUE (route_id, sequence)
);
GO

-- 3. Vehicles and the ZX303 tracker fitted to each one (IMEI binding)
IF OBJECT_ID('dbo.vehicles') IS NULL
CREATE TABLE dbo.vehicles (
    id                INT IDENTITY(1,1) PRIMARY KEY,
    name              VARCHAR(50) NOT NULL,
    plate             VARCHAR(20) NULL,
    imei              VARCHAR(15) NOT NULL UNIQUE
                      CONSTRAINT ck_vehicles_imei CHECK (LEN(imei) = 15 AND imei NOT LIKE '%[^0-9]%'),
    traccar_device_id BIGINT      NULL,           -- filled in by the backend once Traccar knows the IMEI
    route_id          INT         NULL REFERENCES dbo.routes(id) ON DELETE SET NULL,
    is_active         BIT         NOT NULL DEFAULT 1,
    created_at        DATETIME2   NULL DEFAULT SYSDATETIME()
);
GO

-- SQL Server treats NULLs as equal in UNIQUE constraints, so optional unique columns use filtered indexes.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_vehicles_plate')
    CREATE UNIQUE INDEX ux_vehicles_plate ON dbo.vehicles(plate) WHERE plate IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_vehicles_traccar_device_id')
    CREATE UNIQUE INDEX ux_vehicles_traccar_device_id ON dbo.vehicles(traccar_device_id) WHERE traccar_device_id IS NOT NULL;
GO
