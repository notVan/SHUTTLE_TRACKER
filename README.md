# SHUTTLE_TRACKER (FLEET.OPS)

A GPS tracker on each campus shuttle plus a live map app, so students see where shuttles are and when the next one arrives, and administrators get trip logs that keep drivers accountable.

Solar-powered ZX303 GPS trackers on campus shuttles feed a Traccar server. A Spring Boot API and a
SQL Server database (managed with SQL Server Management Studio) sit on top, with an ASP.NET Core MVC frontend for the dashboard.
Full design: [docs/](docs/).

**Status: the backend pipeline is wired, but the features are not finished.** Device → Traccar →
backend → database is connected. The business features (ETA, geofences, history, login) are
marked `TODO Stage 3` in the code. **The frontend is the blank default MVC template** (Welcome page
only). The dashboard pages have not been built yet.

## Layers and folders

Each layer has its own top-level folder and only talks to the layer next to it.

```
 Browser
   │  HTTPS
 frontend/      ASP.NET Core MVC (C#)      default template, not wired yet       :5020 / :7020
   ┆  REST (planned)
 backend/       Spring Boot (Java 25)      FLEET.OPS API: vehicles, routes, live :8080
   │  JPA                    │  REST + WebSocket  (/api/session, /api/devices, /api/socket)
 database/      SQL Server   traccar/      Traccar server: decodes Topin frames  :8082 (UI/API)
   (shuttle + traccar DBs)     │  TCP :5199  Topin hex over 2G GPRS               :5199 (trackers)
                             device/       ZX303 + 5V solar + 18650 1S2P          (hardware)
```

| Folder | Layer | What's in it |
|---|---|---|
| `device/` | Edge (hardware) | Wiring rules and SMS provisioning (`DOMAIN#…#5199#`, `MD#30#`). No code. |
| `traccar/` | Protocol bridge | `traccar.xml`. Traccar decodes the ZX303 Topin stream (IMEI, GPS, speed, battery) and stores raw positions. |
| `database/` | Persistence | SQL Server. `init/*.sql` (T-SQL): logins, the `traccar` database (Traccar fills it) and the `shuttle` app schema (routes, stops, vehicles ⇄ IMEI). `setup-local.ps1` runs them on a local server. Browse it with SSMS. |
| `backend/` | Application / API | Spring Boot app. See its internal layers below. |
| `frontend/` | Presentation | The default ASP.NET Core MVC template (Welcome and Privacy pages). Not connected to the backend yet. |
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
4. **`WebSocketLiveFeed`** broadcasts `{"type":"position","data":{…}}` on `/ws/fleet`. No page listens to it
   yet; a future frontend map would subscribe here and load the initial state from `GET /api/fleet/live`.

The other direction: `POST /api/vehicles` → `VehicleService` → `TraccarClient.createDevice`. Traccar then accepts
that IMEI's frames. Until the frontend has a form for it, call the endpoint directly.

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
   docker compose up -d
   ```
   This starts SQL Server 2022 on port 1433, runs `database/init/*.sql`, then starts Traccar.

   **Without Docker:** install SQL Server 2022 Express or Developer. With the default instance it listens on
   TCP 1433. Turn on **SQL Server and Windows Authentication mode** (SSMS → server Properties → Security)
   and TCP/IP (SQL Server Configuration Manager). Then create the databases:
   ```
   .\database\setup-local.ps1                               # or -Server "localhost\SQLEXPRESS"
   ```
   Then install Traccar for Windows (traccar.org/download), copy `traccar/traccar-local.xml` over
   `C:\Program Files\Traccar\conf\traccar.xml`, and restart the `traccar` service.

   > **LocalDB** (`(localdb)\MSSQLLocalDB`, installed with Visual Studio) works for browsing the schema in
   > SSMS, via `.\database\setup-local.ps1 -Server "(localdb)\MSSQLLocalDB"`. The backend and Traccar can't use
   > it, because it only accepts named pipes and the Java JDBC driver needs TCP.

   **SSMS:** connect to `localhost` (or `localhost,1433` for Docker). Use Windows Authentication, or SQL Server
   Authentication with `shuttle` / `shuttle`. Docker's admin login is `sa` / `Shuttle#Dev2026`. The app tables
   are under **Databases → shuttle → Tables**, and Traccar's raw positions are in `traccar.dbo.tc_positions`.
2. **Traccar account:** open http://localhost:8082 and register the first user. It becomes the admin. Use
   `admin@shuttle.local` / `admin`, or set `TRACCAR_EMAIL` / `TRACCAR_PASSWORD` for the backend.
3. **Backend:**
   ```
   cd backend
   .\mvnw spring-boot:run
   ```
   On start it links the sample vehicle's IMEI to a Traccar device and opens the live socket.
4. **Frontend:** open `Shuttle Tracker.slnx` in Visual Studio and run, or `dotnet run --project frontend`.
   Then go to http://localhost:5020. You should see the default **Welcome** page.
5. **Fake a shuttle:** run `.\scripts\send-test-position.ps1`. Check it arrived with `GET http://localhost:8080/api/fleet/live`.

## Known open items

- **Not yet run end to end.** Backend and frontend compile, and the frontend's Welcome page runs. The
  T-SQL scripts were tested on LocalDB. The backend and Traccar haven't run against a TCP SQL Server yet.
- **Dev passwords** (`shuttle`, `traccar`, `Shuttle#Dev2026`) are in the scripts and config. Change them
  before deploying anywhere real.
- **Frontend is a blank template.** Still to build: a live map page (Leaflet or similar), a vehicle register
  page, and a typed HttpClient for the backend API. `live-feed.allowed-origins` in the backend already allows
  `localhost:5020` / `7020` to connect to `/ws/fleet`.
- **No authentication yet.** The API is open. Add JWT the same way as the Smart Door
  backend before deploying.
- **Stage 3 features (TODOs in code):** next stop and ETA, geofence alerts, online/offline from Traccar's
  `devices` messages, trails/history, route editing in the UI.
- **Sample data is placeholder.** Stop coordinates are rough Legon points and the IMEI is made up
  (`database/init/02_sample_data.sql`).
- **ZX303 SMS syntax** can vary between firmware revisions. Confirm it against the unit's manual
  ([device/README.md](device/README.md)).
