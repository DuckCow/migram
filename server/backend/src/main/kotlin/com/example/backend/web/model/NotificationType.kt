package com.example.backend.web.model

enum class NotificationType {
    // Follow-related
    FOLLOW_REQUEST,      // Someone sent you a follow request
    FOLLOW_ACCEPTED,     // Your follow request was accepted

    // Comment-related
    COMMENT_ON_POST,     // Someone commented on your post
    REPLY_ON_COMMENT,    // Someone replied to your comment

    // Vote-related
    VOTE_ON_POST,        // Someone upvoted your post (milestone: 10, 50, 100, etc.)
    VOTE_ON_COMMENT,     // Someone upvoted your comment (milestone: 10, 50, 100, etc.)

    // Moderation-related
    REPORT_RESOLVED      // Your report was resolved by moderators
}
