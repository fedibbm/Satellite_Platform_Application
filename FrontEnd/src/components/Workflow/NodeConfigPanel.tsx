'use client';

import { useState, useEffect } from 'react';
import { WorkflowNode } from '@/types/workflow';
import { XMarkIcon } from '@heroicons/react/24/outline';

interface NodeConfigPanelProps {
  node: WorkflowNode | null;
  onClose: () => void;
  onSave: (nodeId: string, config: any) => void;
}

export default function NodeConfigPanel({ node, onClose, onSave }: NodeConfigPanelProps) {
  const [config, setConfig] = useState<any>({});
  const [label, setLabel] = useState('');
  const [description, setDescription] = useState('');

  useEffect(() => {
    if (node) {
      setLabel(node.data.label || '');
      setDescription(node.data.description || '');
      setConfig(node.data.config || {});
    }
  }, [node]);

  if (!node) return null;

  const handleSave = () => {
    onSave(node.id, {
      label,
      description,
      config,
    });
    onClose();
  };

  const updateConfig = (key: string, value: any) => {
    setConfig({ ...config, [key]: value });
  };

  const renderConfigFields = () => {
    switch (node.type) {
      case 'trigger':
        return (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Trigger Type
              </label>
              <select
                value={config.triggerType || 'manual'}
                onChange={(e) => updateConfig('triggerType', e.target.value)}
                className="w-full border border-gray-300 rounded px-3 py-2"
              >
                <option value="manual">Manual</option>
                <option value="scheduled">Scheduled (Cron)</option>
                <option value="webhook">Webhook</option>
              </select>
            </div>
            
            {config.triggerType === 'scheduled' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Cron Expression
                </label>
                <input
                  type="text"
                  value={config.cronExpression || ''}
                  onChange={(e) => updateConfig('cronExpression', e.target.value)}
                  placeholder="0 0 * * *"
                  className="w-full border border-gray-300 rounded px-3 py-2"
                />
                <p className="text-xs text-gray-500 mt-1">Example: 0 0 * * * (daily at midnight)</p>
              </div>
            )}
          </div>
        );

      case 'data-input':
        return (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Data Source
              </label>
              <select
                value={config.dataSource || 'gee'}
                onChange={(e) => updateConfig('dataSource', e.target.value)}
                className="w-full border border-gray-300 rounded px-3 py-2"
              >
                <option value="gee">Google Earth Engine</option>
                <option value="project">Existing Project</option>
                <option value="image">Existing Image</option>
              </select>
            </div>

            {config.dataSource === 'gee' && (
              <>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Service Type
                  </label>
                  <select
                    value={config.serviceType || 'get_images'}
                    onChange={(e) => updateConfig('serviceType', e.target.value)}
                    className="w-full border border-gray-300 rounded px-3 py-2"
                  >
                    <option value="get_images">Get Images</option>
                    <option value="get_image_collection">Get Image Collection</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Collection ID
                  </label>
                  <input
                    type="text"
                    value={config.collection_id || ''}
                    onChange={(e) => updateConfig('collection_id', e.target.value)}
                    placeholder="LANDSAT/LC08/C02/T1_L2"
                    className="w-full border border-gray-300 rounded px-3 py-2"
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Start Date
                    </label>
                    <input
                      type="date"
                      value={config.start_date || ''}
                      onChange={(e) => updateConfig('start_date', e.target.value)}
                      className="w-full border border-gray-300 rounded px-3 py-2"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      End Date
                    </label>
                    <input
                      type="date"
                      value={config.end_date || ''}
                      onChange={(e) => updateConfig('end_date', e.target.value)}
                      className="w-full border border-gray-300 rounded px-3 py-2"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Region (GeoJSON)
                  </label>
                  <textarea
                    value={config.region || ''}
                    onChange={(e) => updateConfig('region', e.target.value)}
                    placeholder='{"type":"Polygon","coordinates":[...]}'
                    rows={3}
                    className="w-full border border-gray-300 rounded px-3 py-2 font-mono text-sm"
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Max Cloud Cover (%)
                    </label>
                    <input
                      type="number"
                      value={config.max_cloud_cover || '20'}
                      onChange={(e) => updateConfig('max_cloud_cover', e.target.value)}
                      min="0"
                      max="100"
                      className="w-full border border-gray-300 rounded px-3 py-2"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Scale (meters)
                    </label>
                    <input
                      type="number"
                      value={config.scale || '30'}
                      onChange={(e) => updateConfig('scale', e.target.value)}
                      className="w-full border border-gray-300 rounded px-3 py-2"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Bands (comma-separated)
                  </label>
                  <input
                    type="text"
                    value={config.bands || ''}
                    onChange={(e) => updateConfig('bands', e.target.value)}
                    placeholder="B4,B3,B2"
                    className="w-full border border-gray-300 rounded px-3 py-2"
                  />
                </div>
              </>
            )}

            {config.dataSource === 'project' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Project ID
                </label>
                <input
                  type="text"
                  value={config.projectId || ''}
                  onChange={(e) => updateConfig('projectId', e.target.value)}
                  placeholder="Enter project ID"
                  className="w-full border border-gray-300 rounded px-3 py-2"
                />
              </div>
            )}

            {config.dataSource === 'image' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Image ID
                </label>
                <input
                  type="text"
                  value={config.imageId || ''}
                  onChange={(e) => updateConfig('imageId', e.target.value)}
                  placeholder="Enter image ID"
                  className="w-full border border-gray-300 rounded px-3 py-2"
                />
              </div>
            )}
          </div>
        );

      case 'processing':
        return (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Processing Type
              </label>
              <select
                value={config.processingType || 'ndvi'}
                onChange={(e) => updateConfig('processingType', e.target.value)}
                className="w-full border border-gray-300 rounded px-3 py-2"
              >
                <option value="ndvi">NDVI (Normalized Difference Vegetation Index)</option>
                <option value="evi">EVI (Enhanced Vegetation Index)</option>
                <option value="savi">SAVI (Soil Adjusted Vegetation Index)</option>
                <option value="ndwi">NDWI (Normalized Difference Water Index)</option>
                <option value="water-bodies">Water Bodies Detection</option>
                <option value="change-detection">Change Detection</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Input Source
              </label>
              <select
                value={config.inputSource || 'previous'}
                onChange={(e) => updateConfig('inputSource', e.target.value)}
                className="w-full border border-gray-300 rounded px-3 py-2"
              >
                <option value="previous">From Previous Node</option>
                <option value="file">Upload File</option>
              </select>
            </div>

            {config.inputSource === 'file' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  File Path/URL
                </label>
                <input
                  type="text"
                  value={config.filePath || ''}
                  onChange={(e) => updateConfig('filePath', e.target.value)}
                  placeholder="/path/to/image.tif"
                  className="w-full border border-gray-300 rounded px-3 py-2"
                />
              </div>
            )}

            {(config.processingType === 'savi') && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Soil Brightness Factor (L)
                </label>
                <input
                  type="number"
                  value={config.soilBrightness || '0.5'}
                  onChange={(e) => updateConfig('soilBrightness', e.target.value)}
                  step="0.1"
                  min="0"
                  max="1"
                  className="w-full border border-gray-300 rounded px-3 py-2"
                />
                <p className="text-xs text-gray-500 mt-1">Default: 0.5 (range: 0-1)</p>
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Output Format
              </label>
              <select
                value={config.outputFormat || 'geotiff'}
                onChange={(e) => updateConfig('outputFormat', e.target.value)}
                className="w-full border border-gray-300 rounded px-3 py-2"
              >
                <option value="geotiff">GeoTIFF</option>
                <option value="png">PNG</option>
                <option value="jpeg">JPEG</option>
                <option value="json">JSON (metadata)</option>
              </select>
            </div>
          </div>
        );

      case 'decision':
        return (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Condition Type
              </label>
              <select
                value={config.conditionType || 'comparison'}
                onChange={(e) => updateConfig('conditionType', e.target.value)}
                className="w-full border border-gray-300 rounded px-3 py-2"
              >
                <option value="comparison">Comparison</option>
                <option value="threshold">Threshold</option>
                <option value="expression">Expression</option>
                <option value="data-check">Data Check</option>
              </select>
            </div>

            {config.conditionType === 'comparison' && (
              <>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Field to Compare
                  </label>
                  <input
                    type="text"
                    value={config.field || ''}
                    onChange={(e) => updateConfig('field', e.target.value)}
                    placeholder="result.mean_value"
                    className="w-full border border-gray-300 rounded px-3 py-2"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Operator
                  </label>
                  <select
                    value={config.operator || '>'}
                    onChange={(e) => updateConfig('operator', e.target.value)}
                    className="w-full border border-gray-300 rounded px-3 py-2"
                  >
                    <option value=">">Greater Than (&gt;)</option>
                    <option value=">=">Greater or Equal (&gt;=)</option>
                    <option value="<">Less Than (&lt;)</option>
                    <option value="<=">Less or Equal (&lt;=)</option>
                    <option value="==">Equal (==)</option>
                    <option value="!=">Not Equal (!=)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Value
                  </label>
                  <input
                    type="text"
                    value={config.value || ''}
                    onChange={(e) => updateConfig('value', e.target.value)}
                    placeholder="0.5"
                    className="w-full border border-gray-300 rounded px-3 py-2"
                  />
                </div>
              </>
            )}

            {config.conditionType === 'threshold' && (
              <>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Metric Field
                  </label>
                  <input
                    type="text"
                    value={config.metric || ''}
                    onChange={(e) => updateConfig('metric', e.target.value)}
                    placeholder="ndvi_mean"
                    className="w-full border border-gray-300 rounded px-3 py-2"
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Min Threshold
                    </label>
                    <input
                      type="number"
                      value={config.minThreshold || ''}
                      onChange={(e) => updateConfig('minThreshold', e.target.value)}
                      step="0.1"
                      className="w-full border border-gray-300 rounded px-3 py-2"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Max Threshold
                    </label>
                    <input
                      type="number"
                      value={config.maxThreshold || ''}
                      onChange={(e) => updateConfig('maxThreshold', e.target.value)}
                      step="0.1"
                      className="w-full border border-gray-300 rounded px-3 py-2"
                    />
                  </div>
                </div>
              </>
            )}

            {config.conditionType === 'expression' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Expression
                </label>
                <textarea
                  value={config.expression || ''}
                  onChange={(e) => updateConfig('expression', e.target.value)}
                  placeholder="result.mean_value > 0.5 && result.count > 100"
                  rows={3}
                  className="w-full border border-gray-300 rounded px-3 py-2 font-mono text-sm"
                />
                <p className="text-xs text-gray-500 mt-1">JavaScript-like expression</p>
              </div>
            )}

            {config.conditionType === 'data-check' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Check Type
                </label>
                <select
                  value={config.checkType || 'hasData'}
                  onChange={(e) => updateConfig('checkType', e.target.value)}
                  className="w-full border border-gray-300 rounded px-3 py-2"
                >
                  <option value="hasData">Has Data</option>
                  <option value="isEmpty">Is Empty</option>
                  <option value="isValid">Is Valid</option>
                  <option value="hasError">Has Error</option>
                </select>
              </div>
            )}
          </div>
        );

      case 'output':
        return (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Output Type
              </label>
              <select
                value={config.outputType || 'store'}
                onChange={(e) => updateConfig('outputType', e.target.value)}
                className="w-full border border-gray-300 rounded px-3 py-2"
              >
                <option value="store">Store to Database</option>
                <option value="download">Download File</option>
                <option value="webhook">Send to Webhook</option>
                <option value="email">Send Email</option>
              </select>
            </div>

            {config.outputType === 'store' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Storage Location
                </label>
                <input
                  type="text"
                  value={config.storagePath || ''}
                  onChange={(e) => updateConfig('storagePath', e.target.value)}
                  placeholder="workflows/outputs/"
                  className="w-full border border-gray-300 rounded px-3 py-2"
                />
              </div>
            )}

            {config.outputType === 'webhook' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Webhook URL
                </label>
                <input
                  type="url"
                  value={config.webhookUrl || ''}
                  onChange={(e) => updateConfig('webhookUrl', e.target.value)}
                  placeholder="https://example.com/webhook"
                  className="w-full border border-gray-300 rounded px-3 py-2"
                />
              </div>
            )}

            {config.outputType === 'email' && (
              <>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Email Recipients (comma-separated)
                  </label>
                  <input
                    type="text"
                    value={config.recipients || ''}
                    onChange={(e) => updateConfig('recipients', e.target.value)}
                    placeholder="user1@example.com, user2@example.com"
                    className="w-full border border-gray-300 rounded px-3 py-2"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Email Subject
                  </label>
                  <input
                    type="text"
                    value={config.emailSubject || ''}
                    onChange={(e) => updateConfig('emailSubject', e.target.value)}
                    placeholder="Workflow Execution Results"
                    className="w-full border border-gray-300 rounded px-3 py-2"
                  />
                </div>
              </>
            )}
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-lg font-semibold text-gray-900">
            Configure {node.type.replace('-', ' ').replace(/\b\w/g, l => l.toUpperCase())} Node
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <XMarkIcon className="h-6 w-6" />
          </button>
        </div>

        {/* Body */}
        <div className="p-4 overflow-y-auto flex-1">
          <div className="space-y-4">
            {/* Basic Info */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Node Label *
              </label>
              <input
                type="text"
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                placeholder="Enter node name"
                className="w-full border border-gray-300 rounded px-3 py-2"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Describe what this node does"
                rows={2}
                className="w-full border border-gray-300 rounded px-3 py-2"
              />
            </div>

            <div className="border-t pt-4">
              <h3 className="text-sm font-semibold text-gray-700 mb-3">
                Configuration
              </h3>
              {renderConfigFields()}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-2 p-4 border-t bg-gray-50">
          <button
            onClick={onClose}
            className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            className="px-4 py-2 bg-blue-600 text-white hover:bg-blue-700 rounded"
          >
            Save Configuration
          </button>
        </div>
      </div>
    </div>
  );
}
