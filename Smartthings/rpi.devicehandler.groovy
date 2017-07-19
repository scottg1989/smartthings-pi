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
		capability "Switch"
	}


	simulator {
	}

	tiles {
		standardTile("button", "device.switch", width: 1, height: 1, canChangeIcon: true) {
			state "off", label: 'Off', backgroundColor: "#ffffff", action: "switch.on", nextState: "on" 
			state "on", label: 'On', backgroundColor: "#79b821", action: "switch.on", nextState: "off"
		}
	}
}

def parse(String description) {
	log.debug "PARSE CALLED: " + description
}

def on() {
	log.debug "Executing on"
    
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/test",
        headers: [
            HOST: getHostAddress()
        ]
    )
    return result
}

def off() {
	log.debug "Executing 'off'"
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