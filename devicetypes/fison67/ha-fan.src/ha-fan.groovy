/**
 *  HA Fan (v.0.0.1)
 *
 *  Authors
 *   - fison67@nate.com
 *  Copyright 2020
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
	definition (name: "HA Fan", namespace: "fison67", author: "fison67", vid: "generic-switch", ocfDeviceType: "oic.d.fan") {
        capability "Switch"						
		capability "Fan Speed"
        capability "Switch Level"
		capability "Refresh"
         
        attribute "lastCheckin", "Date"
         
	}


	simulator { }
	preferences { }

	tiles {
		multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'\n${name}', action:"switch.off", icon:"https://postfiles.pstatic.net/MjAxODAzMjlfNjIg/MDAxNTIyMzIzNDI2NjQ2.cPAScBLV_hQaqFRkRqjImmaqyFmY7FY23A23k-t8RZ4g.ORO7eIOdaPHIJwR3tMXLLvU741B6NrncFi2a29ZDWbwg.PNG.shin4299/Fan_tile_on.png?type=w580", backgroundColor:"#73C1EC", nextState:"turningOff"
                attributeState "off", label:'\n${name}', action:"switch.on", icon:"https://postfiles.pstatic.net/MjAxODAzMjlfNjkg/MDAxNTIyMzIzNDI2NjQ4.b5E7CPu8ljgF_eHdHFDmK7wLHQG6iymo2DErBeN2u3Ug.61d9mZ5QYaP-oUoIPnXaHA_rocGnrRxBArjSbjctQGwg.PNG.shin4299/Fan_tile_off.png?type=w580", backgroundColor:"#ffffff", nextState:"turningOn"
                
                attributeState "turningOn", label:'\n${name}', action:"switch.off", icon:"https://postfiles.pstatic.net/MjAxODAzMjlfNjkg/MDAxNTIyMzIzNDI2NjQ4.b5E7CPu8ljgF_eHdHFDmK7wLHQG6iymo2DErBeN2u3Ug.61d9mZ5QYaP-oUoIPnXaHA_rocGnrRxBArjSbjctQGwg.PNG.shin4299/Fan_tile_off.png?type=w580", backgroundColor:"#73C1EC", nextState:"turningOff"
                attributeState "turningOff", label:'\n${name}', action:"switch.on", icon:"https://postfiles.pstatic.net/MjAxODAzMjlfNjIg/MDAxNTIyMzIzNDI2NjQ2.cPAScBLV_hQaqFRkRqjImmaqyFmY7FY23A23k-t8RZ4g.ORO7eIOdaPHIJwR3tMXLLvU741B6NrncFi2a29ZDWbwg.PNG.shin4299/Fan_tile_on.png?type=w580", backgroundColor:"#ffffff", nextState:"turningOn"
			}
            
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"switch level.setLevel"
            }            
            
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
   				attributeState("default", label:'${currentValue}')
          	}

		}
      

	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def setHASetting(url, password, deviceId){
	state.app_url = url
    state.app_pwd = password
    state.entity_id = deviceId
    state.hasSetStatusMap = true
    
    sendEvent(name: "ha_url", value: state.app_url, displayed: false)
}

def setStatusMap(map){
	log.debug map
 	setStatus(map.state)
    if(map.state["speed"] != null){
    	def level = 10
        if(map.attr["speed"] == "low"){
        	level = 32
        }else if(map.attr["speed"] == "medium"){
        	level = 66
        }else{
        	level = 100
        }
    	sendEvent(name: "level", value:level)
    }
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
    sendEvent(name: "lastCheckin", value: now, displayed: false)
    sendEvent(name: "entity_id", value: state.entity_id, displayed: false)
}

def refresh(){
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
	def speed = "low"
    if(0 <= level && level < 33){
    	speed = "low"
    }else if(33 <= level && level < 66){
    	speed = "medium"
    }else{
    	speed = "high"
    }
	processCommand("set_speed", [ "entity_id": state.entity_id, "speed":speed ])
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

def updated() {}

def sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}
