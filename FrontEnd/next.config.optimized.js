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

  // Optimize images
  images: {
    formats: ['image/avif', 'image/webp'],
    minimumCacheTTL: 60,
  },

  // Compiler optimizations
  compiler: {
    removeConsole: process.env.NODE_ENV === 'production' ? {
      exclude: ['error', 'warn'],
    } : false,
  },

  // Experimental features for better performance
  experimental: {
    optimizeCss: true,
    optimizePackageImports: [
      '@mui/material',
      '@mui/icons-material',
      'leaflet',
      'recharts',
    ],
  },

  // Webpack optimizations
  webpack: (config, { isServer }) => {
    // Reduce bundle size by excluding source maps in production
    if (!isServer && process.env.NODE_ENV === 'production') {
      config.devtool = false;
    }

    // Optimize chunks
    config.optimization = {
      ...config.optimization,
      splitChunks: {
        chunks: 'all',
        cacheGroups: {
          default: false,
          vendors: false,
          // Vendor chunk for node_modules
          vendor: {
            name: 'vendor',
            chunks: 'all',
            test: /node_modules/,
            priority: 20,
          },
          // Separate chunk for MUI components
          mui: {
            name: 'mui',
            test: /[\\/]node_modules[\\/]@mui[\\/]/,
            priority: 30,
          },
          // Separate chunk for Leaflet
          leaflet: {
            name: 'leaflet',
            test: /[\\/]node_modules[\\/](leaflet|react-leaflet)[\\/]/,
            priority: 30,
          },
          // Commons chunk for shared code
          common: {
            name: 'common',
            minChunks: 2,
            priority: 10,
            reuseExistingChunk: true,
            enforce: true,
          },
        },
      },
    };

    return config;
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

  // Headers for caching
  async headers() {
    return [
      {
        source: '/:all*(svg|jpg|jpeg|png|gif|ico|webp|avif)',
        headers: [
          {
            key: 'Cache-Control',
            value: 'public, max-age=31536000, immutable',
          },
        ],
      },
      {
        source: '/_next/static/:path*',
        headers: [
          {
            key: 'Cache-Control',
            value: 'public, max-age=31536000, immutable',
          },
        ],
      },
    ];
  },
};

module.exports = nextConfig;
