/**
 *  HA Blind(v.0.0.1)
 *
 * MIT License
 *
 * Copyright (c) 2019 fison67@nate.com
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import groovy.json.JsonSlurper

metadata {
	definition (name: "HA Blind", namespace: "fison67", author: "fison67") {
        capability "Actuator"		
        capability "Switch Level"
        capability "windowShade"
        capability "Switch"
        capability "Refresh"
         
        attribute "lastCheckin", "Date"
        
        command "setStatus"
        command "stop"
	}


	simulator {}

	preferences {}

	tiles {
		multiAttributeTile(name:"windowShade", type: "windowShade", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.windowShade", key: "PRIMARY_CONTROL") {
                attributeState "closed", label: 'closed', action: "open", icon: "st.doors.garage.garage-closed", backgroundColor: "#A8A8C6", nextState: "opening"
                attributeState "open", label: 'open', action: "close", icon: "st.doors.garage.garage-open", backgroundColor: "#F7D73E", nextState: "closing"
                attributeState "closing", label: '${name}', action: "open", icon: "st.contact.contact.closed", backgroundColor: "#B9C6A8"
                attributeState "opening", label: '${name}', action: "close", icon: "st.contact.contact.open", backgroundColor: "#D4CF14"
                attributeState "partially open", label: 'partially\nopen', action: "close", icon: "st.doors.garage.garage-closing", backgroundColor: "#D4ACEE", nextState: "closing"              
			}
            
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'Updated: ${currentValue}')
            }
            
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"setLevel"
            }
		}
        
        standardTile("open", "", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("on", label: 'open', action: "open", icon: "st.doors.garage.garage-open")
        }
        
        standardTile("stop", "", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("stop", label: 'stop', action: "stop", icon: "st.illuminance.illuminance.dark")
        }
       
        standardTile("close", "", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("off", label: 'close', action: "close", icon: "st.doors.garage.garage-closed")
        }
        
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
        }
	}
}


// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def setStatus(String value){
    if(state.entity_id == null){
    	return
    }
	log.debug "Status[${state.entity_id}] >> ${value}"
    
    def level = value as int
    if(level == 0){
    	sendEvent(name:"windowShade", value: "closed")
    }else if(level == 100){
    	sendEvent(name:"windowShade", value: "open")
    }else{
    	sendEvent(name:"windowShade", value: "partially open")
    }
    sendEvent(name:"level", value: level)
    
    def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    sendEvent(name: "lastCheckin", value: new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone), displayed: false)
    sendEvent(name: "entity_id", value: state.entity_id, displayed: false)
}

def setHASetting(url, password, deviceId){
	state.app_url = url
    state.app_pwd = password
    state.entity_id = deviceId
    
    sendEvent(name: "ha_url", value: state.app_url, displayed: false)
}

def refresh(){
	log.debug "Refresh"
    def options = [
     	"method": "GET",
        "path": "/api/states/${state.entity_id}",
        "headers": [
        	"HOST": state.app_url,
            "x-ha-access": state.app_pwd,
            "Content-Type": "application/json"
        ]
    ]
    sendCommand(options, callback)
}

def open(){
	log.debug "Open"
	def body = [
        "entity_id": state.entity_id
    ]
	processCommand("open_cover", body)
}

def closed(){
	log.debug "closed"
	def body = [
        "entity_id": state.entity_id
    ]
	processCommand("close_cover", body)
}

def stop(){
	log.debug "stop"
	def body = [
        "entity_id": state.entity_id
    ]
	processCommand("stop_cover", body)
}

def setLevel(level){
	log.debug "setLevel >> ${level}"
	def body = [
        "entity_id": state.entity_id,
        "position": level
    ]
	processCommand("set_cover_position", body)
}

def processCommand(command, body){
    def temp = state.entity_id.split("\\.")
    def options = [
     	"method": "POST",
        "path": "/api/services/${temp[0]}/${command}",
        "headers": [
        	"HOST": state.app_url,
            "x-ha-access": state.app_pwd,
            "Content-Type": "application/json"
        ],
        "body":body
    ]
    sendCommand(options, null)
}

def callback(physicalgraph.device.HubResponse hubResponse){
	def msg
    try {
        msg = parseLanMessage(hubResponse.description)
		def jsonObj = new JsonSlurper().parseText(msg.body)
        setStatus(jsonObj.state)
    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}

def updated() {}

def sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}
