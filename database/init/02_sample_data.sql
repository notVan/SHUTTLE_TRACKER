-- Sample data so the map has something to draw on day one.
-- PLACEHOLDERS: the stop coordinates are rough points on the Legon campus and the IMEI
-- is made up. Replace them with surveyed stops and the real IMEI printed on your ZX303.

INSERT INTO routes (name, color) VALUES ('Main Gate Loop', '#e5484d');

INSERT INTO stops (route_id, name, sequence, latitude, longitude) VALUES
    (1, 'Main Gate',          1, 5.6377, -0.1863),
    (1, 'Night Market',       2, 5.6441, -0.1851),
    (1, 'Balme Library',      3, 5.6507, -0.1868),
    (1, 'Commonwealth Hall',  4, 5.6553, -0.1903);

INSERT INTO vehicles (name, plate, imei, route_id) VALUES
    ('Shuttle 01', 'GR 4521-24', '868120301000011', 1);
