/**
 *  HA Default Sensor (v.0.0.1)
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
	definition (name: "HA Default Sensor", namespace: "fison67", author: "fison67") {
      	capability "Sensor"
        capability "Refresh"		
        
        attribute "lastCheckin", "Date"
		attribute "status", "string"
         
        command "setStatus"
	}


	simulator {
	}
    
    preferences {
        input name: "dataType", title:"HA Data Type" , type: "enum", required: true, defaultValue: "string", options:["string", "double", "int", "float"]
	}

	tiles {
		multiAttributeTile(name:"status", type:"generic", width:6, height:4) {
            tileAttribute("device.status", key: "PRIMARY_CONTROL") {
				attributeState "val", label:'${currentValue}'
            }
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'Updated: ${currentValue}\n')
            }
        }   
        
        valueTile("tmp_label", "", decoration: "flat", width: 4, height: 2) {
            state "default", label:""
        }
        
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
        }
        
        valueTile("ha_url", "device.ha_url", width: 3, height: 1) {
            state "val", label:'${currentValue}', defaultState: true
        }
        
        valueTile("entity_id", "device.entity_id", width: 3, height: 1) {
            state "val", label:'${currentValue}', defaultState: true
        }
        
        main (["status"])
      	details(["status", "tmp_label", "refresh", "ha_url", "entity_id"])
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
    
    def _value = value
    switch(dataType){
    case "int":
    	_value = value as int
    	break
    case "double":
    	_value = value as double
    	break
    case "float":
    	_value = value as float
    	break
    }
    
    log.debug _value
    
    sendEvent(name: "status", value: _value )
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
        	"HOST": state.app_url,
            "x-ha-access": state.app_pwd,
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
