package com.example.backend.web.controller

import com.example.backend.web.dto.UnreadCountResponse
import com.example.backend.web.model.ChatRoomDTO
import com.example.backend.web.model.MessageDTO
import com.example.backend.web.service.ChatResult
import com.example.backend.web.service.ChatService
import com.example.backend.web.service.DmPermissionResult
import com.example.backend.web.service.FileStorageService
import com.example.backend.web.service.MessageResult
import com.example.backend.web.util.SecurityUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val chatService: ChatService,
    private val fileStorageService: FileStorageService,
    private val securityUtils: SecurityUtils
) {

    /**
     * Get chat rooms for current user
     */
    @GetMapping("/rooms")
    fun getChatRooms(
        @PageableDefault(size = 20, sort = ["lastMessageAt"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ): ResponseEntity<Page<ChatRoomDTO>> {
        val chatRooms = chatService.getChatRooms(pageable)
        return ResponseEntity.ok(chatRooms)
    }

    /**
     * Create or get existing chat room with another user
     */
    @PostMapping("/rooms/{userId}")
    fun createOrGetChatRoom(@PathVariable userId: Long): ResponseEntity<ChatResult> {
        val result = chatService.getOrCreateChatRoom(userId)
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    /**
     * Get messages in a chat room
     */
    @GetMapping("/rooms/{chatRoomId}/messages")
    fun getMessages(
        @PathVariable chatRoomId: Long,
        @PageableDefault(size = 50, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ): ResponseEntity<Page<MessageDTO>> {
        val messages = chatService.getMessages(chatRoomId, pageable)
        return ResponseEntity.ok(messages)
    }

    /**
     * Send a message in a chat room (with optional image)
     */
    @PostMapping("/rooms/{chatRoomId}/messages")
    fun sendMessage(
        @PathVariable chatRoomId: Long,
        @RequestBody request: SendMessageRequest
    ): ResponseEntity<MessageResult> {
        val result = chatService.sendMessage(chatRoomId, request.content, request.imageUrl)
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    /**
     * Mark all messages in a chat room as read
     */
    @PutMapping("/rooms/{chatRoomId}/read")
    fun markMessagesAsRead(@PathVariable chatRoomId: Long): ResponseEntity<Void> {
        chatService.markMessagesAsRead(chatRoomId)
        return ResponseEntity.ok().build()
    }

    /**
     * Get unread message count for a specific chat room
     */
    @GetMapping("/rooms/{chatRoomId}/unread")
    fun getUnreadCount(@PathVariable chatRoomId: Long): ResponseEntity<UnreadCountResponse> {
        val count = chatService.getUnreadCount(chatRoomId)
        return ResponseEntity.ok(UnreadCountResponse(count))
    }

    /**
     * Get total unread message count across all chat rooms
     */
    @GetMapping("/unread")
    fun getTotalUnreadCount(): ResponseEntity<UnreadCountResponse> {
        val count = chatService.getTotalUnreadCount()
        return ResponseEntity.ok(UnreadCountResponse(count))
    }

    /**
     * Check if current user can initiate DM with target user
     * Used by frontend to enable/disable Message button and show appropriate message
     */
    @GetMapping("/can-message/{userId}")
    fun canInitiateDm(@PathVariable userId: Long): ResponseEntity<DmPermissionResult> {
        val result = chatService.canInitiateDm(userId)
        return ResponseEntity.ok(result)
    }

    /**
     * Upload image for chat message
     */
    @PostMapping("/upload-image")
    fun uploadChatImage(@RequestParam("file") file: MultipartFile): ResponseEntity<ImageUploadResponse> {
        // Generate unique filename
        val originalFilename = file.originalFilename ?: "image.jpg"
        val extension = originalFilename.substringAfterLast('.', "jpg")
        val uniqueFilename = "${UUID.randomUUID()}.$extension"

        // Upload to storage (S3 or local)
        val imageUrl = fileStorageService.uploadFile(file, uniqueFilename, "chat")
            ?: return ResponseEntity.badRequest().body(
                ImageUploadResponse(success = false, imageUrl = null, message = "Failed to upload image")
            )

        return ResponseEntity.ok(
            ImageUploadResponse(success = true, imageUrl = imageUrl, message = "Image uploaded successfully")
        )
    }

    /**
     * SSE endpoint for real-time chat messages
     */
    @GetMapping("/stream")
    fun streamChatMessages(): SseEmitter {
        val currentUserId = securityUtils.getCurrentUserId()
        return chatService.registerChatEmitter(currentUserId)
    }
}

data class SendMessageRequest(
    val content: String? = null,
    val imageUrl: String? = null
) {
    init {
        require(content != null || imageUrl != null) {
            "Message must have either content or imageUrl"
        }
    }
}

data class ImageUploadResponse(
    val success: Boolean,
    val imageUrl: String?,
    val message: String
)
