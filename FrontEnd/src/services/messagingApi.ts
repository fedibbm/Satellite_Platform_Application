import { httpClient } from '@/utils/api/http-client';
import {
  Conversation,
  Message,
  SendMessageRequest,
  PageResponse
} from '@/types/messaging';

const API_BASE_URL = '/api/messaging';

export const messagingApi = {
  // Get user's conversations
  getConversations: async (page = 0, size = 20): Promise<PageResponse<Conversation>> => {
    const response = await httpClient.get(`${API_BASE_URL}/conversations?page=${page}&size=${size}`, {
      requiresAuth: true
    });
    return response;
  },

  // Get messages in a conversation
  getMessages: async (conversationId: string, page = 0, size = 50): Promise<PageResponse<Message>> => {
    const response = await httpClient.get(`${API_BASE_URL}/conversations/${conversationId}/messages?page=${page}&size=${size}`, {
      requiresAuth: true
    });
    return response;
  },

  // Send text message (REST fallback)
  sendTextMessage: async (request: SendMessageRequest): Promise<Message> => {
    const response = await httpClient.post(`${API_BASE_URL}/messages`, request, {
      requiresAuth: true
    });
    return response;
  },

  // Mark message as read (REST fallback)
  markAsRead: async (messageId: string): Promise<Message> => {
    const response = await httpClient.put(`${API_BASE_URL}/messages/${messageId}/read`, {}, {
      requiresAuth: true
    });
    return response;
  },

  // Upload and send image
  sendImageMessage: async (recipientId: string, file: File): Promise<Message> => {
    const formData = new FormData();
    formData.append('recipientId', recipientId);
    formData.append('image', file);
    
    const response = await httpClient.post(`${API_BASE_URL}/messages/image`, formData, {
      requiresAuth: true
    });
    return response;
  },

  // Get unread message count
  getUnreadCount: async (): Promise<{ unreadCount: number }> => {
    const response = await httpClient.get(`${API_BASE_URL}/unread-count`, {
      requiresAuth: true
    });
    return response;
  },

  // Get currently online users
  getOnlineUsers: async (): Promise<Record<string, number>> => {
    const response = await httpClient.get(`${API_BASE_URL}/online-users`, {
      requiresAuth: true
    });
    return response;
  },

  // Check if a specific user is online
  checkUserOnline: async (userId: string): Promise<{ online: boolean }> => {
    const response = await httpClient.get(`${API_BASE_URL}/users/${userId}/online`, {
      requiresAuth: true
    });
    return response;
  }
};
