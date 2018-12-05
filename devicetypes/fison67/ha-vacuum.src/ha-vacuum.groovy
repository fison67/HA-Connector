/**
 *  HA Vacuum(v.0.0.1)
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
	definition (name: "HA Vacuum", namespace: "fison67", author: "fison67") {
        capability "Switch"						//"on", "off"
        capability "Refresh"		
        
        attribute "lastCheckin", "Date"
         
        command "setStatus"
        
        command "returnToHome"
        command "spotClean"
	}
    
	simulator {
	}
    
    preferences {
        input name: "baseValue", title:"HA On Value" , type: "string", required: true, defaultValue: "on"
        input name: "cmdBaseOn", title:"HA Command Power On" , type: "string", required: true, defaultValue: "turn_on"
        input name: "cmdBaseOff", title:"HA Command Power Off" , type: "string", required: true, defaultValue: "turn_off"
        input name: "cmdBaseHome", title:"HA Command Return Home" , type: "string", required: true, defaultValue: "return_to_home"
        input name: "cmdBaseSpotClean", title:"HA Command Spot Clean" , type: "string", required: true, defaultValue: "turn_off"
       
	}

	tiles {
		multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"off", icon:"https://github.com/fison67/mi_connector/blob/master/icons/vacuum_on.png?raw=true", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"on", icon:"https://github.com/fison67/mi_connector/blob/master/icons/vacuum_off.png?raw=true", backgroundColor:"#ffffff", nextState:"turningOn"
                
                attributeState "turningOn", label:'${name}', action:"off", icon:"https://github.com/fison67/mi_connector/blob/master/icons/vacuum_on.png?raw=true", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"on", icon:"https://github.com/fison67/mi_connector/blob/master/icons/vacuum_off.png?raw=true", backgroundColor:"#ffffff", nextState:"turningOn"
			}
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'Last Update: ${currentValue}',icon: "st.Health & Wellness.health9")
            }
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
        standardTile("returnToHome", "device.returnToHome", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
            state "default", label:"Home", action:"returnToHome"
        }
        standardTile("spotClean", "device.spotClean", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
            state "default", label:"Spot Clean", action:"spotClean"
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
	commandToHA("on")
}

def off(){
	commandToHA("off")
}

def returnToHome(){
	commandToHA("home")
}

def spotClean(){
	commandToHA("cleanSpot")
}

def commandToHA(cmd){
    
    def command = cmd
    if(cmd == "on"){
    	command = cmdBaseOn ? cmdBaseOn : "turn_on"
    }else if(cmd == "off"){
    	command = cmdBaseOff ? cmdBaseOff : "turn_off"
    }else if(cmd == "home"){
    	command = cmdBaseHome ? cmdBaseHome : "return_to_home"
    }else if(cmd == "cleanSpot"){
    	command = cmdBaseSpotClean ? cmdBaseSpotClean : "clean_spot"
    }
    
	log.debug "Command[${state.entity_id}] >> ${cmd} >> ${command}"
    
    def temp = state.entity_id.split("\\.")
    def options = [
     	"method": "POST",
        "path": "/api/services/" + temp[0] + "/" + command,
        "headers": [
        	"HOST": state.app_url,
            "x-ha-access": state.app_pwd,
            "Content-Type": "application/json"
        ],
        "body":[
        	"entity_id":"${state.entity_id}"
        ]
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

def updated() {
}

def sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}
