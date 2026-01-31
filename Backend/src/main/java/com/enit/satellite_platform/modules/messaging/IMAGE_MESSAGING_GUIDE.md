# Image Messaging Feature

## Overview
The messaging system now supports sending images in addition to text messages. This document explains how image messages work and the design decisions behind them.

---

## Changes Made

### 1. New MessageType Enum
```java
public enum MessageType {
    TEXT,    // Regular text message
    IMAGE    // Image message with file attachment
}
```

### 2. Updated Message Entity
Added fields to support image messages:
- `messageType`: TEXT or IMAGE
- `imageUrl`: Path to the stored image file (only for IMAGE type)
- `content`: Still available for image captions

### 3. Updated Conversation Entity
Added field to track last message type:
- `lastMessageType`: Helps display appropriate preview in UI
- Updated `updateLastMessage()` method to accept message type

### 4. File Storage Configuration
Created `MessagingFileStorageConfig` to handle:
- Upload directory: `upload-dir/messages/{conversationId}/`
- Max file size: 5MB (configurable)
- Allowed formats: jpg, jpeg, png, gif, webp
- Automatic directory creation

---

## Design Decisions

### Why Store Images as Files (Not in MongoDB)?
1. **Performance**: File system is faster for serving images
2. **Scalability**: Don't bloat MongoDB with binary data
3. **CDN Ready**: Easy to move to cloud storage (S3, Azure Blob) later
4. **Simplicity**: Standard HTTP file serving

### File Organization
```
upload-dir/
└── messages/
    ├── conv-001/
    │   ├── image-001.jpg
    │   ├── image-002.png
    │   └── ...
    ├── conv-002/
    │   ├── image-001.jpg
    │   └── ...
    └── ...
```

Each conversation gets its own folder for:
- Easy cleanup when conversation is deleted
- Better organization
- Simpler access control

### Image URLs
Stored as relative paths in the database:
```
/uploads/messages/{conversationId}/{filename}
```

Example:
```
/uploads/messages/conv456/20260131-143025-abc123.jpg
```

---

## Implementation Guide

### Sending an Image Message

**Step 1: Upload Image**
```http
POST /api/messaging/messages/image
Content-Type: multipart/form-data

{
  "recipientId": "user123",
  "image": <binary file>,
  "caption": "Check this out!" (optional)
}
```

**Step 2: Service Layer Logic**
1. Validate file (size, extension)
2. Generate unique filename
3. Save to conversation folder
4. Create Message entity with:
   - `messageType = IMAGE`
   - `imageUrl = /uploads/messages/{convId}/{filename}`
   - `content = caption` (optional)
5. Update conversation with preview: "📷 Image"

**Step 3: Store in Database**
```javascript
{
  messageType: "IMAGE",
  content: "Check this out!",  // caption
  imageUrl: "/uploads/messages/conv456/20260131-143025-abc123.jpg",
  // ... other fields
}
```

### Retrieving Images

**Option 1: Static Resource (Simple)**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/messages/**")
                .addResourceLocations("file:upload-dir/messages/");
    }
}
```

**Option 2: Controller (Better for Auth)**
```java
@GetMapping("/images/{conversationId}/{filename}")
public ResponseEntity<Resource> getImage(
    @PathVariable String conversationId,
    @PathVariable String filename) {
    
    // 1. Verify user is participant in conversation
    // 2. Load file
    // 3. Return with proper content type
}
```

### Displaying in Frontend

**Conversation List**
```javascript
// Show image indicator
if (conversation.lastMessageType === 'IMAGE') {
  preview = '📷 Image';
} else {
  preview = conversation.lastMessagePreview;
}
```

**Message List**
```javascript
// Render based on type
{message.messageType === 'IMAGE' ? (
  <img 
    src={message.imageUrl} 
    alt={message.content || 'Image'}
    loading="lazy"
  />
) : (
  <p>{message.content}</p>
)}
```

---

## Security Considerations

### 1. File Validation
```java
// In service layer
- Check file size (max 5MB)
- Validate extension (jpg, jpeg, png, gif, webp)
- Verify MIME type
- Scan for malicious content (optional)
```

### 2. Access Control
```java
// Before serving image
- Verify user is participant in conversation
- Return 403 if not authorized
```

### 3. File Naming
```java
// Generate unique, unpredictable filenames
String filename = String.format(
    "%s-%s.%s",
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")),
    UUID.randomUUID().toString().substring(0, 8),
    extension
);
// Result: 20260131-143025-abc123.jpg
```

### 4. Path Traversal Prevention
```java
// Sanitize conversation ID and filename
- No "../" in paths
- Only alphanumeric + dash/underscore allowed
```

---

## Configuration Properties

Add to `application.properties`:
```properties
# Messaging file storage
messaging.upload.directory=upload-dir/messages
messaging.upload.max-file-size=5242880
messaging.upload.allowed-extensions=jpg,jpeg,png,gif,webp
```

---

## Database Migration

No migration needed! Existing messages will have:
- `messageType = null` (will default to TEXT in service layer)
- `imageUrl = null`

The system is backward compatible.

---

## Testing Checklist

### Unit Tests
- [ ] Validate allowed file extensions
- [ ] Reject files over size limit
- [ ] Generate unique filenames
- [ ] Create conversation directories

### Integration Tests
- [ ] Upload image successfully
- [ ] Save message with correct imageUrl
- [ ] Retrieve image with auth
- [ ] Block unauthorized access
- [ ] Handle invalid file types
- [ ] Handle missing files

### Manual Testing
- [ ] Send text message (still works)
- [ ] Send image message
- [ ] Send image with caption
- [ ] View image in conversation
- [ ] Conversation list shows "📷 Image"
- [ ] Delete conversation (cleanup files?)

---

## Future Enhancements

### Phase 1 (MVP)
- ✅ Basic image support
- ✅ File validation
- ✅ Access control
- ⬜ Image thumbnails
- ⬜ Image compression

### Phase 2
- ⬜ Multiple images per message
- ⬜ Image preview in conversation list
- ⬜ Image download button
- ⬜ Image metadata (dimensions, size)

### Phase 3
- ⬜ Cloud storage (S3, Azure Blob)
- ⬜ CDN integration
- ⬜ Image optimization pipeline
- ⬜ Automatic cleanup of old images

### Advanced
- ⬜ Video messages
- ⬜ Voice messages
- ⬜ File attachments (PDF, docs)
- ⬜ Image editing (crop, filter)

---

## Performance Considerations

### Storage Estimates
Assuming:
- Average image: 500 KB
- 100,000 users
- 10 images per conversation
- Average 5 active conversations per user

**Storage needed:**
```
100,000 users × 5 conversations × 10 images × 500 KB
= 250 GB
```

### Bandwidth
- Serving images is cheap (static files)
- Use HTTP caching headers
- Consider CDN for production

### Cleanup Strategy
Options:
1. Keep images forever (simplest)
2. Delete when conversation is deleted
3. Delete after N months of inactivity
4. Move to cold storage after N months

---

## API Examples

### Send Image Message
```http
POST /api/messaging/messages/image HTTP/1.1
Authorization: Bearer <token>
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="recipientId"

user123
------WebKitFormBoundary
Content-Disposition: form-data; name="image"; filename="photo.jpg"
Content-Type: image/jpeg

<binary data>
------WebKitFormBoundary
Content-Disposition: form-data; name="caption"

Check out this sunset!
------WebKitFormBoundary--
```

**Response:**
```json
{
  "id": "msg789",
  "conversationId": "conv456",
  "senderId": "user456",
  "recipientId": "user123",
  "messageType": "IMAGE",
  "content": "Check out this sunset!",
  "imageUrl": "/uploads/messages/conv456/20260131-143025-abc123.jpg",
  "sentAt": "2026-01-31T14:30:25",
  "status": "SENT"
}
```

### Get Image
```http
GET /api/messaging/images/conv456/20260131-143025-abc123.jpg HTTP/1.1
Authorization: Bearer <token>
```

**Response:**
```
HTTP/1.1 200 OK
Content-Type: image/jpeg
Content-Length: 524288
Cache-Control: private, max-age=86400

<binary image data>
```

---

## Summary

✅ **What We Added:**
- MessageType enum (TEXT, IMAGE)
- Image URL storage in Message entity
- File storage configuration
- Conversation preview for images

✅ **What We Kept Simple:**
- One image per message
- File system storage (not MongoDB)
- Standard multipart upload
- Relative URL paths

✅ **What's Next:**
- Implement service layer (file handling)
- Create REST endpoints
- Add image serving endpoint
- Frontend integration

The foundation is solid and ready for implementation! 🚀
