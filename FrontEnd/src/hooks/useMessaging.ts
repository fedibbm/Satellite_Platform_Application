'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import { messagingApi } from '@/services/messagingApi';
import { wsService } from '@/services/websocketService';
import {
  Message,
  Conversation,
  TypingIndicator,
  PresenceUpdate,
  SendMessageRequest
} from '@/types/messaging';

export const useMessaging = () => {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [messages, setMessages] = useState<Record<string, Message[]>>({});
  const [unreadCount, setUnreadCount] = useState(0);
  const [typingUsers, setTypingUsers] = useState<Record<string, boolean>>({});
  const [onlineUsers, setOnlineUsers] = useState<Record<string, boolean>>({});
  const [isConnected, setIsConnected] = useState(false);
  const [loading, setLoading] = useState(false);
  
  const typingTimeouts = useRef<Record<string, NodeJS.Timeout>>({});
  const loadedOnce = useRef(false);

  // Initialize WebSocket connection
  useEffect(() => {
    const initWebSocket = async () => {
      try {
        await wsService.connect();
        setIsConnected(true);
        
        // Fetch current online users after connection
        try {
          const onlineUsersData = await messagingApi.getOnlineUsers();
          const onlineMap: Record<string, boolean> = {};
          Object.keys(onlineUsersData).forEach(userId => {
            onlineMap[userId] = true;
          });
          setOnlineUsers(onlineMap);
          console.log('Loaded initial online users:', onlineMap);
        } catch (error) {
          console.error('Failed to load online users:', error);
        }
        
        // Reset loadedOnce when WebSocket connects so conversations load
        loadedOnce.current = false;
      } catch (error) {
        console.error('Failed to connect WebSocket:', error);
        setIsConnected(false);
      }
    };

    initWebSocket();

    // Setup WebSocket event handlers
    const unsubMessage = wsService.onMessage((message) => {
      handleIncomingMessage(message);
    });

    const unsubTyping = wsService.onTyping((indicator) => {
      handleTypingIndicator(indicator);
    });

    const unsubPresence = wsService.onPresence((presence) => {
      handlePresenceUpdate(presence);
    });

    const unsubReceipt = wsService.onReadReceipt((receipt) => {
      handleReadReceipt(receipt);
    });

    return () => {
      unsubMessage();
      unsubTyping();
      unsubPresence();
      unsubReceipt();
      wsService.disconnect();
    };
  }, []);

  const loadConversations = useCallback(async () => {
    try {
      setLoading(true);
      const response = await messagingApi.getConversations(0, 50);
      setConversations(response.content);
    } catch (error) {
      console.error('Failed to load conversations:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadUnreadCount = useCallback(async () => {
    try {
      const response = await messagingApi.getUnreadCount();
      setUnreadCount(response.unreadCount);
    } catch (error) {
      console.error('Failed to load unread count:', error);
    }
  }, []);

  // Load conversations on mount (only once)
  useEffect(() => {
    if (!loadedOnce.current) {
      loadedOnce.current = true;
      loadConversations();
      loadUnreadCount();
    }
  }, [loadConversations, loadUnreadCount]);

  const loadMessages = useCallback(async (conversationId: string) => {
    try {
      const response = await messagingApi.getMessages(conversationId, 0, 50);
      // Sort messages by timestamp (oldest first, newest at bottom)
      const sortedMessages = [...response.content].sort((a, b) => 
        new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime()
      );
      setMessages(prev => ({
        ...prev,
        [conversationId]: sortedMessages
      }));
      
      // Mark only the latest unread message as read (backend will mark others)
      const unreadMessages = sortedMessages.filter(
        msg => !msg.readAt && msg.senderId !== getCurrentUserId()
      );
      if (unreadMessages.length > 0 && isConnected) {
        // Only send one read receipt for the latest unread message
        const latestUnread = unreadMessages[unreadMessages.length - 1];
        wsService.sendReadReceipt(latestUnread.id);
      }
    } catch (error) {
      console.error('Failed to load messages:', error);
    }
  }, [isConnected]);

  const handleIncomingMessage = (message: Message) => {
    // Add message to the conversation
    setMessages(prev => ({
      ...prev,
      [message.conversationId]: [...(prev[message.conversationId] || []), message]
    }));

    // Update conversation list
    setConversations(prev => {
      const existing = prev.find(c => c.id === message.conversationId);
      if (existing) {
        // Move to top and update preview
        const updated = {
          ...existing,
          lastMessageAt: message.sentAt,
          lastMessagePreview: message.content,
          lastMessageType: message.messageType,
          lastMessageSenderId: message.senderId,
          unreadCount: message.senderId !== getCurrentUserId() ? existing.unreadCount + 1 : existing.unreadCount
        };
        return [updated, ...prev.filter(c => c.id !== message.conversationId)];
      }
      // If conversation doesn't exist, reload conversations
      loadConversations();
      return prev;
    });

    // Update unread count if message is from someone else
    if (message.senderId !== getCurrentUserId()) {
      setUnreadCount(prev => prev + 1);
    }
  };

  const handleTypingIndicator = (indicator: TypingIndicator) => {
    setTypingUsers(prev => ({
      ...prev,
      [indicator.senderId]: indicator.typing
    }));

    // Clear typing indicator after 5 seconds
    if (indicator.typing) {
      if (typingTimeouts.current[indicator.senderId]) {
        clearTimeout(typingTimeouts.current[indicator.senderId]);
      }
      typingTimeouts.current[indicator.senderId] = setTimeout(() => {
        setTypingUsers(prev => ({
          ...prev,
          [indicator.senderId]: false
        }));
      }, 5000);
    }
  };

  const handlePresenceUpdate = (presence: PresenceUpdate) => {
    setOnlineUsers(prev => ({
      ...prev,
      [presence.userId]: presence.online
    }));
    
    // Update conversation list with online status
    setConversations(prev => prev.map(conv => 
      conv.otherParticipantId === presence.userId
        ? { ...conv, otherParticipantOnline: presence.online }
        : conv
    ));
  };

  const handleReadReceipt = (receipt: { messageId: string; readerId: string; readAt: string }) => {
    // Update message status to READ
    setMessages(prev => {
      const updated = { ...prev };
      Object.keys(updated).forEach(convId => {
        updated[convId] = updated[convId].map(msg =>
          msg.id === receipt.messageId
            ? { ...msg, readAt: receipt.readAt, status: 'READ' as any }
            : msg
        );
      });
      return updated;
    });
  };

  const sendMessage = async (recipientId: string, content: string, conversationId?: string) => {
    const request: SendMessageRequest = { recipientId, content };

    try {
      if (isConnected) {
        // Send via WebSocket for instant delivery
        wsService.sendMessage(request);
      } else {
        // Fallback to REST API
        const message = await messagingApi.sendTextMessage(request);
        handleIncomingMessage(message);
      }
    } catch (error) {
      console.error('Failed to send message:', error);
      throw error;
    }
  };

  const sendTyping = (recipientId: string, typing: boolean) => {
    if (isConnected) {
      wsService.sendTyping(recipientId, typing);
    }
  };

  const markAsRead = (messageId: string) => {
    if (isConnected) {
      wsService.sendReadReceipt(messageId);
    }
  };

  const sendImage = async (recipientId: string, file: File) => {
    try {
      const message = await messagingApi.sendImageMessage(recipientId, file);
      handleIncomingMessage(message);
      return message;
    } catch (error) {
      console.error('Failed to send image:', error);
      throw error;
    }
  };

  const getCurrentUserId = (): string => {
    // Get from localStorage or auth context
    if (typeof window === 'undefined') return '';
    
    // Try getting from user object first
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        if (user.id) return user.id;
        if (user.email) return user.email;
      } catch (e) {
        console.error('Failed to parse user from localStorage:', e);
      }
    }
    
    return localStorage.getItem('userId') || localStorage.getItem('email') || '';
  };

  const getCurrentUserName = (): string => {
    // Get user's name from localStorage
    if (typeof window === 'undefined') return 'User';
    
    // Try getting from user object first
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        if (user.username) return user.username;
        if (user.name) return user.name;
        if (user.email) return user.email;
      } catch (e) {
        console.error('Failed to parse user from localStorage:', e);
      }
    }
    
    // Fallback to direct keys
    return localStorage.getItem('username') || 
           localStorage.getItem('userName') || 
           localStorage.getItem('name') || 
           localStorage.getItem('email') || 
           'User';
  };

  return {
    conversations,
    messages,
    unreadCount,
    typingUsers,
    onlineUsers,
    isConnected,
    loading,
    loadConversations,
    loadMessages,
    sendMessage,
    sendTyping,
    sendImage,
    markAsRead,
    getCurrentUserId,
    getCurrentUserName,
  };
};
