// Live map: draws routes/stops, places the last known positions, then moves markers
// as the backend pushes {"type":"position","data":LivePositionDto} over its WebSocket.
(function () {
    const cfg = window.fleetConfig;
    const map = L.map('map').setView([5.6475, -0.1880], 15); // Legon campus
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19, attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);

    const routeColor = {};
    for (const route of cfg.routes) {
        routeColor[route.id] = route.color;
        const line = route.stops.map(s => [s.latitude, s.longitude]);
        if (line.length > 1) L.polyline(line, { color: route.color, weight: 4, opacity: 0.5 }).addTo(map);
        for (const s of route.stops) {
            L.circleMarker([s.latitude, s.longitude], { radius: 5, color: route.color })
                .bindTooltip(s.name).addTo(map);
        }
    }

    const markers = {};
    function show(p) {
        const ll = [p.latitude, p.longitude];
        if (!markers[p.vehicleId]) {
            markers[p.vehicleId] = L.circleMarker(ll, {
                radius: 9, color: '#fff', weight: 2, fillOpacity: 1,
                fillColor: routeColor[p.routeId] || '#9aa4b2'
            }).bindTooltip(p.vehicleName).addTo(map);
        } else {
            markers[p.vehicleId].setLatLng(ll);
        }
        const row = document.querySelector(`#vehicle-list li[data-vehicle-id="${p.vehicleId}"] .live`);
        if (row) {
            const battery = p.batteryLevel == null ? '' : ` · battery ${p.batteryLevel}%`;
            row.textContent = `${Math.round(p.speedKmh)} km/h${battery} · ${new Date(p.fixTime).toLocaleTimeString()}`;
        }
        // TODO Stage 3: trail, heading arrow, next stop + ETA, offline state.
    }
    cfg.positions.forEach(show);

    const status = document.getElementById('feed-status');
    function connect() {
        if (!cfg.liveFeedUrl) return;
        const ws = new WebSocket(cfg.liveFeedUrl);
        ws.onopen = () => { status.textContent = 'live'; status.classList.add('up'); };
        ws.onclose = () => {
            status.textContent = 'reconnecting…'; status.classList.remove('up');
            setTimeout(connect, 3000);
        };
        ws.onmessage = (e) => {
            const msg = JSON.parse(e.data);
            if (msg.type === 'position') show(msg.data);
        };
    }
    connect();
})();
