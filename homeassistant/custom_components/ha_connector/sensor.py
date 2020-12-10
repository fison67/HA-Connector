"""Sensor platform for HA-Connector."""
from homeassistant.helpers.entity import Entity
from homeassistant.core import callback


DOMAIN     = 'ha_connector'
NAME_SHORT = 'HA-Connector'
VERSION    = '1.0.0'

CONF_APP_URL = 'app_url'
CONF_APP_ID  = 'app_id'
CONF_ACCESS_TOKEN = 'access_token'

async def async_setup_platform(_hass, _config, async_add_entities, _discovery_info=None):
    """Setup sensor platform."""

    id = _config[DOMAIN][CONF_APP_ID]

    async_add_entities([HASensor(id)])


async def async_setup_entry(_hass, _config_entry, async_add_devices):
    """Setup sensor platform."""
    id   = _config_entry.data[CONF_APP_ID]

    async_add_devices([HASensor(id)])


class HADevice(Entity):
    """HA-Connector Device class."""

    @property
    def device_info(self):
        """Return device information about HA-Connector."""
        return {
            "identifiers": {(DOMAIN, self.unique_id)},
            "name": NAME_SHORT,
            "manufacturer": "fison67/HA-Connector",
            "model": "HA-Connector",
            "sw_version": VERSION,
            "entry_type": "service",
        }


class HASensor(HADevice):
    """HACS Sensor class."""

    def __init__(self, id):
        """Initialize."""
        self._state = 'Love'
        self._id    = id

    @property
    def should_poll(self):
        """No polling needed."""
        return False

    async def async_update(self):
        """Manual updates of the sensor."""
        self._update()

    @callback
    def _update_and_write_state(self, *_):
        """Update the sensor and write state."""
        self._update()
        self.async_write_ha_state()

    @callback
    def _update(self):
        """Update the sensor."""
        self._state = 'Love'

    @property
    def unique_id(self):
        """Return a unique ID to use for this sensor."""
        return ( "sensor.ha_connector" )

    @property
    def name(self):
        """Return the name of the sensor."""
        return "HA Connector"

    @property
    def state(self):
        """Return the state of the sensor."""
        return self._state

    @property
    def icon(self):
        """Return the icon of the sensor."""
        return "mdi:home-assistant"

    @property
    def unit_of_measurement(self):
        """Return the unit of measurement."""
        return ""

    @property
    def device_state_attributes(self):
        """Return attributes for the sensor."""
        data = {}

        data[CONF_APP_ID] = self._id

        return data

    async def async_added_to_hass(self) -> None:
        """Register for status events."""
        self.async_on_remove(
            self.hass.bus.async_listen("ha_connector/status", self._update_and_write_state)
        )

