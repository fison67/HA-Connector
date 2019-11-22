/**
 *  HA Presence Sensor (v.0.0.2)
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
	definition (name: "HA Presence Sensor", namespace: "fison67", author: "fison67") {
		capability "Presence Sensor"
      	capability "Sensor"
        capability "Refresh"		
        
        attribute "lastCheckin", "Date"
         
        command "setStatus"
	}


	simulator {
	}
    
    preferences {
        input name: "presentValue", title:"HA Present Value" , type: "string", required: true, defaultValue: "home"
	}

	tiles {
		multiAttributeTile(name:"presence", type: "generic", width: 6, height: 4){
			tileAttribute ("device.presence", key: "PRIMARY_CONTROL") {
               	attributeState "not present", label:'${name}', backgroundColor: "#ffffff", icon:"st.presence.tile.presence-default" 
            	attributeState "present", label:'present', backgroundColor: "#53a7c0", icon:"st.presence.tile.presence-default" 
			}
            
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'\nLast Update: ${currentValue}')
            }
		}
        
        valueTile("lastPresnce_label", "", decoration: "flat") {
            state "default", label:'Last\nIn'
        }
        valueTile("lastPresnce", "device.lastPresnce", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
        }
        valueTile("lastNotPresnce_label", "", decoration: "flat") {
            state "default", label:'Last\nOut'
        }
        valueTile("lastNotPresnce", "device.lastNotPresnce", decoration: "flat", width: 3, height: 1) {
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
    
    def presentBaseValue = "home"
    if(presentValue){
    	presentBaseValue = presentValue
    }
    
    def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    def _value = (presentBaseValue == value ? "present" : "not present")
    
	log.debug "Status[${state.entity_id}] >> ${value} >> ${_value}"
    
    if(device.currentValue("presence") != _value){
        sendEvent(name: (_value == "present" ? "lastPresnce" : "lastNotPresnce"), value: now, displayed: false )
    }
    sendEvent(name: "presence", value:_value)
    sendEvent(name: "lastCheckin", value: now, displayed: false)
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
