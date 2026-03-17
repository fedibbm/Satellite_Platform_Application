const fs = require('fs');

const filePath = 'src/components/Workflow/NodeConfigPanel.tsx';
let content = fs.readFileSync(filePath, 'utf8');

const searchRegex = /<div>[\s\n]*<label className="block text-sm font-medium text-gray-700 mb-1">[\s\n]*Collection ID[\s\n]*<\/label>[\s\n]*<input(.*?)>[\s\n]*<p className="text-xs text-gray-500 mt-1">Common datasets: COPERNICUS\/S2_SR_HARMONIZED \(Sentinel-2\)<\/p>[\s\n]*<\/div>/s;

const newCollectionInput = `<div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Collection ID
                  </label>
                  <select
                    value={config.collection_id || 'COPERNICUS/S2_SR_HARMONIZED'}
                    onChange={(e) => updateConfig('collection_id', e.target.value)}
                    className="w-full border border-gray-300 rounded px-3 py-2"
                  >
                    <option value="COPERNICUS/S2_SR_HARMONIZED">Sentinel-2 Surface Reflectance (Harmonized)</option>
                    <option value="COPERNICUS/S2_HARMONIZED">Sentinel-2 Top of Atmosphere (Harmonized)</option>
                    <option value="LANDSAT/LC08/C02/T1_L2">Landsat 8 Surface Reflectance</option>
                    <option value="LANDSAT/LE07/C02/T1_L2">Landsat 7 Surface Reflectance</option>
                    <option value="MODIS/061/MOD09GA">MODIS Surface Reflectance (Daily)</option>
                    <option value="USGS/SRTMGL1_003">SRTM Digital Elevation Data (30m)</option>
                  </select>
                </div>`;

// Fallback search in case the script didn't write exactly the block above or the regex misses.
const fullDivSearch = `<div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Collection ID
                  </label>
                  <input
                    type="text"
                    value={config.collection_id || ''}
                    onChange={(e) => updateConfig('collection_id', e.target.value)}
                    placeholder="e.g., COPERNICUS/S2_SR_HARMONIZED"
                    className="w-full border border-gray-300 rounded px-3 py-2"
                  />
                  <p className="text-xs text-gray-500 mt-1">Common datasets: COPERNICUS/S2_SR_HARMONIZED (Sentinel-2)</p>
                </div>`;
                
if (content.includes(fullDivSearch)) {
  content = content.replace(fullDivSearch, newCollectionInput);
}

// Second fallback
const fullDivSearch2 = `<div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Collection ID
                  </label>
                  <input
                    type="text"
                    value={config.collection_id || ''}
                    onChange={(e) => updateConfig('collection_id', e.target.value)}
                    placeholder="e.g., COPERNICUS/S2_SR_HARMONIZED"
                    className="w-full border border-gray-300 rounded px-3 py-2"
                  />
                </div>`;
                
if (content.includes(fullDivSearch2)) {
  content = content.replace(fullDivSearch2, newCollectionInput);
}

fs.writeFileSync(filePath, content);
console.log('Successfully swapped Collection ID input to a dropdown selector.');
