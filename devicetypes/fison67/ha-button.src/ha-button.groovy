/**
 *  HA Button (v.0.0.1)
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
	definition (name: "HA Button", namespace: "fison67", author: "fison67") {
      	capability "Sensor"
        capability "Button"
        capability "Refresh"		
        
        attribute "lastCheckin", "Date"
         
        command "setStatus"
	}

	simulator { }
    
    preferences {
    	input name: "button1", title:"Button #1 Value" , type: "string", required: false, defaultValue: "left"
    	input name: "button2", title:"Button #2 Value" , type: "string", required: false, defaultValue: "right"
    	input name: "button3", title:"Button #3 Value" , type: "string", required: false, defaultValue: "both"
    	input name: "button4", title:"Button #4 Value" , type: "string", required: false, defaultValue: "left_double"
    	input name: "button5", title:"Button #5 Value" , type: "string", required: false, defaultValue: "right_double"
    	input name: "button6", title:"Button #6 Value" , type: "string", required: false, defaultValue: "both_double"
    	input name: "button7", title:"Button #7 Value" , type: "string", required: false, defaultValue: "left_long"
    	input name: "button8", title:"Button #8 Value" , type: "string", required: false, defaultValue: "right_long"
    	input name: "button9", title:"Button #9 Value" , type: "string", required: false, defaultValue: "both_long"
    }

	tiles {
		multiAttributeTile(name:"button", type: "generic", width: 6, height: 4){
			tileAttribute ("device.button", key: "PRIMARY_CONTROL") {
                attributeState "click", label:'Button', icon:"http://postfiles10.naver.net/MjAxODA0MDJfMTYy/MDAxNTIyNjcwOTc1NTE4.h-TVphSLTwUCzXdPnKElZ45Yr4lJLWkL7MF4pt21f5Ig.CmdFO36k5AW8xK08ahvYlWhN3_rk48SJkmknMYVcFycg.PNG.shin4299/buttonAQ_main.png?type=w3", backgroundColor:"#8CB8C9"
			}	
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'\nLast Update: ${currentValue}')
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
   
    def buttonNumber = 1
    switch(value){
    case settings.button1:
    	buttonNumber = 1
    	break
    case settings.button2:
    	buttonNumber = 2
    	break
    case settings.button3:
    	buttonNumber = 3
    	break
    case settings.button4:
    	buttonNumber = 4
    	break
    case settings.button5:
    	buttonNumber = 5
    	break
    case settings.button6:
    	buttonNumber = 6
    	break
    case settings.button7:
    	buttonNumber = 7
    	break
    case settings.button8:
    	buttonNumber = 8
    	break
    case settings.button9:
    	buttonNumber = 9
    	break
    }
    sendEvent(name: "button", value: "pushed", data: [buttonNumber: buttonNumber], descriptionText: "$device.displayName button $buttonNumber was pushed [$value]", isStateChange: true)
    
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
