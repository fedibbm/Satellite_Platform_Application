# Messaging System - Data Model Documentation

## Overview
Clean, scalable 1-to-1 messaging system designed for MongoDB with proper indexing and pagination support.

## Design Principles

### ✅ What We Did RIGHT:
1. **Standalone Message Documents** - Messages are NOT embedded, allowing unlimited conversation history
2. **Proper Indexing** - Compound indexes for all common query patterns
3. **Pagination Support** - Built-in from the start for scalability
4. **Message Status Tracking** - SENT/READ status for delivery confirmation
5. **Lightweight Conversations** - Metadata only, no message embedding
6. **Efficient Queries** - All repository methods use indexed fields

### ❌ What We AVOIDED:
1. **No RabbitMQ** - Unnecessary complexity for 1-to-1 messaging
2. **No Embedded Messages** - Would hit MongoDB's 16MB document limit
3. **No Over-Engineering** - No reactions, bots, admin messages, attachments (yet)
4. **No Blocking Operations** - All queries support pagination

---

## Database Schema

### 1. Messages Collection

```javascript
{
  _id: "msg123",
  conversationId: "conv456",        // FK to conversations
  senderId: "user1",               // Who sent it
  recipientId: "user2",            // Who receives it
  content: "Hello there!",         // Message text
  sentAt: ISODate("2026-01-30T10:00:00Z"),
  readAt: ISODate("2026-01-30T10:05:00Z"),  // null if unread
  status: "READ"                   // SENT or READ
}
```

**Indexes:**
- `conversation_timestamp_idx`: `{conversationId: 1, sentAt: 1}` - For message history
- `recipient_status_idx`: `{recipientId: 1, status: 1}` - For unread count
- `sender_conversation_idx`: `{senderId: 1, conversationId: 1, sentAt: -1}` - For sender history

### 2. Conversations Collection

```javascript
{
  _id: "conv456",
  participants: ["user1", "user2"],     // Set of 2 user IDs
  createdAt: ISODate("2026-01-30T09:00:00Z"),
  lastMessageAt: ISODate("2026-01-30T10:00:00Z"),
  lastMessagePreview: "Hello there!",   // Cached for UI
  lastMessageSenderId: "user1",
  unreadCounts: {                       // Per-user unread counts
    "user1": 0,
    "user2": 1
  }
}
```

**Indexes:**
- `participants_idx`: `{participants: 1}` (unique) - Find conversation between 2 users
- `participant_lastmessage_idx`: `{participants: 1, lastMessageAt: -1}` - Sort by activity

---

## Key Repository Methods

### MessageRepository

```java
// Load message history (paginated, oldest first)
Page<Message> findByConversationIdOrderBySentAtAsc(String conversationId, Pageable pageable);

// Count unread messages for a user
long countByRecipientIdAndStatus(String recipientId, MessageStatus.SENT);

// Get unread messages in a conversation
List<Message> findByConversationIdAndRecipientIdAndStatus(
    String conversationId, 
    String recipientId, 
    MessageStatus.SENT
);
```

### ConversationRepository

```java
// Get user's conversation list (sorted by recent activity)
Page<Conversation> findUserConversationsSortedByRecent(String userId, Pageable pageable);

// Find existing conversation between 2 users
Optional<Conversation> findByParticipants(Set.of("user1", "user2"));
```

---

## Query Patterns & Performance

### 1. Load Conversation List
```java
// Get first page of user's conversations
PageRequest page = PageRequest.of(0, 20); // 20 conversations per page
Page<Conversation> conversations = conversationRepo.findUserConversationsSortedByRecent(userId, page);

// Uses index: participant_lastmessage_idx
// Query: { participants: "userId" } sort { lastMessageAt: -1 }
```

### 2. Load Message History
```java
// Get page 1 of messages (oldest first, like WhatsApp)
PageRequest page = PageRequest.of(0, 50); // 50 messages per page
Page<Message> messages = messageRepo.findByConversationIdOrderBySentAtAsc(conversationId, page);

// Uses index: conversation_timestamp_idx
// Query: { conversationId: "convId" } sort { sentAt: 1 }
```

### 3. Count Unread Messages
```java
// Total unread for a user
long unreadCount = messageRepo.countByRecipientIdAndStatus(userId, MessageStatus.SENT);

// Uses index: recipient_status_idx
// Query: { recipientId: "userId", status: "SENT" }
```

### 4. Mark Messages as Read
```java
// Find all unread messages in a conversation
List<Message> unreadMessages = messageRepo.findByConversationIdAndRecipientIdAndStatus(
    conversationId, userId, MessageStatus.SENT
);

// Mark each as read
unreadMessages.forEach(msg -> msg.markAsRead());
messageRepo.saveAll(unreadMessages);

// Update conversation unread count
conversation.resetUnreadCount(userId);
conversationRepo.save(conversation);
```

### 5. Send New Message
```java
// 1. Find or create conversation
Set<String> participants = Set.of(senderId, recipientId);
Conversation conv = conversationRepo.findByParticipants(participants)
    .orElseGet(() -> {
        Conversation newConv = Conversation.builder()
            .participants(participants)
            .createdAt(LocalDateTime.now())
            .unreadCounts(new HashMap<>())
            .build();
        return conversationRepo.save(newConv);
    });

// 2. Create and save message
Message message = Message.builder()
    .conversationId(conv.getId())
    .senderId(senderId)
    .recipientId(recipientId)
    .content(content)
    .sentAt(LocalDateTime.now())
    .status(MessageStatus.SENT)
    .build();
messageRepo.save(message);

// 3. Update conversation metadata
conv.updateLastMessage(content.substring(0, Math.min(100, content.length())), senderId);
conv.incrementUnreadCount(recipientId);
conversationRepo.save(conv);
```

---

## Scalability Considerations

### Document Sizes
- **Message**: ~200-500 bytes (text only)
- **Conversation**: ~500-1000 bytes (metadata only)

### Example: 1 million messages
- **Storage**: ~200-500 MB (messages only)
- **Conversations**: If 100k unique conversations = ~50-100 MB
- **Total**: ~250-600 MB for 1M messages

### Index Memory
MongoDB keeps indexes in RAM:
- **3 message indexes**: ~5-10 MB per 1M messages
- **2 conversation indexes**: ~1-2 MB per 100k conversations
- **Total RAM**: ~15-20 MB for 1M messages

### Query Performance
All queries use indexes → O(log n) lookup time:
- **Load 20 conversations**: <10ms
- **Load 50 messages**: <10ms
- **Count unread**: <5ms
- **Find conversation**: <5ms (unique index)

---

## Pagination Examples

### Frontend Pagination Pattern
```typescript
// Load conversations (20 per page)
const page = 0;
const size = 20;
const response = await fetch(`/api/messaging/conversations?page=${page}&size=${size}`);

// Load more messages (infinite scroll)
const messageResponse = await fetch(
  `/api/messaging/conversations/${convId}/messages?page=${page}&size=${50}`
);
```

### Backend Controller Pattern
```java
@GetMapping("/conversations")
public ResponseEntity<Page<Conversation>> getConversations(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    String userId = getCurrentUserId();
    Pageable pageable = PageRequest.of(page, size);
    Page<Conversation> conversations = conversationRepo
        .findUserConversationsSortedByRecent(userId, pageable);
    return ResponseEntity.ok(conversations);
}
```

---

## Next Steps (Step 2)

Now that the data model is solid, the next phase is:

1. **Create Service Layer**
   - ConversationService
   - MessageService
   - With business logic and validations

2. **Create REST Controllers**
   - GET /api/messaging/conversations (list conversations)
   - GET /api/messaging/conversations/{id}/messages (message history)
   - POST /api/messaging/messages (send message)
   - PUT /api/messaging/messages/{id}/read (mark as read)

3. **Add DTOs**
   - Request/Response objects
   - User info enrichment (username, avatar)

**WebSocket will come in Step 3** - after REST API is solid!

---

## Why This Design Works

### ✅ Scalability
- No document size limits (messages are separate)
- Efficient pagination (all queries support it)
- Proper indexing (all queries use indexes)

### ✅ Performance
- Fast queries (< 10ms for most operations)
- Low memory footprint (indexes fit in RAM)
- Optimized for common patterns

### ✅ Maintainability
- Clean separation (messages ≠ conversations)
- Simple schema (easy to understand)
- Well-documented (this file!)

### ✅ User Experience
- Message status (sent/read)
- Unread counts (per conversation)
- Conversation preview (cached in metadata)
- Pagination (smooth infinite scroll)

---

## Testing the Data Model

Run the application and check the logs for:
```
Initializing messaging module indexes...
Message collection indexes:
  - conversation_timestamp_idx
  - recipient_status_idx
  - sender_conversation_idx
Conversation collection indexes:
  - participants_idx
  - participant_lastmessage_idx
Messaging module indexes initialized successfully.
```

This confirms all indexes are created properly!
