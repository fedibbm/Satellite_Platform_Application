# User Presence System - Complete Guide

## Overview
The user presence system tracks when users connect/disconnect via WebSocket and broadcasts their online/offline status to all connected clients in real-time.

## Architecture

### Backend Components

#### 1. **UserPresenceService** 
Location: `Backend/src/main/java/com/enit/satellite_platform/modules/messaging/websocket/UserPresenceService.java`

**Key Features:**
- Tracks user sessions using ObjectId (not email)
- Supports multiple sessions per user (multi-device)
- Listens to WebSocket connect/disconnect events
- Broadcasts presence updates to `/topic/presence`

**How it works:**
```java
// On connection:
1. Extract email from JWT Principal
2. Convert email → ObjectId using UserRepository
3. Store session: userSessions[userId][sessionId] = true
4. Broadcast { userId, online: true, timestamp }

// On disconnection:
1. Extract email from JWT Principal
2. Convert email → ObjectId
3. Remove session from tracking
4. If no more sessions → Broadcast { userId, online: false, timestamp }
```

#### 2. **MessagingController Endpoints**

**GET /api/messaging/online-users**
- Returns all currently online users with session counts
- Response: `{ "userId1": 2, "userId2": 1 }` (number = session count)

**GET /api/messaging/users/{userId}/online**
- Checks if specific user is online
- Response: `{ "online": true/false }`

#### 3. **WebSocket Configuration**
Location: `Backend/src/main/java/com/enit/satellite_platform/modules/messaging/config/WebSocketConfig.java`

- **Connect endpoint:** `ws://localhost:8080/ws`
- **Presence topic:** `/topic/presence` (public broadcast)
- **Authentication:** JWT via `WebSocketAuthInterceptor`

### Frontend Components

#### 1. **WebSocket Service**
Location: `FrontEnd/src/services/websocketService.ts`

**Presence Subscription:**
```typescript
// Automatically subscribes to /topic/presence
this.client.subscribe('/topic/presence', (message) => {
  const presence: PresenceUpdate = JSON.parse(message.body);
  // { userId: string, online: boolean, timestamp: number }
  this.presenceCallbacks.forEach(cb => cb(presence));
});
```

**Usage:**
```typescript
import { wsService } from '@/services/websocketService';

// Subscribe to presence updates
const unsubscribe = wsService.onPresence((presence) => {
  console.log(`User ${presence.userId} is ${presence.online ? 'online' : 'offline'}`);
});

// Clean up
unsubscribe();
```

#### 2. **useMessaging Hook**
Location: `FrontEnd/src/hooks/useMessaging.ts`

**Automatic Integration:**
```typescript
const handlePresenceUpdate = (presence: PresenceUpdate) => {
  // Update online users map
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
```

#### 3. **Messaging API**
Location: `FrontEnd/src/services/messagingApi.ts`

```typescript
// Get all online users
const onlineUsers = await messagingApi.getOnlineUsers();
// Returns: { "userId1": 2, "userId2": 1 }

// Check specific user
const result = await messagingApi.checkUserOnline(userId);
// Returns: { online: true }
```

## Data Flow

### User Connects:
```
1. Frontend: Connect to ws://localhost:8080/ws with JWT token
2. WebSocketAuthInterceptor: Validates JWT, sets authentication
3. UserPresenceService: handleWebSocketConnect()
   - Extracts email from Principal
   - Converts email → ObjectId
   - Stores session mapping
4. Broadcasts to /topic/presence:
   { userId: "507f1f77bcf86cd799439011", online: true, timestamp: 1234567890 }
5. All connected clients receive update
6. Frontend: Updates onlineUsers state and conversation list
```

### User Disconnects:
```
1. WebSocket connection closes
2. UserPresenceService: handleWebSocketDisconnect()
   - Removes session from tracking
   - If last session → Broadcasts offline status
3. All clients receive update and update UI
```

## Testing

### Manual Testing

1. **Start the backend:**
```bash
cd Backend
./mvnw spring-boot:run
```

2. **Test with HTTP client:**
```http
### Get online users
GET http://localhost:8080/api/messaging/online-users
Authorization: Bearer YOUR_JWT_TOKEN

### Check specific user
GET http://localhost:8080/api/messaging/users/USER_OBJECT_ID/online
Authorization: Bearer YOUR_JWT_TOKEN
```

3. **Test WebSocket:**
- Open multiple browser tabs
- Login with different users
- Watch browser console for presence updates
- Close tabs and verify offline notifications

### Frontend Integration

```typescript
import { useMessaging } from '@/hooks/useMessaging';

function ChatComponent() {
  const { onlineUsers, conversations } = useMessaging();
  
  return (
    <div>
      {conversations.map(conv => (
        <div key={conv.id}>
          {conv.otherParticipantName}
          {conv.otherParticipantOnline && <span>🟢 Online</span>}
        </div>
      ))}
    </div>
  );
}
```

## Key Points

✅ **ObjectId Usage:** System uses ObjectId (from MongoDB) not email for user identification
✅ **Multi-Device:** Supports multiple simultaneous sessions per user
✅ **Real-time:** Instant broadcasts via WebSocket (no polling needed)
✅ **Automatic:** Frontend hook automatically updates UI on presence changes
✅ **Secure:** JWT authentication required for WebSocket connections

## Troubleshooting

**User shows as offline but is connected:**
- Check backend logs for email → ObjectId conversion errors
- Verify UserRepository.findByEmail() is working
- Check JWT token contains correct email

**Presence updates not received:**
- Verify WebSocket connection established (check browser console)
- Confirm subscription to `/topic/presence`
- Check JWT token is valid and included in connection headers

**Multiple sessions not working:**
- Verify ConcurrentHashMap properly tracks sessions
- Check sessionId is unique per connection
- Review disconnect logic to ensure sessions are properly removed

## Future Enhancements

- [ ] Add "last seen" timestamp for offline users
- [ ] Add "typing..." indicators (already in WebSocketMessagingController)
- [ ] Add presence status (Available, Away, Busy, Do Not Disturb)
- [ ] Persist presence status in Redis for scalability
- [ ] Add WebSocket heartbeat monitoring
