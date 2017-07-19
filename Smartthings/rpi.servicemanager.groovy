/**
 *  Connect Raspberry Pi
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
definition(
		name: "Connect Raspberry Pi",
		namespace: "scottg1989",
		author: "Scott Gulliver",
		description: "Connect to a local Raspberry Pi running a REST server",
		category: "My Apps",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


preferences {
	page(name: "deviceDiscovery", title: "Raspberry Pi Setup", content: "deviceDiscovery")
}

def searchTarget() {
	return "urn:schemas-upnp-org:service:raspberry-pi:1"
}

def deviceDiscovery() {
    log.info "DDS"
	def options = [:]
	def devices = getVerifiedDevices()
	devices.each {
		def value = it.value.name ?: "Raspberry Pi ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
		def key = it.value.mac
		options["${key}"] = value
	}
	ssdpSubscribe()

	ssdpDiscover()
	verifyDevices()

	return dynamicPage(name: "deviceDiscovery", title: "Discovery Started...", nextPage: "", refreshInterval: 5, install: true, uninstall: true) {
		section("Please wait while we discover Raspberry Pi's on your network. Select your device below once discovered.") {
			input "selectedDevices", "enum", required: false, title: "Select Devices (${options.size() ?: 0} found)", multiple: true, options: options
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    log.info "initialize"
	unsubscribe()
	unschedule()

	ssdpSubscribe()

	if (selectedDevices) {
		addDevices()
	}

	runEvery5Minutes("ssdpDiscover")
}

void ssdpDiscover() {
	log.info "Sending " + searchTarget()
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery ${searchTarget()}", physicalgraph.device.Protocol.LAN))
}

void ssdpSubscribe() {
	log.info "Subscribe " + searchTarget()
	subscribe(location, "ssdpTerm.${searchTarget()}", ssdpHandler)
}

Map verifiedDevices() {
	def devices = getVerifiedDevices()
	def map = [:]
	devices.each {
		def value = it.value.name ?: "Raspberry Pi ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
		def key = it.value.mac
		map["${key}"] = value
	}
	map
}

void verifyDevices() {
	def devices = getDevices().findAll { it?.value?.verified != true }
	devices.each {
		int port = convertHexToInt(it.value.deviceAddress)
		String ip = convertHexToIP(it.value.networkAddress)
		String host = "${ip}:${port}"
		sendHubCommand(new physicalgraph.device.HubAction("""GET ${it.value.ssdpPath} HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: deviceDescriptionHandler]))
	}
}

def getVerifiedDevices() {
	getDevices().findAll{ it.value.verified == true }
}

def getDevices() {
	if (!state.devices) {
		state.devices = [:]
	}
	state.devices
}

def addDevices() {
	def devices = getDevices()

	selectedDevices.each { dni ->
		def selectedDevice = devices.find { it.value.mac == dni }
		def d
		if (selectedDevice) {
			d = getChildDevices()?.find {
				it.deviceNetworkId == selectedDevice.value.mac
			}
		}

		if (!d) {
			log.debug "Creating UPnP Device with dni: ${selectedDevice.value.mac}"
			addChildDevice("scottg1989", "Raspberry Pi", selectedDevice.value.mac, selectedDevice?.value.hub, [
				"label": selectedDevice?.value?.name ?: "Raspberry Pi",
				"data": [
					"mac": selectedDevice.value.mac,
					"ip": selectedDevice.value.networkAddress,
					"port": selectedDevice.value.deviceAddress
				]
			])
		}
	}
}

def ssdpHandler(evt) {
	def description = evt.description
	def hub = evt?.hubId
    
    
	def parsedEvent = parseLanMessage(description)
	parsedEvent << ["hub":hub]

	def devices = getDevices()
	String ssdpUSN = parsedEvent.ssdpUSN.toString()
	if (devices."${ssdpUSN}") {
		def d = devices."${ssdpUSN}"
		if (d.networkAddress != parsedEvent.networkAddress || d.deviceAddress != parsedEvent.deviceAddress) {
			d.networkAddress = parsedEvent.networkAddress
			d.deviceAddress = parsedEvent.deviceAddress
			def child = getChildDevice(parsedEvent.mac)
			if (child) {
				child.sync(parsedEvent.networkAddress, parsedEvent.deviceAddress)
			}
		}
	} else {
		devices << ["${ssdpUSN}": parsedEvent]
	}
}

void deviceDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
	def body = hubResponse.xml
	def devices = getDevices()
	def device = devices.find { it?.key?.contains(body?.device?.UDN?.text()) }
    log.info "device"
    log.info device
	if (device) {
		device.value << [name: body?.device?.roomName?.text(), model:body?.device?.modelName?.text(), serialNumber:body?.device?.serialNum?.text(), verified: true]
        log.info "device.value"
        log.info device.value
	}
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}