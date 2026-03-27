import ee
import urllib.request
import json
import os
try:
    ee.Initialize()
except:
    pass

img = ee.Image('COPERNICUS/S2_SR_HARMONIZED/20210101T103341_20210101T103554_T31TCJ')
params = {
    'scale': 300,
    'crs': 'EPSG:4326',
    'filePerBand': False,
    'format': 'GEO_TIFF'
}
url = img.getDownloadURL(params)
print("URL:", url)
