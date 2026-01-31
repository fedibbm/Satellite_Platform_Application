# Thunder Client Testing Guide for Messaging API

## 🔧 Setup

### Base URL
```
http://localhost:8080
```

### Authentication
All endpoints require JWT token in the Authorization header:
```
Authorization: Bearer YOUR_JWT_TOKEN
```

**Your current token:**
```
eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX1RIRU1BVElDSUFOIl0sIm5hbWUiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInN1YiI6InRoZW1hdGljaWFuQGV4YW1wbGUuY29tIiwiaWF0IjoxNzY5NzgyODA3LCJleHAiOjE3Njk4NjkyMDd9.8uE0N2Zvuaje3rReBRVjqzGbc_mVVHE60_Ne9PsPOlaMc3kntakXbEeJA-EKaFZZjRTDuOFZRX5C-mnJal6GeA
```

**Recipient ID (for testing):**
```
6978eff038aa1e365ee5fcfd
```

---

## 📨 Test Endpoints in Order

### 1️⃣ Send Text Message (First Test!)

**Method:** `POST`  
**URL:** `http://localhost:8080/api/messaging/messages`

**Headers:**
- `Content-Type`: `application/json`
- `Authorization`: `Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX1RIRU1BVElDSUFOIl0sIm5hbWUiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInN1YiI6InRoZW1hdGljaWFuQGV4YW1wbGUuY29tIiwiaWF0IjoxNzY5NzgyODA3LCJleHAiOjE3Njk4NjkyMDd9.8uE0N2Zvuaje3rReBRVjqzGbc_mVVHE60_Ne9PsPOlaMc3kntakXbEeJA-EKaFZZjRTDuOFZRX5C-mnJal6GeA`

**Body (JSON):**
```json
{
  "recipientId": "6978eff038aa1e365ee5fcfd",
  "content": "Hello! This is my first message from Thunder Client!"
}
```

**Expected Response:** `201 Created`
```json
{
  "id": "msg123...",
  "conversationId": "conv456...",
  "senderId": "thematician@example.com",
  "recipientId": "6978eff038aa1e365ee5fcfd",
  "messageType": "TEXT",
  "content": "Hello! This is my first message from Thunder Client!",
  "imageUrl": null,
  "sentAt": "2026-01-31T...",
  "readAt": null,
  "status": "SENT"
}
```

✅ **Save the `conversationId` from the response for next tests!**

---

### 2️⃣ Get Your Conversations

**Method:** `GET`  
**URL:** `http://localhost:8080/api/messaging/conversations`

**Headers:**
- `Authorization`: `Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX1RIRU1BVElDSUFOIl0sIm5hbWUiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInN1YiI6InRoZW1hdGljaWFuQGV4YW1wbGUuY29tIiwiaWF0IjoxNzY5NzgyODA3LCJleHAiOjE3Njk4NjkyMDd9.8uE0N2Zvuaje3rReBRVjqzGbc_mVVHE60_Ne9PsPOlaMc3kntakXbEeJA-EKaFZZjRTDuOFZRX5C-mnJal6GeA`

**Query Params (optional):**
- `page`: `0`
- `size`: `20`

**Expected Response:** `200 OK`
```json
{
  "content": [
    {
      "id": "conv456...",
      "createdAt": "2026-01-31T...",
      "lastMessageAt": "2026-01-31T...",
      "lastMessagePreview": "Hello! This is my first message...",
      "lastMessageType": "TEXT",
      "lastMessageSenderId": "thematician@example.com",
      "unreadCount": 0,
      "otherParticipantId": "6978eff038aa1e365ee5fcfd"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 3️⃣ Get Messages in Conversation

**Method:** `GET`  
**URL:** `http://localhost:8080/api/messaging/conversations/{conversationId}/messages`

Replace `{conversationId}` with the ID from step 1!

Example: `http://localhost:8080/api/messaging/conversations/67a12e7fc05ced73a68eba99/messages`

**Headers:**
- `Authorization`: `Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX1RIRU1BVElDSUFOIl0sIm5hbWUiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInN1YiI6InRoZW1hdGljaWFuQGV4YW1wbGUuY29tIiwiaWF0IjoxNzY5NzgyODA3LCJleHAiOjE3Njk4NjkyMDd9.8uE0N2Zvuaje3rReBRVjqzGbc_mVVHE60_Ne9PsPOlaMc3kntakXbEeJA-EKaFZZjRTDuOFZRX5C-mnJal6GeA`

**Query Params (optional):**
- `page`: `0`
- `size`: `50`

**Expected Response:** `200 OK` with list of messages

---

### 4️⃣ Get Unread Count

**Method:** `GET`  
**URL:** `http://localhost:8080/api/messaging/unread-count`

**Headers:**
- `Authorization`: `Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX1RIRU1BVElDSUFOIl0sIm5hbWUiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInN1YiI6InRoZW1hdGljaWFuQGV4YW1wbGUuY29tIiwiaWF0IjoxNzY5NzgyODA3LCJleHAiOjE3Njk4NjkyMDd9.8uE0N2Zvuaje3rReBRVjqzGbc_mVVHE60_Ne9PsPOlaMc3kntakXbEeJA-EKaFZZjRTDuOFZRX5C-mnJal6GeA`

**Expected Response:** `200 OK`
```json
{
  "totalUnreadCount": 0,
  "conversationsWithUnread": 0
}
```

---

### 5️⃣ Send Another Message

**Method:** `POST`  
**URL:** `http://localhost:8080/api/messaging/messages`

**Headers:**
- `Content-Type`: `application/json`
- `Authorization`: `Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX1RIRU1BVElDSUFOIl0sIm5hbWUiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInN1YiI6InRoZW1hdGljaWFuQGV4YW1wbGUuY29tIiwiaWF0IjoxNzY5NzgyODA3LCJleHAiOjE3Njk4NjkyMDd9.8uE0N2Zvuaje3rReBRVjqzGbc_mVVHE60_Ne9PsPOlaMc3kntakXbEeJA-EKaFZZjRTDuOFZRX5C-mnJal6GeA`

**Body (JSON):**
```json
{
  "recipientId": "6978eff038aa1e365ee5fcfd",
  "content": "This is my second message! 🚀"
}
```

---

### 6️⃣ Mark Message as Read

**Method:** `PUT`  
**URL:** `http://localhost:8080/api/messaging/messages/{messageId}/read`

Replace `{messageId}` with the message ID from step 1 response!

**Headers:**
- `Authorization`: `Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX1RIRU1BVElDSUFOIl0sIm5hbWUiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInN1YiI6InRoZW1hdGljaWFuQGV4YW1wbGUuY29tIiwiaWF0IjoxNzY5NzgyODA3LCJleHAiOjE3Njk4NjkyMDd9.8uE0N2Zvuaje3rReBRVjqzGbc_mVVHE60_Ne9PsPOlaMc3kntakXbEeJA-EKaFZZjRTDuOFZRX5C-mnJal6GeA`

**Expected Response:** `200 OK`
```json
{
  "id": "msg123...",
  "status": "READ",
  "readAt": "2026-01-31T..."
}
```

---

### 7️⃣ Mark All Conversation Messages as Read

**Method:** `PUT`  
**URL:** `http://localhost:8080/api/messaging/conversations/{conversationId}/read`

Replace `{conversationId}` with your conversation ID!

**Headers:**
- `Authorization`: `Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX1RIRU1BVElDSUFOIl0sIm5hbWUiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInN1YiI6InRoZW1hdGljaWFuQGV4YW1wbGUuY29tIiwiaWF0IjoxNzY5NzgyODA3LCJleHAiOjE3Njk4NjkyMDd9.8uE0N2Zvuaje3rReBRVjqzGbc_mVVHE60_Ne9PsPOlaMc3kntakXbEeJA-EKaFZZjRTDuOFZRX5C-mnJal6GeA`

**Expected Response:** `200 OK` (empty body)

---

### 8️⃣ Send Image Message

**Method:** `POST`  
**URL:** `http://localhost:8080/api/messaging/messages/image`

**Headers:**
- `Authorization`: `Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX1RIRU1BVElDSUFOIl0sIm5hbWUiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInN1YiI6InRoZW1hdGljaWFuQGV4YW1wbGUuY29tIiwiaWF0IjoxNzY5NzgyODA3LCJleHAiOjE3Njk4NjkyMDd9.8uE0N2Zvuaje3rReBRVjqzGbc_mVVHE60_Ne9PsPOlaMc3kntakXbEeJA-EKaFZZjRTDuOFZRX5C-mnJal6GeA`

**Body Type:** `Form (multipart/form-data)`

**Form Fields:**
- `recipientId`: `6978eff038aa1e365ee5fcfd` (text)
- `image`: [Select a JPG/PNG file] (file)
- `caption`: `Check out this image!` (text, optional)

**Expected Response:** `201 Created`
```json
{
  "id": "msg789...",
  "conversationId": "conv456...",
  "messageType": "IMAGE",
  "content": "Check out this image!",
  "imageUrl": "/api/messaging/images/conv456.../20260131-143025-abc123.jpg",
  "sentAt": "2026-01-31T...",
  "status": "SENT"
}
```

---

### 9️⃣ Get Image

**Method:** `GET`  
**URL:** Copy the `imageUrl` from step 8 response

Example: `http://localhost:8080/api/messaging/images/conv456.../20260131-143025-abc123.jpg`

**Headers:**
- `Authorization`: `Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX1RIRU1BVElDSUFOIl0sIm5hbWUiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInN1YiI6InRoZW1hdGljaWFuQGV4YW1wbGUuY29tIiwiaWF0IjoxNzY5NzgyODA3LCJleHAiOjE3Njk4NjkyMDd9.8uE0N2Zvuaje3rReBRVjqzGbc_mVVHE60_Ne9PsPOlaMc3kntakXbEeJA-EKaFZZjRTDuOFZRX5C-mnJal6GeA`

**Expected Response:** `200 OK` with image binary data

---

### 🔟 Get Specific Conversation

**Method:** `GET`  
**URL:** `http://localhost:8080/api/messaging/conversations/{conversationId}`

**Headers:**
- `Authorization`: `Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX1RIRU1BVElDSUFOIl0sIm5hbWUiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInN1YiI6InRoZW1hdGljaWFuQGV4YW1wbGUuY29tIiwiaWF0IjoxNzY5NzgyODA3LCJleHAiOjE3Njk4NjkyMDd9.8uE0N2Zvuaje3rReBRVjqzGbc_mVVHE60_Ne9PsPOlaMc3kntakXbEeJA-EKaFZZjRTDuOFZRX5C-mnJal6GeA`

**Expected Response:** `200 OK` with conversation details

---

## 🎯 Quick Thunder Client Setup

### Step-by-Step:

1. **Open Thunder Client** in VS Code (Lightning icon in sidebar)

2. **Create New Request** (Click "New Request")

3. **Set Method and URL** (e.g., POST, http://localhost:8080/api/messaging/messages)

4. **Add Headers:**
   - Click "Headers" tab
   - Add `Authorization`: `Bearer [paste token]`
   - Add `Content-Type`: `application/json` (for JSON requests)

5. **Add Body:**
   - Click "Body" tab
   - Select "JSON" for text messages
   - Select "Form" for image messages
   - Paste the JSON from examples above

6. **Click Send!** 🚀

---

## ❌ Common Errors

### 401 Unauthorized
```json
{
  "timestamp": "...",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required"
}
```
**Fix:** Check your JWT token in Authorization header

### 403 Forbidden
```json
{
  "timestamp": "...",
  "status": 403,
  "error": "Forbidden",
  "message": "You are not authorized to access this conversation"
}
```
**Fix:** You're trying to access a conversation you're not part of

### 404 Not Found
```json
{
  "timestamp": "...",
  "status": 404,
  "error": "Not Found",
  "message": "Conversation not found: conv456"
}
```
**Fix:** Check the conversation ID exists

### 400 Bad Request
```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Validation Failed",
  "validationErrors": {
    "recipientId": "Recipient ID is required"
  }
}
```
**Fix:** Check required fields in request body

---

## 📝 Testing Checklist

- [ ] Send text message
- [ ] List conversations (should see 1)
- [ ] Get messages in conversation
- [ ] Check unread count
- [ ] Send another text message
- [ ] List conversations (should see updated preview)
- [ ] Mark message as read
- [ ] Check unread count (should be 0)
- [ ] Send image message
- [ ] Get image URL (should display image)
- [ ] Get specific conversation
- [ ] List all conversations with pagination

---

## 🚀 Pro Tips

1. **Save Responses:** Thunder Client auto-saves responses for comparison
2. **Environment Variables:** Create environment for `baseUrl` and `token`
3. **Collections:** Group related requests in collections
4. **Test Runner:** Run all tests sequentially with one click

---

## Need Help?

Check the application logs for detailed error messages:
```bash
cd Backend
./mvnw spring-boot:run
```

Watch for errors in the console output!
