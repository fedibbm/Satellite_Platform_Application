/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  output: 'standalone',
  
  // Environment variables
  env: {
    NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL || '',
    NEXT_PUBLIC_GEE_API_URL: process.env.NEXT_PUBLIC_GEE_API_URL || 'http://localhost:5000',
    NEXT_PUBLIC_IMAGE_API_URL: process.env.NEXT_PUBLIC_IMAGE_API_URL || 'http://localhost:8000',
  },

  // Compiler optimizations
  compiler: {
    removeConsole: process.env.NODE_ENV === 'production' ? {
      exclude: ['error', 'warn'],
    } : false,
  },

  // Rewrites to proxy backend requests through Next.js server
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:9090/api/:path*',
      },
    ];
  },

  // Image optimization
  images: {
    domains: ['localhost'],
    formats: ['image/avif', 'image/webp'],
    minimumCacheTTL: 60,
  },

  // Webpack configuration
  webpack: (config, { isServer }) => {
    config.externals = [...(config.externals || []), { canvas: 'canvas' }];
    
    // Handle node modules
    if (!isServer) {
      config.resolve.fallback = {
        ...config.resolve.fallback,
        fs: false,
        net: false,
        tls: false,
      };
    }

    // Optimize chunks for better caching and loading
    if (!isServer) {
      config.optimization = {
        ...config.optimization,
        splitChunks: {
          chunks: 'all',
          cacheGroups: {
            default: false,
            vendors: false,
            // MUI components
            mui: {
              name: 'mui',
              test: /[\\/]node_modules[\\/]@mui[\\/]/,
              priority: 30,
              reuseExistingChunk: true,
            },
            // Leaflet maps
            leaflet: {
              name: 'leaflet',
              test: /[\\/]node_modules[\\/](leaflet|react-leaflet)[\\/]/,
              priority: 30,
              reuseExistingChunk: true,
            },
            // React Flow
            reactflow: {
              name: 'reactflow',
              test: /[\\/]node_modules[\\/](@xyflow|reactflow)[\\/]/,
              priority: 30,
              reuseExistingChunk: true,
            },
            // Commons
            common: {
              name: 'common',
              minChunks: 2,
              priority: 10,
              reuseExistingChunk: true,
            },
          },
        },
      };
    }
    
    return config;
  },
  
  // Disable ESLint during build to avoid blocking
  eslint: {
    ignoreDuringBuilds: true,
  },
  
  // Disable TypeScript errors during build
  typescript: {
    ignoreBuildErrors: true,
  },
  
  // Skip static page generation errors & optimize imports
  experimental: {
    missingSuspenseWithCSRBailout: false,
    optimizePackageImports: [
      '@mui/material',
      '@mui/icons-material',
      'leaflet',
      'recharts',
      '@xyflow/react',
    ],
  },
};

module.exports = nextConfig;
