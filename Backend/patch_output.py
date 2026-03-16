import re

file_path = 'src/main/java/com/enit/satellite_platform/modules/workflow/execution/nodes/OutputNodeExecutor.java'
with open(file_path, 'r') as f:
    content = f.read()

target = '                            req.setResultType(pType);\n                            // Set base64 string as data payload instead of actual file\n                            req.setData(base64.substring(0, Math.min(100, base64.length())) + "... (base64)");'

replacement = '''                            try {
                                req.setType(com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingType.fromString(pType));
                            } catch (Exception paramE) {
                                req.setType(com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingType.NDVI); // Default fallback
                            }
                            
                            Map<String, Object> dataMap = new HashMap<>();
                            dataMap.put("processedImageBase64", base64.substring(0, Math.min(100, base64.length())) + "...(truncated for storage)");
                            // In real scenario, convert base64 to byte[] and save as file
                            req.setData(dataMap);'''

content = content.replace(target, replacement)
with open(file_path, 'w') as f:
    f.write(content)
