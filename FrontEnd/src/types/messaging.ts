// Messaging types

export enum MessageType {
  TEXT = 'TEXT',
  IMAGE = 'IMAGE'
}

export enum MessageStatus {
  SENT = 'SENT',
  DELIVERED = 'DELIVERED',
  READ = 'READ'
}

export interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  recipientId: string;
  messageType: MessageType;
  content: string;
  imageUrl: string | null;
  sentAt: string;
  readAt: string | null;
  status: MessageStatus;
  senderName?: string;
  senderAvatar?: string;
}

export interface Conversation {
  id: string;
  createdAt: string;
  lastMessageAt: string;
  lastMessagePreview: string;
  lastMessageType: MessageType;
  lastMessageSenderId: string;
  unreadCount: number;
  otherParticipantId: string;
  otherParticipantName: string | null;
  otherParticipantAvatar: string | null;
  otherParticipantOnline: boolean | null;
}

export interface TypingIndicator {
  senderId: string;
  recipientId: string;
  typing: boolean;
  timestamp: number;
}

export interface PresenceUpdate {
  userId: string;
  online: boolean;
  timestamp: number;
}

export interface ReadReceipt {
  messageId: string;
  readBy: string;
  readAt: string;
}

export interface SendMessageRequest {
  recipientId: string;
  content: string;
}

export interface PageResponse<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
