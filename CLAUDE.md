---
title: Aroma-Link Diffuser Unofficial API
author: Simon Mason
created: 2025-11-08
sources:
  - https://github.com/ady624/hubitat-aroma-link
  - https://community.hubitat.com/t/release-aroma-link-integration/134014
---

# Overview

This document consolidates the reverse-engineered details of the Aroma-Link cloud API that powers JCloud and other OEM cold-air diffusers. It draws on Adrian Caramaliu’s original Hubitat integration (`ady624/hubitat-aroma-link`) and the accompanying Hubitat Community discussion. The goal is to provide a stable reference for future driver and app development when no official API specification is available.

The API provides authenticated HTTPS endpoints for logging in, enumerating devices, and issuing control commands. Real-time state feedback appears to rely on a websocket channel that has not yet been decoded; as a result, current integrations treat Hubitat as the source of truth for state.

# Base Configuration

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

> **TLS note:** The integration currently sets `ignoreSSLIssues: true` because the vendor’s certificate chain has lapsed in the past. Long term, prefer pinning or validating certificates once the vendor resolves their certificates.

# Authentication Flow

1. **Credential Hashing**  
   Passwords are MD5 hashed client-side (`md5(settings.password)`) before being transmitted. The API expects the hashed value in the `password` query parameter.

2. **Token Request** – `POST /v2/app/token/`  
   - **Query parameters**
     - `userName` – Aroma-Link account email.
     - `password` – MD5 hash of the plain-text password.
   - **Response (200 / code 200)**
     - `data.accessToken` – Bearer token for subsequent calls.
     - `data.accessTokenValidity` – Lifetime appended to `now()` to compute expiration (milliseconds).
     - `data.id` – Numeric account identifier (`userId` in later calls).
   - **Error handling**
     - Non-200 responses or missing `resp.data.code == 200` are treated as login failures.

3. **Session Tracking**  
   - `state.session` stores `authToken`, `expiration`, and `userId`.
   - `login()` refreshes the token when `now() > expiration`.
   - If login fails, downstream commands set `networkStatus` to `offline`.

# Device Enumeration

## GET /v1/app/device/listAll/{userId}

- **Headers:** default headers plus `access_token`.
- **Response:** A nested structure of groups and devices. Each `group` entry represents a logical device group (e.g., a location or HVAC zone), with child items that include diffuser metadata.
- **Parsed device fields (per integration code)**
  - `id` – Device identifier (long).
  - `text` – Human-readable name.
  - `deviceNo` – Device serial number or SKU.
  - `deviceType` – Hardware family (e.g., `P300`).
  - `groupId`, `groupName` – Parent group references.
  - `hasFan`, `hasLamp`, `hasPump`, `hasWeight`, `hasBattery`.
  - `battery` – Battery percentage (where applicable).
  - `remainOil` – Remaining oil percentage.
  - `lowRemainOij` – Low-oil flag (`0`/`1`; spelled as provided by the API).
  - `isFragranceLevel` – Indicates fractional oil-level reporting.
  - `status`, `isError`, `errorMsg` – Device health.
  - `netType` – Network transport indicator.
- `workingTime`, `pauseTime`, `intensity` – Scheduler values surfaced when the cloud payload includes them (SmartApp falls back to `0/0/1` if absent).
  - `onlineStatus` – Boolean connection status.

### Child Device Lifecycle (Hubitat App)

- Existing devices are matched by `deviceNetworkId = "aroma-link-${id}"`.
- New diffusers are provisioned via `addChildDevice("ady624", "Aroma-Link Diffuser", ...)`.
- Devices absent from the latest response are deleted to avoid orphaned entries.

# Device Control

## POST /v1/app/data/switch

Issues control commands through form-encoded parameters:

| Field | Type | Description |
| --- | --- | --- |
| `deviceId` | long | Numeric ID extracted from the child device network ID. |
| `userId` | long | Logged-in user identifier. |
| `onOff` | int (0/1) | Primary power control (1 = on, 0 = off). |
| `fan` | int (0/1) | Fan toggle (interpreted as fan speed “on/off”). Optional unless fan control is requested. |

Additional command fields exist, but the production SmartApp still limits outbound calls to `onOff`. Simon’s 2025 enhancements introduce an “API tester” child device that can post experimental payload keys such as `work` (seconds), `pause` (seconds), and `intensity` (1–9) to the same endpoint for discovery.

### POST /v1/app/data/workSetApp (Experimental)

- **Aliases:** `PATH_SCHEDULER` in the SmartApp.
- **Purpose:** Intended to set diffuser work/pause schedules and intensity in a single call.
- **Parameters (per SmartApp stubs):**
  - `deviceId`, `userId` – same as the switch command.
  - `work` – integer seconds of active diffusion (validated 1–300).
  - `pause` – integer seconds between cycles (validated 60–3600).
  - `intensity` – integer diffusion level (1–9).
- **Status:** No confirmed responses yet; helper methods log warnings until hardware validation is completed.

### Response Handling

- Success: `resp.status == 200` and `resp.data.code == 200`.
- Failure: Warns that the device appears offline and updates the Hubitat attribute `networkStatus` accordingly.

# SmartApp & Driver Behaviors

## SmartApp (`aroma-link-integration-app.groovy`)

- Schedules `refreshDiffusers()` every five minutes after initialization.
- Maintains `state.diffusers` with the latest metadata for debugging or display (including `workingTime`, `pauseTime`, and `intensity`, defaulting to `0/0/1` when the API omits them).
- Exposes component callbacks for child drivers:
  - `componentRefresh(DeviceWrapper)` – triggers a full re-sync (login + device list).
  - `componentOn(DeviceWrapper)` – sends `onOff = 1`.
  - `componentOff(DeviceWrapper)` – sends `onOff = 0`.
  - `componentSetSpeed(DeviceWrapper, speed)` – sends `fan = (speed == "on" ? 1 : 0)`.
- Optionally spawns "API Tester" child devices that can invoke `executeApiTest()` to send arbitrary key/value pairs to the switch endpoint for discovery work.

## Original Driver (`aroma-link-diffuser-driver.groovy`)

Capabilities:
- `Refresh`
- `Switch`
- `FanControl`

Attributes:
- `networkStatus` (`offline`/`online`)

Commands / Methods:
- `refresh()` – delegates to parent.
- `on()` / `off()` – mirrors switch state locally and relays to parent.
- `setSpeed("on"|"off")` / `cycleSpeed()` – toggles the `FanControl` capability but ultimately maps to the same on/off API toggles.
- `update(diffuser)` – currently updates only the `networkStatus` attribute based on the `onlineStatus` boolean returned from `/listAll`.

### Enhanced Driver (Simon Mason, 2025)

The current repository (`Aroma-Link-Diffuser.groovy`) layers additional telemetry and experimental commands on top of Adrian’s base driver. Highlights include:
- New attributes for device metadata (`deviceType`, `groupName`, `hasFan`, `remainOil`, etc.).
- Last-seen tracking and troubleshooting instrumentation.
- Experimental scheduler helpers (`setScheduler`, `quickDiffuse`) that assume future SmartApp support for extended command parameters; today they call an unimplemented `parent.componentSetScheduler()` and instruct users to lean on the API tester instead.
- Convenience commands (`fanOn`, `fanOff`, `getStatus`, `testConnection`) built on the existing REST endpoints.

# Data Model Reference

| Attribute | Type | Source | Notes |
| --- | --- | --- | --- |
| `networkStatus` | enum (`online`/`offline`) | `/listAll`, command responses | Baseline connectivity indicator. |
| `deviceType`, `deviceNumber`, `groupName` | string | `/listAll` | Added by enhanced driver for UI context. |
| `remainOil`, `battery` | number (%) | `/listAll` | Many devices report `null`; monitor for future firmware updates. |
| `lowRemainOij` | enum (`true`/`false`/`unknown`) | `/listAll` | Low-oil warning (typo preserved from API). |
| `intensity`, `workingTime`, `pauseTime` | number | `/listAll` (when provided) | SmartApp now records these values, defaulting to `1/0/0` when the payload omits them. |
| `hasFan`, `hasLamp`, `hasPump`, `hasWeight`, `hasBattery` | string (`true`/`false`/`null`) | `/listAll` | Capabilities vary per device SKU. |

# Known Limitations & Observations

- **State Feedback:** Adrian confirmed on the Hubitat forum that live state is available via a websocket feed that remains undocumented. While REST endpoints suffice for control, Hubitat relies on last-command state and periodic refreshes (`[RELEASE] Aroma-Link Integration`, post #1).
- **Wi-Fi Stability:** Some users report diffusers falling offline every few days, necessitating automated power cycling via smart plugs (post #7).
- **Audible Alerts:** Device beeps on power/fan changes; community members have physically disconnected the speaker to avoid confusion with door-lock chimes (posts #3, #20).
- **SSL Cert Expiry:** An expired vendor certificate temporarily broke the integration; the app now ignores certificate errors while awaiting an official fix (post #14).
- **Scheduler & Intensity:** External research (linked Home Assistant thread) uncovered request parameters for work/pause scheduling and intensity adjustments. The enhanced SmartApp can record the fields if the API returns them and offers API-tester hooks to experiment with setting them, but there is still no validated production workflow.
- **Fan Semantics:** The vendor app exposes a simple fan toggle—no multiple speeds have been confirmed. Consequently, recent driver revisions map fan control directly to the primary switch capability (post #20).

# Implementation Checklist

1. **Install both the SmartApp and driver** to ensure `addChildDevice` succeeds during discovery.
2. **Capture credentials securely** and hash passwords with MD5 before transmission.
3. **Maintain session state** and renew tokens before expiry to avoid unnecessary login traffic.
4. **Handle null fields** defensively—many metadata fields are missing for specific SKUs.
5. **Throttle refresh calls** (default is every 5 minutes) to stay within whatever rate limits the vendor imposes.
6. **Plan for monitoring**—alert when `networkStatus` transitions to `offline`, and consider automated recovery (power cycling) as necessary.

# Open Questions

- Full parameter list for `/v1/app/data/switch` beyond `onOff` and `fan`.
- Format and authentication for the websocket channel that the mobile app uses for live status.
- Whether Aroma-Link offers device-specific endpoints under `/v1/app/device/{id}` (reserved in code but not yet invoked).
- Clarification on oil-level reporting across different diffuser models.

Contributions and additional field captures are welcome. Submit pull requests or forum updates as new behaviors are decoded.


