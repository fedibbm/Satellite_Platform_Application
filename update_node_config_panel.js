const fs = require('fs');

const filePath = 'FrontEnd/src/components/Workflow/NodeConfigPanel.tsx';
let content = fs.readFileSync(filePath, 'utf8');

// Add the import at the top
if (!content.includes('import dynamic from ')) {
  content = content.replace(
    "import { XMarkIcon } from '@heroicons/react/24/outline';",
    "import { XMarkIcon } from '@heroicons/react/24/outline';\nimport dynamic from 'next/dynamic';\n\nconst Map = dynamic(() => import('@/components/Map'), { ssr: false });"
  );
}

// Replace the Region area
const regionRegex = /<label className="block text-sm font-medium text-gray-700 mb-1">[\s\n]*Region \(GeoJSON\)[\s\n]*<\/label>[\s\n]*<textarea[\s\n]*value=\{config\.region \|\| ''\}[\s\n]*onChange=\{\(e\) => updateConfig\('region', e\.target\.value\)\}[\s\n]*placeholder='\{"type":"Polygon","coordinates":\[\.\.\.\]\}'[\s\n]*rows=\{3\}[\s\n]*className="w-full border border-gray-300 rounded px-3 py-2 font-mono text-sm"[\s\n]*\/>/;

const mapReplacement = `<label className="block text-sm font-medium text-gray-700 mb-1 flex justify-between">
                    <span>Region (Draw on Map or Edit GeoJSON)</span>
                    {config.region && (
                      <button 
                        type="button" 
                        onClick={() => updateConfig('region', '')}
                        className="text-xs text-red-600 hover:text-red-800"
                      >
                        Clear Region
                      </button>
                    )}
                  </label>
                  <div className="h-64 w-full mb-2 border border-gray-300 rounded overflow-hidden">
                    <Map
                      onShapeCreated={(e: any) => {
                        const geoJson = e.layer.toGeoJSON();
                        // Usually the API wants the geometry for GEE queries, or the whole Feature
                        const regionData = geoJson.geometry ? geoJson.geometry : geoJson;
                        updateConfig('region', JSON.stringify(regionData, null, 2));
                      }}
                      onClearShape={() => updateConfig('region', '')}
                      initialRegion={config.region ? (() => { 
                        try { 
                          const parsed = JSON.parse(config.region); 
                          return parsed.type === 'Feature' ? parsed : { type: 'Feature', properties: {}, geometry: parsed };
                        } catch { 
                          return null; 
                        } 
                      })() : null}
                    />
                  </div>
                  <textarea
                    value={config.region || ''}
                    onChange={(e) => updateConfig('region', e.target.value)}
                    placeholder='{"type":"Polygon","coordinates":[...]}'
                    rows={4}
                    className="w-full border border-gray-300 rounded px-3 py-2 font-mono text-xs"
                  />`;

content = content.replace(regionRegex, mapReplacement);

fs.writeFileSync(filePath, content);
console.log('Updated NodeConfigPanel.tsx successfully.');
