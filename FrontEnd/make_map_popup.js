const fs = require('fs');

const filePath = 'src/components/Workflow/NodeConfigPanel.tsx';
let content = fs.readFileSync(filePath, 'utf8');

if (!content.includes('isMapExpanded')) {
  // Add state
  content = content.replace(
    /const \[description, setDescription\] = useState\(''\);/,
    "const [description, setDescription] = useState('');\n  const [isMapExpanded, setIsMapExpanded] = useState(false);"
  );

  // Add the button and conditional rendering
  const oldHeader = `<label className="block text-sm font-medium text-gray-700 mb-1 flex justify-between">
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
                  <div className="h-64 w-full mb-2 border border-gray-300 rounded overflow-hidden">`;

  const newHeader = `<label className="block text-sm font-medium text-gray-700 mb-1 flex justify-between">
                    <span>Region (Draw on Map or Edit GeoJSON)</span>
                    <div className="space-x-3">
                      <button 
                        type="button" 
                        onClick={() => setIsMapExpanded(true)}
                        className="text-xs text-blue-600 hover:text-blue-800 font-semibold"
                      >
                        ⛶ Expand Map
                      </button>
                      {config.region && (
                        <button 
                          type="button" 
                          onClick={() => updateConfig('region', '')}
                          className="text-xs text-red-600 hover:text-red-800"
                        >
                          Clear Region
                        </button>
                      )}
                    </div>
                  </label>
                  
                  {isMapExpanded && (
                    <div className="fixed inset-0 z-[100] bg-black bg-opacity-70 flex items-center justify-center p-4 sm:p-8">
                      <div className="bg-white w-full h-full rounded-lg shadow-2xl flex flex-col flex-1 overflow-hidden">
                        <div className="flex justify-between items-center p-4 border-b bg-gray-50 flex-shrink-0">
                          <h3 className="text-lg font-bold text-gray-800">Select Region</h3>
                          <button 
                            type="button"
                            onClick={() => setIsMapExpanded(false)} 
                            className="px-4 py-2 bg-blue-600 text-white font-medium rounded hover:bg-blue-700 shadow flex items-center transition"
                          >
                            Done Selecting
                          </button>
                        </div>
                        <div className="flex-1 w-full relative min-h-0 z-0">
                          <Map
                            onShapeCreated={(e: any) => {
                              const geoJson = e.layer.toGeoJSON();
                              const regionData = geoJson.geometry ? geoJson.geometry : geoJson;
                              updateConfig('region', JSON.stringify(regionData, null, 2));
                            }}
                            onClearShape={() => updateConfig('region', '')}
                            initialRegion={config.region ? (() => { 
                              try { 
                                const parsed = JSON.parse(config.region); 
                                return parsed.type === 'Feature' ? parsed : { type: 'Feature', properties: {}, geometry: parsed };
                              } catch { return null; } 
                            })() : null}
                          />
                        </div>
                      </div>
                    </div>
                  )}

                  {!isMapExpanded && (
                  <div className="h-64 w-full mb-2 border border-gray-300 rounded overflow-hidden">`;

  content = content.replace(oldHeader, newHeader);
  
  const mapEndStr = `})() : null}
                    />
                  </div>`;
                  
  const newMapEndStr = `})() : null}
                    />
                  </div>
                  )}`;
                  
  content = content.replace(mapEndStr, newMapEndStr);

  fs.writeFileSync(filePath, content);
  console.log('Successfully updated NodeConfigPanel to include expanded map state');
} else {
  console.log('Already updated');
}
