/*
IMPORT URL: https://raw.githubusercontent.com/psyvision/led-controller-hubitat/master/driver.groovy
*/

def version() {"v1.0.20190916"}

metadata {
    definition (name: "Raspberry Pi RGBW Light", namespace: "caunt", author: "Richard Caunt") {
        capability "Actuator"
        capability "Color Control"
        capability "Refresh"
        capability "Switch"
        capability "Switch Level"
       
        attribute "switch", "bool"
        attribute "level", "int"
        attribute "color", "string"
        attribute "RGB", "string"
        attribute "hue", "int"
        attribute "saturation", "int"
    }

    preferences {
        input "ipAddress", "text", title:"IP Address of Raspberry Pi", required:true
    }
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    state.version = version()
}

def refresh() { 
    updateStatus()
}

def updateStatus() {
    log.info "Updating status"

    def params = [
        uri: "http://${ipAddress}:4000/api/status"
    ]

    httpGet(params) { response ->
        if (response.status != 200) {
            log.error "Received HTTP error ${response.status}."
        }
        else {
            if (response.data.success == true) {
                def status = response.data.status
                updateSwitch(status)
            }
        }
    }
}

/*
def logsOff(){
    log.warn "debug logging disabled..."
    //device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def updated(){
    log.info "updated..."
    log.warn "Hue in degrees is: ${hiRezHue == true}"
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    /*if (logEnable) 
        runIn(1800,logsOff)
}
*/

def startLevelChange(direction){
    if (direction == "up") {
        // TOOD: Implement
    }
    else if (direction == "down") {
        // TOOD: Implement
    }
}

def stopLevelChange(){
    // TOOD: Implement
}

def on() {
    setStatus("on")
}

def off() {
    setStatus("off")
}

def setStatus(value) {
    log.info "Setting status to ${value}"
    
    def params = [
        uri: "http://${ipAddress}:4000/api/status/${value}",
        contentType: "application/json",
        requestContentType: "application/x-www-form-urlencoded"
    ]
    
    httpPost(params) { response -> 
        if (response.status != 200) {
            log.error "Received HTTP error ${response.status}."
        }
        else {
            if (response.data.success == true) {
                def status = response.data.status
                updateSwitch(status)
            }
        }
    }
}

def setLevel(value,rate) {
    log.info "setLevel $value, $rate"
    def unit
    sendEvent(name:"level",value:value,descriptionText:"Set level to $value", unit: unit)
}

def setColor(value){
    log.info "Setting colour to ${value}"
    
    if (value.hue != null && value.saturation != null)  {
        rgb = huesatToRGB(value.hue, value.saturation)
        
        def params = [
            uri: "http://${ipAddress}:4000/api/colour",
            body: [
                r: rgb.r,
                g: rgb.g,
                b: rgb.b
            ],
            contentType: "application/json",
            requestContentType: "application/x-www-form-urlencoded"
        ]
        
        httpPost(params) { response -> 
            if (response.status != 200) {
                log.error "Received HTTP error ${response.status}."
            }
            else {
                if (response.data.success == true) {
                    def colour = response.data.colour
                    
                    sendEvent(name: "hue", value: value.hue, descriptionText: "Set hue to $hue", unit: "%" )
                    sendEvent(name: "saturation", value: value.saturation, descriptionText: "Set saturation to $hue", unit: "%")
                    
                    sendEvent(name: "color", value: value)
                    sendEvent(name: "RGB", value: colour)
                }
            }
        }
    }
}

def setHue(hue){
    log.info "setHue ${hue}"

    hue > 99 ? (hue = 99) : null

    // sendEvent(name: "hue", value: hue,descriptionText:"Set hue to $hue", unit: "%" )
    setColor(hue: hue, level: device.currentValue("level"), saturation: device.currentValue("saturation"))
}

def setSaturation(saturation){
    log.info "setSaturation ${saturation}"

	saturation > 100 ? (saturation = 100) : null

    // sendEvent(name: "saturation", value: saturation,descriptionText:"Set saturation to $hue", unit: "%")   
    setColor(hue: device.currentValue("hue"), saturation: saturation, level: device.currentValue("level"))
}

def setLevel(value) {
    log.info "Setting level to ${value}"

   	value > 100 ? (value = 100) : null

    //setColor(hue: device.currentValue("hue"), saturation: device.currentValue("saturation"), level: level)

    def params = [
        uri: "http://${ipAddress}:4000/api/level",
        body: [ level: value ],
        contentType: "application/json",
        requestContentType: "application/x-www-form-urlencoded"
    ]
    
    httpPost(params) { response -> 
        if (response.status != 200) {
            log.error "Received HTTP error ${response.status}."
        }
        else {
            if (response.data.success == true) {
                def level = response.data.level
                updateLevel(level)
            }
        }
    }
}

def updateSwitch(value) {
    def descriptionText = "${device.getDisplayName()} is ${value}"
    log.info descriptionText
    sendEvent(name: "switch", value: value, descriptionText: descriptionText)
}

def updateLevel(value) {
    def descriptionText = "${device.getDisplayName()} level is ${value}"
    log.info descriptionText
    sendEvent(name: "level", value: value, descriptionText: descriptionText)
}

def huesatToRGB(float hue, float sat) {
	while (hue >= 100) {
        hue -= 100
    }

	int h = (int)(hue / 100 * 6)
	float f = hue / 100 * 6 - h
	
    int p = Math.round(255 * (1 - (sat / 100)))
	int q = Math.round(255 * (1 - (sat / 100) * f))
	int t = Math.round(255 * (1 - (sat / 100) * (1 - f)))
	
    switch (h) {
		case 0: return [r: "255", g: t, b: p]
		case 1: return [r: q, g: "255", b: p]
		case 2: return [r: p, g: "255", b: t]
        case 3: return [r: p, g: q, b: "255"]
        case 4: return [r: t, g: p, b: "255"]
		case 5: return [r: "255", g: p, b: q]
	}
}