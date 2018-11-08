"""
HA Connector
Copyright (c) 2018 fison67 <fison67@nate.com>
Licensed under MIT
following to your configuration.yaml file.
ha_connector2:
  app_url: xxx
  app_id: xxx
  access_token: xxxxxx
"""
import logging
import yaml
import os
import re

import json
import requests
import time

from collections import OrderedDict
import collections

import homeassistant.util.yaml as util_yaml
from homeassistant.config import YAML_CONFIG_FILE, load_yaml_config_file

DOMAIN = 'ha_connector2'

removeSpeacilChar = '[-=\[\]\(\)\s.#/?:$}]'

class Thing(str):
    pass

def thing_constructor(self, node):
    name = node.value
    return Thing("file__" + name)

def loadYaml(path):
    stream = open(path, "r")
    return yaml.safe_load(stream)

def dump(_dict: dict) -> str:
    """Dump YAML to a string and remove null."""
    return yaml.safe_dump(
        _dict, default_flow_style=False, allow_unicode=True) #\
#        .replace(': null\n', ':\n')

def save_yaml_from_json(path, data):
    """Save YAML to a file."""
    # Dump before writing to not truncate the file if dumping fails
    yaml.dump(data, path, default_flow_style=False)

def save_yaml(path, data):
    """Save YAML to a file."""
    # Dump before writing to not truncate the file if dumping fails
    data = dump(data)
    with open(path, 'w', encoding='utf-8') as outfile:
        outfile.write(data)

def getSTDevice(config):
    app_url = config[DOMAIN].get('app_url')
    app_id = config[DOMAIN].get('app_id')
    access_token = config[DOMAIN].get('access_token')

    url = app_url + app_id + "/getSTDevices?access_token=" + access_token 
    response = requests.get(url);
    return json.loads(response.text)

def removeNonUseCustomize(path, st_device_list):
    tempDoc = util_yaml.load_yaml(path)
    removeKey = []
    for key, value in tempDoc.items():
       if key[0:10] == "switch.st_" or key[0:10] == "sensor.st_":
          exist = False
          for item in st_device_list:
             _name = key[10:].lower().replace(' ', '_')
             dni = re.sub(removeSpeacilChar, '_', item['id'].lower() + "_" + item['dni'].lower())
             if dni == _name:
                exist = True
                break

          if exist == False:
             removeKey.append(key)

    for key in removeKey:
       tempDoc.pop(key)
    if len(removeKey) > 0:
       logging.error("Updated >> " + path)
       save_yaml(path, tempDoc)
       return True

    return False

def removeNonUseConfig(path, type, st_device_list):
    tempDoc = util_yaml.load_yaml(path)
    removeKey = []
    for data in tempDoc:
       item = dict(data)
       try:
          if item['name'][0:3] == 'st_' and type == item['platform']:
             exist = False
             for stDevice in st_device_list:
                name = item['name']
                _name = "st_" + re.sub(removeSpeacilChar, '_', stDevice['id'].lower() + "_" + stDevice['dni'].lower())
                if name == _name:
                   exist = True
                   break
             if exist == False:
                removeKey.append(data)

       except:
          continue

    for key in removeKey:
       tempDoc.remove(key)
    if len(removeKey) > 0:
       logging.error("Updated >> " + path)
       save_yaml(path, tempDoc)
       return True

    return False

def setup(hass, config):
    st_device_list = getSTDevice(config)

    app_url = config[DOMAIN].get('app_url')
    app_id = config[DOMAIN].get('app_id')
    access_token = config[DOMAIN].get('access_token')
    url = app_url + app_id + "/get?access_token=" + access_token

    configPath = hass.config.path(YAML_CONFIG_FILE)
    stream = open(configPath, "r")
    yaml.SafeLoader.add_constructor('!include', thing_constructor)

    docs = yaml.safe_load(stream)

    list = ["fan", "light", "switch"]

    isNeedToReboot = False

    customizePath = getCustomizePath(docs)
    if customizePath != "":
       customizePath = hass.config.path(customizePath)

    for item in st_device_list:
       file_path = findPath(hass.config, docs, item['type'])
       if file_path:
          exist = isExistDevice(file_path, item)
          if exist == False:
             target = None
             subDoc = util_yaml.load_yaml(file_path)
             target = None
             name = "st_" + re.sub(removeSpeacilChar, '_', (item['id'].lower() + "_" + item['dni'].lower()))
             if item['type'] == "sensor":
                target = OrderedDict([("platform","stsensor"),("name",name),("resource",url + "&dni=" + item['dni']),("value_template","{{ value_json.state }}"),("json_attributes",item['attr'])])
             elif item['type'] == "switch":
                target = OrderedDict([("platform","stswitch"),("name",name),("resource",url + "&dni=" + item['dni']),("res_on", (url + "&dni=" + item['dni'] + "&turn=on")),("res_off", (url + "&dni=" + item['dni'] + "&turn=off")),("json_attributes",item['attr'])])
             if target:
                count = 0
                for __item in subDoc:
                   count += 1

                if count > 0:
                   logging.error("Added ST Device >> " + item['id'] + ", DeviceNetworkId=" + item['dni'])
                   isNeedToReboot = True
                   subDoc.append(target)
                   save_yaml(file_path, subDoc)
                else:
                   dictlist = [target for x in range(1)]
                   save_yaml(file_path, dictlist)

                if customizePath != "":
                   customizeDoc = util_yaml.load_yaml(customizePath)
                   tempName = item['type'] + "." + name
                   if isExistName(tempName, customizeDoc) == False:
                       customizeDoc.update( {tempName:{'friendly_name':item['name']}} )
                       save_yaml(customizePath, customizeDoc)


    if customizePath != "":
       modified = removeNonUseCustomize(customizePath, st_device_list)
       if modified == True:
          isNeedToReboot = True

    switchConfigPath = getIncludeFilePath(hass.config, docs, "switch")
    if switchConfigPath != "":
       modified = removeNonUseConfig(switchConfigPath, "stswitch", st_device_list)
       if modified == True:
          isNeedToReboot = True

    sensorConfigPath = getIncludeFilePath(hass.config, docs, "sensor")
    if sensorConfigPath != "":
       modified = removeNonUseConfig(sensorConfigPath, "stsensor", st_device_list)
       if modified == True:
          isNeedToReboot = True

    if isNeedToReboot:
       logging.error("You must restart HA!!!")

    return True


def findPath(config, docs, type):
   for key, value in docs.items():
      try:
         isFile = docs[key].startswith("file__")
         path = config.path(value[6:])
         if key == "sensor" and type == "sensor":
            return path
         elif key == "switch" and type == "switch":
            return path
      except:
         continue
   return ""

def isExistDevice(path, stData):
   deviceName = "st_" + re.sub(removeSpeacilChar, '_', stData['id'].lower() + "_" + stData['dni'].lower())
   type = stData['type']
   docs = util_yaml.load_yaml(path)
   for item in docs:
       for key, value in item.items():
          if key == "name" and value == deviceName:
             return True

   return False

def isExistName(name, docs):
   for key, value in docs.items():
      if key == name:
         return True
   return False

def getCustomizePath(docs):
   for key, value in docs.items():
      if "homeassistant" == key:
         item = dict(value)
         customize_file = item['customize']
         return (customize_file[6:])
   return ""

def getIncludeFilePath(config, docs, type):
   for key, value in docs.items():
      if key == type:
         return config.path(value[6:])


def ordered_dict_prepend(dct, key, value, dict_setitem=dict.__setitem__):
    root = dct._OrderedDict__root
    first = root[1]

    if key in dct:
        link = dct._OrderedDict__map[key]
        link_prev, link_next, _ = link
        link_prev[1] = link_next
        link_next[0] = link_prev
        link[0] = root
        link[1] = first
        root[1] = first[0] = link
    else:
        root[1] = first[0] = dct._OrderedDict__map[key] = [root, first, key]
        dict_setitem(dct, key, value)
