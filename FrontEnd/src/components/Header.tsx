'use client'

import { AppBar, Toolbar, Typography, Box, ButtonBase } from '@mui/material';
import Link from 'next/link'
import Image from 'next/image'
import { useState, useEffect } from 'react'
import { useAuth } from '../hooks/useAuth';
import AccountCircle from '@mui/icons-material/AccountCircle';

interface HeaderProps {
  title: string;
}

export default function Header({ title }: HeaderProps) {
  const { user, logout, isLoggedIn } = useAuth();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

 

  const handleLogout = () => {
    alert("logged out");
    logout();
  }

  const loggedIn =  isLoggedIn(); // boolean for convenience

  return (
    <AppBar position="static">
      <Toolbar className="justify-between">
        {/* Logo and App Name */}
        <Link href="/" className="flex items-center space-x-2">
          <div className="relative w-10 h-10">
            <Image
              src="/images/logo.png"
              alt="SatelliteIP Logo"
              fill
              sizes="(max-width: 640px) 40px, 40px"
              priority
              className="object-contain"
            />
          </div>
          <span className="text-xl font-bold text-white">SatelliteIP</span>
        </Link>

        {/* Desktop Navigation */}
        <nav className="hidden md:flex items-center space-x-8 ml-auto">
          <Link href="/" className="text-white hover:text-blue-200 transition-colors">
            Home
          </Link>
          <Link href="/dashboard" className="text-white hover:text-blue-200 transition-colors">
            Dashboard
          </Link>

          {loggedIn ? (
            <ButtonBase
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                '&:hover': {
                  cursor: 'pointer'
                }
              }}
              onClick={handleLogout}
            >
              <AccountCircle sx={{ color: 'white' }} />
              <Typography variant="body1" sx={{ color: 'white' }}>
                Logged In
              </Typography>
            </ButtonBase>
          ) : (
            <Link
              href="/auth/login"
              className="text-white hover:text-blue-200 transition-colors"
            >
              Sign In
            </Link>
          )}
        </nav>

        {/* Mobile menu button */}
        <div className="md:hidden">
          <button
            type="button"
            aria-label="Toggle navigation menu"
            aria-expanded={isMobileMenuOpen}
            className="text-white hover:text-blue-200 focus:outline-none focus:ring-2 focus:ring-blue-200 focus:ring-offset-2 rounded-md p-2"
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          >
            <svg
              className="h-6 w-6"
              fill="none"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
              viewBox="0 0 24 24"
              stroke="currentColor"
              aria-hidden="true"
            >
              <path d="M4 6h16M4 12h16M4 18h16"></path>
            </svg>
          </button>
        </div>
      </Toolbar>

      {/* Mobile Menu */}
      <div className={`${isMobileMenuOpen ? 'block' : 'hidden'} md:hidden absolute top-full left-0 w-full bg-gray-800 shadow-lg`}>
        <div className="px-2 pt-2 pb-3 space-y-1">
          <Link
            href="/"
            className="block px-3 py-2 rounded-md text-white hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-200"
          >
            Home
          </Link>
          <Link
            href="/dashboard"
            className="block px-3 py-2 rounded-md text-white hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-200"
          >
            Dashboard
          </Link>

          {loggedIn ? (
            <ButtonBase
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                px: 3,
                py: 2,
                '&:hover': { backgroundColor: 'rgba(255,255,255,0.1)', cursor: 'pointer' }
              }}
              onClick={handleLogout}
            >
              <AccountCircle sx={{ color: 'white' }} />
              <Typography variant="body1" sx={{ color: 'white' }}>
                Logged In
              </Typography>
            </ButtonBase>
          ) : (
            <Link
              href="/auth/login"
              className="block px-3 py-2 rounded-md text-white hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-200"
            >
              Sign In
            </Link>
          )}
        </div>
      </div>
    </AppBar>
  );
}
