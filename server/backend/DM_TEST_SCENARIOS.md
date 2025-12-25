# DM Privacy Policy - Test Scenarios

## Test Data Overview

### Users
| ID | Nickname | Email | Privacy Setting | Country |
|----|----------|-------|-----------------|---------|
| 1 | Fast Cat | test1@example.com | FOLLOWERS_ONLY | Japan |
| 2 | Slow Rabbit | test2@example.com | FOLLOWERS_ONLY | Canada |
| 3 | Happy Panda | test3@example.com | FOLLOWERS_ONLY | South Korea |
| 4 | Open Fox | test4@example.com | **EVERYONE** | United States |

### Follow Relationships
```
user1 ←→ user2  (mutual)
user1 ←→ user3  (mutual)
user1  → user4  (one-way, user4 doesn't follow back)
```

---

## Scenario 1: FOLLOWERS_ONLY Privacy (Default)

### Test Case 1.1: Mutual Followers Can DM
**Setup:**
- user1 and user2 have mutual follow
- Both have FOLLOWERS_ONLY privacy

**Test:**
```
POST /api/chat/rooms/2
Authorization: Bearer {user1_token}
```

**Expected Result:**
```json
{
  "success": true,
  "message": "Chat room ready",
  "chatRoom": { ... }
}
```
✅ **Pass Criteria:** Chat room created successfully

---

### Test Case 1.2: Non-Follower Cannot Initiate DM
**Setup:**
- user4 does NOT follow user1
- user1 has FOLLOWERS_ONLY privacy

**Test:**
```
POST /api/chat/rooms/1
Authorization: Bearer {user4_token}
```

**Expected Result:**
```json
{
  "success": false,
  "message": "This user only accepts messages from approved followers",
  "chatRoom": null
}
```
✅ **Pass Criteria:** Request rejected with proper error message

---

### Test Case 1.3: Can Check DM Permission Before Attempting
**Setup:**
- user4 wants to know if they can DM user1

**Test:**
```
GET /api/chat/can-message/1
Authorization: Bearer {user4_token}
```

**Expected Result:**
```json
{
  "canInitiateDm": false,
  "reason": "This user only accepts messages from approved followers"
}
```
✅ **Pass Criteria:** Permission check returns false with reason

---

## Scenario 2: EVERYONE Privacy Setting

### Test Case 2.1: Non-Follower Can Initiate DM
**Setup:**
- user2 does NOT follow user4
- user4 has EVERYONE privacy

**Test:**
```
POST /api/chat/rooms/4
Authorization: Bearer {user2_token}
```

**Expected Result:**
```json
{
  "success": true,
  "message": "Chat room ready",
  "chatRoom": { ... }
}
```
✅ **Pass Criteria:** Chat room created successfully (no follow required)

---

### Test Case 2.2: Anyone Can Check Permission Successfully
**Setup:**
- user3 wants to check if they can DM user4
- user3 does NOT follow user4

**Test:**
```
GET /api/chat/can-message/4
Authorization: Bearer {user3_token}
```

**Expected Result:**
```json
{
  "canInitiateDm": true,
  "reason": null
}
```
✅ **Pass Criteria:** Permission granted (user4 allows EVERYONE)

---

### Test Case 2.3: Follower Can Also Initiate DM
**Setup:**
- user1 follows user4
- user4 has EVERYONE privacy

**Test:**
```
POST /api/chat/rooms/4
Authorization: Bearer {user1_token}
```

**Expected Result:**
```json
{
  "success": true,
  "message": "Chat room ready",
  "chatRoom": { ... }
}
```
✅ **Pass Criteria:** Chat room created (follow + EVERYONE both allow)

---

## Scenario 3: Thread Continuation

### Test Case 3.1: Both Users Can Message in Existing Thread
**Setup:**
- user1 and user2 have existing chat room
- One of them unfollows the other

**Test 1 - user1 sends message:**
```
POST /api/chat/rooms/{chatRoomId}/messages
Authorization: Bearer {user1_token}
Body: { "content": "Hello!" }
```

**Test 2 - user2 sends message:**
```
POST /api/chat/rooms/{chatRoomId}/messages
Authorization: Bearer {user2_token}
Body: { "content": "Hi back!" }
```

**Expected Result:**
Both requests succeed with:
```json
{
  "success": true,
  "message": "Message sent",
  "messageDTO": { ... }
}
```
✅ **Pass Criteria:** Both users can message (thread existence grants permission)

---

### Test Case 3.2: Existing Thread Persists After Unfollow
**Setup:**
- user1 and user2 have chat room
- user1 unfollows user2

**Test:**
```
GET /api/chat/rooms
Authorization: Bearer {user1_token}
```

**Expected Result:**
Chat room still appears in list
```json
{
  "content": [
    {
      "id": 1,
      "otherUser": { "id": 2, "nickname": "Slow Rabbit" },
      "status": "ACTIVE",
      ...
    }
  ]
}
```
✅ **Pass Criteria:** Chat room still exists and is ACTIVE

---

## Scenario 4: Privacy Setting Updates

### Test Case 4.1: User Can Update to EVERYONE
**Setup:**
- user1 has FOLLOWERS_ONLY privacy

**Test:**
```
PUT /users/me/privacy
Authorization: Bearer {user1_token}
Content-Type: application/json

{
  "allowDmsFrom": "EVERYONE"
}
```

**Expected Result:**
```json
{
  "id": 1,
  "name": "Test User 1",
  "allowDmsFrom": "EVERYONE",
  ...
}
```
✅ **Pass Criteria:** Privacy setting updated successfully

---

### Test Case 4.2: Privacy Change Affects New DM Initiations
**Setup:**
- user1 updates privacy to EVERYONE
- user4 (who doesn't follow user1) tries to DM

**Test 1 - Update privacy:**
```
PUT /users/me/privacy
Authorization: Bearer {user1_token}
Body: { "allowDmsFrom": "EVERYONE" }
```

**Test 2 - user4 checks permission:**
```
GET /api/chat/can-message/1
Authorization: Bearer {user4_token}
```

**Expected Result:**
```json
{
  "canInitiateDm": true,
  "reason": null
}
```
✅ **Pass Criteria:** user4 can now initiate DM (privacy changed to EVERYONE)

---

### Test Case 4.3: UserDTO Includes Privacy Setting
**Setup:**
- Any authenticated user

**Test:**
```
GET /users/me
Authorization: Bearer {user_token}
```

**Expected Result:**
```json
{
  "id": 1,
  "name": "Test User 1",
  "allowDmsFrom": "FOLLOWERS_ONLY",
  ...
}
```
✅ **Pass Criteria:** Response includes allowDmsFrom field

---

## Scenario 5: Block Overrides Everything

### Test Case 5.1: Cannot DM Blocked User
**Setup:**
- user1 has blocked user2
- user1 and user2 had mutual follow

**Test:**
```
POST /api/chat/rooms/1
Authorization: Bearer {user2_token}
```

**Expected Result:**
```json
{
  "success": false,
  "message": "Cannot create chat room with this user",
  "chatRoom": null
}
```
✅ **Pass Criteria:** DM blocked (even though follow exists)

---

### Test Case 5.2: Existing Thread Becomes Blocked
**Setup:**
- user1 and user2 have existing chat room
- user1 blocks user2

**Test:**
```
POST /api/chat/rooms/{chatRoomId}/messages
Authorization: Bearer {user2_token}
Body: { "content": "Test" }
```

**Expected Result:**
```json
{
  "success": false,
  "message": "Cannot send message to this user",
  "messageDTO": null
}
```
✅ **Pass Criteria:** Message sending blocked

---

## Scenario 6: Edge Cases

### Test Case 6.1: Cannot DM Yourself
**Test:**
```
POST /api/chat/rooms/1
Authorization: Bearer {user1_token}  (user1 trying to DM themselves)
```

**Expected Result:**
```json
{
  "success": false,
  "message": "Cannot create chat room with yourself",
  "chatRoom": null
}
```
✅ **Pass Criteria:** Self-DM prevented

---

### Test Case 6.2: Permission Check for Yourself
**Test:**
```
GET /api/chat/can-message/1
Authorization: Bearer {user1_token}  (user1 checking themselves)
```

**Expected Result:**
```json
{
  "canInitiateDm": false,
  "reason": "Cannot message yourself"
}
```
✅ **Pass Criteria:** Self-message permission denied

---

## Quick Test Matrix

| Scenario | user1→user2 | user1→user3 | user1→user4 | user4→user1 | user2→user4 | user3→user4 |
|----------|-------------|-------------|-------------|-------------|-------------|-------------|
| **Can Initiate DM** | ✅ Mutual | ✅ Mutual | ✅ EVERYONE | ❌ No follow | ✅ EVERYONE | ✅ EVERYONE |
| **Reason** | Follow | Follow | Privacy | FOLLOWERS_ONLY | Privacy | Privacy |

---

## Automation Test Script (Pseudocode)

```bash
# Setup: Get JWT tokens for each user
TOKEN_1=$(get_token "test1@example.com")
TOKEN_2=$(get_token "test2@example.com")
TOKEN_3=$(get_token "test3@example.com")
TOKEN_4=$(get_token "test4@example.com")

# Test 1: Mutual followers can DM (FOLLOWERS_ONLY)
assert_success POST /api/chat/rooms/2 -H "Authorization: Bearer $TOKEN_1"

# Test 2: Non-follower cannot DM (FOLLOWERS_ONLY)
assert_failure POST /api/chat/rooms/1 -H "Authorization: Bearer $TOKEN_4"

# Test 3: Anyone can DM user with EVERYONE privacy
assert_success POST /api/chat/rooms/4 -H "Authorization: Bearer $TOKEN_2"
assert_success POST /api/chat/rooms/4 -H "Authorization: Bearer $TOKEN_3"

# Test 4: Permission check works
assert_equals '{"canInitiateDm":true,"reason":null}' \
  GET /api/chat/can-message/4 -H "Authorization: Bearer $TOKEN_2"

# Test 5: Update privacy setting
assert_success PUT /users/me/privacy \
  -H "Authorization: Bearer $TOKEN_1" \
  -d '{"allowDmsFrom":"EVERYONE"}'

# Test 6: UserDTO includes allowDmsFrom
response=$(GET /users/me -H "Authorization: Bearer $TOKEN_4")
assert_contains "$response" '"allowDmsFrom":"EVERYONE"'

echo "All tests passed! ✅"
```

---

## Performance Testing

### Load Test Scenarios
1. **DM Permission Check**
   - 100 concurrent requests to `/api/chat/can-message/{userId}`
   - Expected: < 200ms response time

2. **Chat Room Creation**
   - 50 concurrent new DM threads
   - Expected: < 500ms response time

3. **Privacy Setting Update**
   - 100 users updating privacy simultaneously
   - Expected: No race conditions, all updates succeed

---

## Security Testing

### Penetration Test Scenarios
1. **Authentication Bypass**
   - ❌ Try accessing `/api/chat/can-message/1` without token
   - Expected: 401 Unauthorized

2. **Authorization Bypass**
   - ❌ Try accessing another user's chat room
   - Expected: 403 Forbidden

3. **IDOR (Insecure Direct Object Reference)**
   - ❌ Try creating chat room with non-existent user ID
   - Expected: 404 Not Found

4. **Privacy Setting Tampering**
   - ❌ Try updating another user's privacy setting
   - Expected: 403 Forbidden

---

## Test Execution Log Template

```
Test Execution Date: ___________
Tester: ___________
Environment: Production / Staging / Dev

| Test Case | Status | Notes |
|-----------|--------|-------|
| 1.1 Mutual Followers Can DM | ⬜ | |
| 1.2 Non-Follower Cannot Initiate | ⬜ | |
| 1.3 Permission Check | ⬜ | |
| 2.1 EVERYONE: Non-Follower Can DM | ⬜ | |
| 2.2 EVERYONE: Permission Check | ⬜ | |
| 2.3 EVERYONE: Follower Can DM | ⬜ | |
| 3.1 Thread Continuation | ⬜ | |
| 3.2 Thread Persists After Unfollow | ⬜ | |
| 4.1 Update to EVERYONE | ⬜ | |
| 4.2 Privacy Change Effect | ⬜ | |
| 4.3 UserDTO Includes Privacy | ⬜ | |
| 5.1 Cannot DM Blocked User | ⬜ | |
| 5.2 Blocked Thread | ⬜ | |
| 6.1 Cannot DM Yourself | ⬜ | |
| 6.2 Self Permission Check | ⬜ | |

Overall Status: ⬜ PASS / ⬜ FAIL
```
