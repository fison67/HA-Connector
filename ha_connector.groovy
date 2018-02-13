/**
 *  HA Connector (v.0.0.1)
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
 */
 
import groovy.json.JsonSlurper
import groovy.json.JsonOutput


definition(
    name: "HA Connector",
    namespace: "fison67",
    author: "fison67",
    description: "A Connector between HA and ST",
    category: "My Apps",
    iconUrl: "https://home-assistant.io/demo/favicon-192x192.png",
    iconX2Url: "https://home-assistant.io/demo/favicon-192x192.png",
    iconX3Url: "https://home-assistant.io/demo/favicon-192x192.png",
    oauth: true
)

preferences {
   page(name: "mainPage")
   page(name: "haDevicePage")
   page(name: "haAddDevicePage")
   page(name: "haDeleteDevicePage")
}


def mainPage() {
//    log.debug "Executing mainPage"
    dynamicPage(name: "mainPage", title: "Home Assistant Manage", nextPage: null, uninstall: true, install: true) {
        section("Configure HA API"){
           input "haAddress", "string", title: "HA address", required: true
           input "haPassword", "string", title: "HA Password", required: true
           href "haDevicePage", title: "Get HA Devices", description:""
           href "haAddDevicePage", title: "Add HA Device", description:""
           href "haDeleteDevicePage", title: "Delete HA Device", description:""
       }
       section() {
            paragraph "View this SmartApp's configuration to use it in other places."
            href url:"${apiServerUrl("/api/smartapps/installations/${app.id}/config?access_token=${state.accessToken}")}", style:"embedded", required:false, title:"Config", description:"Tap, select, copy, then click \"Done\""
       }
    }
}

def haDevicePage(){
	log.debug "Executing haDevicePage"
    getDataList()
    
    dynamicPage(name: "haDevicePage", title:"Get HA Devices", refreshInterval:5) {
        section("Please wait for the API to answer, this might take a couple of seconds.") {
            if(state.latestHttpResponse) {
                if(state.latestHttpResponse == 200) {
                    paragraph "Connected \nOK: 200"
                } else {
                    paragraph "Connection error \nHTTP response code: " + state.latestHttpResponse
                }
            }
        }
    }
}

def haAddDevicePage(){
    def addedDNIList = []
	def childDevices = getAllChildDevices()
    childDevices.each {childDevice->
		addedDNIList.push(childDevice.deviceNetworkId)
    }
    
    def list = []
    list.push("None")
    state.dataList.each { 
    	def entity_id = "${it.entity_id}"
    	def friendly_name = "${it.attributes.friendly_name}"
        if(friendly_name == null){
        	friendly_name = ""
        }
       	if(!addedDNIList.contains("ha-connector-" + entity_id)){
        	if(entity_id.contains("light.") || entity_id.contains("switch.") || entity_id.contains("fan.") || entity_id.contains("sensor.")){
        		list.push("${friendly_name} [ ${entity_id} ]")
            }
        }
    }
   
    dynamicPage(name: "haAddDevicePage", nextPage: "mainPage") {
        section ("Add HA Devices") {
            input(name: "selectedAddHADevice", title:"Select" , type: "enum", required: true, options: list, defaultValue: "None")
		}
	}

}

def haDeleteDevicePage(){
	log.debug "Executing Delete Page"
    
    def list = []
    list.push("None")
	def childDevices = getAllChildDevices()
    childDevices.each {childDevice->
		list.push(childDevice.label + " -> " + childDevice.deviceNetworkId)
    }
    dynamicPage(name: "haDeleteDevicePage", nextPage: "mainPage") {
        section ("Delete HA Device") {
            input(name: "selectedDeleteHADevice", title:"Select" , type: "enum", required: true, options: list, defaultValue: "None")
		}
	}
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
    
    if (!state.accessToken) {
        createAccessToken()
    }
    
    app.updateSetting("selectedAddHADevice", "None")
    app.updateSetting("selectedDeleteHADevice", "None")
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    // Unsubscribe from all events
    unsubscribe()
    // Subscribe to stuff
    initialize()
    
    app.updateSetting("selectedAddHADevice", "None")
    app.updateSetting("selectedDeleteHADevice", "None")
}

// Return list of displayNames
def getDeviceNames(devices) {
    def list = []
    devices.each{device->
        list.push(device.displayName)
    }
    list
}

def getHADeviceByEntityId(entity_id){
	def target
	state.dataList.each {haDevice -> 
         if(haDevice.entity_id == entity_id){
         	target = haDevice
         }
	}
    target
}

def addHAChildDevice(){
	String[] dth1_list = ["active", "inactive", "open", "closed", "dry", "wet", "clear", "detected", "not present", "present", "on", "off"]
    if(settings.selectedAddHADevice){
        if(settings.selectedAddHADevice != "None"){
            log.debug "ADD >> " + settings.selectedAddHADevice

            def tmp = settings.selectedAddHADevice.split(" \\[ ")
            def tmp2 = tmp[1].split(" \\]")
            def entity_id = tmp2[0]
            def dni = "ha-connector-" + entity_id
            def haDevice = getHADeviceByEntityId(entity_id)
            if(haDevice){
                def dth = "HA DTH2"
                def name = haDevice.attributes.friendly_name
                if(!name){
                    name = entity_id
                }
                for (String element:dth1_list ) {
                    if ( element.equals(haDevice.state) || element == haDevice.state) {
                        dth = "HA DTH1"
                        break
                    }
                }
                def childDevice = addChildDevice("fison67", dth, dni, location.hubs[0].id, [
                    "label": name
                ])

                childDevice.setHASetting(settings.haAddress, settings.haPassword, entity_id)
                childDevice.setStatus(haDevice.state)
                if(haDevice.attributes.unit_of_measurement){
                    childDevice.setUnitOfMeasurement(haDevice.attributes.unit_of_measurement)
                }
                childDevice.refresh()
            }
        }
	}        
}

def deleteChildDevice(){
	if(settings.selectedDeleteHADevice){
    	if(settings.selectedDeleteHADevice != "None"){
            log.debug "DELETE >> " + settings.selectedDeleteHADevice
            def nameAndDni = settings.selectedDeleteHADevice.split(" -> ")
            try{
                deleteChildDevice(nameAndDni[1])
            }catch(err){}
     	}       
    }
}

def initialize() {
	log.debug "initialize"

	deleteChildDevice()
    addHAChildDevice()

}

def dataCallback(physicalgraph.device.HubResponse hubResponse) {
    def msg, json, status
    try {
        msg = parseLanMessage(hubResponse.description)
        status = msg.status
        json = msg.json
        state.dataList = json
    	state.latestHttpResponse = status
    } catch (e) {
        logger('warn', "Exception caught while parsing data: "+e);
    }
}

def getDataList(){
    def options = [
     	"method": "GET",
        "path": "/api/states",
        "headers": [
        	"HOST": settings.haAddress,
            "x-ha-access": settings.haPassword,
            "Content-Type": "application/json"
        ]
    ]
    
    def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: dataCallback])
    sendHubCommand(myhubAction)
}

def deviceCommandList(device) {
  	device.supportedCommands.collectEntries { command->
    	[
      		(command.name): (command.arguments)
    	]
  	}
}

def deviceAttributeList(device) {
  	device.supportedAttributes.collectEntries { attribute->
    	try {
      		[
        		(attribute.name): device.currentValue(attribute.name)
      		]
    	} catch(e) {
      		[
        		(attribute.name): null
      		]
    	}
  	}
}

def updateDevice(){
	def dni = "ha-connector-" + params.entity_id 
    try{
    	def device = getChildDevice(dni)
        if(device){
        	log.debug "HA -> ST >> [${dni} : ${params.value}]"
            device.setStatus(params.value)
            if(params.unit){
            	device.setUnitOfMeasurement(params.unit)
            }
     	}
    }catch(err){
        log.error "${err}"
    }
	
	 def deviceJson = new groovy.json.JsonOutput().toJson([result: true])
	 render contentType: "application/json", data: deviceJson  
}

def authError() {
    [error: "Permission denied"]
}

def renderConfig() {
    def configJson = new groovy.json.JsonOutput().toJson([
        description: "HA Connector API",
        platforms: [
            [
                platform: "SmartThings HA Connector",
                name: "HA Connector",
                app_url: apiServerUrl("/api/smartapps/installations/"),
                app_id: app.id,
                access_token:  state.accessToken
            ]
        ],
    ])

    def configString = new groovy.json.JsonOutput().prettyPrint(configJson)
    render contentType: "text/plain", data: configString
}

mappings {
    if (!params.access_token || (params.access_token && params.access_token != state.accessToken)) {
        path("/config")                         { action: [GET: "authError"] }
        path("/update")                         { action: [GET: "authError"]  }

    } else {
        path("/config")                         { action: [GET: "renderConfig"]  }
        path("/update")                         { action: [GET: "updateDevice"]  }
    }
}