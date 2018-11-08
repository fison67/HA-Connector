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

import homeassistant.loader as loader
from homeassistant.const import (STATE_UNKNOWN, EVENT_STATE_CHANGED)
from homeassistant.remote import JSONEncoder

DOMAIN = "ha_connector"

def setup(hass, config):
    app_url = config[DOMAIN].get('app_url')
    app_id = config[DOMAIN].get('app_id')
    access_token = config[DOMAIN].get('access_token')

    def event_listener(event):

        state = event.data.get('new_state')
        if state is None or state.state in (STATE_UNKNOWN, ''):
            return None

        jsonData = {};
        newState = event.data['new_state'];
        if newState is None:
          return;

#        oldState = event.data['old_state'];
#        if oldState is None:
#          return;

        url = app_url + app_id + "/update?access_token=" + access_token + "&entity_id=" + newState.entity_id + "&value=" + newState.state;
        try:
           if newState.attributes.unit_of_measurement:
              url += "&unit=" + newState.attributes.unit_of_measurement
        except:
           url = url

        response = requests.get(url);

    hass.bus.listen(EVENT_STATE_CHANGED, event_listener)

    return True
