-- Sample data so the map has something to draw on day one.
-- PLACEHOLDERS: the stop coordinates are rough points on the Legon campus and the IMEI
-- is made up. Replace them with surveyed stops and the real IMEI printed on your ZX303.

USE shuttle;
GO
SET QUOTED_IDENTIFIER ON;   -- needed to write to tables with filtered indexes
GO

IF NOT EXISTS (SELECT 1 FROM dbo.routes)
BEGIN
    DECLARE @route INT;

    INSERT INTO dbo.routes (name, color) VALUES ('Main Gate Loop', '#e5484d');
    SET @route = SCOPE_IDENTITY();

    INSERT INTO dbo.stops (route_id, name, sequence, latitude, longitude) VALUES
        (@route, 'Main Gate',          1, 5.6377, -0.1863),
        (@route, 'Night Market',       2, 5.6441, -0.1851),
        (@route, 'Balme Library',      3, 5.6507, -0.1868),
        (@route, 'Commonwealth Hall',  4, 5.6553, -0.1903);

    INSERT INTO dbo.vehicles (name, plate, imei, route_id) VALUES
        ('Shuttle 01', 'GR 4521-24', '868120301000011', @route);
END
GO
