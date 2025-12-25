package com.example.backend.web.model

/**
 * DM Privacy Setting (Default follow-gated, user opt-in for non-follower DMs)
 *
 * Determines who can INITIATE a DM with this user:
 * - FOLLOWERS_ONLY: Only approved followers can start a DM (default)
 * - EVERYONE: Anyone (not blocked) can start a DM (user-enabled option)
 *
 * DM Initiation Rule:
 * A can initiate DM to B if:
 * - B has NOT blocked A
 * - AND (approved Follow(A->B) exists OR B.dm_privacy == EVERYONE)
 *
 * Thread/Reply Rule:
 * - Once a DM thread exists, BOTH users can freely message
 * - Thread existence grants reply permission regardless of follow direction
 * - No message count/frequency restrictions (open DM)
 *
 * Block Rule:
 * - Block overrides everything - prevents initiation and continuation
 *
 * Unfollow Rule:
 * - Unfollow does NOT delete existing threads
 * - Existing threads remain usable unless blocked
 */
enum class DmPrivacySetting {
    FOLLOWERS_ONLY,  // Default: Only approved followers can initiate DM
    EVERYONE         // Optional: Anyone (not blocked) can initiate DM
}

/**
 * Chat Room Status (Simplified, Open DM)
 *
 * Only two states:
 * - ACTIVE: DM thread exists and messaging is allowed
 * - BLOCKED: Either user has blocked the other (hides thread for blocker)
 *
 * No approval flow - once created, thread is immediately ACTIVE
 */
enum class ChatRoomStatus {
    ACTIVE,    // Active conversation (both can message freely)
    BLOCKED    // Thread blocked by either user
}
