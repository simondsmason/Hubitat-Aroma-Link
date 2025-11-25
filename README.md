# Hubitat Aroma-Link Integration

Enhanced Hubitat integration for Aroma-Link cloud-connected diffusers, based on the original work by Adrian Caramaliu (ady624).

## Overview

This integration connects Hubitat to Aroma-Link diffusers via their cloud API, providing device control, status monitoring, and experimental API testing capabilities.

## Original Author

- **Original Developer:** Adrian Caramaliu (ady624)
- **Original Repository:** [ady624/hubitat-aroma-link](https://github.com/ady624/hubitat-aroma-link)
- **Hubitat Forum Thread:** [[RELEASE] Aroma-Link Integration](https://community.hubitat.com/t/release-aroma-link-integration/134014)

## Enhancements by Simon Mason (2025)

This fork adds:
- **Schedule Reading:** Reads `workingTime`, `pauseTime`, and `intensity` from the API
- **API Tester Devices:** Experimental child devices for discovering new API parameters
- **Enhanced Status Polling:** Configurable polling intervals (1, 2, 5, 10, 15, 30 minutes)
- **Additional Device Metadata:** Signal strength, firmware version, last update tracking
- **Detailed Logging:** Debug mode with full payload dumps
- **Enhanced UI:** Settings page, Device Information page, troubleshooting attributes
- **Auto-refresh:** Optional refresh after sending commands

## Files

### SmartApp
- **`Aroma-Link-Diffuser-Integration.groovy`** - Main integration SmartApp (enhanced version)
- **`Aroma-Link-Integration-Original.groovy`** - Original SmartApp by ady624 (for reference)

### Driver
- **`Aroma-Link-Diffuser.groovy`** - Enhanced diffuser driver with additional attributes and commands
- **`Aroma-Link-Diffuser-Original.groovy`** - Original driver by ady624 (for reference)

### API Tester
- **`Aroma-Link-API-Tester.groovy`** - Experimental driver for testing new API parameters

### Documentation
- **`Aroma-Link-API-Documentation.md`** - Comprehensive API documentation and implementation notes

## Installation

1. **Install the SmartApp:**
   - Go to Hubitat → Apps Code → New App
   - Paste the contents of `Aroma-Link-Diffuser-Integration.groovy`
   - Save

2. **Install the Main Driver:**
   - Go to Hubitat → Drivers Code → New Driver
   - Paste the contents of `Aroma-Link-Diffuser.groovy`
   - Save (namespace: `ady624`, name: `Aroma-Link Diffuser`)

3. **Install the API Tester Driver (Optional):**
   - Go to Hubitat → Drivers Code → New Driver
   - Paste the contents of `Aroma-Link-API-Tester.groovy`
   - Save (namespace: `simonmason`, name: `Aroma-Link API Tester`)

4. **Configure the SmartApp:**
   - Go to Apps → Add Built-in App → Aroma-Link Diffuser Integration
   - Enter your Aroma-Link username and password
   - Enable "Create API Tester devices" if you want to experiment with new API parameters

## Features

### Basic Control
- Turn diffusers on/off
- Refresh device status
- Monitor network connectivity

### Enhanced Attributes
- Device type, group name, network type
- Battery level (where supported)
- Oil level and low oil warnings
- Signal strength and firmware version
- Schedule settings (working time, pause time, intensity)

### API Testing
- Test known commands (`onOff`, `fan`, `intensity`)
- Test custom parameters for discovery
- View HTTP status codes and API responses
- Automated fan command discovery

## Known Limitations

- **Fan Control:** The original fan control API endpoint appears non-functional; fan commands currently map to power control
- **Schedule Settings:** While the integration can read schedule values from the API, setting schedules via the API is experimental and not yet fully validated
- **State Feedback:** Real-time state updates rely on periodic polling; websocket support is not yet implemented
- **SSL Certificates:** The integration ignores SSL certificate issues due to vendor certificate problems

## Change History

- **0.2.3** (2025-01-08) - Enhanced by Simon Mason
  - Added schedule reading (workingTime, pauseTime, intensity)
  - Added API Tester device support
  - Enhanced status polling with configurable intervals
  - Added detailed logging and debug output
  - Enhanced UI with settings and device information pages
  - Added troubleshooting attributes and improved error handling

## License

Licensed under the Apache License, Version 2.0. See the original repository for full license details.

## Credits

- **Original Development:** Adrian Caramaliu (ady624)
- **Enhancements:** Simon Mason (2025)
- **API Documentation:** Based on reverse engineering and community research

## Support

For issues and questions:
- Check the [Hubitat Community Forum thread](https://community.hubitat.com/t/release-aroma-link-integration/134014)
- Review the API documentation in `Aroma-Link-API-Documentation.md`
- Use the troubleshooting features in the SmartApp's Device Information page

