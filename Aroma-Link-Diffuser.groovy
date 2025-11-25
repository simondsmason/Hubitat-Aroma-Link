/**
 *  Aroma-Link Diffuser Driver
 *
 *  Copyright 2024 Adrian Caramaliu
 *  Enhanced by Simon Mason (2025) - Added device information attributes, improved state tracking,
 *  network status monitoring, enhanced error handling, and extended control capabilities
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
  definition(name: "Aroma-Link Diffuser", namespace: "ady624", author: "Adrian Caramaliu") {
    capability "Refresh"
    capability "Switch"
    capability "Actuator"

    // Original attributes
    attribute "networkStatus", "enum", ["offline", "online"]
    
    // Enhanced attributes (added by Simon Mason)
    attribute "deviceType", "string"
    attribute "deviceNumber", "string"
    attribute "groupName", "string"
    attribute "netType", "string"
    attribute "battery", "number"
    attribute "remainOil", "number"
    attribute "status", "string"
    attribute "lastSeen", "string"
    attribute "lastUpdate", "string"
    attribute "signalStrength", "string"
    attribute "firmwareVersion", "string"
    attribute "workingTime", "number"
    attribute "pauseTime", "number"
    attribute "intensity", "number"
    attribute "hasFan", "string"
    attribute "hasLamp", "string"
    attribute "hasPump", "string"
    attribute "hasWeight", "string"
    attribute "hasBattery", "string"
    attribute "isFragranceLevel", "string"
    attribute "lowRemainOij", "enum", ["true", "false", "unknown"]
    attribute "lowOilWarning", "enum", ["active", "normal", "unknown"]  // User-friendly version
    
    // Troubleshooting attributes (added by Simon Mason)
    attribute "deviceNetworkId", "string"
    attribute "parentApp", "string"
    attribute "driverVersion", "string"
    
    // Enhanced commands (added by Simon Mason - EXPERIMENTAL/UNTESTED)
    command "setScheduler", ["number", "number", "number"]
    command "quickDiffuse", ["number"]
    command "getStatus"
    command "testConnection"
    command "fanOn"
    command "fanOff"
    command "showTroubleshootingInfo"
    
}

  preferences {
    section("Enhanced Settings (Simon Mason)") {
        input "enableDetailedLogging", "bool", title: "Enable detailed logging", defaultValue: false, required: false
        input "trackLastSeen", "bool", title: "Track last communication time", defaultValue: true, required: false
        input "showAllAttributes", "bool", title: "Display all device attributes", defaultValue: true, required: false
        input "autoUpdateInterval", "number", title: "Auto-update interval (seconds)", defaultValue: 30, range: "10..300", required: false
    }
    section("Troubleshooting") {
        input "showNetworkInfo", "bool", title: "Show network ID and parent info", defaultValue: true, required: false
    }
  }
}

String driverVersion() { return "0.2.1" }
String driverModified() { return "2025-01-08" }

void installed() {
    log.info "Aroma-Link Diffuser driver installed (v${driverVersion()}) - Enhanced by Simon Mason"
    
    // Initialize enhanced attributes
    sendEvent([name: "status", value: "unknown"])
    sendEvent([name: "deviceType", value: "unknown"])
    sendEvent([name: "netType", value: "unknown"])
    sendEvent([name: "lowOilWarning", value: "unknown"])
    sendEvent([name: "driverVersion", value: driverVersion()])
    
    // Initialize troubleshooting attributes
    updateTroubleshootingInfo()
    
    if (trackLastSeen) {
        updateLastSeen()
    }
    
    log.info "Device Network ID: ${device.deviceNetworkId}"
    log.info "Parent App: ${parent ? parent.getLabel() : 'NO PARENT FOUND'}"
}

void updated() {
    if (enableDetailedLogging) {
        log.debug "Aroma-Link Diffuser driver updated with enhanced features"
    }
    
    // Update troubleshooting info
    updateTroubleshootingInfo()
    
    // Schedule auto-updates if enabled
    unschedule()
    if (autoUpdateInterval && autoUpdateInterval > 0) {
        if (autoUpdateInterval >= 60) {
            // For intervals >= 60 seconds, use minute-based scheduling
            def minutes = Math.round(autoUpdateInterval / 60)
            schedule("0 */${minutes} * * * ?", "refresh")
            if (enableDetailedLogging) {
                log.debug "Auto-update scheduled every ${minutes} minute(s)"
            }
        } else {
            // For intervals < 60 seconds, use runIn repeatedly
            runIn(autoUpdateInterval, "scheduleNextRefresh")
            if (enableDetailedLogging) {
                log.debug "Auto-update scheduled every ${autoUpdateInterval} seconds using runIn"
            }
        }
    }
}

void updateTroubleshootingInfo() {
    // Always update these for troubleshooting
    updateAttribute("deviceNetworkId", device.deviceNetworkId ?: "UNKNOWN")
    updateAttribute("parentApp", parent ? parent.getLabel() : "NO PARENT")
    updateAttribute("driverVersion", driverVersion())
    
    if (enableDetailedLogging) {
        log.debug "Troubleshooting info updated:"
        log.debug "  Device Network ID: ${device.deviceNetworkId}"
        log.debug "  Parent App: ${parent ? parent.getLabel() : 'NO PARENT'}"
        log.debug "  Driver Version: ${driverVersion()}"
    }
}

void scheduleNextRefresh() {
    refresh()
    if (autoUpdateInterval && autoUpdateInterval > 0 && autoUpdateInterval < 60) {
        runIn(autoUpdateInterval, "scheduleNextRefresh")
    }
}

void update(diffuser) {
    if (enableDetailedLogging) {
        log.debug "Updating device with data: ${diffuser}"
    }
    
    // Update troubleshooting info on every update
    updateTroubleshootingInfo()
    
    // Original update
    updateAttribute("networkStatus", diffuser.onlineStatus ? "online" : "offline")
    
    // Enhanced updates (added by Simon Mason)
    // Always update basic attributes regardless of showAllAttributes setting
    updateAttribute("deviceType", diffuser.deviceType ?: "unknown")
    updateAttribute("deviceNumber", diffuser.deviceNo ?: "unknown")
    updateAttribute("groupName", diffuser.groupName ?: "unknown")
    updateAttribute("netType", diffuser.netType ?: "unknown")
    updateAttribute("status", diffuser.status ?: "normal")
    
    // Explicitly update all attributes - set to null if API doesn't provide them
    if (diffuser.battery != null) {
        updateAttribute("battery", diffuser.battery, "%")
    } else {
        updateAttribute("battery", null)
    }
    
    if (diffuser.remainOil != null) {
        updateAttribute("remainOil", diffuser.remainOil, "%")
    } else {
        updateAttribute("remainOil", null)
    }
    
    if (diffuser.signalStrength != null && diffuser.signalStrength != "unknown") {
        updateAttribute("signalStrength", diffuser.signalStrength)
    } else {
        updateAttribute("signalStrength", null)
    }
    
    if (diffuser.firmwareVersion != null && diffuser.firmwareVersion != "unknown") {
        updateAttribute("firmwareVersion", diffuser.firmwareVersion)
    } else {
        updateAttribute("firmwareVersion", null)
    }
    
    if (diffuser.workingTime != null) {
        updateAttribute("workingTime", diffuser.workingTime, "minutes")
    } else {
        updateAttribute("workingTime", null)
    }
    
    if (diffuser.pauseTime != null) {
        updateAttribute("pauseTime", diffuser.pauseTime, "minutes")
    } else {
        updateAttribute("pauseTime", null)
    }
    
    if (diffuser.intensity != null) {
        updateAttribute("intensity", diffuser.intensity)
    } else {
        updateAttribute("intensity", null)
    }
    
    if (showAllAttributes) {
        // Device capabilities - explicitly set to null if not provided
        if (diffuser.hasFan != null) {
            updateAttribute("hasFan", diffuser.hasFan ? "true" : "false")
        } else {
            updateAttribute("hasFan", null)
        }
        
        if (diffuser.hasLamp != null) {
            updateAttribute("hasLamp", diffuser.hasLamp ? "true" : "false")
        } else {
            updateAttribute("hasLamp", null)
        }
        
        if (diffuser.hasPump != null) {
            updateAttribute("hasPump", diffuser.hasPump ? "true" : "false")
        } else {
            updateAttribute("hasPump", null)
        }
        
        if (diffuser.hasWeight != null) {
            updateAttribute("hasWeight", diffuser.hasWeight ? "true" : "false")
        } else {
            updateAttribute("hasWeight", null)
        }
        
        if (diffuser.hasBattery != null) {
            updateAttribute("hasBattery", diffuser.hasBattery ? "true" : "false")
        } else {
            updateAttribute("hasBattery", null)
        }
        
        if (diffuser.isFragranceLevel != null) {
            updateAttribute("isFragranceLevel", diffuser.isFragranceLevel ? "true" : "false")
        } else {
            updateAttribute("isFragranceLevel", null)
        }
    }
    
    // Update low oil warning - appears to be actively reported (not null)
    if (diffuser.lowRemainOij != null) {
        def lowOilStatus = diffuser.lowRemainOij ? "true" : "false"
        updateAttribute("lowRemainOij", lowOilStatus)
        
        // Also set user-friendly version
        def warningStatus = diffuser.lowRemainOij ? "active" : "normal"
        updateAttribute("lowOilWarning", warningStatus)
        
        if (enableDetailedLogging) {
            log.debug "Low oil warning status: ${warningStatus} (API value: ${diffuser.lowRemainOij})"
        }
    } else {
        updateAttribute("lowRemainOij", "unknown")
        updateAttribute("lowOilWarning", "unknown")
    }
    
    // Update timestamps
    if (diffuser.lastUpdate) {
        def timestamp = new Date(diffuser.lastUpdate as long).format("yyyy-MM-dd HH:mm:ss", location.timeZone)
        updateAttribute("lastUpdate", timestamp)
    }
    
    // Always update lastSeen when device is online
    if (diffuser.onlineStatus) {
        updateLastSeen()
    }
    
    // Enhanced low oil warning handling (UNTESTED - based on API field behavior)
    if (diffuser.lowRemainOij) {
        log.warn "${device.displayName}: LOW OIL WARNING detected from device!"
        sendEvent([
            name: "lowOilWarning", 
            value: "active", 
            descriptionText: "${device.displayName} reports low oil level",
            isStateChange: true
        ])
    }
    
    if (enableDetailedLogging) {
        log.debug "Device update completed. Current attributes:"
        log.debug "  networkStatus: ${device.currentValue('networkStatus')}"
        log.debug "  intensity: ${device.currentValue('intensity')}"
        log.debug "  remainOil: ${device.currentValue('remainOil')}"
        log.debug "  battery: ${device.currentValue('battery')}"
        log.debug "  status: ${device.currentValue('status')}"
        log.debug "  lastSeen: ${device.currentValue('lastSeen')}"
        log.debug "  deviceNetworkId: ${device.currentValue('deviceNetworkId')}"
        log.debug "  parentApp: ${device.currentValue('parentApp')}"
    }
}

void updateAttribute(String name, value, unit = null) {
    if (device.currentValue(name) as String != value as String) {
        def eventMap = [name: name, value: value]
        if (unit) eventMap.unit = unit
        
        sendEvent(eventMap)
        
        if (enableDetailedLogging) {
            log.debug "Updated ${name}: ${value}${unit ? ' ' + unit : ''}"
        }
    }
}

void updateLastSeen() {
    if (trackLastSeen) {
        def timestamp = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
        updateAttribute("lastSeen", timestamp)
    }
}

// Original methods
public void refresh() {
    if (enableDetailedLogging) {
        log.debug "Refresh requested for device: ${device.deviceNetworkId}"
        log.debug "Parent app: ${parent ? parent.getLabel() : 'NO PARENT'}"
    }
    parent.componentRefresh(device)
}

public void on() {
    if (device.currentValue("switch") != "on") {
        sendEvent([name: "switch", value: "on"])
        if (enableDetailedLogging) {
            log.debug "Switch turned on"
        }
    }
    parent.componentOn(device)
    updateLastSeen()
}

public void off() {
    if (device.currentValue("switch") != "off") {
        sendEvent([name: "switch", value: "off"])
        if (enableDetailedLogging) {
            log.debug "Switch turned off"
        }
    }
    parent.componentOff(device)
    updateLastSeen()
}

// Enhanced methods (added by Simon Mason)
public void fanOn() {
    if (enableDetailedLogging) {
        log.debug "Fan turned on (currently same as power on - separate fan API not yet discovered)"
    }
    on()
}

public void fanOff() {
    if (enableDetailedLogging) {
        log.debug "Fan turned off (currently same as power off - separate fan API not yet discovered)"
    }
    off()
}

public void setScheduler(workDuration, pauseDuration = 900, intensity = 1) {
    if (workDuration < 1 || workDuration > 300) {
        log.error "Invalid work duration: ${workDuration}. Must be between 1 and 300 seconds."
        return
    }
    
    if (pauseDuration < 60 || pauseDuration > 3600) {
        log.error "Invalid pause duration: ${pauseDuration}. Must be between 60 and 3600 seconds."
        return
    }
    
    if (intensity < 1 || intensity > 9) {
        log.error "Invalid intensity: ${intensity}. Must be between 1 and 9."
        return
    }
    
    log.warn "EXPERIMENTAL: setScheduler command based on Home Assistant findings - not yet tested!"
    log.info "Attempting to set scheduler: work=${workDuration}s, pause=${pauseDuration}s, intensity=${intensity}"
    
    parent.componentSetScheduler(device, workDuration as Integer, pauseDuration as Integer, intensity as Integer)
    updateLastSeen()
}

public void quickDiffuse(seconds) {
    if (seconds < 5 || seconds > 60) {
        log.error "Invalid duration: ${seconds}. Must be between 5 and 60 seconds."
        return
    }
    
    log.warn "EXPERIMENTAL: quickDiffuse command - not yet tested!"
    log.info "Attempting quick diffuse for ${seconds} seconds"
    
    // Set scheduler for quick diffusion, then turn on
    parent.componentSetScheduler(device, seconds as Integer, 900, 1)
    runIn(2, "on")
    updateLastSeen()
}

public void getStatus() {
    if (enableDetailedLogging) {
        log.debug "Getting device status"
    }
    
    parent.componentGetStatus(device)
    
    // Log current status including troubleshooting info
    def status = [
        switch: device.currentValue("switch"),
        networkStatus: device.currentValue("networkStatus"),
        intensity: device.currentValue("intensity"),
        workingTime: device.currentValue("workingTime"),
        pauseTime: device.currentValue("pauseTime"),
        remainOil: device.currentValue("remainOil"),
        battery: device.currentValue("battery"),
        status: device.currentValue("status"),
        lastSeen: device.currentValue("lastSeen"),
        deviceNetworkId: device.currentValue("deviceNetworkId"),
        parentApp: device.currentValue("parentApp"),
        driverVersion: device.currentValue("driverVersion")
    ]
    
    log.info "${device.displayName} status: ${status}"
}

public void testConnection() {
    if (enableDetailedLogging) {
        log.debug "Testing connection"
    }
    
    // Show troubleshooting info
    showTroubleshootingInfo()
    
    // Trigger a refresh to test connectivity
    parent.componentRefresh(device)
    
    // Check if we got updated within the last minute
    def lastSeenStr = device.currentValue("lastSeen")
    if (lastSeenStr) {
        try {
            def lastSeen = Date.parse("yyyy-MM-dd HH:mm:ss", lastSeenStr)
            def timeDiff = (now() - lastSeen.time) / 1000 // seconds
            
            if (timeDiff < 60) {
                log.info "${device.displayName}: Connection test successful (last seen ${Math.round(timeDiff)}s ago)"
                sendEvent([name: "networkStatus", value: "online"])
            } else {
                log.warn "${device.displayName}: Connection test failed (last seen ${Math.round(timeDiff)}s ago)"
                sendEvent([name: "networkStatus", value: "offline"])
            }
        } catch (Exception e) {
            log.error "Error parsing last seen time: ${e.message}"
            sendEvent([name: "networkStatus", value: "offline"])
        }
    } else {
        log.warn "${device.displayName}: Connection test inconclusive - no recent communication"
        sendEvent([name: "networkStatus", value: "offline"])
    }
}

public void showTroubleshootingInfo() {
    log.info "=== TROUBLESHOOTING INFO FOR ${device.displayName} ==="
    log.info "Device Network ID: ${device.deviceNetworkId}"
    log.info "Parent App: ${parent ? parent.getLabel() : 'NO PARENT FOUND'}"
    log.info "Driver Version: ${driverVersion()}"
    log.info "Device Name: ${device.name}"
    log.info "Device Label: ${device.label}"
    log.info "Hub: ${device.hub?.name}"
    log.info "Last Activity: ${device.getLastActivity()}"
    log.info "Current Attributes:"
    log.info "  deviceNetworkId: ${device.currentValue('deviceNetworkId')}"
    log.info "  parentApp: ${device.currentValue('parentApp')}"
    log.info "  networkStatus: ${device.currentValue('networkStatus')}"
    log.info "  switch: ${device.currentValue('switch')}"
    log.info "==============================================="
}

// Utility methods
public Map getDeviceInfo() {
    return [
        deviceType: device.currentValue("deviceType"),
        deviceNumber: device.currentValue("deviceNumber"),
        groupName: device.currentValue("groupName"),
        netType: device.currentValue("netType"),
        hasFan: device.currentValue("hasFan"),
        hasLamp: device.currentValue("hasLamp"),
        hasPump: device.currentValue("hasPump"),
        hasWeight: device.currentValue("hasWeight"),
        hasBattery: device.currentValue("hasBattery"),
        isFragranceLevel: device.currentValue("isFragranceLevel"),
        signalStrength: device.currentValue("signalStrength"),
        firmwareVersion: device.currentValue("firmwareVersion"),
        deviceNetworkId: device.currentValue("deviceNetworkId"),
        parentApp: device.currentValue("parentApp")
    ]
}

public Map getOperatingInfo() {
    return [
        switch: device.currentValue("switch"),
        intensity: device.currentValue("intensity"),
        workingTime: device.currentValue("workingTime"),
        pauseTime: device.currentValue("pauseTime"),
        remainOil: device.currentValue("remainOil"),
        battery: device.currentValue("battery"),
        status: device.currentValue("status")
    ]
}

public Map getNetworkInfo() {
    return [
        networkStatus: device.currentValue("networkStatus"),
        netType: device.currentValue("netType"),
        signalStrength: device.currentValue("signalStrength"),
        lastSeen: device.currentValue("lastSeen"),
        lastUpdate: device.currentValue("lastUpdate"),
        deviceNetworkId: device.currentValue("deviceNetworkId"),
        parentApp: device.currentValue("parentApp")
    ]
}

// Parse method for potential future websocket integration
def parse(String description) {
    if (enableDetailedLogging) {
        log.debug "Parse called with: ${description}"
    }
    
    // This method is ready for future websocket implementation
    // Currently just logs the description for debugging
}