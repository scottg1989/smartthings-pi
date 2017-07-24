/**
 *  Device Type Definition File
 *
 *  Device Type: Raspberry Pi
 *
 *  Copyright (c) 2017 Scott Gulliver
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:

 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.

 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 *
 */
 

preferences {
        input("ip", "string", title:"IP Address", description: "IP address of the Raspberry Pi", defaultValue: "192.168.1.211", required: true, displayDuringSetup: true)
        input("port", "string", title:"Port", description: "Port at which the REST server is running on the Raspberry Pi", defaultValue: 33333 , required: true, displayDuringSetup: true)
}

metadata {
	definition (name: "Raspberry Pi", namespace: "scottg1989", author: "Scott Gulliver") {
        attribute   "online",       "string"
        attribute   "pingReceived", "string"

		capability "Audio Notification"
        capability "Alarm"
        
		command "testSpeak"
		command "testSingleChime"
		command "testNotification"
		command "testDoorbell"
		command "testAlarm"
	}


	simulator {
	}

	tiles {
        standardTile("online", "device.online", inactiveLabel: false) {
            state "YES" , label:'Online', backgroundColor:"#6BC168", icon:"https://www.raspberrypi.org/app/uploads/2011/10/Raspi-PGB001.png"
            state "NO", label:'Offline', backgroundColor:"#F73D3D", icon:"https://www.raspberrypi.org/app/uploads/2011/10/Raspi-PGB001.png"
        }
        
        standardTile("speach", "device.switch", inactiveLabel: false, height: 1, width: 1, decoration: "flat") {
            state "default", label:"Test Speach", action:"testSpeak", icon:"st.Electronics.electronics14"
        }
        standardTile("singleChime", "device.switch", inactiveLabel: false, height: 1, width: 1, decoration: "flat") {
            state "default", label:"Single Chime", action:"testSingleChime", icon:"st.Electronics.electronics14"
        }
        standardTile("notification", "device.switch", inactiveLabel: false, height: 1, width: 1, decoration: "flat") {
            state "default", label:"Notification", action:"testNotification", icon:"st.Electronics.electronics14"
        }
        standardTile("doorbell", "device.switch", inactiveLabel: false, height: 1, width: 1, decoration: "flat") {
            state "default", label:"Doorbell", action:"testDoorbell", icon:"st.Electronics.electronics14"
        }
        standardTile("alarm", "device.switch", inactiveLabel: false, height: 1, width: 1, decoration: "flat") {
            state "default", label:"Alarm", action:"testAlarm", icon:"st.Electronics.electronics14"
        }

        standardTile("alarmState", "device.alarm", width: 2, height: 2) {      
			state "off", label:'No alarm', icon:"st.alarm.alarm.alarm", backgroundColor:"#cccccc"
			state "siren", label:'Alarm! (Siren)', icon:"st.alarm.alarm.alarm", backgroundColor:"#fc1e1e"
			state "strobe", label:'Alarm! (Strobe)', icon:"st.alarm.alarm.alarm", backgroundColor:"#fc1e1e"
			state "both", label:'Alarm! (Both)', icon:"st.alarm.alarm.alarm", backgroundColor:"#fc1e1e"
        }
        standardTile("off", "device.alarm", inactiveLabel: false, decoration: "flat") {
 			state "default", label:'Alarm Off', action:"alarm.off"
        } 
        standardTile("on", "device.alarm", inactiveLabel: false, decoration: "flat") {
 			state "default", label:'Alarm On', action:"alarm.both"
        }
	}
}

def parse(String description) {
    def msg = parseLanMessage(description)
    def msgType = msg.body.substring(0, msg.body.indexOf(":"))

    switch(msgType)
    {
        case "health":
            def statusOk = msg.status == 200
            sendEvent(name:"pingReceived", value:statusOk?"YES":"NO", displayed:false, isStateChange: true)
            break
        case "alarm":
            def alarmState = msg.body.substring(msg.body.indexOf(":") + 1)
            def alarmOn = alarmState == "on"
            sendEvent(name:"alarm", value:alarmOn?"both":"off", displayed:false, isStateChange: true)
            break
    }
    
}

def installed() {
	setupSchedules()
}

def updated() {
	setupSchedules()
}

def setupSchedules() {
	log.info "setupSchedules"
    unschedule(healthCheck)
    runEvery1Minute(healthCheck)
}

def healthCheck() {
	log.info "Running health check"
    setDeviceNetworkId()
    
    //check for flag being set
    def pingReceived = device.currentValue("pingReceived") == "YES"
    sendEvent(name:"online", value:pingReceived?"YES":"NO", displayed:false, isStateChange: true)
    sendEvent(name:"pingReceived", value:"NO", displayed:false, isStateChange: true)
    sendHubCommand(makeRestCall("GET", "/health"))
}


// test commands

def testSpeak() {
     return playText('Test speach.')
}

def testSingleChime() {
     return playTrack('SingleChime')
}

def testNotification() {
     return playTrack('Notification')
}

def testDoorbell() {
     return playTrack('Doorbell')
}

def testAlarm() {
     return playTrack('Alarm')
}


// commands

def playText(message, volume=null) {
	log.info "Executing Command playText($message)"
    return makeRestCall("GET", "/speak?msg=" + urlEncode(message))
}

def playTextAndResume(message, volume=null) {
    log.info "playTextAndResume not yet supported."
}

def playTextAndRestore(message, volume=null) {
    log.info "playTextAndRestore not yet supported."
}

def playTrack(uri, level=null) {
	log.info "Executing Command playTrack($uri)"
    return makeRestCall("GET", "/playSound?track=" + uri)
}

def playTrackAndResume(uri, level=null) {
    log.info "playTrackAndResume not yet supported."
}

def playTrackAndRestore(uri, level=null) {
    log.info "playTrackAndRestore not yet supported."
}

// alarm commands

def both() {
    log.info "Running both command"
    return startAlarm()
}

def off() {
    log.info "Running off command"
    return cancelAlarm()
}

def siren() {
    log.info "Running siren command"
    return startAlarm()
}

def strobe() {
    log.info "Running strobe command"
    return startAlarm()
}


// helper functions

def startAlarm() {
    return makeRestCall("GET", "/alarm/on")
}

def cancelAlarm() {
    return makeRestCall("GET", "/alarm/off")
}

def urlEncode(toEncode) {
    return java.net.URLEncoder.encode(toEncode, "UTF-8")
}

def makeRestCall(method, path) {
    return new physicalgraph.device.HubAction(
        method: method,
        path: path,
        headers: [
            HOST: getHostAddress()
        ]
    );
}

private getCallBackAddress() {
    return device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}

private getHostAddress() {
    def ip = getDataValue("ip")
    def port = getDataValue("port")

    if (!ip || !port) {
        def parts = device.deviceNetworkId.split(":")
        if (parts.length == 2) {
            ip = parts[0]
            port = parts[1]
        } else {
            log.warn "Can't figure out ip and port for device: ${device.id}"
        }
    }

    log.debug "Using IP: $ip and port: $port for device: ${device.id}"
    return convertHexToIP(ip) + ":" + convertHexToInt(port)
}

private Integer convertHexToInt(hex) {
    return Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private setDeviceNetworkId() {
    def ip = getDataValue("ip")
    def port = getDataValue("port")
  	device.deviceNetworkId = "$ip:$port"
  	log.debug "Device Network Id set to ${ip}:${port}"
}