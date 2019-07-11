"""
Support for ST switches(HA Connector).
Copyright (c) 2018 fison67 <fison67@nate.com>
Licensed under MIT
"""
import asyncio
import logging
import json

import aiohttp
import async_timeout
import voluptuous as vol

from homeassistant.components.switch import (SwitchDevice, PLATFORM_SCHEMA)
from homeassistant.const import (
    CONF_NAME, CONF_RESOURCE, CONF_TIMEOUT, CONF_METHOD)
from homeassistant.helpers.aiohttp_client import async_get_clientsession
import homeassistant.helpers.config_validation as cv
from homeassistant.helpers.template import Template

_LOGGER = logging.getLogger(__name__)

DEFAULT_NAME = 'ST Switch'
DEFAULT_TIMEOUT = 10

CONF_JSON_ATTRS = 'json_attributes'

PLATFORM_SCHEMA = PLATFORM_SCHEMA.extend({
    vol.Required(CONF_RESOURCE): cv.url,
    vol.Optional(CONF_NAME, default=DEFAULT_NAME): cv.string,
    vol.Optional(CONF_JSON_ATTRS, default=[]): cv.ensure_list_csv,
    vol.Optional(CONF_TIMEOUT, default=DEFAULT_TIMEOUT): cv.positive_int,
})


# pylint: disable=unused-argument
@asyncio.coroutine
def async_setup_platform(hass, config, async_add_devices, discovery_info=None):
    """Set up the RESTful switch."""
    method = config.get(CONF_METHOD)
    name = config.get(CONF_NAME)
    resource = config.get(CONF_RESOURCE)
    timeout = config.get(CONF_TIMEOUT)
    json_attrs = config.get(CONF_JSON_ATTRS)

    try:
        switch = STSwitch(name, resource, method, timeout, json_attrs, hass)

        req = yield from switch.get_device_state(hass)
        if req.status >= 400:
            _LOGGER.error("Got non-ok response from resource: %s", req.status)
        else:
            async_add_devices([switch])
    except (TypeError, ValueError):
        _LOGGER.error("Missing resource or schema in configuration. "
                      "Add http:// or https:// to your URL")
    except (asyncio.TimeoutError, aiohttp.ClientError):
        _LOGGER.error("No route to resource/endpoint: %s", resource)


class STSwitch(SwitchDevice):
    """Representation of a switch that can be toggled using REST."""

    def __init__(self, name, resource, method, timeout, json_attrs, hass):
        """Initialize the REST switch."""
        self._state = None
        self._name = name
        self._resource = resource
        self._method = method
        self._res_on = resource + "&turn=on"
        self._res_off = resource + "&turn=off"
        self._timeout = timeout
        self._json_attrs = json_attrs

    @property
    def name(self):
        """Return the name of the switch."""
        return self._name

    @property
    def is_on(self):
        """Return true if device is on."""
        return self._state

    @property
    def device_state_attributes(self):
        """Return the state attributes."""
        try:
           if self._attributes == None:
              self._attributes = {}
        except:
           self._attributes = {}
        return self._attributes

    @asyncio.coroutine
    def async_turn_on(self):
        """Turn the device on."""
        websession = async_get_clientsession(self.hass)
        with async_timeout.timeout(self._timeout, loop=self.hass.loop):
            req = yield from websession.post(self._res_on)
            text = yield from req.text()

    @asyncio.coroutine
    def async_turn_off(self):
        """Turn the device off."""
        websession = async_get_clientsession(self.hass)
        with async_timeout.timeout(self._timeout, loop=self.hass.loop):
            req = yield from websession.post(self._res_off)
            text = yield from req.text()

    @asyncio.coroutine
    def set_device_state(self, body):
        """Send a state update to the device."""
        websession = async_get_clientsession(self.hass)
        with async_timeout.timeout(self._timeout, loop=self.hass.loop):
            req = yield from getattr(websession, self._method)(
                self._resource, auth=self._auth, data=bytes(body, 'utf-8'))
            return req

    @asyncio.coroutine
    def async_update(self):
        """Get the current state, catching errors."""
        try:
            yield from self.get_device_state(self.hass)
        except (asyncio.TimeoutError, aiohttp.ClientError):
            _LOGGER.exception("Error while fetch data.")

    @asyncio.coroutine
    def get_device_state(self, hass):
        """Get the latest data from REST API and update the state."""
        websession = async_get_clientsession(hass)

        with async_timeout.timeout(self._timeout, loop=hass.loop):
            req = yield from websession.get(self._resource)
            text = yield from req.text()

        jsonObj = json.loads(text)

        if self._json_attrs:
            self._attributes = {}
            try:
                if isinstance(jsonObj, dict):
                    attrs = {k: jsonObj['attributes'][k] for k in self._json_attrs
                             if k in jsonObj['attributes']}
                    self._attributes = attrs
                else:
                    _LOGGER.warning("JSON result was not a dictionary")
            except ValueError:
                _LOGGER.warning("REST result could not be parsed as JSON")
           #     _LOGGER.debug("Erroneous JSON: %s", value)


        if jsonObj['state'] == 'on':
            self._state = True
        elif jsonObj['state'] == 'off':
            self._state = False
        else:
            self._state = None

        return req
