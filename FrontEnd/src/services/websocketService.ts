import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {
  Message,
  TypingIndicator,
  PresenceUpdate,
  ReadReceipt,
  SendMessageRequest
} from '@/types/messaging';

// Connect directly to backend WebSocket (cookies set with domain=localhost work across ports)
const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:8080/ws';

export type MessageCallback = (message: Message) => void;
export type TypingCallback = (indicator: TypingIndicator) => void;
export type PresenceCallback = (presence: PresenceUpdate) => void;
export type ReceiptCallback = (receipt: ReadReceipt) => void;
export type ErrorCallback = (error: any) => void;

class WebSocketService {
  private client: Client | null = null;
  private connected = false;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 2000;

  // Callbacks
  private messageCallbacks: MessageCallback[] = [];
  private typingCallbacks: TypingCallback[] = [];
  private presenceCallbacks: PresenceCallback[] = [];
  private receiptCallbacks: ReceiptCallback[] = [];
  private errorCallbacks: ErrorCallback[] = [];
  private onConnectCallback: (() => void) | null = null;
  private onDisconnectCallback: (() => void) | null = null;

  constructor() {
    if (typeof window !== 'undefined') {
      // Only initialize in browser environment
      // Don't initialize immediately - wait for connect() call
    }
  }

  private getCookie(name: string): string | null {
    if (typeof document === 'undefined') return null;
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
      return parts.pop()?.split(';').shift() || null;
    }
    return null;
  }

  private initializeClient() {
    // Check if user is authenticated by checking for user in localStorage
    const userStr = typeof window !== 'undefined' ? localStorage.getItem('user') : null;
    if (!userStr) {
      console.warn('No user found, WebSocket connection will fail');
      return;
    }

    console.log('Initializing WebSocket client with cookie-based authentication');

    // HTTP-only cookies are automatically sent with the WebSocket handshake
    // No need to extract or pass tokens manually
    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL, null, {
        // Browser will automatically include HTTP-only cookies in handshake
        withCredentials: true
      }),
      connectHeaders: {},
      debug: (str) => {
        console.log('[STOMP]', str);
      },
      reconnectDelay: this.reconnectDelay,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log('✅ WebSocket Connected');
        this.connected = true;
        this.reconnectAttempts = 0;
        this.setupSubscriptions();
        if (this.onConnectCallback) {
          this.onConnectCallback();
        }
      },
      onDisconnect: () => {
        console.log('❌ WebSocket Disconnected');
        this.connected = false;
        if (this.onDisconnectCallback) {
          this.onDisconnectCallback();
        }
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
        this.errorCallbacks.forEach(cb => cb(frame));
      }
    });
  }

  private setupSubscriptions() {
    if (!this.client) return;

    // Subscribe to private message queue
    this.client.subscribe('/user/queue/messages', (message) => {
      try {
        const msg: Message = JSON.parse(message.body);
        this.messageCallbacks.forEach(cb => cb(msg));
      } catch (error) {
        console.error('Error parsing message:', error);
      }
    });

    // Subscribe to typing indicators
    this.client.subscribe('/user/queue/typing', (message) => {
      try {
        const indicator: TypingIndicator = JSON.parse(message.body);
        this.typingCallbacks.forEach(cb => cb(indicator));
      } catch (error) {
        console.error('Error parsing typing indicator:', error);
      }
    });

    // Subscribe to read receipts
    this.client.subscribe('/user/queue/receipts', (message) => {
      try {
        const receipt: ReadReceipt = JSON.parse(message.body);
        this.receiptCallbacks.forEach(cb => cb(receipt));
      } catch (error) {
        console.error('Error parsing read receipt:', error);
      }
    });

    // Subscribe to presence updates
    this.client.subscribe('/topic/presence', (message) => {
      try {
        const presence: PresenceUpdate = JSON.parse(message.body);
        this.presenceCallbacks.forEach(cb => cb(presence));
      } catch (error) {
        console.error('Error parsing presence update:', error);
      }
    });

    // Subscribe to errors
    this.client.subscribe('/user/queue/errors', (message) => {
      try {
        const error = JSON.parse(message.body);
        console.error('Server error:', error);
        this.errorCallbacks.forEach(cb => cb(error));
      } catch (error) {
        console.error('Error parsing error message:', error);
      }
    });
  }

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.connected) {
        resolve();
        return;
      }

      if (!this.client) {
        this.initializeClient();
      }

      if (!this.client) {
        reject(new Error('Failed to initialize WebSocket client'));
        return;
      }

      this.onConnectCallback = () => resolve();
      this.client.activate();

      // Timeout after 10 seconds
      setTimeout(() => {
        if (!this.connected) {
          reject(new Error('WebSocket connection timeout'));
        }
      }, 10000);
    });
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.connected = false;
    }
  }

  isConnected(): boolean {
    return this.connected;
  }

  // Send message via WebSocket
  sendMessage(request: SendMessageRequest) {
    if (!this.client || !this.connected) {
      throw new Error('WebSocket not connected');
    }

    this.client.publish({
      destination: '/app/chat.send',
      body: JSON.stringify(request)
    });
  }

  // Send typing indicator
  sendTyping(recipientId: string, typing: boolean) {
    if (!this.client || !this.connected) return;

    this.client.publish({
      destination: '/app/chat.typing',
      body: JSON.stringify({ recipientId, typing })
    });
  }

  // Send read receipt
  sendReadReceipt(messageId: string) {
    if (!this.client || !this.connected) return;

    this.client.publish({
      destination: '/app/chat.read',
      body: messageId
    });
  }

  // Check user status
  checkUserStatus(userId: string) {
    if (!this.client || !this.connected) return;

    this.client.publish({
      destination: '/app/chat.status',
      body: userId
    });
  }

  // Register callbacks
  onMessage(callback: MessageCallback): () => void {
    this.messageCallbacks.push(callback);
    return () => {
      this.messageCallbacks = this.messageCallbacks.filter(cb => cb !== callback);
    };
  }

  onTyping(callback: TypingCallback): () => void {
    this.typingCallbacks.push(callback);
    return () => {
      this.typingCallbacks = this.typingCallbacks.filter(cb => cb !== callback);
    };
  }

  onPresence(callback: PresenceCallback): () => void {
    this.presenceCallbacks.push(callback);
    return () => {
      this.presenceCallbacks = this.presenceCallbacks.filter(cb => cb !== callback);
    };
  }

  onReadReceipt(callback: ReceiptCallback): () => void {
    this.receiptCallbacks.push(callback);
    return () => {
      this.receiptCallbacks = this.receiptCallbacks.filter(cb => cb !== callback);
    };
  }

  onError(callback: ErrorCallback): () => void {
    this.errorCallbacks.push(callback);
    return () => {
      this.errorCallbacks = this.errorCallbacks.filter(cb => cb !== callback);
    };
  }

  setOnConnect(callback: () => void) {
    this.onConnectCallback = callback;
  }

  setOnDisconnect(callback: () => void) {
    this.onDisconnectCallback = callback;
  }
}

// Singleton instance
export const wsService = new WebSocketService();
