'use client';

import { useState, useEffect, useMemo } from 'react';

/**
 * Lightweight hook that only tracks unread message count
 * without establishing WebSocket connection.
 * Use this in components that only need the unread badge.
 */
export const useUnreadCount = () => {
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    // Try to get cached count from localStorage
    const cached = localStorage.getItem('unreadMessageCount');
    if (cached) {
      try {
        setUnreadCount(parseInt(cached, 10) || 0);
      } catch (e) {
        setUnreadCount(0);
      }
    }

    // Listen for storage events from other tabs/windows
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === 'unreadMessageCount' && e.newValue) {
        try {
          setUnreadCount(parseInt(e.newValue, 10) || 0);
        } catch (err) {
          setUnreadCount(0);
        }
      }
    };

    window.addEventListener('storage', handleStorageChange);
    
    return () => {
      window.removeEventListener('storage', handleStorageChange);
    };
  }, []);

  const updateUnreadCount = useMemo(() => (count: number) => {
    setUnreadCount(count);
    localStorage.setItem('unreadMessageCount', count.toString());
  }, []);

  return { unreadCount, updateUnreadCount };
};
