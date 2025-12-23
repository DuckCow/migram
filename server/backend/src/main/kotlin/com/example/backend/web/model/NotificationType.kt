package com.example.backend.web.model

enum class NotificationType {
    FOLLOW_REQUEST,      // Someone sent you a follow request
    FOLLOW_ACCEPTED,     // Your follow request was accepted
    COMMENT_ON_POST,     // Someone commented on your post
    REPLY_ON_COMMENT     // Someone replied to your comment
}
