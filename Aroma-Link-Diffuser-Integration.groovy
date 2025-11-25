/**
 * -----------------------
 * ------ SMART APP ------
 * -----------------------
 *
 *  Aroma-Link Diffuser Integration
 *
 *  Copyright 2024 Adrian Caramaliu
 *  Enhanced by Simon Mason (2025) - Added device status polling, improved error handling,
 *  additional device information retrieval, extended API capabilities, and Network ID display
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

import groovy.transform.Field
import java.security.MessageDigest
import com.hubitat.app.DeviceWrapper

@Field BASE_URI = "https://www.aroma-link.com"
@Field PATH_LOGIN = "/v2/app/token/"
@Field PATH_LIST = "/v1/app/device/listAll/"
@Field PATH_DEVICE = "/v1/app/device/"
@Field PATH_CONTROL = "/v1/app/data/switch"
@Field PATH_NEWSWITCH = "/v1/app/data/newSwitch"
@Field PATH_SCHEDULER = "/v1/app/data/workSetApp"

String appVersion() { return "0.2.3" }
String appModified() { return "2025-01-08"}
String appAuthor() { return "Adrian Caramaliu" }
String gitBranch() { return "ady624" }
String getAppImg(imgName) { return "https://raw.githubusercontent.com/${gitBranch()}/hubitat-aroma-link/master/icons/$imgName" }

definition(
    name: "Aroma-Link Diffuser Integration",
    namespace: "ady624",
    author: "Adrian Caramaliu",
    description: "Integrate Aroma-Link with Hubitat. Enhanced by Simon Mason with improved status polling, device information retrieval, Network ID display, and extended API capabilities.",
    category: "Integrations",
    importUrl: "https://github.com/ady624/hubitat-aroma-link.groovy",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences {
    page(name: "pgMain", title: "Aroma-Link Integration")
    page(name: "pgLogin", title: "Aroma-Link Login")
    page(name: "pgLoginFailure", title: "Aroma-Link Integration")
    page(name: "pgSettings", title: "Enhanced Settings")
    page(name: "pgDeviceInfo", title: "Device Information")
    page(name: "pgUninstall", title: "Uninstall")
}

def appInfoSect(sect=true) {
    def str = ""
    str += "${app?.name} (v${appVersion()})"
    str += "\nAuthor: ${appAuthor()}"
    str += "\nEnhanced by: Simon Mason"
    section() { paragraph str, image: getAppImg("aroma-link@2x.png") }
}

def pgMain() {
    if (state.previousVersion == null){
        state.previousVersion = 0;
    }

    if (!state.latestVersion){
        state.currentVersion = [:]
        state.currentVersion['SmartApp'] = appVersion()
    } else {
        state.previousVersion = appVersion()
    }

    state.lastPage = "pgMain"

    if (!settings.username){
        return pgLogin()
    }
    
    dynamicPage(name: "pgMain", nextPage: "", uninstall: false, install: true) {
        appInfoSect()      
        section("Aroma-Link Account"){
            href "pgLogin", title: settings.username, description: "Tap to modify", params: [nextPageName: "pgMain"]
        }
        section("Connected diffusers:"){
            if (state.diffusers) {
                state.diffusers.each { diffuserId, diffuser -> 
                    def status = diffuser.onlineStatus ? "🟢 Online" : "🔴 Offline"
                    def oilLevel = diffuser.remainOil ? " (${diffuser.remainOil}% oil)" : ""
                    def deviceInfo = "Network ID: ${diffuserId}"
                    
                    paragraph "• ${diffuser.name} - ${status}${oilLevel}"
                    paragraph "  ${deviceInfo}"
                    
                    // Show tester device info if enabled (with null check)
                    if (settings.createApiTester == true) {
                        def testerId = "${diffuserId}-tester"
                        def testerDevice = getChildDevice(testerId)
                        def testerStatus = testerDevice ? "✅ Created" : "❌ Missing"
                        paragraph "  API Tester: ${testerStatus} (${testerId})"
                    }
                }
            } else {
                paragraph "No diffusers found. Check your login credentials and device connectivity."
            }
        }
        section("Enhanced Features") {
            href "pgSettings", title: "Enhanced Settings", description: "Configure polling, logging, API testing, and advanced features"
            if (settings.enableStatusPolling) {
                def lastPoll = state.lastPollTime ? new Date(state.lastPollTime).format("HH:mm:ss") : "Never"
                paragraph "Status polling: Every ${settings.pollInterval ?: 5} minutes (Last: ${lastPoll})"
            }
            if (settings.enableDetailedLogging) {
                paragraph "📝 Detailed logging enabled"
            }
            if (settings.createApiTester == true) {
                paragraph "🔧 API Tester devices enabled"
            }
        }
        section("Device Information") {
            href "pgDeviceInfo", title: "Show All Device Network IDs", description: "View detailed device information and troubleshooting data"
        }
        section("") {
            paragraph "Tap below to completely uninstall this SmartApp and child devices"
            href(name: "", title: "",  description: "Tap to Uninstall", required: false, page: "pgUninstall")
        }
    }
}

def pgSettings() {
    dynamicPage(name: "pgSettings", title: "Enhanced Settings", nextPage: "pgMain", uninstall: false, install: false) {
        section("Status Polling (Enhanced by Simon Mason)") {
            input "enableStatusPolling", "bool", title: "Enable automatic status polling", defaultValue: true, required: false
            if (enableStatusPolling) {
                input "pollInterval", "enum", title: "Polling interval", 
                      options: ["1":"1 minute", "2":"2 minutes", "5":"5 minutes", "10":"10 minutes", "15":"15 minutes", "30":"30 minutes"], 
                      defaultValue: "5", required: false
            }
        }
        section("Device Information") {
            input "trackDeviceDetails", "bool", title: "Track detailed device information", defaultValue: true, required: false
            input "monitorOilLevels", "bool", title: "Monitor oil levels", defaultValue: true, required: false
            input "trackLastSeen", "bool", title: "Track last communication time", defaultValue: true, required: false
        }
        section("API Testing (Simon Mason)") {
            input "createApiTester", "bool", title: "Create API Tester devices for discovering new commands", defaultValue: false, required: false
        }
        section("Logging") {
            input "enableDetailedLogging", "bool", title: "Enable detailed logging", defaultValue: false, required: false
            input "logDeviceCommands", "bool", title: "Log device commands", defaultValue: true, required: false
        }
        section("Advanced Features") {
            input "enableExtendedAPI", "bool", title: "Enable extended API calls (intensity, timer)", defaultValue: true, required: false
            input "autoRefreshOnCommand", "bool", title: "Auto-refresh after commands", defaultValue: true, required: false
        }
    }
}

def pgDeviceInfo() {
    dynamicPage(name: "pgDeviceInfo", title: "Device Information", nextPage: "pgMain", uninstall: false, install: false) {
        section("Real Diffuser Devices") {
            if (state.diffusers) {
                state.diffusers.each { diffuserId, diffuser ->
                    def device = getChildDevice(diffuserId)
                    def deviceStatus = device ? "✅ Active" : "❌ Missing"
                    
                    paragraph """
                    <b>${diffuser.name}</b><br/>
                    Network ID: <code>${diffuserId}</code><br/>
                    Device Status: ${deviceStatus}<br/>
                    API ID: ${diffuser.id}<br/>
                    Group: ${diffuser.groupName}<br/>
                    Online: ${diffuser.onlineStatus ? "Yes" : "No"}
                    """
                }
            } else {
                paragraph "No diffuser data available"
            }
        }
        
        section("API Tester Devices") {
            if (settings.createApiTester == true && state.diffusers) {
                state.diffusers.each { diffuserId, diffuser ->
                    def testerId = "${diffuserId}-tester"
                    def testerDevice = getChildDevice(testerId)
                    def testerStatus = testerDevice ? "✅ Active" : "❌ Missing"
                    
                    paragraph """
                    <b>${diffuser.name} API Tester</b><br/>
                    Network ID: <code>${testerId}</code><br/>
                    Device Status: ${testerStatus}<br/>
                    Parent Check: ${testerDevice && testerDevice.parent ? "✅ Connected" : "❌ No Parent"}
                    """
                    
                    if (!testerDevice) {
                        paragraph "<i>Tester device missing - try disabling/enabling 'Create API Tester devices'</i>"
                    }
                }
            } else if (settings.createApiTester == true) {
                paragraph "API Tester enabled but no diffusers found"
            } else {
                paragraph "API Tester disabled in Enhanced Settings"
            }
        }
        
        section("All Child Devices") {
            if (getChildDevices()) {
                getChildDevices().each { device ->
                    def deviceType = device.getTypeName()
                    def parentStatus = device.parent ? "Has Parent" : "NO PARENT"
                    paragraph "• ${device.displayName} (${deviceType})<br/>  ID: <code>${device.deviceNetworkId}</code><br/>  ${parentStatus}"
                }
            } else {
                paragraph "No child devices found"
            }
        }
        
        section("Troubleshooting") {
            paragraph """
            <b>Network ID Format:</b><br/>
            • Real devices: <code>aroma-link-[number]</code><br/>
            • API testers: <code>aroma-link-[number]-tester</code><br/><br/>
            
            <b>Common Issues:</b><br/>
            • "No parent app" error = Wrong Network ID format<br/>
            • Missing devices = Check login credentials<br/>
            • Tester not working = Recreate with correct Network ID<br/><br/>
            
            <b>To Fix Tester Issues:</b><br/>
            1. Disable 'Create API Tester devices' in Enhanced Settings<br/>
            2. Save settings<br/>
            3. Enable 'Create API Tester devices' again<br/>
            4. Save settings - new tester should be created automatically
            """
        }
    }
}

def pgLogin(params) {
    state.installMsg = ""
    def showUninstall = username != null && password != null
    return dynamicPage(name: "pgLogin", title: "Connect to Aroma-Link", nextPage:"pgLoginFailure", uninstall:false, install: false, submitOnChange: true) {
        section("Credentials"){
            input("username", "text", title: "Username", description: "Aroma-Link username")
            input("password", "password", title: "Password", description: "Aroma-link password")
        }
        section("Connection Info") {
            paragraph "Enter the same username and password you use for the Aroma-Link mobile app."
            paragraph "Your password will be securely hashed before transmission."
        }
    }
}

def pgLoginFailure(){
    if (doLogin()) {
        refreshDiffusers()
        return pgMain()
    } else {
        return dynamicPage(name: "pgLoginFailure", title: "Login Error", install:false, uninstall:false) {
            section(""){
                paragraph "The username or password you entered is incorrect. Go back and try again. "
                paragraph "Make sure you're using the same credentials as your Aroma-Link mobile app."
            }
        }
    }
}

def pgUninstall() {
    def msg = ""
    childDevices.each {
        try{
            deleteChildDevice(it.deviceNetworkId, true)
            msg = "Devices have been removed. Tap remove to complete the process."
        } catch (e) {
            log.error "Error deleting ${it.deviceNetworkId}: ${e}"
            msg = "There was a problem removing your device(s). Check the IDE logs for details."
        }
    }

    return dynamicPage(name: "pgUninstall",  title: "Uninstall", install:false, uninstall:true) {
        section("Uninstall"){
            paragraph msg
        }
    }
}

def installed() {
    log.info "Aroma-Link Integration installed - Enhanced by Simon Mason"
    initialize()
}

def updated() {
    log.info "Aroma-Link Integration updated"
    initialize()
}

def initialize() {
    unschedule()
    
    runEvery5Minutes("refreshDiffusers")
    
    if (settings.enableStatusPolling && settings.pollInterval) {
        def interval = settings.pollInterval as Integer
        switch(interval) {
            case 1:
                runEvery1Minute("enhancedStatusPoll")
                break
            case 2:
                schedule("0 */2 * * * ?", "enhancedStatusPoll")
                break
            case 10:
                runEvery10Minutes("enhancedStatusPoll")
                break
            case 15:
                runEvery15Minutes("enhancedStatusPoll")
                break
            case 30:
                runEvery30Minutes("enhancedStatusPoll")
                break
            default:
                break
        }
        if (settings.enableDetailedLogging) {
            log.info "Enhanced status polling enabled: every ${interval} minute(s)"
        }
    }
}

def enhancedStatusPoll() {
    if (settings.enableDetailedLogging) {
        log.debug "Running enhanced status poll"
    }
    
    state.lastPollTime = now()
    refreshDiffusers()
    
    getChildDevices().each { device ->
        def diffuserId = device.deviceNetworkId
        def diffuserData = state.diffusers[diffuserId]
        
        if (diffuserData && settings.trackLastSeen) {
            device.updateLastSeen()
        }
    }
}

String md5(String s){
    MessageDigest.getInstance("MD5").digest(s.bytes).encodeHex().toString()
}

private login() {
    if (!state.session || (now() > state.session.expiration)) {
        if (settings.enableDetailedLogging) {
            log.warn "Token has expired. Logging in again."
        }
        doLogin()
    } else {
        return true;
    }
}

private doLogin() {
    state.session = [ authToken: null, expiration: 0 ]
    return doUserNameAuth()
}

private getApiHeaders() {
    headers = [
        "Accept-Encoding": "gzip",
        "User-Agent": "okhttp/4.5.0",
        "version": "406"
        ]
    if (state.session?.authToken) {
        headers["access_token"] = state.session.authToken
    }
    return headers
}

def doUserNameAuth() {
    def result = true
    log.info "Performing login..."
    try {
        httpPost([ 
            uri: BASE_URI, 
            path: PATH_LOGIN,
            headers: getApiHeaders(),
            query: [
                "userName": settings.username,
                "password": md5(settings.password)
            ],
            requestContentType: "application/x-www-form-urlencoded",
            ignoreSSLIssues: true
        ]) { resp ->
        if ((resp.status == 200) && resp.data && (resp.data.code == 200)) {
                state.session = [
                    authToken: resp.data.data.accessToken,
                    expiration: now() + resp.data.data.accessTokenValidity as long,
                    userId: resp.data.data.id
                ]
                log.info "Login successful"
                result = true                
            } else {
                log.error "Error logging in: ${resp.status}"
                result = false
            }
        }
    } catch (e) {
        log.error "Error logging in: ${e}"
        return false
    }
    return result
}

def refreshDiffusers(){
    state.currentVersion = [:]
    state.currentVersion['SmartApp'] = appVersion()
    state.diffusers = [:]
    deviceIds = getChildDevices()*.deviceNetworkId
    
    if (login()) {
        httpGet([ 
            uri: BASE_URI, 
            path: PATH_LIST + state.session.userId.toString(),
            headers: getApiHeaders(),
            ignoreSSLIssues: true
        ]) { resp ->
            if ((resp.status == 200) && resp.data && (resp.data.code == 200)) {
                resp.data.data.each { group ->
                    if (group.type == "group") {
                        group.children.each { device ->
                            if (device.type == "device") {
                                diffuser = [
                                    "id": device.id,
                                    "name": device.text,
                                    "groupId": group.id,
                                    "groupName": group.text,
                                    "deviceNo": device.deviceNo,
                                    "deviceType": device.deviceType,
                                    "hasFan": device.hasFan,
                                    "hasLamp": device.hasLamp,
                                    "hasPump": device.hasPump,
                                    "hasWeight": device.hasWeight,
                                    "hasBattery": device.hasBattery,
                                    "battery": device.battery,
                                    "status": device.isError ? (device.errorMsg ?: "error") : "normal",
                                    "isFragranceLevel": device.isFragranceLevel,
                                    "lowRemainOij": device.lowRemainOij,
                                    "remainOil": device.remainOil,
                                    "netType": device.netType,
                                    "onlineStatus": device.onlineStatus,
                                    "lastUpdate": now(),
                                    "signalStrength": device.signalStrength ?: "unknown",
                                    "firmwareVersion": device.firmwareVersion ?: "unknown",
                                    "workingTime": device.workingTime ?: 0,
                                    "pauseTime": device.pauseTime ?: 0,
                                    "intensity": device.intensity ?: 1
                                    ]
                                if (settings.enableDetailedLogging) {
                                    log.debug "Diffuser raw payload (${device.text ?: device.id}): ${device}"
                                    log.debug "Diffuser normalized data (${diffuser.name}): work=${diffuser.workingTime}s, pause=${diffuser.pauseTime}s, intensity=${diffuser.intensity}, battery=${diffuser.battery}, oil=${diffuser.remainOil}"
                                }
                                diffuserId = "aroma-link-${diffuser.id}"
                                state.diffusers[diffuserId] = diffuser
                                device = getChildDevice(diffuserId)
                                if (device) {
                                    device.update(diffuser)
                                } else {
                                    log.info "Adding new device for diffuser ${diffuser.name}"
                                    dw = addChildDevice("ady624", "Aroma-Link Diffuser", diffuserId, ["name": diffuser.name])
                                    dw.update(diffuser)
                                    dw.sendEvent([name: "networkStatus", value: diffuser.onlineStatus ? "online" : "offline"])
                                }
                                
                                // Create API Tester device if enabled (with null check)
                                if (settings.createApiTester == true) {
                                    def testerId = "${diffuserId}-tester"
                                    def testerDevice = getChildDevice(testerId)
                                    if (!testerDevice) {
                                        log.info "Creating API Tester device for ${diffuser.name}"
                                        try {
                                            def testerDw = addChildDevice("simonmason", "Aroma-Link API Tester", testerId, [
                                                "name": "${diffuser.name} API Tester",
                                                "label": "${diffuser.name} API Tester"
                                            ])
                                            log.info "API Tester device created: ${testerId}"
                                        } catch (Exception e) {
                                            log.error "Failed to create API Tester device: ${e.message}"
                                            log.error "Make sure the 'Aroma-Link API Tester' driver is installed with namespace 'simonmason'"
                                        }
                                    }
                                }
                                
                                deviceIds -= diffuserId
                                if (settings.createApiTester == true) {
                                    deviceIds -= "${diffuserId}-tester"
                                }
                            }
                        }
                    }
                }
                
                if (settings.enableDetailedLogging) {
                    log.debug "Refreshed ${state.diffusers.size()} diffuser(s)"
                }
            }
            deviceIds.each { deviceId -> 
                log.warn "Deleting device ${deviceId}"
                deleteChildDevice(deviceId)
            }
        }
    }
}

private void sendDeviceCommand(DeviceWrapper dw, String command, int value) {
    long deviceId = dw.getDeviceNetworkId().minus("aroma-link-") as long
    if (login()) {
        httpPost([ 
            uri: BASE_URI, 
            path: PATH_CONTROL,
            headers: getApiHeaders(),
            body: [
                "deviceId": deviceId,
                "userId": state.session.userId,
                "${command}": value
            ],
            requestContentType: "application/x-www-form-urlencoded",
            ignoreSSLIssues: true
        ]) { resp ->
            if (settings.logDeviceCommands) {
                log.info("Sent command: deviceId=${deviceId}, command=${command}, value=${value}, status=${resp.status}")
            }
            if (resp.status != 200 || resp.data.code != 200) {
                log.warn("Device ${deviceId} appears offline")
            }
            dw.sendEvent([name: "networkStatus", value: resp.status == 200 && resp.data.code == 200 ? "online" : "offline"])
            
            if (settings.autoRefreshOnCommand) {
                runIn(2, "refreshDiffusers")
            }
        }
    } else {
        dw.sendEvent([name: "networkStatus", value: "offline"])
    }
}

// Component methods
public void componentRefresh(DeviceWrapper dw) {
    if (settings.enableDetailedLogging) {
        log.debug "Component refresh requested for ${dw.displayName}"
    }
    refreshDiffusers()
}

public void componentOn(DeviceWrapper dw) {  
    sendDeviceCommand(dw, "onOff", 1)
}

public void componentOff(DeviceWrapper dw) {
    sendDeviceCommand(dw, "onOff", 0)   
}

public void componentSetSpeed(DeviceWrapper dw, speed) {
    log.warn "Fan control API not yet discovered - using power control instead"
    log.info "The JCloud manual shows a [Fan] button, but the API endpoint hasn't been found yet"
    
    if (speed == "on") {
        componentOn(dw)
    } else {
        componentOff(dw)
    }
}

// DIRECT API Testing Methods - bypassing the method call issue entirely
def executeApiTest(testerDevice, command, value, description) {
    // Get the numeric device ID directly
    def testerId = testerDevice.getDeviceNetworkId()
    def deviceIdStr = testerId.minus("aroma-link-").minus("-tester")
    def deviceId = deviceIdStr as long
    
    log.info "=== DIRECT API TEST: ${command}=${value} (${description}) ==="
    log.info "Testing on device ID: ${deviceId}"
    
    // Update tester device directly
    testerDevice.sendEvent([name: "lastCommand", value: "${command}=${value}"])
    testerDevice.sendEvent([name: "testResult", value: "pending"])
    
    if (login()) {
        try {
            httpPost([ 
                uri: BASE_URI, 
                path: PATH_CONTROL,
                headers: getApiHeaders(),
                body: [
                    "deviceId": deviceId,
                    "userId": state.session.userId,
                    "${command}": value
                ],
                requestContentType: "application/x-www-form-urlencoded",
                ignoreSSLIssues: true
            ]) { resp ->
                def responseText = resp.data ? resp.data.toString() : "No response data"
                def httpStatus = resp.status ?: 0
                def apiCode = resp.data?.code ?: 0
                
                log.info "Direct Test Response: HTTP ${httpStatus}, API Code ${apiCode}, Data: ${responseText}"
                
                // Update tester device with results
                testerDevice.sendEvent([name: "lastHttpStatus", value: httpStatus])
                testerDevice.sendEvent([name: "lastApiCode", value: apiCode])
                testerDevice.sendEvent([name: "lastResponse", value: responseText])
                
                if (httpStatus == 200 && apiCode == 200) {
                    testerDevice.sendEvent([name: "testResult", value: "success"])
                    log.info "✅ DIRECT SUCCESS: ${command}=${value} WORKED!"
                } else {
                    testerDevice.sendEvent([name: "testResult", value: "failed"])
                    log.warn "❌ DIRECT FAILED: ${command}=${value} rejected"
                }
            }
        } catch (Exception e) {
            log.error "Direct test failed: ${e.message}"
            testerDevice.sendEvent([name: "testResult", value: "failed"])
            testerDevice.sendEvent([name: "lastResponse", value: "HTTP Error: ${e.message}"])
        }
    } else {
        log.error "Login failed for direct test"
        testerDevice.sendEvent([name: "testResult", value: "failed"])
        testerDevice.sendEvent([name: "lastResponse", value: "Authentication failed"])
    }
}

def uninstall(){
    getChildDevices().each {
        try{
            deleteChildDevice(it.deviceNetworkId, true)
        } catch (e) {
            log.error "Error deleting ${it.deviceNetworkId}: ${e}"
        }
    }
}

def uninstalled() {
    log.info "Aroma-Link removal complete."
}