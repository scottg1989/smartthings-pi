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
		capability "Audio Notification"
        
		command "testSpeak"
		command "testSingleChime"
		command "testDoubleChime"
		command "testDoorbell"
		command "testAlarm"
	}


	simulator {
	}

	tiles {
        standardTile("speach", "device.switch", inactiveLabel: false, height: 1, width: 1, decoration: "flat") {
            state "default", label:"Test Speach", action:"testSpeak", icon:"st.secondary.refresh"
        }
        standardTile("singleChime", "device.switch", inactiveLabel: false, height: 1, width: 1, decoration: "flat") {
            state "default", label:"Single Chime", action:"testSingleChime", icon:"st.secondary.refresh"
        }
        standardTile("doubleChime", "device.switch", inactiveLabel: false, height: 1, width: 1, decoration: "flat") {
            state "default", label:"Double Chime", action:"testDoubleChime", icon:"st.secondary.refresh"
        }
        standardTile("doorbell", "device.switch", inactiveLabel: false, height: 1, width: 1, decoration: "flat") {
            state "default", label:"Doorbell", action:"testDoorbell", icon:"st.secondary.refresh"
        }
        standardTile("alarm", "device.switch", inactiveLabel: false, height: 1, width: 1, decoration: "flat") {
            state "default", label:"Alarm", action:"testAlarm", icon:"st.secondary.refresh"
        }
	}
}

def parse(String description) {
	log.debug "PARSE CALLED: " + description
}


// test commands

def testSpeak() {
     return playText('Test speach.')
}

def testSingleChime() {
     return playTrack('SingleChime')
}

def testDoubleChime() {
     return playTrack('DoubleChime')
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
    return makeRestCall("GET", "/test?msg=" + urlEncode(message))
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


// helper functions

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