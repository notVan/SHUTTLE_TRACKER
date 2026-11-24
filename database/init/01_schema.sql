-- Shuttle Tracker - PostgreSQL schema (database `shuttle`)
-- Runs automatically the first time the postgres container starts (empty data volume).
--
-- Raw GPS positions are NOT stored here: Traccar owns them (traccar.tc_positions).
-- This database holds what Traccar doesn't know about: shuttle routes, stops,
-- and which 15-digit IMEI is fitted to which vehicle.

-- 1. Shuttle routes
CREATE TABLE routes (
    id        SERIAL PRIMARY KEY,
    name      VARCHAR(100) NOT NULL UNIQUE,
    color     VARCHAR(7)   NOT NULL DEFAULT '#3e8ef7',   -- hex colour used on the map
    is_active BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 2. Stops along a route, in driving order
CREATE TABLE stops (
    id        SERIAL PRIMARY KEY,
    route_id  INT              NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    name      VARCHAR(100)     NOT NULL,
    sequence  INT              NOT NULL,
    latitude  DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    UNIQUE (route_id, sequence)
);

-- 3. Vehicles and the ZX303 tracker fitted to each one (IMEI binding)
CREATE TABLE vehicles (
    id                SERIAL PRIMARY KEY,
    name              VARCHAR(50) NOT NULL,
    plate             VARCHAR(20) UNIQUE,
    imei              VARCHAR(15) NOT NULL UNIQUE CHECK (imei ~ '^[0-9]{15}$'),
    traccar_device_id BIGINT UNIQUE,              -- filled in by the backend once Traccar knows the IMEI
    route_id          INT REFERENCES routes(id) ON DELETE SET NULL,
    is_active         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);
