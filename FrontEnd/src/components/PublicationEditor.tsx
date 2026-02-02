'use client';

import { useState, useCallback } from 'react';
import dynamic from 'next/dynamic';
import { Box, TextField, Chip, Button, Paper, Typography, IconButton, Tooltip } from '@mui/material';
import SaveIcon from '@mui/icons-material/Save';
import PreviewIcon from '@mui/icons-material/Preview';
import SatelliteIcon from '@mui/icons-material/Satellite';
import FunctionsIcon from '@mui/icons-material/Functions';
import FolderIcon from '@mui/icons-material/Folder';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import ArticleIcon from '@mui/icons-material/Article';
import AddIcon from '@mui/icons-material/Add';
import MenuIcon from '@mui/icons-material/Menu';
import CloseIcon from '@mui/icons-material/Close';
import { marked } from 'marked';
import markedKatex from 'marked-katex-extension';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import remarkMath from 'remark-math';
import rehypeKatex from 'rehype-katex';
import rehypeHighlight from 'rehype-highlight';
import 'react-markdown-editor-lite/lib/index.css';
import 'katex/dist/katex.min.css';
import 'highlight.js/styles/github.css';

// Configure marked with KaTeX extension
marked.use(markedKatex({
  throwOnError: false,
  displayMode: true,
}));

// Dynamic import to avoid SSR issues
const MdEditor = dynamic(() => import('react-markdown-editor-lite'), {
  ssr: false,
});

interface PublicationMetadata {
  title: string;
  tags: string[];
  description: string;
}

interface PublicationEditorProps {
  onSave?: (content: string, metadata: PublicationMetadata) => void;
  initialContent?: string;
  initialMetadata?: PublicationMetadata;
  isDisabled?: boolean;
}

export default function PublicationEditor({
  onSave,
  initialContent = '',
  initialMetadata = { title: '', tags: [], description: '' },
  isDisabled = false,
}: PublicationEditorProps) {
  const [content, setContent] = useState(initialContent);
  const [metadata, setMetadata] = useState<PublicationMetadata>(initialMetadata);
  const [tagInput, setTagInput] = useState('');
  const [viewMode, setViewMode] = useState<'edit' | 'preview'>('edit');
  const [sidebarOpen, setSidebarOpen] = useState(true);

  // Custom plugin: Insert NDVI formula
  const insertNDVI = useCallback(() => {
    const formula = '\n$$NDVI = \\frac{NIR - Red}{NIR + Red}$$\n\n';
    setContent(prev => prev + formula);
  }, []);

  // Custom plugin: Insert EVI formula
  const insertEVI = useCallback(() => {
    const formula = '\n$$EVI = 2.5 \\times \\frac{NIR - Red}{NIR + 6 \\times Red - 7.5 \\times Blue + 1}$$\n\n';
    setContent(prev => prev + formula);
  }, []);

  // Custom plugin: Insert SAVI formula
  const insertSAVI = useCallback(() => {
    const formula = '\n$$SAVI = \\frac{(NIR - Red) \\times (1 + L)}{NIR + Red + L}$$\n\n*Where L = 0.5 for moderate vegetation cover*\n\n';
    setContent(prev => prev + formula);
  }, []);

  // Custom plugin: Insert study area template
  const insertStudyArea = useCallback(() => {
    const template = `\n## Study Area

**Location:** [Latitude, Longitude]  
**Area Coverage:** [X km²]  
**Time Period:** [Start Date] - [End Date]  
**Satellite:** [Sentinel-2 / Landsat-8 / etc.]  
**Cloud Cover:** [X%]

---

`;
    setContent(prev => prev + template);
  }, []);

  // Custom plugin: Insert methodology template
  const insertMethodology = useCallback(() => {
    const template = `\n## Methodology

### Data Acquisition
- **Source:** Google Earth Engine
- **Collection:** 
- **Date Range:** 
- **Bands Used:** 

### Preprocessing
1. Cloud masking
2. Atmospheric correction
3. Resampling

### Analysis
- Vegetation indices calculation
- Statistical analysis
- Temporal comparison

---

`;
    setContent(prev => prev + template);
  }, []);

  // Custom plugin: Insert GEE code template
  const insertGEECode = useCallback(() => {
    const template = `\n\`\`\`javascript
// Google Earth Engine Code
var region = ee.Geometry.Rectangle([minLon, minLat, maxLon, maxLat]);

var collection = ee.ImageCollection('COPERNICUS/S2_SR')
  .filterBounds(region)
  .filterDate('2024-01-01', '2024-12-31')
  .filter(ee.Filter.lt('CLOUDY_PIXEL_PERCENTAGE', 20));

// Calculate NDVI
var ndvi = collection.map(function(image) {
  return image.normalizedDifference(['B8', 'B4']).rename('NDVI');
});

Map.addLayer(ndvi.mean(), {min: -1, max: 1, palette: ['red', 'yellow', 'green']}, 'NDVI');
\`\`\`

`;
    setContent(prev => prev + template);
  }, []);

  const handleEditorChange = ({ text }: { html: string; text: string }) => {
    setContent(text);
  };

  const handleAddTag = () => {
    if (tagInput.trim() && !metadata.tags.includes(tagInput.trim())) {
      setMetadata({
        ...metadata,
        tags: [...metadata.tags, tagInput.trim()],
      });
      setTagInput('');
    }
  };

  const handleDeleteTag = (tagToDelete: string) => {
    setMetadata({
      ...metadata,
      tags: metadata.tags.filter((tag) => tag !== tagToDelete),
    });
  };

  const handleSave = () => {
    if (onSave) {
      onSave(content, metadata);
    }
  };

  // Render markdown to HTML for preview
  const renderMarkdown = (text: string) => {
    try {
      return marked(text, {
        breaks: true,
        gfm: true,
      }) as string;
    } catch (error) {
      console.error('Markdown parsing error:', error);
      return text;
    }
  };

  // Custom toolbar config
  const customToolbar = {
    toolbar: [
      'header',
      'bold',
      'italic',
      'underline',
      'strikethrough',
      '|',
      'list-unordered',
      'list-ordered',
      'block-quote',
      '|',
      'link',
      'image',
      'table',
      'code-inline',
      'code-block',
      '|',
      'undo',
      'redo',
      '|',
      'full-screen',
    ],
  };

  return (
    <Box sx={{ width: '100%', display: 'flex', gap: 3, minHeight: 'calc(100vh - 200px)', position: 'relative' }}>
      {/* Left Sidebar - Collapsible */}
      <Box 
        sx={{ 
          width: sidebarOpen ? 280 : 0,
          flexShrink: 0,
          transition: 'width 0.3s ease',
          overflow: 'hidden',
        }}
      >
        <Paper 
          elevation={0} 
          sx={{ 
            p: 2.5, 
            mb: 3,
            background: 'white',
            border: '1px solid #e0e0e0',
            borderRadius: 2,
            position: 'sticky',
            top: 20,
            width: 280,
          }}
        >
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2.5 }}>
            <Typography variant="h6" sx={{ color: '#1a1a1a', fontWeight: 600, fontSize: '0.95rem' }}>
              Publication Settings
            </Typography>
            <IconButton 
              size="small" 
              onClick={() => setSidebarOpen(false)}
              sx={{ color: '#666' }}
            >
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>

          <TextField
            fullWidth
            label="Title"
            placeholder="Your publication title"
            value={metadata.title}
            onChange={(e) => setMetadata({ ...metadata, title: e.target.value })}
            sx={{ mb: 2 }}
            required
            size="small"
          />

          <TextField
            fullWidth
            label="Description"
            placeholder="Brief description"
            value={metadata.description}
            onChange={(e) => setMetadata({ ...metadata, description: e.target.value })}
            multiline
            rows={2}
            sx={{ mb: 2 }}
            size="small"
          />

          <Box sx={{ mb: 2.5 }}>
            <Typography variant="caption" sx={{ mb: 1, color: '#666', fontWeight: 500, display: 'block' }}>
              Tags
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, mb: 1.5 }}>
              <TextField
                placeholder="Add tag"
                value={tagInput}
                onChange={(e) => setTagInput(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleAddTag()}
                size="small"
                sx={{ flex: 1 }}
              />
              <IconButton 
                onClick={handleAddTag}
                sx={{
                  background: '#667eea',
                  color: 'white',
                  width: 36,
                  height: 36,
                  '&:hover': { background: '#5568d3' },
                }}
                size="small"
              >
                +
              </IconButton>
            </Box>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
              {metadata.tags.map((tag) => (
                <Chip
                  key={tag}
                  label={tag}
                  onDelete={() => handleDeleteTag(tag)}
                  size="small"
                  sx={{
                    background: '#f5f5f5',
                    border: '1px solid #e0e0e0',
                    height: 24,
                    '& .MuiChip-label': { fontSize: '0.75rem', px: 1 },
                    '& .MuiChip-deleteIcon': {
                      fontSize: '1rem',
                      color: '#666',
                      '&:hover': { color: '#1a1a1a' }
                    }
                  }}
                />
              ))}
            </Box>
          </Box>

          <Box sx={{ borderTop: '1px solid #e0e0e0', pt: 2.5 }}>
            <Typography variant="caption" sx={{ mb: 1.5, color: '#666', fontWeight: 500, display: 'block' }}>
              Quick Templates
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
              {[
                { icon: FunctionsIcon, label: 'NDVI', onClick: insertNDVI },
                { icon: FunctionsIcon, label: 'EVI', onClick: insertEVI },
                { icon: FunctionsIcon, label: 'SAVI', onClick: insertSAVI },
                { icon: LocationOnIcon, label: 'Study Area', onClick: insertStudyArea },
                { icon: SatelliteIcon, label: 'Methodology', onClick: insertMethodology },
                { icon: FolderIcon, label: 'GEE Code', onClick: insertGEECode },
              ].map((item, index) => {
                const Icon = item.icon;
                return (
                  <Button
                    key={index}
                    variant="text"
                    startIcon={<Icon sx={{ fontSize: 16 }} />}
                    onClick={item.onClick}
                    sx={{
                      justifyContent: 'flex-start',
                      color: '#666',
                      fontWeight: 400,
                      textTransform: 'none',
                      fontSize: '0.8125rem',
                      px: 1.5,
                      py: 0.75,
                      '&:hover': {
                        background: '#f5f5f5',
                        color: '#1a1a1a',
                      },
                    }}
                  >
                    {item.label}
                  </Button>
                );
              })}
            </Box>
          </Box>
        </Paper>
      </Box>

      {/* Main Editor Area */}
      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        {/* Mode Toggle Bar with Sidebar Toggle */}
        <Box 
          sx={{ 
            mb: 2,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <Box 
            sx={{ 
              display: 'flex',
              gap: 1,
              background: 'white',
              border: '1px solid #e0e0e0',
              borderRadius: 2,
              p: 0.5,
            }}
          >
            {!sidebarOpen && (
              <IconButton
                onClick={() => setSidebarOpen(true)}
                sx={{
                  color: '#666',
                  '&:hover': { background: '#f5f5f5' },
                }}
              >
                <MenuIcon />
              </IconButton>
            )}
            <Button
              variant={viewMode === 'edit' ? 'contained' : 'text'}
              onClick={() => setViewMode('edit')}
              sx={{
                textTransform: 'none',
                fontWeight: 500,
                px: 3,
                py: 1,
                background: viewMode === 'edit' ? '#667eea' : 'transparent',
                color: viewMode === 'edit' ? 'white' : '#666',
                '&:hover': {
                  background: viewMode === 'edit' ? '#5568d3' : '#f5f5f5',
                },
              }}
            >
              Edit
            </Button>
            <Button
              variant={viewMode === 'preview' ? 'contained' : 'text'}
              onClick={() => setViewMode('preview')}
              sx={{
                textTransform: 'none',
                fontWeight: 500,
                px: 3,
                py: 1,
                background: viewMode === 'preview' ? '#667eea' : 'transparent',
                color: viewMode === 'preview' ? 'white' : '#666',
                '&:hover': {
                  background: viewMode === 'preview' ? '#5568d3' : '#f5f5f5',
                },
              }}
            >
              Preview
            </Button>
          </Box>
        </Box>

        {/* Editor or Preview */}
        <Paper 
          elevation={0} 
          sx={{ 
            overflow: 'hidden',
            border: '1px solid #e0e0e0',
            borderRadius: 2,
            background: 'white',
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          {viewMode === 'edit' ? (
            <MdEditor
              value={content}
              style={{ height: '100%', minHeight: '600px', border: 'none' }}
              renderHTML={renderMarkdown}
              onChange={handleEditorChange}
              config={{
                view: {
                  menu: true,
                  md: true,
                  html: false,
                },
                ...customToolbar,
              }}
            />
          ) : (
            <Box 
              sx={{ 
                p: 4, 
                overflow: 'auto',
                height: '100%',
                minHeight: '600px',
                '& h1': { fontSize: '2rem', fontWeight: 700, mt: 3, mb: 2 },
                '& h2': { fontSize: '1.5rem', fontWeight: 600, mt: 2.5, mb: 1.5 },
                '& h3': { fontSize: '1.25rem', fontWeight: 600, mt: 2, mb: 1 },
                '& p': { lineHeight: 1.7, mb: 2 },
                '& code': { 
                  background: '#f5f5f5', 
                  padding: '2px 6px', 
                  borderRadius: 1,
                  fontSize: '0.9em',
                  fontFamily: 'monospace',
                },
                '& pre': { 
                  background: '#f5f5f5', 
                  p: 2, 
                  borderRadius: 1,
                  overflow: 'auto',
                  mb: 2,
                },
                '& img': { maxWidth: '100%', height: 'auto', borderRadius: 1, mb: 2 },
                '& table': { 
                  width: '100%', 
                  borderCollapse: 'collapse',
                  mb: 2,
                  '& th, & td': {
                    border: '1px solid #e0e0e0',
                    padding: '8px 12px',
                  },
                  '& th': {
                    background: '#f5f5f5',
                    fontWeight: 600,
                  },
                },
                '& blockquote': {
                  borderLeft: '4px solid #667eea',
                  pl: 2,
                  ml: 0,
                  color: '#666',
                  fontStyle: 'italic',
                },
                '& ul, & ol': { mb: 2, pl: 3 },
                '& li': { mb: 0.5 },
              }}
            >
              <ReactMarkdown
                remarkPlugins={[remarkGfm, remarkMath]}
                rehypePlugins={[rehypeKatex, rehypeHighlight]}
              >
                {content || '*Start writing to see preview...*'}
              </ReactMarkdown>
            </Box>
          )}
        </Paper>

        {/* Action Buttons */}
        <Box 
          sx={{ 
            mt: 3, 
            display: 'flex', 
            gap: 2, 
            justifyContent: 'flex-end',
          }}
        >
          <Button
            variant="outlined"
            startIcon={<PreviewIcon />}
            size="large"
            sx={{
              px: 3,
              borderColor: '#e0e0e0',
              color: '#666',
              fontWeight: 500,
              textTransform: 'none',
              '&:hover': {
                borderColor: '#1a1a1a',
                color: '#1a1a1a',
                background: '#f5f5f5',
              },
            }}
          >
            Preview
          </Button>
          <Button
            variant="outlined"
            startIcon={<SaveIcon />}
            onClick={handleSave}
            size="large"
            disabled={isDisabled}
            sx={{
              px: 3,
              borderColor: '#e0e0e0',
              color: '#666',
              fontWeight: 500,
              textTransform: 'none',
              '&:hover': {
                borderColor: '#1a1a1a',
                color: '#1a1a1a',
                background: '#f5f5f5',
              },
            }}
          >
            Save Draft
          </Button>
          <Button
            variant="contained"
            onClick={handleSave}
            size="large"
            disabled={isDisabled}
            sx={{
              px: 4,
              background: '#667eea',
              fontWeight: 500,
              textTransform: 'none',
              '&:hover': { 
                background: '#5568d3',
              },
            }}
          >
            Publish
          </Button>
        </Box>
      </Box>
    </Box>
  );
}
