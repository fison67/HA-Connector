/**
 *  HA Motion Sensor (v.0.0.1)
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
	definition (name: "HA Motion Sensor", namespace: "fison67", author: "fison67") {
		capability "Motion Sensor"
      	capability "Sensor"
        capability "Refresh"		
        
        attribute "lastCheckin", "Date"
         
        command "setStatus"
	}


	simulator {
	}
    
    preferences {
        input name: "motionValue", title:"HA Motion Value" , type: "string", required: true, defaultValue: "on"
	}

	tiles {
		multiAttributeTile(name:"motion", type: "generic", width: 6, height: 4){
			tileAttribute ("device.motion", key: "PRIMARY_CONTROL") {
				attributeState "active", label:'motion', icon:"http://postfiles1.naver.net/MjAxODA0MDNfMTAz/MDAxNTIyNzI0MDQ3OTU1.KlL6RhQNyk29a6B2xLdYi8f7mWkZ_hDJmvLTcUYxFUog.zOxJRz6RrrZsUTkFj8BefZycoyKxoL0Eeq7Ep6Pdxw0g.PNG.shin4299/motion_on1.png?type=w3", backgroundColor:"#00a0dc"
				attributeState "inactive", label:'no motion', icon:"http://postfiles5.naver.net/MjAxODA0MDNfMTky/MDAxNTIyNzIzMDU3MTM4.mWDrfCVxx5OgUmoCZos7CkVgVY8jm3Ho4WgWeFnMbhMg.MB0MzqQCJM80xAFZ19imwE9AnHQ58Px2gHAOr9DSJLQg.PNG.shin4299/motion_off.png?type=w3", backgroundColor:"#ffffff"
			}	
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'\nLast Update: ${currentValue}')
            }
		}
        
        valueTile("lastMotion_label", "", decoration: "flat") {
            state "default", label:'Last\nMotion'
        }
        valueTile("lastMotion", "device.lastMotion", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
            state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
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
    
    def motionBaseValue = "on"
    if(motionValue){
    	motionBaseValue = motionValue
    }
    
    def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    def _value = (motionBaseValue == value ? "active" : "inactive")
    
	log.debug "Status[${state.entity_id}] >> ${value} >> ${_value}"
    
    if(device.currentValue("motion") != _value && _value == "active"){
        sendEvent(name: "lastMotion", value: now, displayed: false )
    }
    sendEvent(name: "motion", value:_value)
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
