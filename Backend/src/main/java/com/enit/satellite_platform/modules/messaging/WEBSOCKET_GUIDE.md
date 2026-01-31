# WebSocket Real-Time Messaging - Complete Guide

## Overview
This guide covers the WebSocket integration for real-time messaging features in the Satellite Platform Application.

## Features Implemented ✅
- ✅ Real-time message delivery
- ✅ Typing indicators
- ✅ Read receipts
- ✅ User presence tracking (online/offline)
- ✅ JWT authentication for WebSocket connections
- ✅ Private message queues
- ✅ Error handling

## WebSocket Endpoints

### Connection URL
```
ws://localhost:8080/ws
```
With SockJS fallback: `http://localhost:8080/ws/`

### Destinations

#### Subscribe (Client Receives)
- `/user/queue/messages` - Private messages for the authenticated user
- `/user/queue/typing` - Typing indicators from other users
- `/user/queue/receipts` - Read receipts for sent messages
- `/user/queue/status` - User online/offline status responses
- `/user/queue/errors` - Error messages
- `/topic/presence` - Global presence updates (all users)

#### Publish (Client Sends)
- `/app/chat.send` - Send a text message
- `/app/chat.typing` - Send typing indicator
- `/app/chat.read` - Mark message as read
- `/app/chat.status` - Check if a user is online

## Authentication

### JWT Token
WebSocket connection requires JWT authentication via:
1. **Native Header** (Recommended): `Authorization: Bearer <token>`
2. **Query Parameter**: `token=<token>`

### Example with SockJS + STOMP.js

```javascript
// Your JWT token
const token = "eyJhbGciOiJIUzUxMiJ9...";

// Connect
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

// Connect with authentication
stompClient.connect(
    { Authorization: `Bearer ${token}` },
    (frame) => {
        console.log('Connected:', frame);
        
        // Subscribe to private message queue
        stompClient.subscribe('/user/queue/messages', (message) => {
            const msg = JSON.parse(message.body);
            console.log('New message:', msg);
            // Update UI with new message
        });
        
        // Subscribe to presence updates
        stompClient.subscribe('/topic/presence', (presence) => {
            const status = JSON.parse(presence.body);
            console.log('User status:', status);
            // Update UI: userId is online/offline
        });
        
        // Subscribe to typing indicators
        stompClient.subscribe('/user/queue/typing', (indicator) => {
            const typing = JSON.parse(indicator.body);
            console.log('Typing:', typing);
            // Show "User is typing..." indicator
        });
        
        // Subscribe to read receipts
        stompClient.subscribe('/user/queue/receipts', (receipt) => {
            const r = JSON.parse(receipt.body);
            console.log('Message read:', r);
            // Update message status to "Read"
        });
    },
    (error) => {
        console.error('Connection error:', error);
    }
);
```

## Usage Examples

### 1. Send a Message

```javascript
// Send message
stompClient.send('/app/chat.send', {}, JSON.stringify({
    recipientId: '6978eff038aa1e365ee5fcfd',
    content: 'Hello from WebSocket!'
}));
```

**Payload:**
```json
{
    "recipientId": "6978eff038aa1e365ee5fcfd",
    "content": "Hello from WebSocket!"
}
```

**Recipient receives at** `/user/queue/messages`:
```json
{
    "id": "697e123...",
    "conversationId": "697e009...",
    "senderId": "thematician@example.com",
    "recipientId": "6978eff038aa1e365ee5fcfd",
    "messageType": "TEXT",
    "content": "Hello from WebSocket!",
    "imageUrl": null,
    "sentAt": "2026-01-31T14:30:00.000",
    "readAt": null,
    "status": "SENT"
}
```

### 2. Send Typing Indicator

```javascript
// User starts typing
stompClient.send('/app/chat.typing', {}, JSON.stringify({
    recipientId: '6978eff038aa1e365ee5fcfd',
    typing: true
}));

// User stops typing (after 3 seconds of inactivity)
setTimeout(() => {
    stompClient.send('/app/chat.typing', {}, JSON.stringify({
        recipientId: '6978eff038aa1e365ee5fcfd',
        typing: false
    }));
}, 3000);
```

**Payload:**
```json
{
    "recipientId": "6978eff038aa1e365ee5fcfd",
    "typing": true
}
```

**Recipient receives at** `/user/queue/typing`:
```json
{
    "senderId": "thematician@example.com",
    "recipientId": "6978eff038aa1e365ee5fcfd",
    "typing": true,
    "timestamp": 1738338600000
}
```

### 3. Mark Message as Read

```javascript
// User reads a message
stompClient.send('/app/chat.read', {}, '697e123...');
```

**Sender receives at** `/user/queue/receipts`:
```json
{
    "messageId": "697e123...",
    "readBy": "6978eff038aa1e365ee5fcfd",
    "readAt": "2026-01-31T14:35:00.000"
}
```

### 4. Check User Status

```javascript
// Check if user is online
stompClient.send('/app/chat.status', {}, '6978eff038aa1e365ee5fcfd');
```

**You receive at** `/user/queue/status`:
```json
{
    "userId": "6978eff038aa1e365ee5fcfd",
    "online": true
}
```

### 5. Monitor Global Presence

```javascript
// Subscribe to global presence updates
stompClient.subscribe('/topic/presence', (message) => {
    const status = JSON.parse(message.body);
    console.log(`${status.userId} is now ${status.online ? 'ONLINE' : 'OFFLINE'}`);
    
    // Update UI
    updateUserPresence(status.userId, status.online);
});
```

**Broadcasts when user connects/disconnects:**
```json
{
    "userId": "thematician@example.com",
    "online": true,
    "timestamp": 1738338600000
}
```

## Complete Client Implementation

### HTML
```html
<!DOCTYPE html>
<html>
<head>
    <title>WebSocket Messaging Test</title>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
</head>
<body>
    <h1>Real-Time Messaging</h1>
    
    <div id="connection-status">Disconnected</div>
    
    <div id="messages"></div>
    
    <input type="text" id="recipient" placeholder="Recipient ID" />
    <input type="text" id="message" placeholder="Message" />
    <button onclick="sendMessage()">Send</button>
    
    <div id="typing-indicator"></div>
    
    <script src="client.js"></script>
</body>
</html>
```

### JavaScript (client.js)
```javascript
// Configuration
const WS_URL = 'http://localhost:8080/ws';
const JWT_TOKEN = 'eyJhbGciOiJIUzUxMiJ9...'; // Your actual token

let stompClient = null;
let currentRecipient = null;
let typingTimeout = null;

// Connect to WebSocket
function connect() {
    const socket = new SockJS(WS_URL);
    stompClient = Stomp.over(socket);
    
    // Optional: Reduce debug output
    stompClient.debug = (msg) => console.log('[STOMP]', msg);
    
    stompClient.connect(
        { Authorization: `Bearer ${JWT_TOKEN}` },
        onConnected,
        onError
    );
}

function onConnected(frame) {
    console.log('Connected:', frame);
    document.getElementById('connection-status').textContent = 'Connected';
    
    // Subscribe to all channels
    stompClient.subscribe('/user/queue/messages', onMessageReceived);
    stompClient.subscribe('/user/queue/typing', onTypingIndicator);
    stompClient.subscribe('/user/queue/receipts', onReadReceipt);
    stompClient.subscribe('/user/queue/status', onStatusResponse);
    stompClient.subscribe('/user/queue/errors', onError);
    stompClient.subscribe('/topic/presence', onPresenceUpdate);
}

function onError(error) {
    console.error('WebSocket error:', error);
    document.getElementById('connection-status').textContent = 'Error';
}

function onMessageReceived(payload) {
    const message = JSON.parse(payload.body);
    console.log('Message received:', message);
    
    // Display message
    const messagesDiv = document.getElementById('messages');
    const msgElement = document.createElement('div');
    msgElement.textContent = `${message.senderId}: ${message.content}`;
    messagesDiv.appendChild(msgElement);
    
    // Auto-mark as read if it's incoming
    if (message.senderId !== getCurrentUserId()) {
        markAsRead(message.id);
    }
}

function onTypingIndicator(payload) {
    const indicator = JSON.parse(payload.body);
    console.log('Typing indicator:', indicator);
    
    const typingDiv = document.getElementById('typing-indicator');
    if (indicator.typing) {
        typingDiv.textContent = `${indicator.senderId} is typing...`;
    } else {
        typingDiv.textContent = '';
    }
}

function onReadReceipt(payload) {
    const receipt = JSON.parse(payload.body);
    console.log('Read receipt:', receipt);
    
    // Update message UI to show "Read"
    const messageElement = document.querySelector(`[data-message-id="${receipt.messageId}"]`);
    if (messageElement) {
        messageElement.classList.add('read');
    }
}

function onStatusResponse(payload) {
    const status = JSON.parse(payload.body);
    console.log('User status:', status);
    
    // Update UI based on status
}

function onPresenceUpdate(payload) {
    const presence = JSON.parse(payload.body);
    console.log('Presence update:', presence);
    
    // Update user list or status indicators
}

function sendMessage() {
    const recipientId = document.getElementById('recipient').value;
    const content = document.getElementById('message').value;
    
    if (!recipientId || !content) {
        alert('Please enter recipient and message');
        return;
    }
    
    stompClient.send('/app/chat.send', {}, JSON.stringify({
        recipientId: recipientId,
        content: content
    }));
    
    document.getElementById('message').value = '';
}

function sendTypingIndicator(isTyping) {
    const recipientId = document.getElementById('recipient').value;
    
    if (!recipientId) return;
    
    stompClient.send('/app/chat.typing', {}, JSON.stringify({
        recipientId: recipientId,
        typing: isTyping
    }));
}

function markAsRead(messageId) {
    stompClient.send('/app/chat.read', {}, messageId);
}

function checkUserStatus(userId) {
    stompClient.send('/app/chat.status', {}, userId);
}

function getCurrentUserId() {
    // Extract from JWT or store in session
    return 'thematician@example.com';
}

// Handle typing indicator on input
document.getElementById('message').addEventListener('input', () => {
    clearTimeout(typingTimeout);
    sendTypingIndicator(true);
    
    // Stop typing after 3 seconds of inactivity
    typingTimeout = setTimeout(() => {
        sendTypingIndicator(false);
    }, 3000);
});

// Connect on page load
window.addEventListener('load', connect);
```

## Integration with React/Vue

### React Hook Example
```javascript
import { useEffect, useRef, useState } from 'react';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

export const useWebSocket = (token) => {
    const stompClientRef = useRef(null);
    const [connected, setConnected] = useState(false);
    const [messages, setMessages] = useState([]);

    useEffect(() => {
        const socket = new SockJS('http://localhost:8080/ws');
        const client = Stomp.over(socket);

        client.connect(
            { Authorization: `Bearer ${token}` },
            () => {
                setConnected(true);
                
                client.subscribe('/user/queue/messages', (message) => {
                    const msg = JSON.parse(message.body);
                    setMessages(prev => [...prev, msg]);
                });
            }
        );

        stompClientRef.current = client;

        return () => {
            if (client.connected) {
                client.disconnect();
            }
        };
    }, [token]);

    const sendMessage = (recipientId, content) => {
        if (stompClientRef.current?.connected) {
            stompClientRef.current.send('/app/chat.send', {}, JSON.stringify({
                recipientId,
                content
            }));
        }
    };

    return { connected, messages, sendMessage };
};
```

## Testing with Thunder Client

Unfortunately, Thunder Client doesn't support WebSocket testing. Use:
- **Postman** (has WebSocket support)
- **Browser Console** (with the JavaScript code above)
- **wscat** command-line tool

### Using wscat
```bash
npm install -g wscat

# Connect (note: need to handle STOMP protocol manually)
wscat -c ws://localhost:8080/ws
```

## Troubleshooting

### Connection Fails
1. Check JWT token is valid: `curl http://localhost:8080/api/messaging/conversations -H "Authorization: Bearer YOUR_TOKEN"`
2. Check backend logs for authentication errors
3. Verify WebSocket endpoint is accessible: Browser should be able to access `http://localhost:8080/ws/info`

### Messages Not Received
1. Check subscription destination matches publish destination
2. Ensure user is authenticated
3. Check browser console for errors
4. Verify backend logs show message delivery

### CORS Issues
- WebSocket config already allows all origins in dev
- For production, update `setAllowedOriginPatterns()` in WebSocketConfig

## Architecture

### Components
- **WebSocketConfig**: STOMP/WebSocket configuration
- **WebSocketAuthInterceptor**: JWT authentication for WebSocket connections
- **UserPresenceService**: Track user online/offline status
- **WebSocketMessagingController**: Handle real-time message events

### Flow
1. Client connects to `/ws` with JWT token
2. `WebSocketAuthInterceptor` validates JWT and sets Spring Security context
3. `UserPresenceService` tracks connection/disconnection events
4. Client subscribes to private queues (`/user/queue/*`)
5. Client sends messages to `/app/chat.*` endpoints
6. Server routes messages to appropriate recipients via `/user/queue/messages`
7. Server broadcasts presence to `/topic/presence`

## Next Steps

### Enhancements
- [ ] Message delivery confirmation
- [ ] Offline message queue
- [ ] Multi-device support
- [ ] Group chat support
- [ ] File upload via WebSocket
- [ ] Voice/video call signaling

---

**Status**: ✅ WebSocket integration complete and ready for testing
**Last Updated**: January 31, 2026
