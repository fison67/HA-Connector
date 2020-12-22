"""Config flow for K-Weather."""
import logging

import voluptuous as vol

from homeassistant import config_entries
from homeassistant.core import callback
from homeassistant.const import (CONF_SCAN_INTERVAL)

DOMAIN = 'ha_connector'

CONF_APP_URL    = 'app_url'
CONF_APP_ID     = 'app_id'
CONF_ACCESS_TOKEN = 'access_token'

_LOGGER = logging.getLogger(__name__)

class HAConnectorConfigFlow(config_entries.ConfigFlow, domain=DOMAIN):
    """Handle a config flow for HA-Connector."""

    VERSION = 1
    CONNECTION_CLASS = config_entries.CONN_CLASS_CLOUD_POLL

    def __init__(self):
        """Initialize flow."""
        self._app_url: Required[str] = None
        self._app_id: Required[str] = None
        self._access_token: Required[str] = None

    async def async_step_user(self, user_input=None):
        """Handle the initial step."""
        errors = {}

        if user_input is not None:
            self._app_url         = user_input[CONF_APP_URL]
            self._app_id          = user_input[CONF_APP_ID]
            self._access_token    = user_input[CONF_ACCESS_TOKEN]

            return self.async_create_entry(title=DOMAIN, data=user_input)

        if self._async_current_entries():
            return self.async_abort(reason="single_instance_allowed")

        if user_input is None:
            return self._show_user_form(errors)

        #return self.async_create_entry(title=DOMAIN, data=user_input)

    async def async_step_import(self, import_info):
        """Handle import from config file."""
        return await self.async_step_user(import_info)

    @callback
    def _show_user_form(self, errors=None):
        schema = vol.Schema(
            {
                vol.Required(CONF_APP_URL, default=None): str,
                vol.Required(CONF_APP_ID,  default=None): str,
                vol.Required(CONF_ACCESS_TOKEN, default=None): str,
            }
        )

        return self.async_show_form(
            step_id="user", data_schema=schema, errors=errors or {}
        )
