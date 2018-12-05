/**
 *  HA White Light (v.0.0.1)
 *
 *  Authors
 *   - fison67@nate.com
 *  Copyright 2018
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
	definition (name: "HA White Light", namespace: "fison67", author: "fison67") {
        capability "Switch"						//"on", "off"
        capability "Light"
		capability "Color Temperature"
        capability "Switch Level"
        capability "Refresh"
        
        attribute "lastCheckin", "Date"
         
        command "setStatus"
	}
    
	simulator {
	}
    
    preferences {
        input name: "baseValue", title:"HA On Value" , type: "string", required: true, defaultValue: "on"
        input name: "baseBrightnessValue", title:"HA Brightness Param Value" , type: "string", required: true, defaultValue: "brightness_pct"
        input name: "baseColorValue", title:"HA Color Param Value" , type: "string", required: true, defaultValue: "color_temp"
        input name: "transitionSecond", title:"Transition Second" , type: "number", required: true, defaultValue: 3
        input name: "colorTempMin", title:"Color Temperature Min" , type: "number", required: true, defaultValue: 2700
        input name: "colorTempMax", title:"Color Temperature Max" , type: "number", required: true, defaultValue: 7500
	}

	tiles {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"https://postfiles.pstatic.net/MjAxODAzMjdfNjgg/MDAxNTIyMTUzOTg0NzMx.YZwxpTpbz-9oqHVDLhcLyOcdWvn6TE0RPdpB_D7kWzwg.97WcX3XnDGPr5kATUZhhGRYJ1IO1MNV2pbDvg8DXruog.PNG.shin4299/Yeelight_tile_on.png?type=w580", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"https://postfiles.pstatic.net/MjAxODAzMjdfMTA0/MDAxNTIyMTUzOTg0NzIz.62-IbE4S7wAOxe3hufTJctU8mlQmrIUQztDaSTnf3kog.sxe2rqceUxFEPqrfYZ_DLkjxM5IPSotCqhErG87DI0Mg.PNG.shin4299/Yeelight_tile_off.png?type=w580", backgroundColor:"#ffffff", nextState:"turningOn"
                
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"https://postfiles.pstatic.net/MjAxODAzMjdfMTA0/MDAxNTIyMTUzOTg0NzIz.62-IbE4S7wAOxe3hufTJctU8mlQmrIUQztDaSTnf3kog.sxe2rqceUxFEPqrfYZ_DLkjxM5IPSotCqhErG87DI0Mg.PNG.shin4299/Yeelight_tile_off.png?type=w580", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.ofn", icon:"https://postfiles.pstatic.net/MjAxODAzMjdfNjgg/MDAxNTIyMTUzOTg0NzMx.YZwxpTpbz-9oqHVDLhcLyOcdWvn6TE0RPdpB_D7kWzwg.97WcX3XnDGPr5kATUZhhGRYJ1IO1MNV2pbDvg8DXruog.PNG.shin4299/Yeelight_tile_on.png?type=w580", backgroundColor:"#ffffff", nextState:"turningOn"
			}
            
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'Updated: ${currentValue}')
            }
            
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"switch level.setLevel"
            }
		}
        
        controlTile("colorTempSliderControl", "device.colorTemperature", "slider", range:"(2700..8000)", height: 1, width: 6) {
			state "colorTemperature", action:"setColorTemperature"
        }
        
        valueTile("lastOn_label", "", decoration: "flat") {
            state "default", label:'Last\nOn'
        }
        valueTile("lastOn", "device.lastOn", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
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
    
    def switchBaseValue = "on"
    if(baseValue){
    	switchBaseValue = baseValue
    }
    
    def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    def _value = (switchBaseValue == value ? "on" : "off")
    
    if(device.currentValue("switch") != _value){
        sendEvent(name: (_value == "on" ? "lastOn" : "lastOff"), value: now, displayed: false )
    }
    sendEvent(name: "switch", value:_value)
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

def on(){
    def body = [
        "entity_id":"${state.entity_id}"
    ]
	processCommand(getCommandStr("on"), body)
}

def off(){
    def body = [
        "entity_id":"${state.entity_id}"
    ]
	processCommand(getCommandStr("off"), body)
}

def setColorTemperature(_temperature){
	def temperature = _temperature
   	def min = colorTempMin ? colorTempMin : 2700
    def max = colorTempMax ? colorTempMax : 7500
    if(min > temperature){
    	temperature = min
    }
    if(max < temperature){
    	temperature = max
    }
    def body = [
        "entity_id":"${state.entity_id}",
        (baseColorValue ? baseColorValue : "kelvin"): temperature
    ]
	processCommand(getCommandStr("on"), body)
}

def setLevel(level){
	def body = [
        "entity_id":"${state.entity_id}",
        (baseBrightnessValue ? baseBrightnessValue : "brightness_pct"): level
    ]
	processCommand(getCommandStr("on"), body)
}

def getCommandStr(cmd){
	def command = cmd
    if(cmd == "on"){
    	command = cmdBaseOn ? cmdBaseOn : "turn_on"
    }else if(cmd == "off"){
    	command = cmdBaseOff ? cmdBaseOff : "turn_off"
    }
    return command
}

def processCommand(command, body){
	log.debug command + " : " + body
    def temp = state.entity_id.split("\\.")
    def options = [
     	"method": "POST",
        "path": "/api/services/" + temp[0] + "/" + command,
        "headers": [
        	"HOST": state.app_url,
            "x-ha-access": state.app_pwd,
            "Content-Type": "application/json"
        ],
        "body":body
    ]
    log.debug options
    sendCommand(options, null)
}

def callback(physicalgraph.device.HubResponse hubResponse){
    try {
        def msg = parseLanMessage(hubResponse.description)
		def jsonObj = new JsonSlurper().parseText(msg.body)
        
        setStatus(jsonObj.state)
        sendEvent(name: "level", value: (jsonObj.attributes.brightness / 255 * 100) as int)
        log.debug jsonObj
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
