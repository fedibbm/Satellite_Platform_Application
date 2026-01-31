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

  // Initialize WebSocket connection
  useEffect(() => {
    const initWebSocket = async () => {
      try {
        await wsService.connect();
        setIsConnected(true);
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

  // Load conversations on mount
  useEffect(() => {
    loadConversations();
    loadUnreadCount();
  }, []);

  const loadConversations = async () => {
    try {
      setLoading(true);
      const response = await messagingApi.getConversations(0, 50);
      setConversations(response.content);
    } catch (error) {
      console.error('Failed to load conversations:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadMessages = async (conversationId: string) => {
    try {
      const response = await messagingApi.getMessages(conversationId, 0, 50);
      setMessages(prev => ({
        ...prev,
        [conversationId]: response.content.reverse() // Reverse to show oldest first
      }));
      
      // Mark all messages as read
      const unreadMessages = response.content.filter(msg => !msg.readAt && msg.recipientId !== getCurrentUserId());
      unreadMessages.forEach(msg => {
        if (isConnected) {
          wsService.sendReadReceipt(msg.id);
        }
      });
    } catch (error) {
      console.error('Failed to load messages:', error);
    }
  };

  const loadUnreadCount = async () => {
    try {
      const response = await messagingApi.getUnreadCount();
      setUnreadCount(response.unreadCount);
    } catch (error) {
      console.error('Failed to load unread count:', error);
    }
  };

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

  const handleReadReceipt = (receipt) => {
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
    return localStorage.getItem('userId') || localStorage.getItem('email') || '';
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
    getCurrentUserId
  };
};
