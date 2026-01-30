'use client'

import Link from 'next/link'
import { useAuth } from '@/hooks/useAuth'
import LoginIcon from '@mui/icons-material/Login'

export default function Home() {
  const { isLoggedIn } = useAuth()
  const loggedIn = isLoggedIn()

  const features = [
    {
      icon: (
        <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z" />
        </svg>
      ),
      title: 'Cloud Processing',
      description: 'Process satellite imagery with powerful cloud-based tools and algorithms.'
    },
    {
      icon: (
        <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
      ),
      title: 'Real-time Analysis',
      description: 'Analyze and visualize geospatial data with interactive maps and charts.'
    },
    {
      icon: (
        <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
        </svg>
      ),
      title: 'Team Collaboration',
      description: 'Share projects and collaborate with your team in real-time.'
    },
    {
      icon: (
        <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" />
        </svg>
      ),
      title: 'Secure Storage',
      description: 'Store and manage your satellite imagery with enterprise-grade security.'
    },
    {
      icon: (
        <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      ),
      title: 'Google Earth Engine',
      description: 'Seamless integration with Google Earth Engine for access to petabytes of satellite data.'
    },
    {
      icon: (
        <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
        </svg>
      ),
      title: 'Project Management',
      description: 'Organize your research with comprehensive project and task management tools.'
    }
  ]

  const technologies = [
    { name: 'Google Earth Engine', description: 'Planetary-scale geospatial analysis' },
    { name: 'Leaflet Maps', description: 'Interactive mapping and visualization' },
    { name: 'Advanced Algorithms', description: 'Custom processing pipelines' },
    { name: 'RESTful API', description: 'Programmatic access to platform features' }
  ]

  return (
    <div className="min-h-screen bg-white">
      {/* Hero Section */}
      <section className="relative py-16 md:py-24 bg-white overflow-hidden">
        {/* Background Pattern */}
        <div className="absolute inset-0 opacity-5">
          <div className="absolute inset-0" style={{
            backgroundImage: `url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23667eea' fill-opacity='1'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")`,
          }}></div>
        </div>

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            {/* Left Column - Content */}
            <div>
              <div className="inline-flex items-center px-4 py-2 bg-[#667eea] bg-opacity-10 rounded-full mb-6">
                <span className="text-sm font-semibold text-[#667eea]">ENIT Research Project</span>
              </div>
              
              <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold text-gray-900 mb-6 leading-tight">
                Satellite Image
                <span className="block text-[#667eea]">Processing Platform</span>
              </h1>
              
              <p className="text-lg md:text-xl text-gray-600 mb-8 leading-relaxed">
                Advanced research platform for satellite imagery analysis, geospatial data processing, and Earth observation studies.
              </p>

              {/* CTA Buttons */}
              <div className="flex flex-col sm:flex-row gap-4 mb-12">
                {loggedIn ? (
                  <Link
                    href="/dashboard"
                    className="inline-flex items-center justify-center px-8 py-4 text-lg font-semibold text-white bg-[#667eea] rounded-lg hover:bg-[#5568d3] transition-all transform hover:scale-105 shadow-lg"
                  >
                    Go to Dashboard
                    <svg className="w-5 h-5 ml-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
                    </svg>
                  </Link>
                ) : (
                  <>
                    <Link
                      href="/auth/register"
                      className="inline-flex items-center justify-center px-8 py-4 text-lg font-semibold text-white bg-[#667eea] rounded-lg hover:bg-[#5568d3] transition-all transform hover:scale-105 shadow-lg"
                    >
                      Get Started
                    </Link>
                    <Link
                      href="/auth/login"
                      className="inline-flex items-center space-x-2 px-8 py-4 text-lg font-semibold text-[#667eea] bg-white border-2 border-[#667eea] rounded-lg hover:bg-[#667eea] hover:text-white transition-all duration-200 shadow-md"
                    >
                      <LoginIcon sx={{ fontSize: 20 }} />
                      <span>Sign In</span>
                    </Link>
                  </>
                )}
              </div>

              {/* Stats */}
              <div className="grid grid-cols-3 gap-6">
                <div>
                  <div className="text-2xl md:text-3xl font-bold text-[#667eea] mb-1">GEE</div>
                  <div className="text-sm text-gray-600">Integration</div>
                </div>
                <div>
                  <div className="text-2xl md:text-3xl font-bold text-[#764ba2] mb-1">Cloud</div>
                  <div className="text-sm text-gray-600">Processing</div>
                </div>
                <div>
                  <div className="text-2xl md:text-3xl font-bold text-[#667eea] mb-1">Real-time</div>
                  <div className="text-sm text-gray-600">Analysis</div>
                </div>
              </div>
            </div>

            {/* Right Column - Visual Elements */}
            <div className="relative hidden lg:block">
              <div className="relative">
                {/* Decorative circles */}
                <div className="absolute top-0 right-0 w-72 h-72 bg-[#667eea] opacity-10 rounded-full blur-3xl"></div>
                <div className="absolute bottom-0 left-0 w-72 h-72 bg-[#764ba2] opacity-10 rounded-full blur-3xl"></div>
                
                {/* Feature Cards */}
                <div className="relative space-y-4">
                  <div className="bg-white rounded-xl shadow-xl p-6 border border-gray-100 transform rotate-2 hover:rotate-0 transition-transform">
                    <div className="flex items-center space-x-4">
                      <div className="w-12 h-12 bg-[#667eea] bg-opacity-10 rounded-lg flex items-center justify-center">
                        <svg className="w-6 h-6 text-[#667eea]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                      </div>
                      <div>
                        <div className="font-semibold text-gray-900">Earth Engine</div>
                        <div className="text-sm text-gray-600">Petabytes of satellite data</div>
                      </div>
                    </div>
                  </div>

                  <div className="bg-white rounded-xl shadow-xl p-6 border border-gray-100 transform -rotate-2 hover:rotate-0 transition-transform ml-8">
                    <div className="flex items-center space-x-4">
                      <div className="w-12 h-12 bg-[#764ba2] bg-opacity-10 rounded-lg flex items-center justify-center">
                        <svg className="w-6 h-6 text-[#764ba2]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                        </svg>
                      </div>
                      <div>
                        <div className="font-semibold text-gray-900">Advanced Analytics</div>
                        <div className="text-sm text-gray-600">Real-time visualization</div>
                      </div>
                    </div>
                  </div>

                  <div className="bg-white rounded-xl shadow-xl p-6 border border-gray-100 transform rotate-1 hover:rotate-0 transition-transform">
                    <div className="flex items-center space-x-4">
                      <div className="w-12 h-12 bg-[#667eea] bg-opacity-10 rounded-lg flex items-center justify-center">
                        <svg className="w-6 h-6 text-[#667eea]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                        </svg>
                      </div>
                      <div>
                        <div className="font-semibold text-gray-900">Collaboration</div>
                        <div className="text-sm text-gray-600">Share and work together</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">
              Platform Capabilities
            </h2>
            <p className="text-lg text-gray-600 max-w-2xl mx-auto">
              Comprehensive tools for geospatial research and satellite imagery analysis
            </p>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {features.map((feature, index) => (
              <div
                key={index}
                className="bg-white rounded-xl p-6 shadow-md hover:shadow-xl transition-all transform hover:-translate-y-1"
              >
                <div className="w-16 h-16 bg-[#667eea] bg-opacity-10 rounded-lg flex items-center justify-center text-[#667eea] mb-4">
                  {feature.icon}
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-2">
                  {feature.title}
                </h3>
                <p className="text-gray-600">
                  {feature.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">
              How It Works
            </h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-12">
            <div className="text-center">
              <div className="w-16 h-16 bg-[#667eea] text-white rounded-full flex items-center justify-center text-2xl font-bold mx-auto mb-4">
                1
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">Upload Images</h3>
              <p className="text-gray-600">
                Upload your satellite imagery or connect to Google Earth Engine
              </p>
            </div>

            <div className="text-center">
              <div className="w-16 h-16 bg-[#764ba2] text-white rounded-full flex items-center justify-center text-2xl font-bold mx-auto mb-4">
                2
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">Process & Analyze</h3>
              <p className="text-gray-600">
                Apply advanced algorithms and analysis tools to your data
              </p>
            </div>

            <div className="text-center">
              <div className="w-16 h-16 bg-[#667eea] text-white rounded-full flex items-center justify-center text-2xl font-bold mx-auto mb-4">
                3
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">Visualize Results</h3>
              <p className="text-gray-600">
                View insights through interactive maps and export your results
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Technologies Section */}
      <section className="py-20 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">
              Built With Modern Technologies
            </h2>
            <p className="text-lg text-gray-600 max-w-2xl mx-auto">
              Leveraging industry-leading tools and platforms
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {technologies.map((tech, index) => (
              <div key={index} className="bg-white rounded-lg p-6 shadow-md">
                <h3 className="text-lg font-semibold text-gray-900 mb-2">{tech.name}</h3>
                <p className="text-gray-600 text-sm">{tech.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* About Section */}
      <section className="py-20 bg-white">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center">
            <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-6">
              About the Project
            </h2>
            <div className="text-left space-y-4 text-gray-700 leading-relaxed">
              <p>
                This platform is developed as part of a research project at <span className="font-semibold text-[#667eea]">ENIT (École Nationale d'Ingénieurs de Tunis)</span>, 
                providing researchers and students with advanced tools for satellite imagery analysis and geospatial data processing.
              </p>
              <p>
                The platform integrates with Google Earth Engine to provide access to vast satellite imagery archives, 
                enabling large-scale environmental monitoring, land use analysis, and remote sensing research.
              </p>
              <p>
                Whether you're studying urban development, agricultural patterns, environmental changes, or conducting 
                geospatial analysis, this platform provides the tools needed for comprehensive research.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      {!loggedIn && (
        <section className="py-00 bg-gradient-to-r from-[#667eea] to-[#764ba2] relative overflow-hidden">
          {/* Background Pattern */}
          <div className="absolute inset-0 opacity-10">
            <div className="absolute inset-0" style={{
              backgroundImage: `url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='1'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")`,
            }}></div>
          </div>

          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative">
            <div className="px-8 py-12 md:px-12 md:py-16 text-center">
              <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">
                Ready to Start Your Research?
              </h2>
              <p className="text-lg text-white text-opacity-90 mb-8 max-w-2xl mx-auto">
                Join ENIT researchers and students using advanced satellite imagery analysis tools
              </p>

              <div className="flex flex-col sm:flex-row gap-4 justify-center items-center mb-10">
                <Link
                  href="/auth/register"
                  className="inline-flex items-center justify-center px-8 py-3 text-base font-semibold text-[#667eea] bg-white rounded-lg hover:bg-gray-50 transition-all shadow-md"
                >
                  <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                  </svg>
                  Create Account
                </Link>
                <Link
                  href="/auth/login"
                  className="inline-flex items-center justify-center px-8 py-3 text-base font-semibold text-white border-2 border-white rounded-lg hover:bg-white hover:text-[#667eea] transition-all"
                >
                  <LoginIcon sx={{ fontSize: 20, mr: 1 }} />
                  Sign In
                </Link>
              </div>

              {/* Stats */}
              <div className="grid grid-cols-3 gap-6 pt-8 border-t border-white border-opacity-30 max-w-2xl mx-auto">
                <div>
                  <div className="text-2xl font-bold text-white mb-1">Free</div>
                  <div className="text-sm text-white text-opacity-80">For Researchers</div>
                </div>
                <div>
                  <div className="text-2xl font-bold text-white mb-1">24/7</div>
                  <div className="text-sm text-white text-opacity-80">Access</div>
                </div>
                <div>
                  <div className="text-2xl font-bold text-white mb-1">ENIT</div>
                  <div className="text-sm text-white text-opacity-80">Supported</div>
                </div>
              </div>
            </div>
          </div>
        </section>
      )}

      {/* Footer */}
      <footer className="bg-gray-900 text-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            {/* About */}
            <div className="col-span-1 md:col-span-2">
              <h3 className="text-xl font-bold mb-4">SatelliteIP</h3>
              <p className="text-gray-400 mb-4">
                A research platform for satellite imagery analysis and geospatial data processing, 
                developed at ENIT.
              </p>
              <p className="text-sm text-gray-500">
                © 2026 ENIT Research Project. All rights reserved.
              </p>
            </div>

            {/* Quick Links */}
            <div>
              <h4 className="text-lg font-semibold mb-4">Platform</h4>
              <ul className="space-y-2">
                <li>
                  <Link href="/dashboard" className="text-gray-400 hover:text-white transition-colors">
                    Dashboard
                  </Link>
                </li>
                <li>
                  <Link href="/projects" className="text-gray-400 hover:text-white transition-colors">
                    Projects
                  </Link>
                </li>
                <li>
                  <Link href="/analysis" className="text-gray-400 hover:text-white transition-colors">
                    Analysis
                  </Link>
                </li>
                <li>
                  <Link href="/storage" className="text-gray-400 hover:text-white transition-colors">
                    Storage
                  </Link>
                </li>
              </ul>
            </div>

            {/* Resources */}
            <div>
              <h4 className="text-lg font-semibold mb-4">Resources</h4>
              <ul className="space-y-2">
                <li>
                  <a href="https://enit.rnu.tn" target="_blank" rel="noopener noreferrer" className="text-gray-400 hover:text-white transition-colors">
                    ENIT Website
                  </a>
                </li>
                <li>
                  <a href="https://earthengine.google.com" target="_blank" rel="noopener noreferrer" className="text-gray-400 hover:text-white transition-colors">
                    Google Earth Engine
                  </a>
                </li>
                <li>
                  <Link href="/auth/register" className="text-gray-400 hover:text-white transition-colors">
                    Sign Up
                  </Link>
                </li>
                <li>
                  <Link href="/auth/login" className="text-gray-400 hover:text-white transition-colors">
                    Sign In
                  </Link>
                </li>
              </ul>
            </div>
          </div>

          <div className="border-t border-gray-800 mt-8 pt-8 flex flex-col md:flex-row justify-between items-center">
            <p className="text-sm text-gray-500">
              Built for research and educational purposes
            </p>
            <div className="flex space-x-6 mt-4 md:mt-0">
              <span className="text-sm text-gray-500">ENIT Research Platform</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}
