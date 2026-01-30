'use client'

import { AppBar, Toolbar, Typography, Box, ButtonBase, Badge, Avatar, IconButton } from '@mui/material';
import Link from 'next/link'
import Image from 'next/image'
import { useState, useEffect } from 'react'
import { useAuth } from '../hooks/useAuth';
import { usePathname, useRouter } from 'next/navigation';
import AccountCircle from '@mui/icons-material/AccountCircle';
import DashboardIcon from '@mui/icons-material/Dashboard';
import FolderIcon from '@mui/icons-material/Folder';
import AnalyticsIcon from '@mui/icons-material/Analytics';
import StorageIcon from '@mui/icons-material/Storage';
import HomeIcon from '@mui/icons-material/Home';
import LogoutIcon from '@mui/icons-material/Logout';
import LoginIcon from '@mui/icons-material/Login';
import WorkIcon from '@mui/icons-material/Work';
import GroupsIcon from '@mui/icons-material/Groups';
import SearchIcon from '@mui/icons-material/Search';
import MessageIcon from '@mui/icons-material/Message';
import ArticleIcon from '@mui/icons-material/Article';
import PeopleIcon from '@mui/icons-material/People';
import ForumIcon from '@mui/icons-material/Forum';

interface HeaderProps {
  title: string;
}

export default function Header({ title }: HeaderProps) {
  const { user, logout, isLoggedIn } = useAuth();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [activePortal, setActivePortal] = useState<'workspace' | 'community'>('workspace');
  const pathname = usePathname();
  const router = useRouter();

  // Initialize portal state from localStorage or URL path on mount
  useEffect(() => {
    // Check if current path is a community route
    if (pathname.startsWith('/community')) {
      setActivePortal('community');
      localStorage.setItem('activePortal', 'community');
    } else {
      // Try to load from localStorage, default to workspace
      const savedPortal = localStorage.getItem('activePortal') as 'workspace' | 'community' | null;
      if (savedPortal) {
        setActivePortal(savedPortal);
      }
    }
  }, [pathname]);

  const handleLogout = () => {
    logout();
  }

  const loggedIn = isLoggedIn();

  const workspaceLinks = [
    { name: 'Home', href: '/', icon: HomeIcon },
    { name: 'Dashboard', href: '/dashboard', icon: DashboardIcon },
    { name: 'Projects', href: '/projects', icon: FolderIcon },
    { name: 'Analysis', href: '/analysis', icon: AnalyticsIcon },
    { name: 'Storage', href: '/storage', icon: StorageIcon },
  ];

  const communityLinks = [
    { name: 'Publications', href: '/community/publications', icon: ArticleIcon },
    { name: 'Users', href: '/community/users', icon: PeopleIcon },
    { name: 'Forums', href: '/community/forums', icon: ForumIcon },
  ];

  const navLinks = activePortal === 'workspace' ? workspaceLinks : communityLinks;

  const handlePortalSwitch = (portal: 'workspace' | 'community') => {
    setActivePortal(portal);
    localStorage.setItem('activePortal', portal);
    if (portal === 'workspace') {
      router.push('/dashboard');
    } else {
      router.push('/community/publications');
    }
  };

  const isActive = (path: string) => pathname === path;

  return (
    <AppBar 
      position="static" 
      sx={{ 
        background: '#667eea',
        boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)'
      }}
    >
      <Toolbar className="justify-between py-1">
        {/* Logo and App Name */}
        <Link href="/" className="flex items-center space-x-3 hover:opacity-80 transition-opacity">
          <div className="relative w-10 h-10 bg-white rounded-full p-1.5">
            <Image
              src="/images/logo.png"
              alt="SatelliteIP Logo"
              fill
              sizes="(max-width: 640px) 40px, 40px"
              priority
              className="object-contain"
            />
          </div>
          <span className="text-2xl font-bold text-white tracking-tight">SatelliteIP</span>
        </Link>

        {/* Portal Switcher */}
        <div className="hidden md:flex items-center ml-6 bg-white bg-opacity-15 rounded-lg p-1">
          <button
            onClick={() => handlePortalSwitch('workspace')}
            className={`flex items-center space-x-2 px-4 py-2 rounded-md transition-all duration-200 ${
              activePortal === 'workspace'
                ? 'bg-white text-purple-600 font-semibold shadow-md'
                : 'text-white hover:bg-white hover:bg-opacity-10'
            }`}
          >
            <WorkIcon sx={{ fontSize: 18 }} />
            <span className="text-sm">Workspace</span>
          </button>
          <button
            onClick={() => handlePortalSwitch('community')}
            className={`flex items-center space-x-2 px-4 py-2 rounded-md transition-all duration-200 ${
              activePortal === 'community'
                ? 'bg-white text-purple-600 font-semibold shadow-md'
                : 'text-white hover:bg-white hover:bg-opacity-10'
            }`}
          >
            <GroupsIcon sx={{ fontSize: 18 }} />
            <span className="text-sm">Community</span>
          </button>
        </div>

        {/* Desktop Navigation */}
        <nav className="hidden md:flex items-center space-x-1 ml-auto">{navLinks.map((link) => {
            const Icon = link.icon;
            return (
              <Link
                key={link.href}
                href={link.href}
                className={`flex items-center space-x-2 px-4 py-2 rounded-lg transition-all duration-200 ${
                  isActive(link.href)
                    ? 'bg-white bg-opacity-20 text-white font-semibold shadow-md'
                    : 'text-white text-opacity-90 hover:bg-white hover:bg-opacity-10 hover:text-white'
                }`}
              >
                <Icon sx={{ fontSize: 20 }} />
                <span className="text-sm">{link.name}</span>
              </Link>
            );
          })}

          {/* Right Section: Search, Messages, User */}
          <div className="ml-4 pl-4 border-l border-white border-opacity-30 flex items-center space-x-2">
            {/* Search Icon */}
            <IconButton
              size="small"
              sx={{
                color: 'white',
                '&:hover': { backgroundColor: 'rgba(255, 255, 255, 0.1)' }
              }}
              aria-label="Search"
            >
              <SearchIcon />
            </IconButton>

            {/* Messages Icon with Badge */}
            <IconButton
              size="small"
              sx={{
                color: 'white',
                '&:hover': { backgroundColor: 'rgba(255, 255, 255, 0.1)' }
              }}
              aria-label="Messages"
            >
              <Badge badgeContent={3} color="error">
                <MessageIcon />
              </Badge>
            </IconButton>

            {/* User Section */}
            {loggedIn ? (
              <ButtonBase
                onClick={handleLogout}
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 1.5,
                  px: 2,
                  py: 1,
                  borderRadius: 2,
                  transition: 'all 0.2s',
                  '&:hover': {
                    backgroundColor: 'rgba(255, 255, 255, 0.1)',
                  }
                }}
              >
                <Avatar sx={{ width: 32, height: 32, bgcolor: 'white', color: '#667eea' }}>
                  {user?.username?.charAt(0) || 'U'}
                </Avatar>
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}>
                  <Typography variant="body2" sx={{ color: 'white', fontWeight: 600, lineHeight: 1.2 }}>
                    {user?.username || 'User'}
                  </Typography>
                  <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.8)', fontSize: '0.65rem' }}>
                    Click to logout
                  </Typography>
                </Box>
                <LogoutIcon sx={{ fontSize: 18, color: 'white', ml: 0.5 }} />
              </ButtonBase>
            ) : (
              <Link
                href="/auth/login"
                className="flex items-center space-x-2 px-4 py-2 bg-white text-purple-600 rounded-lg hover:bg-opacity-90 transition-all duration-200 font-semibold shadow-md"
              >
                <LoginIcon sx={{ fontSize: 20 }} />
                <span>Sign In</span>
              </Link>
            )}
          </div>
        </nav>

        {/* Mobile menu button */}
        <div className="md:hidden">
          <button
            type="button"
            aria-label="Toggle navigation menu"
            aria-expanded={isMobileMenuOpen}
            className="text-white hover:bg-white hover:bg-opacity-10 focus:outline-none focus:ring-2 focus:ring-white focus:ring-opacity-50 rounded-lg p-2 transition-all"
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          >
            {isMobileMenuOpen ? (
              <svg className="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            ) : (
              <svg className="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            )}
          </button>
        </div>
      </Toolbar>

      {/* Mobile Menu */}
      <div 
        className={`${
          isMobileMenuOpen ? 'max-h-screen opacity-100' : 'max-h-0 opacity-0'
        } md:hidden overflow-hidden transition-all duration-300 ease-in-out bg-white bg-opacity-10 backdrop-blur-lg`}
      >
        <div className="px-4 pt-2 pb-4 space-y-1">
          {/* Mobile Portal Switcher */}
          <div className="flex items-center space-x-2 mb-4 bg-white bg-opacity-10 rounded-lg p-1">
            <button
              onClick={() => handlePortalSwitch('workspace')}
              className={`flex items-center justify-center space-x-2 px-3 py-2 rounded-md transition-all duration-200 flex-1 ${
                activePortal === 'workspace'
                  ? 'bg-white text-purple-600 font-semibold shadow-md'
                  : 'text-white'
              }`}
            >
              <WorkIcon sx={{ fontSize: 18 }} />
              <span className="text-sm">Workspace</span>
            </button>
            <button
              onClick={() => handlePortalSwitch('community')}
              className={`flex items-center justify-center space-x-2 px-3 py-2 rounded-md transition-all duration-200 flex-1 ${
                activePortal === 'community'
                  ? 'bg-white text-purple-600 font-semibold shadow-md'
                  : 'text-white'
              }`}
            >
              <GroupsIcon sx={{ fontSize: 18 }} />
              <span className="text-sm">Community</span>
            </button>
          </div>

          {navLinks.map((link) => {
            const Icon = link.icon;
            return (
              <Link
                key={link.href}
                href={link.href}
                onClick={() => setIsMobileMenuOpen(false)}
                className={`flex items-center space-x-3 px-4 py-3 rounded-lg transition-all ${
                  isActive(link.href)
                    ? 'bg-white bg-opacity-20 text-white font-semibold'
                    : 'text-white hover:bg-white hover:bg-opacity-10'
                }`}
              >
                <Icon sx={{ fontSize: 22 }} />
                <span>{link.name}</span>
              </Link>
            );
          })}

          {/* Mobile User Section */}
          <div className="pt-4 mt-4 border-t border-white border-opacity-30">
            {loggedIn ? (
              <ButtonBase
                onClick={() => {
                  handleLogout();
                  setIsMobileMenuOpen(false);
                }}
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 2,
                  width: '100%',
                  px: 2,
                  py: 2,
                  borderRadius: 2,
                  '&:hover': { backgroundColor: 'rgba(255,255,255,0.1)' }
                }}
              >
                <Avatar sx={{ width: 36, height: 36, bgcolor: 'white', color: '#667eea' }}>
                  {user?.username?.charAt(0) || 'U'}
                </Avatar>
                <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}>
                  <Typography variant="body1" sx={{ color: 'white', fontWeight: 600 }}>
                    {user?.username || 'User'}
                  </Typography>
                  <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.8)' }}>
                    Tap to logout
                  </Typography>
                </Box>
                <LogoutIcon sx={{ color: 'white' }} />
              </ButtonBase>
            ) : (
              <Link
                href="/auth/login"
                onClick={() => setIsMobileMenuOpen(false)}
                className="flex items-center space-x-3 px-4 py-3 bg-white text-purple-600 rounded-lg hover:bg-opacity-90 transition-all font-semibold justify-center"
              >
                <LoginIcon />
                <span>Sign In</span>
              </Link>
            )}
          </div>
        </div>
      </div>
    </AppBar>
  );
}
