# Device layer: ZX303 solar tracker (hardware, no code)

There is no firmware to write. The ZX303 is configured by SMS, and it streams Topin binary frames to
Traccar over 2G GPRS on TCP 5199.

## Power (deck slide 03)

```
5V mini solar panel ──diode──▶ TP4056 charger IN+ ──▶ 18650 1S2P pack (3.7V) ──▶ ZX303 PCB
```

> **Wire the 18650 cells in PARALLEL only (3.7V).** Series wiring (7.4V) destroys the ZX303.

Enclosure: IP65 ABS box (RF-transparent, so GPS and 2G signal pass through).

## Provisioning by SMS (send to the SIM inside the tracker)

| SMS | Effect |
|---|---|
| `DOMAIN#<server-ip-or-host>#5199#` | Server address. Traccar's Topin port is 5199. |
| `MD#30#` | Report interval: 30 seconds while moving. |
| `IMEI#` | Tracker replies with its 15-digit IMEI. Register it on the dashboard's Vehicles page. |

Command syntax can vary between firmware revisions. Check the manual that came with your units.

## Check a new tracker

1. Register its IMEI on the dashboard (**Vehicles** page). This also creates the device in Traccar.
2. Power the tracker outdoors and wait for a GPS fix.
3. The device should show **online** in Traccar's UI (http://localhost:8082). Its marker then appears
   on the FLEET.OPS map.
4. If it never comes online, look for `topin` lines in Traccar's log (`/opt/traccar/logs/tracker-server.log`).
   An "Unknown device" message there means the IMEI isn't registered.
