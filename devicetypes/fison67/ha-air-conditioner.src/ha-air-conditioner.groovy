/**
 *  HA Air Conditioner (v.0.0.2)
 *
 *  Authors
 *   - fison67@nate.com
 *  Copyright 2019
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
 
import groovy.json.JsonSlurper

metadata {
	definition (name: "HA Air Conditioner", namespace: "fison67", author: "fison67") {
        capability "Switch"						//"on", "off"
        capability "Switch Level"
        capability "Temperature Measurement"
        capability "Refresh"		
        
        attribute "lastCheckin", "Date"
        attribute "mode", "string"
         
        command "setStatus"
        command "coolMode"
        command "dryMode"
        command "setMode"
        command "setCurrentMode", ["string"]
        command "setCurrentTemperature", ["number"]
        command "setTargetTemperature", ["number"]
        
	}
    
	simulator {
	}
    
    preferences {
        input name: "baseOffValue", title:"HA OFF Value" , type: "string", required: true, defaultValue: "off"
        input name: "modeCool", title:"Cool Value" , type: "string", required: true, defaultValue: "냉방"
        input name: "modeDry", title:"Dry Value" , type: "string", required: true, defaultValue: "제습"
	}

	tiles {
		multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"off", icon:"st.switches.light.on", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
                
                attributeState "turningOn", label:'${name}', action:"off", icon:"st.switches.light.on", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
			}
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'Last Update: ${currentValue}',icon: "st.Health & Wellness.health9")
            }
		}
        controlTile("temperatureControl", "device.level", "slider", range:"(18..30)", height: 2, width: 2) {
            state "level", action:"setLevel"
        }
        valueTile("mode_label", "", decoration: "flat") {
            state "default", label:'Mode'
        }
        valueTile("mode", "device.mode", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }
        valueTile("curTemp_label", "", decoration: "flat") {
            state "default", label:'Current\nTemp'
        }
        valueTile("temperature", "device.temperature", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }
        standardTile("coolMode", "device.coolMode", decoration: "flat", height: 1, width: 3) {
			state "default", label: "Cool", action: "coolMode"
		}
        standardTile("dryMode", "device.dryMode", decoration: "flat", height: 1, width: 3) {
			state "default", label: "Dry", action: "dryMode"
		}
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
        }
        
        valueTile("lastOn_label", "", decoration: "flat") {
            state "default", label:'Last\nOn'
        }
        valueTile("lastOn", "device.lastOn", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }
        valueTile("lastOff_label", "", decoration: "flat") {
            state "default", label:'Last\nOff'
        }
        valueTile("lastOff", "device.lastOff", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }
        
        valueTile("ha_url", "device.ha_url", width: 3, height: 1) {
            state "val", label:'${currentValue}', defaultState: true
        }
        
        valueTile("entity_id", "device.entity_id", width: 3, height: 1) {
            state "val", label:'${currentValue}', defaultState: true
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
    
    def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    def baseVal = baseOffValue
    if(baseVal == null){
    	baseVal = "off"
    }
    def _value = (baseVal == value ? "off" : "on")
    
    if(device.currentValue("switch") != _value){
        sendEvent(name: (_value == "on" ? "lastOn" : "lastOff"), value: now, displayed: false )
    }
    sendEvent(name: "switch", value:_value)
    sendEvent(name: "mode", value:value)
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
        	"HOST": parent._getServerURL(),
            "Authorization": "Bearer " + parent._getPassword(),
            "Content-Type": "application/json"
        ]
    ]
    sendCommand(options, callback)
}

def setLevel(level){
	processCommand("set_temperature", [ "entity_id": state.entity_id, "temperature":level ])
}

def setCurrentTemperature(temperature){
    sendEvent(name: "temperature", value: temperature as int)
}

def setTargetTemperature(temperature){
    sendEvent(name: "level", value: temperature as int)
}

def coolMode(){
	setMode(modeCool)
}

def dryMode(){
	setMode(modeDry)
}

def setMode(mode){
	processCommand("set_operation_mode", [ "entity_id": state.entity_id, "operation_mode":mode ])
}

def on(){
	processCommand("turn_on", [ "entity_id": state.entity_id ])
}

def off(){
	processCommand("turn_off", [ "entity_id": state.entity_id ])
}

def processCommand(command, body){
    def temp = state.entity_id.split("\\.")
    def options = [
     	"method": "POST",
        "path": "/api/services/${temp[0]}/${command}",
        "headers": [
        	"HOST": parent._getServerURL(),
            "Authorization": "Bearer " + parent._getPassword(),
            "Content-Type": "application/json"
        ],
        "body":body
    ]
    log.debug options
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

def updated() {
}

def sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}
