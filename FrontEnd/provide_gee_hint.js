const fs = require('fs');
const filePath = 'src/components/Workflow/NodeConfigPanel.tsx';
let content = fs.readFileSync(filePath, 'utf8');

const placeholderSearch = 'placeholder="LANDSAT/LC08/C02/T1_L2"';
const replacement = 'placeholder="e.g., COPERNICUS/S2_SR_HARMONIZED"';

content = content.replace(placeholderSearch, replacement);

if (!content.includes('Common datasets:')) {
  // Find where collection_id is
  const searchPattern = `onChange={(e) => updateConfig('collection_id', e.target.value)}
                    placeholder="e.g., COPERNICUS/S2_SR_HARMONIZED"
                    className="w-full border border-gray-300 rounded px-3 py-2"
                  />`;
                  
  const replacementPattern = `onChange={(e) => updateConfig('collection_id', e.target.value)}
                    placeholder="e.g., COPERNICUS/S2_SR_HARMONIZED"
                    className="w-full border border-gray-300 rounded px-3 py-2"
                  />
                  <p className="text-xs text-gray-500 mt-1">Common datasets: COPERNICUS/S2_SR_HARMONIZED (Sentinel-2)</p>`;

  content = content.replace(searchPattern, replacementPattern);
}

fs.writeFileSync(filePath, content);
