/**
 *  Aroma-Link API Tester Driver - Direct Method Version
 *
 *  Copyright 2025 Simon Mason
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
 *  Version History:
 *  v1.03 - 2025-01-08 - Direct method approach to bypass method calling issues
 *
 */

metadata {
    definition(
        name: "Aroma-Link API Tester", 
        namespace: "simonmason", 
        author: "Simon Mason"
    ) {
        capability "Actuator"
        capability "Refresh"
        
        // Test result attributes
        attribute "lastCommand", "string"
        attribute "lastResponse", "string"
        attribute "lastHttpStatus", "number"
        attribute "lastApiCode", "number"
        attribute "testResult", "enum", ["success", "failed", "pending", "unknown"]
        attribute "lastTestTime", "string"
        
        // Troubleshooting attributes
        attribute "deviceNetworkId", "string"
        attribute "parentApp", "string"
        attribute "driverVersion", "string"
        attribute "expectedNetworkId", "string"
        
        // Known command test commands
        command "testKnownOnOff", ["number"]
        command "testKnownFan", ["number"]
        command "testKnownIntensity", ["number"]
        
        // Custom command testing
        command "testCustomCommand", ["string", "string", "string"]
        
        // Quick fan discovery
        command "testFanCommands"
        
        // Utility commands
        command "clearResults"
        command "getLastResults"
        command "showTroubleshootingInfo"
    }
    
    preferences {
        section("API Testing Settings") {
            input "enableDebugLogging", "bool", title: "Enable debug logging", defaultValue: true, required: false
            input "enableDetailedResults", "bool", title: "Show detailed API responses", defaultValue: true, required: false
            input "saveTestHistory", "bool", title: "Save test history in logs", defaultValue: true, required: false
        }
        
        section("Custom Test Parameters") {
            input "customParameter", "string", title: "Custom parameter name", description: "e.g., fanControl, airFlow, etc.", required: false
            input "customValue", "string", title: "Custom parameter value", description: "e.g., 1, 0, on, off", required: false
        }
    }
}

String driverVersion() { return "1.03" }
String driverDate() { return "2025-01-08" }

def installed() {
    log.info "Aroma-Link API Tester installed (v${driverVersion()}) - Direct Method Version"
    
    // Initialize attributes
    sendEvent([name: "testResult", value: "unknown"])
    sendEvent([name: "lastCommand", value: "none"])
    sendEvent([name: "lastResponse", value: "none"])
    sendEvent([name: "lastHttpStatus", value: 0])
    sendEvent([name: "lastApiCode", value: 0])
    sendEvent([name: "lastTestTime", value: "never"])
    sendEvent([name: "driverVersion", value: driverVersion()])
    
    // Initialize troubleshooting attributes
    updateTroubleshootingInfo()
    
    log.info "API Tester ready for testing Aroma-Link commands"
    log.info "Device Network ID: ${device.deviceNetworkId}"
    log.info "Using DIRECT method approach to bypass calling issues"
    
    // Show troubleshooting info immediately
    showTroubleshootingInfo()
}

def updated() {
    log.info "Aroma-Link API Tester updated - Direct Method Version"
    
    updateTroubleshootingInfo()
    
    if (enableDebugLogging) {
        log.debug "Debug logging enabled"
    }
}

// Known command tests using DIRECT method
def testKnownOnOff(value) {
    def command = "onOff"
    def testValue = value as Integer
    
    log.info "=== TESTING KNOWN COMMAND (DIRECT): ${command} = ${testValue} ==="
    
    sendTestCommandDirect(command, testValue, "Known onOff command test")
}

def testKnownFan(value) {
    def command = "fan"
    def testValue = value as Integer
    
    log.info "=== TESTING KNOWN COMMAND (DIRECT): ${command} = ${testValue} ==="
    
    sendTestCommandDirect(command, testValue, "Known fan command test (may not exist)")
}

def testKnownIntensity(value) {
    def command = "intensity"
    def testValue = value as Integer
    
    log.info "=== TESTING KNOWN COMMAND (DIRECT): ${command} = ${testValue} ==="
    
    sendTestCommandDirect(command, testValue, "Known intensity command test")
}

// Custom command tests
def testCustomCommand(paramName, paramValue, description = "Custom command test") {
    if (!paramName || !paramValue) {
        log.error "Custom command test requires both parameter name and value"
        updateTestResult("failed", "Missing parameter name or value", "error")
        return
    }
    
    log.info "=== TESTING CUSTOM COMMAND (DIRECT): ${paramName} = ${paramValue} ==="
    log.info "Description: ${description}"
    
    sendTestCommandDirect(paramName, paramValue, description)
}

// Fan discovery using DIRECT method
def testFanCommands() {
    log.info "=== STARTING FAN COMMAND DISCOVERY (DIRECT) ==="
    
    def fanCommands = [
        "fan", "fanControl", "fanSpeed", "fanOn", "fanOff", 
        "airFlow", "airSpeed", "ventilation", "blower",
        "fanLevel", "fanMode", "fanIntensity"
    ]
    
    def currentDelay = 0
    
    fanCommands.each { command ->
        runIn(currentDelay, "testSingleFanCommandDirect", [data: [command: command, value: 1]])
        currentDelay += 3
        runIn(currentDelay, "testSingleFanCommandDirect", [data: [command: command, value: 0]])
        currentDelay += 3
    }
    
    log.info "Scheduled ${fanCommands.size() * 2} fan command tests with 3s delays"
}

def testSingleFanCommandDirect(data) {
    sendTestCommandDirect(data.command, data.value, "Fan discovery test: ${data.command}")
}

// DIRECT testing method - bypasses all method calling issues
private void sendTestCommandDirect(String command, def value, String description) {
    if (!parent) {
        log.error "No parent app found - this driver must be a child of the Aroma-Link app"
        updateTestResult("failed", "No parent app", "error")
        return
    }
    
    updateTestResult("pending", "Sending direct command...", "info")
    
    try {
        // Use the parent app's DIRECT method
        def deviceId = device.deviceNetworkId
        if (!deviceId || !deviceId.contains("aroma-link-")) {
            log.error "Invalid device network ID format: ${deviceId}"
            log.error "Should be like: aroma-link-398065-tester"
            updateTestResult("failed", "Invalid device ID format", "error")
            return
        }
        
        // Call parent's DIRECT method that bypasses all the method calling issues
        parent.executeApiTest(this, command, value, description)
        
    } catch (Exception e) {
        log.error "Failed to send direct test command: ${e.message}"
        updateTestResult("failed", "Exception: ${e.message}", "error")
    }
}

// Result handling methods
def updateTestResult(String result, String response, String logLevel = "info") {
    def timestamp = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    
    sendEvent([name: "testResult", value: result])
    sendEvent([name: "lastResponse", value: response])
    sendEvent([name: "lastTestTime", value: timestamp])
    
    if (enableDetailedResults) {
        switch(logLevel) {
            case "error":
                log.error "Test Result: ${result} - ${response}"
                break
            case "warn":
                log.warn "Test Result: ${result} - ${response}"
                break
            default:
                log.info "Test Result: ${result} - ${response}"
        }
    }
    
    if (saveTestHistory) {
        log.info "TEST HISTORY [${timestamp}]: ${result} - ${response}"
    }
}

// Utility commands
def clearResults() {
    log.info "Clearing test results"
    
    sendEvent([name: "testResult", value: "unknown"])
    sendEvent([name: "lastCommand", value: "none"])
    sendEvent([name: "lastResponse", value: "none"])
    sendEvent([name: "lastHttpStatus", value: 0])
    sendEvent([name: "lastApiCode", value: 0])
    sendEvent([name: "lastTestTime", value: "cleared"])
}

def getLastResults() {
    def results = [
        testResult: device.currentValue("testResult"),
        lastCommand: device.currentValue("lastCommand"),
        lastResponse: device.currentValue("lastResponse"),
        lastHttpStatus: device.currentValue("lastHttpStatus"),
        lastApiCode: device.currentValue("lastApiCode"),
        lastTestTime: device.currentValue("lastTestTime")
    ]
    
    log.info "Last Test Results: ${results}"
    return results
}

def refresh() {
    log.info "API Tester refresh - showing current status (Direct Method Version)"
    getLastResults()
    
    log.info "Available test commands (DIRECT METHOD):"
    log.info "- testKnownOnOff(1 or 0)"
    log.info "- testKnownFan(1 or 0)" 
    log.info "- testKnownIntensity(1-9)"
    log.info "- testCustomCommand('param', 'value', 'description')"
    log.info "- testFanCommands() - tests multiple fan command variations"
    log.info "- clearResults() - clears all test results"
    
    log.info "Device Network ID: ${device.deviceNetworkId}"
    log.info "Parent App: ${parent ? 'Connected' : 'NOT FOUND'}"
    
    // Show troubleshooting info
    showTroubleshootingInfo()
}

// Troubleshooting methods
void updateTroubleshootingInfo() {
    // Always update these for troubleshooting
    sendEvent([name: "deviceNetworkId", value: device.deviceNetworkId ?: "UNKNOWN"])
    sendEvent([name: "parentApp", value: parent ? parent.getLabel() : "NO PARENT"])
    sendEvent([name: "driverVersion", value: driverVersion()])
    
    // Calculate expected Network ID based on current ID
    def currentId = device.deviceNetworkId ?: ""
    def expectedId = "UNKNOWN"
    
    if (currentId.contains("aroma-link-") && currentId.contains("-tester")) {
        expectedId = "Correct format"
    } else if (currentId.contains("aroma-link-")) {
        expectedId = "${currentId}-tester"
    } else {
        expectedId = "Should be: aroma-link-[number]-tester"
    }
    
    sendEvent([name: "expectedNetworkId", value: expectedId])
    
    if (enableDebugLogging) {
        log.debug "Troubleshooting info updated:"
        log.debug "  Device Network ID: ${device.deviceNetworkId}"
        log.debug "  Parent App: ${parent ? parent.getLabel() : 'NO PARENT'}"
        log.debug "  Expected Network ID: ${expectedId}"
    }
}

void showTroubleshootingInfo() {
    log.info "=== API TESTER TROUBLESHOOTING INFO (DIRECT METHOD) ==="
    log.info "Device Name: ${device.displayName}"
    log.info "Device Network ID: ${device.deviceNetworkId}"
    log.info "Expected Format: aroma-link-[number]-tester"
    log.info "Parent App: ${parent ? parent.getLabel() : 'NO PARENT FOUND'}"
    log.info "Driver Version: ${driverVersion()} (Direct Method)"
    
    def currentId = device.deviceNetworkId ?: ""
    if (currentId.contains("aroma-link-") && currentId.contains("-tester")) {
        log.info "✅ Network ID format looks correct"
    } else {
        log.warn "❌ Network ID format is incorrect"
        log.warn "Current: ${currentId}"
        log.warn "Should be: aroma-link-[your-diffuser-number]-tester"
    }
    
    if (parent) {
        log.info "✅ Parent app connection found"
        log.info "✅ Using DIRECT method to bypass calling issues"
    } else {
        log.error "❌ NO PARENT APP - this will cause 'No parent app' errors"
        log.error "This device must be created by the Aroma-Link Integration app"
    }
    
    log.info "Current Test Status:"
    log.info "  Last Test: ${device.currentValue('lastTestTime')}"
    log.info "  Last Command: ${device.currentValue('lastCommand')}"
    log.info "  Last Result: ${device.currentValue('testResult')}"
    log.info "  Last Response: ${device.currentValue('lastResponse')}"
    log.info "==========================================="
}

// Parse method for potential future use
def parse(String description) {
    if (enableDebugLogging) {
        log.debug "Parse called with: ${description}"
    }
    
    // This method is ready for future enhancements
    // Currently just logs the description for debugging
}