"""
HA Connector
Copyright (c) 2018 fison67 <fison67@nate.com>
Licensed under MIT
following to your configuration.yaml file.
ha_connector:
  app_url: xxx
  app_id: xxx
  access_token: xxxxxx
"""
import requests
import logging
import json

import homeassistant.loader as loader
from homeassistant.const import (STATE_UNKNOWN, EVENT_STATE_CHANGED)
#from homeassistant.remote import JSONEncoder

DOMAIN = "ha_connector"

def setup(hass, config):
    app_url = config[DOMAIN].get('app_url')
    app_id = config[DOMAIN].get('app_id')
    access_token = config[DOMAIN].get('access_token')
    registerList = getRegisteredHADeviceList(app_url, app_id, access_token)
    #_memory = {}

    def event_listener(event):

        newState = event.data['new_state']
        id = newState.entity_id
        if newState is None or newState.state in (STATE_UNKNOWN, '') or id not in registerList:
            return None
        '''
        lastUpdateTime = newState.last_changed.timestamp()
        if id in _memory:
            if _memory[id] == lastUpdateTime:
                    return None

        _memory[id] = lastUpdateTime
        '''
        url = app_url + app_id + "/update?access_token=" + access_token + "&entity_id=" + newState.entity_id + "&value=" + newState.state;
        try:
           if newState.attributes.unit_of_measurement:
              url += "&unit=" + newState.attributes.unit_of_measurement
        except:
           url = url

        try:
           attr = json.dumps(newState.as_dict().get('attributes'))
           url += "&attr="+base64.b64encode(attr.encode()).decode()
        except:
           attr = ""

        response = requests.get(url)


    hass.bus.listen(EVENT_STATE_CHANGED, event_listener)

    return True


def getRegisteredHADeviceList(app_url, app_id, access_token):
    response = requests.get(app_url + app_id + "/getHADevices?access_token=" + access_token)
    return json.loads(response.text)['list']
