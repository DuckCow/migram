package com.example.backend.web.service

import com.example.backend.web.model.*
import com.example.backend.web.repository.ChatRoomRepository
import com.example.backend.web.repository.MessageRepository
import com.example.backend.web.repository.UserRepository
import com.example.backend.web.util.SecurityUtils
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

data class ChatResult(
    val success: Boolean,
    val message: String,
    val chatRoom: ChatRoomDTO? = null
)

data class MessageResult(
    val success: Boolean,
    val message: String,
    val messageDTO: MessageDTO? = null
)

data class DmPermissionResult(
    val canInitiateDm: Boolean,
    val reason: String? = null  // null if allowed, error message if not allowed
)

@Service
@Transactional
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val followService: FollowService,
    private val blockService: BlockService,
    private val securityUtils: SecurityUtils
) {

    // SSE emitters for real-time chat
    // Key: userId, Value: SseEmitter
    private val chatEmitters = ConcurrentHashMap<Long, SseEmitter>()

    /**
     * Create or get existing chat room with another user
     *
     * DM Initiation Rule:
     * A can initiate DM to B if:
     * - B has NOT blocked A
     * - AND (approved Follow(A->B) exists OR B.dm_privacy == EVERYONE)
     *
     * Permission Priority:
     * 1. Block (highest) - Cannot DM if blocked
     * 2. Thread exists - If thread exists, both can message (reply allowed)
     * 3. Follow exists OR privacy is EVERYONE - Required to INITIATE new thread
     *
     * Once thread exists:
     * - Both users can freely message (no limits, no approval)
     * - Thread existence grants reply permission regardless of follow direction
     */
    fun getOrCreateChatRoom(otherUserId: Long): ChatResult {
        val currentUserId = securityUtils.getCurrentUserId()

        if (currentUserId == otherUserId) {
            return ChatResult(false, "Cannot create chat room with yourself")
        }

        val currentUser = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("Current user not found") }
        val otherUser = userRepository.findById(otherUserId)
            .orElseThrow { EntityNotFoundException("Other user not found") }

        // 1. Check if current user is blocked by other user
        if (blockService.isBlockedBy(otherUserId)) {
            return ChatResult(false, "Cannot create chat room with this user")
        }

        // 2. Check if thread already exists (reply allowed)
        val existingChatRoom = chatRoomRepository.findByUsers(currentUser, otherUser)
        if (existingChatRoom != null) {
            val chatRoomDTO = existingChatRoom.toChatRoomDTO(currentUserId, messageRepository)
            return ChatResult(true, "Chat room ready", chatRoomDTO)
        }

        // 3. For NEW thread initiation, check:
        //    - Follow(currentUser -> otherUser) exists (other user accepted current user as follower)
        //    OR
        //    - otherUser.dm_privacy == EVERYONE (user opted-in to accept DMs from everyone)
        val isApprovedFollower = followService.isFollowing(currentUserId, otherUserId)

        if (!isApprovedFollower && otherUser.allowDmsFrom == DmPrivacySetting.FOLLOWERS_ONLY) {
            return ChatResult(false, "This user only accepts messages from approved followers")
        }

        // 4. Create new thread (immediately ACTIVE)
        val newChatRoom = chatRoomRepository.save(
            ChatRoom(
                user1 = currentUser,
                user2 = otherUser,
                initiator = currentUser,  // Track who started (for analytics)
                status = ChatRoomStatus.ACTIVE  // No approval needed, immediately active
            )
        )

        val chatRoomDTO = newChatRoom.toChatRoomDTO(currentUserId, messageRepository)

        return ChatResult(true, "Chat room ready", chatRoomDTO)
    }

    /**
     * Check if current user can initiate DM with target user
     *
     * Used by frontend to enable/disable Message button and show appropriate message
     *
     * Returns:
     * - canInitiateDm: true if allowed, false if not
     * - reason: null if allowed, error message if not allowed
     */
    @Transactional(readOnly = true)
    fun canInitiateDm(otherUserId: Long): DmPermissionResult {
        val currentUserId = securityUtils.getCurrentUserId()

        // Cannot DM yourself
        if (currentUserId == otherUserId) {
            return DmPermissionResult(false, "Cannot message yourself")
        }

        val otherUser = userRepository.findById(otherUserId)
            .orElseThrow { EntityNotFoundException("User not found") }

        // Check if blocked
        if (blockService.isBlockedBy(otherUserId)) {
            return DmPermissionResult(false, "Cannot message this user")
        }

        // Check if thread already exists (can always continue existing conversation)
        val currentUser = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("Current user not found") }
        val existingChatRoom = chatRoomRepository.findByUsers(currentUser, otherUser)
        if (existingChatRoom != null) {
            return DmPermissionResult(true)
        }

        // For new thread initiation, check:
        // - Follow(currentUser -> otherUser) exists (approved follower)
        // OR
        // - otherUser.dm_privacy == EVERYONE
        val isApprovedFollower = followService.isFollowing(currentUserId, otherUserId)

        if (!isApprovedFollower && otherUser.allowDmsFrom == DmPrivacySetting.FOLLOWERS_ONLY) {
            return DmPermissionResult(false, "This user only accepts messages from approved followers")
        }

        return DmPermissionResult(true)
    }

    /**
     * Send a message in a chat room (Open DM)
     *
     * Permission priority:
     * 1. Block (highest) - Cannot message if blocked
     * 2. Thread exists + ACTIVE - Both users can freely message
     *
     * Checks:
     * 1. User is part of the chat room
     * 2. Chat room is ACTIVE (not BLOCKED)
     * 3. No block relationship exists
     *
     * No message limits - both users can send unlimited messages
     */
    fun sendMessage(chatRoomId: Long, content: String): MessageResult {
        val currentUserId = securityUtils.getCurrentUserId()
        val currentUser = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("Current user not found") }

        val chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow { EntityNotFoundException("Chat room not found") }

        // Verify user is part of this chat room
        if (!chatRoom.containsUser(currentUserId)) {
            return MessageResult(false, "You are not part of this chat room")
        }

        val otherUser = chatRoom.getOtherUser(currentUserId)

        // Check if chat room is blocked
        if (chatRoom.status == ChatRoomStatus.BLOCKED) {
            return MessageResult(false, "This conversation has been blocked")
        }

        // Check block relationship
        if (blockService.hasBlockRelationship(currentUserId, otherUser.id)) {
            return MessageResult(false, "Cannot send message to this user")
        }

        // Create message (no limits, no approval needed)
        val message = Message(
            chatRoom = chatRoom,
            sender = currentUser,
            content = content
        )
        val savedMessage = messageRepository.save(message)

        // Update chat room metadata
        chatRoom.lastMessageAt = savedMessage.createdAt
        chatRoom.lastMessagePreview = if (content.length > 100) {
            content.substring(0, 100) + "..."
        } else {
            content
        }
        chatRoomRepository.save(chatRoom)

        val messageDTO = savedMessage.toDTO()

        // Send real-time notification to other user via SSE
        sendMessageNotification(otherUser.id, messageDTO)

        return MessageResult(true, "Message sent", messageDTO)
    }

    /**
     * Get messages in a chat room
     */
    @Transactional(readOnly = true)
    fun getMessages(chatRoomId: Long, pageable: Pageable): Page<MessageDTO> {
        val currentUserId = securityUtils.getCurrentUserId()

        val chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow { EntityNotFoundException("Chat room not found") }

        // Verify user is part of this chat room
        if (!chatRoom.containsUser(currentUserId)) {
            throw SecurityException("You are not part of this chat room")
        }

        return messageRepository.findByChatRoomOrderByCreatedAtDesc(chatRoom, pageable)
            .map { it.toDTO() }
    }

    /**
     * Mark all messages in a chat room as read
     */
    fun markMessagesAsRead(chatRoomId: Long) {
        val currentUserId = securityUtils.getCurrentUserId()
        val currentUser = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("Current user not found") }

        val chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow { EntityNotFoundException("Chat room not found") }

        // Verify user is part of this chat room
        if (!chatRoom.containsUser(currentUserId)) {
            throw SecurityException("You are not part of this chat room")
        }

        messageRepository.markAllAsRead(chatRoom, currentUser)
    }

    /**
     * Get chat rooms for current user
     */
    @Transactional(readOnly = true)
    fun getChatRooms(pageable: Pageable): Page<ChatRoomDTO> {
        val currentUserId = securityUtils.getCurrentUserId()
        val currentUser = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("Current user not found") }

        return chatRoomRepository.findByUser(currentUser, pageable)
            .map { it.toChatRoomDTO(currentUserId, messageRepository) }
    }

    /**
     * Get unread message count for a specific chat room
     */
    @Transactional(readOnly = true)
    fun getUnreadCount(chatRoomId: Long): Long {
        val currentUserId = securityUtils.getCurrentUserId()
        val currentUser = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("Current user not found") }

        val chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow { EntityNotFoundException("Chat room not found") }

        if (!chatRoom.containsUser(currentUserId)) {
            return 0
        }

        return messageRepository.countUnreadMessages(chatRoom, currentUser)
    }

    /**
     * Get total unread message count across all chat rooms
     */
    @Transactional(readOnly = true)
    fun getTotalUnreadCount(): Long {
        val currentUserId = securityUtils.getCurrentUserId()
        val currentUser = userRepository.findById(currentUserId)
            .orElseThrow { EntityNotFoundException("Current user not found") }

        val chatRooms = chatRoomRepository.findAllByUser(currentUser)

        if (chatRooms.isEmpty()) {
            return 0
        }

        return messageRepository.countTotalUnreadMessages(chatRooms, currentUser)
    }

    /**
     * Register SSE emitter for real-time chat
     */
    fun registerChatEmitter(userId: Long): SseEmitter {
        val emitter = SseEmitter(Long.MAX_VALUE)

        emitter.onCompletion {
            chatEmitters.remove(userId)
        }

        emitter.onTimeout {
            chatEmitters.remove(userId)
        }

        emitter.onError {
            chatEmitters.remove(userId)
        }

        chatEmitters[userId] = emitter

        return emitter
    }

    /**
     * Send message notification via SSE
     */
    private fun sendMessageNotification(userId: Long, messageDTO: MessageDTO) {
        val emitter = chatEmitters[userId]
        emitter?.let {
            try {
                it.send(
                    SseEmitter.event()
                        .name("new_message")
                        .data(messageDTO)
                )
            } catch (e: Exception) {
                chatEmitters.remove(userId)
            }
        }
    }
}

/**
 * Extension function to convert ChatRoom to ChatRoomDTO
 */
private fun ChatRoom.toChatRoomDTO(currentUserId: Long, messageRepository: MessageRepository): ChatRoomDTO {
    val otherUser = getOtherUser(currentUserId)
    val currentUser = if (user1.id == currentUserId) user1 else user2
    val unreadCount = messageRepository.countUnreadMessages(this, currentUser)

    return ChatRoomDTO(
        id = id,
        otherUser = otherUser.toDTO(),
        lastMessageAt = lastMessageAt?.toString(),
        lastMessagePreview = lastMessagePreview,
        unreadCount = unreadCount.toInt(),
        createdAt = createdAt.toString(),
        status = status.name,
        initiatorId = initiator.id,
        isPending = false  // No pending status in Open DM model
    )
}
