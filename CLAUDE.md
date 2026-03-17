# Hubitat-Aroma-Link Developer Notes

> Reference parent NOTES_TO_MYSELF.md for general coding standards, hub IPs, version conventions, and all other guidelines.

## API Reference

### Base Configuration

| Setting | Value | Notes |
| --- | --- | --- |
| Base URI | `https://www.aroma-link.com` | Defined as `BASE_URI` in the integration app |
| Login Path | `/v2/app/token/` | `PATH_LOGIN` |
| Device List Path | `/v1/app/device/listAll/` | `PATH_LIST` |
| Device Detail Path | `/v1/app/device/` | `PATH_DEVICE` (reserved, not currently called) |
| Control Path | `/v1/app/data/switch` | `PATH_CONTROL` |
| Alternate Control Path | `/v1/app/data/newSwitch` | `PATH_NEWSWITCH` (declared but not yet confirmed) |
| Scheduler Path | `/v1/app/data/workSetApp` | `PATH_SCHEDULER` (used by experimental helpers) |
| Default Headers | `Accept-Encoding: gzip`, `User-Agent: okhttp/4.5.0`, `version: 406` | Populated in `getApiHeaders()` |
| Auth Header | `access_token: <token>` | Added after a successful login |

> **TLS note:** The integration currently sets `ignoreSSLIssues: true` because the vendor's certificate chain has lapsed in the past.

### Authentication Flow

1. **Credential Hashing** — Passwords are MD5 hashed client-side before being transmitted.
2. **Token Request** — `POST /v2/app/token/` with `userName` and `password` (MD5 hash). Response includes `data.accessToken`, `data.accessTokenValidity`, and `data.id`.
3. **Session Tracking** — `state.session` stores `authToken`, `expiration`, and `userId`. Token is refreshed when `now() > expiration`.

### Device Enumeration

`GET /v1/app/device/listAll/{userId}` returns groups with child device metadata including: `id`, `text`, `deviceNo`, `deviceType`, `groupId`, `groupName`, `hasFan`, `hasLamp`, `hasPump`, `hasWeight`, `hasBattery`, `battery`, `remainOil`, `lowRemainOij`, `isFragranceLevel`, `status`, `isError`, `errorMsg`, `netType`, `workingTime`, `pauseTime`, `intensity`, `onlineStatus`.

### Device Control

`POST /v1/app/data/switch` fields: `deviceId`, `userId`, `onOff` (0/1), `fan` (0/1).

`POST /v1/app/data/workSetApp` (experimental): `deviceId`, `userId`, `work` (1–300s), `pause` (60–3600s), `intensity` (1–9). No confirmed responses yet.

### Data Model Reference

| Attribute | Type | Source | Notes |
| --- | --- | --- | --- |
| `networkStatus` | enum (`online`/`offline`) | `/listAll`, command responses | Baseline connectivity indicator |
| `deviceType`, `deviceNumber`, `groupName` | string | `/listAll` | Added by enhanced driver |
| `remainOil`, `battery` | number (%) | `/listAll` | Many devices report `null` |
| `lowRemainOij` | enum (`true`/`false`/`unknown`) | `/listAll` | Low-oil warning (typo preserved from API) |
| `intensity`, `workingTime`, `pauseTime` | number | `/listAll` (when provided) | SmartApp defaults to `1/0/0` when omitted |
| `hasFan`, `hasLamp`, `hasPump`, `hasWeight`, `hasBattery` | string | `/listAll` | Capabilities vary per device SKU |

## Known Limitations

- **State Feedback:** Live state is available via websocket (undocumented). REST endpoints used for control; Hubitat relies on last-command state and periodic refreshes.
- **Wi-Fi Stability:** Some diffusers fall offline every few days — automated power cycling via smart plug may be needed.
- **Audible Alerts:** Device beeps on power/fan changes.
- **SSL Cert Expiry:** Integration ignores cert errors (`ignoreSSLIssues: true`) while awaiting vendor fix.
- **Scheduler & Intensity:** `workSetApp` endpoint is unvalidated. API tester hooks available for experimentation.
- **Fan Semantics:** No multiple speeds confirmed — fan control maps to primary switch capability.

## Open Questions

- Full parameter list for `/v1/app/data/switch` beyond `onOff` and `fan`.
- Format and authentication for the websocket channel used by the mobile app.
- Whether `/v1/app/device/{id}` is functional (reserved in code, not invoked).
- Oil-level reporting format across different device models.
