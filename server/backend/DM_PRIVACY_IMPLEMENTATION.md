# DM Privacy Policy Implementation Summary

## Overview
Implemented **Default follow-gated DM with user opt-in for non-follower DMs** policy.

---

## DM Privacy Settings

### Enum: `DmPrivacySetting`
- **`FOLLOWERS_ONLY`** (default): Only approved followers can initiate DM
- **`EVERYONE`**: Anyone (not blocked) can initiate DM

### DM Initiation Rule
User A can initiate DM to User B if:
- B has NOT blocked A
- AND (approved Follow(A→B) exists OR B.dm_privacy == EVERYONE)

### Thread/Reply Rule
- Once a DM thread exists, BOTH users can freely message
- No message count/frequency restrictions (Open DM)
- Thread existence grants reply permission regardless of follow direction

### Block Rule
- Block overrides everything - prevents both initiation and continuation

### Unfollow Rule
- Unfollow does NOT delete existing threads
- Existing threads remain usable unless blocked

---

## API Endpoints

### 1. Check DM Permission (NEW)
**Check if current user can initiate DM with target user**

```http
GET /api/chat/can-message/{userId}
Authorization: Bearer {jwt_token}
```

**Response:**
```json
{
  "canInitiateDm": true,
  "reason": null
}
```

**Or if not allowed:**
```json
{
  "canInitiateDm": false,
  "reason": "This user only accepts messages from approved followers"
}
```

**Use Case:** Frontend can call this to enable/disable Message button and show appropriate error message.

**Implementation:** `ChatController.kt:107-115`, `ChatService.kt:115-160`

---

### 2. Update DM Privacy Setting (NEW)
**Allow users to change their DM privacy preference**

```http
PUT /users/me/privacy
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "allowDmsFrom": "EVERYONE"
}
```

**Response:** Updated UserDTO with new privacy setting
```json
{
  "id": 1,
  "name": "User Name",
  "email": "user@example.com",
  "allowDmsFrom": "EVERYONE",
  ...
}
```

**Implementation:** `UserController.kt:41-45`, `UserService.kt:177-186`

---

### 3. Create or Get Chat Room (UPDATED)
**Create new DM thread or get existing one**

```http
POST /api/chat/rooms/{userId}
Authorization: Bearer {jwt_token}
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Chat room ready",
  "chatRoom": {
    "id": 1,
    "otherUser": {...},
    "lastMessageAt": "2025-12-25T18:00:00",
    "lastMessagePreview": "Hello!",
    "unreadCount": 0,
    "status": "ACTIVE",
    "isPending": false
  }
}
```

**Error Response (400):**
```json
{
  "success": false,
  "message": "This user only accepts messages from approved followers",
  "chatRoom": null
}
```

**Updated Logic:**
- Checks if target user is blocked
- If thread exists, returns it (reply allowed)
- For new threads: checks Follow(A→B) exists OR target.allowDmsFrom == EVERYONE
- Creates thread immediately as ACTIVE (no PENDING status)

**Implementation:** `ChatService.kt:44-113`

---

### 4. Get Current User (UPDATED)
**Now includes DM privacy setting**

```http
GET /users/me
Authorization: Bearer {jwt_token}
```

**Response:**
```json
{
  "id": 1,
  "name": "User Name",
  "email": "user@example.com",
  "allowDmsFrom": "FOLLOWERS_ONLY",
  "nickname": "Cool Cat",
  ...
}
```

**Implementation:** `User.kt:80-107`, `Extensions.kt:80-92`

---

## Database Schema Changes

### User Table
Added column:
```sql
allow_dms_from VARCHAR(20) DEFAULT 'FOLLOWERS_ONLY'
```

### ChatRoom Table
Simplified statuses:
- `ACTIVE`: Active conversation (both can message freely)
- `BLOCKED`: Thread blocked by either user
- Removed: `PENDING`, `IGNORED` (no longer needed)

---

## Test Data

### Test Users
1. **user1 - Fast Cat** (Japan)
   - Privacy: `FOLLOWERS_ONLY`
   - Email: test1@example.com

2. **user2 - Slow Rabbit** (Canada)
   - Privacy: `FOLLOWERS_ONLY`
   - Email: test2@example.com

3. **user3 - Happy Panda** (South Korea)
   - Privacy: `FOLLOWERS_ONLY`
   - Email: test3@example.com

4. **user4 - Open Fox** (United States) 🆕
   - Privacy: `EVERYONE`
   - Email: test4@example.com

### Follow Relationships
- user1 ↔ user2 (mutual)
- user1 ↔ user3 (mutual)
- user1 → user4 (one-way, user4 doesn't follow back)

### Test Scenarios

#### Scenario 1: FOLLOWERS_ONLY Privacy
- ✅ user1 can DM user2 (mutual follow)
- ✅ user1 can DM user3 (mutual follow)
- ❌ user4 CANNOT initiate DM to user1 (user4 doesn't follow user1)

#### Scenario 2: EVERYONE Privacy
- ✅ user1 can DM user4 (user4 allows EVERYONE)
- ✅ user2 can DM user4 (user4 allows EVERYONE, no follow required)
- ✅ user3 can DM user4 (user4 allows EVERYONE, no follow required)

#### Scenario 3: Thread Continuation
- ✅ Once DM thread exists, both users can freely message
- ✅ Unfollowing does NOT delete or disable thread
- ❌ Blocking disables thread

---

## Frontend Integration Guide

### 1. Display Message Button on Profile
```typescript
// Check if current user can message this profile user
const checkDmPermission = async (userId: number) => {
  const response = await fetch(`/api/chat/can-message/${userId}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const result = await response.json();

  if (result.canInitiateDm) {
    // Show enabled "Message" button
  } else {
    // Show disabled button with tooltip: result.reason
    // e.g., "This user only accepts messages from approved followers"
  }
};
```

### 2. Create/Open DM Thread
```typescript
const openDmThread = async (userId: number) => {
  const response = await fetch(`/api/chat/rooms/${userId}`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const result = await response.json();

  if (result.success) {
    // Navigate to chat room: result.chatRoom.id
  } else {
    // Show error: result.message
  }
};
```

### 3. DM Privacy Settings UI
```typescript
const updateDmPrivacy = async (setting: 'FOLLOWERS_ONLY' | 'EVERYONE') => {
  const response = await fetch('/users/me/privacy', {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ allowDmsFrom: setting })
  });
  const updatedUser = await response.json();
  // Update UI with updatedUser.allowDmsFrom
};
```

**Settings UI Example:**
```
Message Privacy

Who can send you message requests?
○ Everyone
● Approved followers only (default)

Note: People you've already messaged can always send you messages.
```

---

## Implementation Files

### Core Logic
- `DmPrivacySetting.kt` - Enum definitions and policy documentation
- `ChatService.kt` - DM initiation and messaging logic
- `ChatController.kt` - REST API endpoints
- `UserService.kt` - DM privacy setting updates
- `User.kt` - User model with allowDmsFrom field
- `Extensions.kt` - User.toDTO() includes allowDmsFrom

### Configuration
- `SecurityConfig.kt` - Added `/users/me/**` to authenticated routes
- `DataLoader.kt` - Test data with EVERYONE privacy user

---

## Security Considerations

### Authentication Required
All DM-related endpoints require JWT authentication:
- `/api/chat/**` - All chat endpoints
- `/users/me/privacy` - Privacy setting updates

### Permission Checks
1. **Block Check** (highest priority)
   - Cannot initiate or continue DM if blocked

2. **Thread Exists**
   - Existing thread grants both users message permission

3. **Follow + Privacy Check**
   - For new threads: Follow(A→B) OR target.allowDmsFrom == EVERYONE

### Rate Limiting
Not implemented. Consider adding:
- Max DM initiation attempts per minute
- Max messages per conversation per minute

---

## Migration Notes

### From Previous Implementation
If migrating from message request system:
1. Database: Set all existing users' `allow_dms_from` to `FOLLOWERS_ONLY`
2. Chat rooms: Convert all `PENDING` status to `ACTIVE`
3. Remove: Message request approval/ignore endpoints
4. Update frontend: Remove message request acceptance flow

### Database Migration
```sql
-- Add column with default
ALTER TABLE users ADD COLUMN allow_dms_from VARCHAR(20) DEFAULT 'FOLLOWERS_ONLY';

-- Update existing chat rooms
UPDATE chat_rooms SET status = 'ACTIVE' WHERE status = 'PENDING';
```

---

## Future Enhancements

### Possible Features
1. **Message Request Labels**
   - When dm_privacy == EVERYONE, optionally label as "Message request"
   - Allow users to accept/ignore (soft approval without blocking thread creation)

2. **Message Filtering**
   - Filter messages from non-followers to separate inbox
   - User reviews and accepts/ignores

3. **Notification Preferences**
   - Separate notification settings for followers vs. non-followers

4. **Analytics**
   - Track DM initiation success/failure rates
   - Monitor EVERYONE vs. FOLLOWERS_ONLY adoption

---

## Testing Checklist

### Manual Testing
- [ ] User with FOLLOWERS_ONLY can only receive DMs from followers
- [ ] User with EVERYONE can receive DMs from anyone (not blocked)
- [ ] `/api/chat/can-message/{userId}` returns correct permissions
- [ ] Creating chat room works with FOLLOWERS_ONLY setting
- [ ] Creating chat room works with EVERYONE setting
- [ ] Updating privacy setting via `/users/me/privacy` works
- [ ] UserDTO includes `allowDmsFrom` field
- [ ] Thread continuation works after unfollow
- [ ] Blocking prevents DM initiation and continuation

### Integration Testing
- [ ] Test with real OAuth tokens
- [ ] Test with Flutter app integration
- [ ] Test SSE real-time messaging
- [ ] Test with concurrent requests

---

## Contact & Support

**Implementation Date:** 2025-12-25

**Files Modified:**
- DmPrivacySetting.kt
- ChatService.kt
- ChatController.kt
- UserController.kt
- UserService.kt
- User.kt
- Extensions.kt
- SecurityConfig.kt
- DataLoader.kt
- ChatRoom.kt
- ChatRoomRepository.kt

**Server Status:**
- ✅ Build: Successful
- ✅ Server: Running on port 8080
- ✅ Health: UP
- ✅ Test Data: 4 users with different privacy settings
