# SHUTTLE_TRACKER (FLEET.OPS)

A GPS tracker on each campus shuttle plus a live map app, so students see where shuttles are and when the next one arrives, and administrators get trip logs that keep drivers accountable.

Solar-powered ZX303 GPS trackers on campus shuttles feed a Traccar server. A Spring Boot API, a
PostgreSQL database and an ASP.NET Core MVC dashboard with a live Leaflet map sit on top.
Full design: [docs/](docs/).

**Status: the pipeline is wired end to end, but the features are not finished.** Every layer exists and
talks to the layer next to it. The business features (ETA, geofences, history, login) are
marked `TODO Stage 3` in the code.

## Layers and folders

Each layer has its own top-level folder and only talks to the layer next to it.

```
 Browser (Leaflet map)
   │  HTTPS                                   ▲ WebSocket  ws://:8080/ws/fleet  (live positions)
 frontend/      ASP.NET Core MVC (C#)      dashboard + vehicle register          :5020 / :7020
   │  REST
 backend/       Spring Boot (Java 25)      FLEET.OPS API: vehicles, routes, live :8080
   │  JPA                    │  REST + WebSocket  (/api/session, /api/devices, /api/socket)
 database/      PostgreSQL   traccar/      Traccar server: decodes Topin frames  :8082 (UI/API)
   (shuttle + traccar DBs)     │  TCP :5199  Topin hex over 2G GPRS               :5199 (trackers)
                             device/       ZX303 + 5V solar + 18650 1S2P          (hardware)
```

| Folder | Layer | What's in it |
|---|---|---|
| `device/` | Edge (hardware) | Wiring rules and SMS provisioning (`DOMAIN#…#5199#`, `MD#30#`). No code. |
| `traccar/` | Protocol bridge | `traccar.xml`. Traccar decodes the ZX303 Topin stream (IMEI, GPS, speed, battery) and stores raw positions. |
| `database/` | Persistence | `init/*.sql`: the `traccar` database (Traccar fills it) and the `shuttle` app schema (routes, stops, vehicles ⇄ IMEI). |
| `backend/` | Application / API | Spring Boot app. See its internal layers below. |
| `frontend/` | Presentation | MVC app. `Controllers/` → `Services/BackendApiClient.cs` → backend. `wwwroot/js/fleet-map.js` subscribes to the live feed. |
| `scripts/` | Testing | `send-test-position.ps1` fakes a moving shuttle through Traccar's OsmAnd port (5055). No hardware needed. |
| `docs/` | — | Project presentation. |
| `docker-compose.yml` | — | Runs the database and Traccar. |

### Backend internal layers (`backend/src/main/java/com/shuttletracker/`)

| Package | Role | May call |
|---|---|---|
| `controller/` | REST endpoints, HTTP only | `service` |
| `service/` | Business rules: IMEI binding, live fleet state | `repository`, `traccar.TraccarClient`, `live.LiveFeedPublisher` |
| `repository/` | Spring Data JPA | database |
| `model/` | JPA entities: `Route`, `Stop`, `Vehicle` | — |
| `dto/` | API request/response records | — |
| `traccar/` | `TraccarClient` (REST out: login, register devices) and `TraccarSocketListener` (live positions in → `FleetService`) | `service` (inbound only) |
| `live/` | `LiveFeedPublisher` interface + `WebSocketLiveFeed` (`/ws/fleet` push to dashboards) | — |
| `config/` | WebSocket registration, allowed origins | — |

### How a GPS fix reaches the map

1. **ZX303** gets a GPS fix and sends a Topin frame over 2G to **Traccar :5199**.
2. **Traccar** decodes it, matches the IMEI to a Traccar device, stores the row in `traccar.tc_positions`, and pushes
   `{"positions":[…]}` on its `/api/socket`.
3. **`TraccarSocketListener`** receives it and passes it to **`FleetService`**. That maps Traccar `deviceId` →
   `vehicles.traccar_device_id`, converts knots to km/h, and keeps the latest fix per shuttle.
4. **`WebSocketLiveFeed`** broadcasts `{"type":"position","data":{…}}` to every open dashboard.
5. **`fleet-map.js`** moves the marker. On page load the MVC `HomeController` fetches the initial state over REST.

The other direction: registering a shuttle on the **Vehicles** page goes frontend → `POST /api/vehicles` →
`VehicleService` → `TraccarClient.createDevice`. Traccar then accepts that IMEI's frames.

### REST API

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/vehicles` | shuttles + IMEI + route + whether linked to Traccar |
| POST | `/api/vehicles` | `{name, plate, imei, routeId}`: register a shuttle and create its Traccar device |
| GET | `/api/routes` | routes with ordered stops |
| GET | `/api/fleet/live` | latest position of every shuttle |
| WS | `/ws/fleet` | live `position` messages |
| GET | `/actuator/health` | health check |

## Running it (development)

1. **Infrastructure** (Docker Desktop):
   ```
   docker compose up -d postgres traccar
   ```
   **Without Docker:** run `database/setup-local.sql` with your local PostgreSQL 18:
   ```
   & "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -f database/setup-local.sql
   ```
   Then install Traccar for Windows (traccar.org/download), copy `traccar/traccar-local.xml` over
   `C:\Program Files\Traccar\conf\traccar.xml`, and restart the `traccar` service.
2. **Traccar account:** open http://localhost:8082 and register the first user. It becomes the admin. Use
   `admin@shuttle.local` / `admin`, or set `TRACCAR_EMAIL` / `TRACCAR_PASSWORD` for the backend.
3. **Backend:**
   ```
   cd backend
   .\mvnw spring-boot:run
   ```
   On start it links the sample vehicle's IMEI to a Traccar device and opens the live socket.
4. **Frontend:** open `Shuttle Tracker.slnx` in Visual Studio and run, or `dotnet run --project frontend`.
   Then go to http://localhost:5020.
5. **Fake a shuttle:** run `.\scripts\send-test-position.ps1`. The marker should move along the sample route.

## Known open items

- **Not yet run end to end.** Backend and frontend compile. Traccar and the database weren't available
  when this was scaffolded, so steps 1–5 above are untested.
- **Live map goes browser → backend directly.** The WebSocket (`/ws/fleet`) skips the MVC frontend.
  `live-feed.allowed-origins` limits which origins may connect. If the layering must be strict, add a
  SignalR hub to the frontend that relays the backend feed.
- **No authentication yet.** The API and dashboard are open. Add JWT the same way as the Smart Door
  backend before deploying.
- **Stage 3 features (TODOs in code):** next stop and ETA, geofence alerts, online/offline from Traccar's
  `devices` messages, trails/history, route editing in the UI.
- **Sample data is placeholder.** Stop coordinates are rough Legon points and the IMEI is made up
  (`database/init/02_sample_data.sql`).
- **ZX303 SMS syntax** can vary between firmware revisions. Confirm it against the unit's manual
  ([device/README.md](device/README.md)).
