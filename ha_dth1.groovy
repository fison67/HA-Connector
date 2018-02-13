/**
 *  HA DTH1 (v.0.0.1)
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
	definition (name: "HA DTH1", namespace: "fison67", author: "fison67") {
		capability "Motion Sensor"				//"active", "inactive"
      	capability "Contact Sensor"  			//"open","closed"
        capability "Switch"						//"on", "off"
        capability "Smoke Detector"    			//"detected", "clear", "tested"
        capability "Water Sensor"      			//"dry", "wet"
        capability "Sound Sensor"      			//"detected", "not detected"
        capability "Presence Sensor"			//"present", "not present"
        capability "Acceleration Sensor"		//"active", "inactive"
         
        attribute "status", "string"
         
        command "setStatus"
        command "refresh"
        command "localOn"
        command "localOff"
	}


	simulator {
	}

	tiles {
		multiAttributeTile(name:"status", type: "generic", width: 6, height: 4){
			tileAttribute ("device.status", key: "PRIMARY_CONTROL") {
				attributeState "active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#00a0dc"
				attributeState "inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
                attributeState "open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#e86d13"
                attributeState "closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#00a0dc"
                state "dry", label:'${name}', backgroundColor: "#ffffff", icon:"st.alarm.water.dry" 
            	state "wet", label:'${name}', backgroundColor: "#53a7c0", icon:"st.alarm.water.wet"
                state "clear", label:'${name}', backgroundColor: "#ffffff", icon:"st.alarm.smoke.clear" 
            	state "detected", label:'${name}', backgroundColor: "#e86d13", icon:"st.alarm.smoke.smoke" 
                state "not present", label:'${name}', backgroundColor: "#ffffff", icon:"st.presence.tile.presence-default" 
            	state "present", label:'${name}', backgroundColor: "#53a7c0", icon:"st.presence.tile.presence-default" 
                attributeState "on", label:'${name}', action:"localOff", icon:"st.switches.light.on", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"localOn", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
                
                attributeState "turningOn", label:'${name}', action:"localOff", icon:"st.switches.light.on", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"localOn", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
			}
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'Last Update: ${currentValue}',icon: "st.Health & Wellness.health9")
            }
		}
        
        valueTile("entity_id", "device.entity_id", width: 4, height: 2) {
            state "val", label:'${currentValue}', defaultState: true
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
	log.debug "Status[${state.entity_id}] >> '${value}'"
    sendEvent(name:"status", value:value)
    
    def now = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
    sendEvent(name: "lastCheckin", value: now)
    sendEvent(name: "entity_id", value: state.entity_id)
}

def setHASetting(url, password, deviceId){
	state.app_url = url
    state.app_pwd = password
    state.entity_id = deviceId
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

def localOn(){
	log.debug "Command [${state.entity_id}] >> on" 
    def temp = state.entity_id.split("\\.")
    def options = [
     	"method": "POST",
        "path": "/api/services/" + temp[0] + "/turn_on",
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

def localOff(){
	log.debug "Command [${state.entity_id}] >> off"
    def temp = state.entity_id.split("\\.")
    def options = [
     	"method": "POST",
        "path": "/api/services/" + temp[0] + "/turn_off",
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