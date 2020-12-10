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
import base64

import voluptuous as vol

import homeassistant.loader as loader
from homeassistant.const import (STATE_UNKNOWN, EVENT_STATE_CHANGED)
import homeassistant.helpers.config_validation as cv
from homeassistant import config_entries
from homeassistant.helpers.aiohttp_client import async_get_clientsession

from homeassistant.helpers import discovery

_LOGGER = logging.getLogger(__name__)

DOMAIN = "ha_connector"

CONF_APP_URL = 'app_url'
CONF_APP_ID  = 'app_id'
CONF_ACCESS_TOKEN = 'access_token'


def base_config_schema(config: dict = {}) -> dict:
    """Return a shcema configuration dict for HA-Connector."""
    if not config:
        config = {
            CONF_APP_URL: "",
            CONF_APP_ID: "",
            CONF_ACCESS_TOKEN: "xxxxxxxxxxxxxxxxxxxxxxxxxxx",
        }
    return {
        vol.Required(CONF_APP_URL): str,
        vol.Required(CONF_APP_ID): str,
        vol.Required(CONF_ACCESS_TOKEN, default=config.get(CONF_ACCESS_TOKEN)): str,
    }


def config_combined() -> dict:
    """Combine the configuration options."""
    base = base_config_schema()

    return base

CONFIG_SCHEMA = vol.Schema({DOMAIN: config_combined()}, extra=vol.ALLOW_EXTRA)

async def async_setup(hass, config):

    if DOMAIN not in config:
        return True

    app_url      = config[DOMAIN][CONF_APP_URL]
    app_id       = config[DOMAIN][CONF_APP_ID]
    access_token = config[DOMAIN][CONF_ACCESS_TOKEN]

    session = async_get_clientsession(hass)

    registerList = await getRegisteredHADeviceList(session, app_url, app_id, access_token)

    hass.async_create_task(discovery.async_load_platform(hass, "sensor", DOMAIN, {}, config))


    async def event_listener(event):

        newState = event.data['new_state']

        if newState is None or newState.state in (STATE_UNKNOWN, '') or newState.entity_id not in registerList:
            return None

        id  = newState.entity_id
        url = app_url + app_id + "/update?access_token=" + access_token + "&entity_id=" + newState.entity_id + "&value=" + newState.state

        try:
            url += "&unit=" + newState.as_dict()['attributes']['unit_of_measurement']
        except:
            url = url

        try:
            attr = json.dumps(newState.as_dict().get('attributes'))
            attr = base64.b64encode(attr.encode()).decode()
        except:
            attr = ""

        try:
            oldstate = event.data['old_state'].state
        except:
            oldstate = ""

        url += "&attr="+attr+"&old="+oldstate

        response = await session.get(url)


    async def do_refresh(call):
        nonlocal registerList
        registerList = await getRegisteredHADeviceList(session, app_url, app_id, access_token)


    hass.bus.async_listen(EVENT_STATE_CHANGED, event_listener)
    hass.services.async_register(DOMAIN, "refresh", do_refresh)

    return True


async def getRegisteredHADeviceList(session, app_url, app_id, access_token):
    response = await session.get(app_url + app_id + "/getHADevices?access_token=" + access_token)

    return json.loads( await response.text() )['list']


async def async_setup_entry(hass, config_entry):
    """Set up this integration using UI."""

    if hass.data.get(DOMAIN) is not None:
        return False
    if config_entry.source == config_entries.SOURCE_IMPORT:
        hass.async_create_task(hass.config_entries.async_remove(config_entry.entry_id))
        return False

    app_url = config_entry.data[CONF_APP_URL]
    app_id  = config_entry.data[CONF_APP_ID]
    access_token = config_entry.data[CONF_ACCESS_TOKEN]

    session = async_get_clientsession(hass)

    registerList = await getRegisteredHADeviceList(session, app_url, app_id, access_token)

    hass.async_add_job(hass.config_entries.async_forward_entry_setup(config_entry, "sensor"))


    async def event_listener(event):

        newState = event.data['new_state']

        if newState is None or newState.state in (STATE_UNKNOWN, '') or newState.entity_id not in registerList:
            return None

        id  = newState.entity_id
        url = app_url + app_id + "/update?access_token=" + access_token + "&entity_id=" + newState.entity_id + "&value=" + newState.state

        try:
            url += "&unit=" + newState.as_dict()['attributes']['unit_of_measurement']
        except:
            url = url

        try:
            attr = json.dumps(newState.as_dict().get('attributes'))
            attr = base64.b64encode(attr.encode()).decode()
        except:
            attr = ""

        try:
            oldstate = event.data['old_state'].state
        except:
            oldstate = ""

        url += "&attr="+attr+"&old="+oldstate

        response = await session.get(url)


    async def do_refresh(call):
        nonlocal registerList
        registerList = await getRegisteredHADeviceList(session, app_url, app_id, access_token)


    hass.bus.async_listen(EVENT_STATE_CHANGED, event_listener)
    hass.services.async_register(DOMAIN, "refresh", do_refresh)

    return True

