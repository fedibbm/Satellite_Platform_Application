import axios from 'axios';
import {
  Conversation,
  Message,
  SendMessageRequest,
  PageResponse
} from '@/types/messaging';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';

// Create axios instance with auth token
const createAuthenticatedAxios = () => {
  const token = localStorage.getItem('token');
  return axios.create({
    baseURL: `${API_BASE_URL}/api/messaging`,
    headers: {
      'Authorization': token ? `Bearer ${token}` : '',
      'Content-Type': 'application/json'
    }
  });
};

export const messagingApi = {
  // Get user's conversations
  getConversations: async (page = 0, size = 20): Promise<PageResponse<Conversation>> => {
    const api = createAuthenticatedAxios();
    const response = await api.get('/conversations', {
      params: { page, size }
    });
    return response.data;
  },

  // Get messages in a conversation
  getMessages: async (conversationId: string, page = 0, size = 50): Promise<PageResponse<Message>> => {
    const api = createAuthenticatedAxios();
    const response = await api.get(`/conversations/${conversationId}/messages`, {
      params: { page, size }
    });
    return response.data;
  },

  // Send text message (REST fallback)
  sendTextMessage: async (request: SendMessageRequest): Promise<Message> => {
    const api = createAuthenticatedAxios();
    const response = await api.post('/messages', request);
    return response.data;
  },

  // Mark message as read (REST fallback)
  markAsRead: async (messageId: string): Promise<Message> => {
    const api = createAuthenticatedAxios();
    const response = await api.put(`/messages/${messageId}/read`);
    return response.data;
  },

  // Upload and send image
  sendImageMessage: async (recipientId: string, file: File): Promise<Message> => {
    const api = createAuthenticatedAxios();
    const formData = new FormData();
    formData.append('recipientId', recipientId);
    formData.append('image', file);
    
    const response = await api.post('/messages/image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    return response.data;
  },

  // Get unread message count
  getUnreadCount: async (): Promise<{ unreadCount: number }> => {
    const api = createAuthenticatedAxios();
    const response = await api.get('/unread-count');
    return response.data;
  },

  // Get currently online users
  getOnlineUsers: async (): Promise<Record<string, number>> => {
    const api = createAuthenticatedAxios();
    const response = await api.get('/online-users');
    return response.data;
  },

  // Check if a specific user is online
  checkUserOnline: async (userId: string): Promise<{ online: boolean }> => {
    const api = createAuthenticatedAxios();
    const response = await api.get(`/users/${userId}/online`);
    return response.data;
  }
};
