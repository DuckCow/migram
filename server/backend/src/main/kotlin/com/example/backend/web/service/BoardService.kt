package com.example.backend.web.service

import com.example.backend.web.model.*
import com.example.backend.web.repository.BoardRepository
import com.example.backend.web.repository.PostRepository
import com.example.backend.web.repository.UserRepository
import com.example.backend.web.util.SecurityUtils
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class BoardService(
    private val boardRepository: BoardRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val securityUtils: SecurityUtils
) {

    /**
     * Get all ACTIVE boards (public listing)
     */
    fun getAllActiveBoards(pageable: Pageable): Page<BoardResponseDTO> {
        return boardRepository.findByStatus(BoardStatus.ACTIVE, pageable)
            .map { board ->
                val postCount = postRepository.countByBoard(board)
                board.toResponseDTO(postCount)
            }
    }

    /**
     * Get active boards by type (COUNTRY, CITY, TOPIC)
     */
    fun getBoardsByType(boardType: BoardType, pageable: Pageable): Page<BoardResponseDTO> {
        return boardRepository.findByBoardTypeAndStatus(boardType, BoardStatus.ACTIVE, pageable)
            .map { board ->
                val postCount = postRepository.countByBoard(board)
                board.toResponseDTO(postCount)
            }
    }

    /**
     * Get board by ID
     */
    fun getBoardById(boardId: Long): BoardResponseDTO {
        val board = boardRepository.findById(boardId)
            .orElseThrow { EntityNotFoundException("Board not found") }

        if (board.status != BoardStatus.ACTIVE) {
            throw IllegalStateException("Board is not active")
        }

        val postCount = postRepository.countByBoard(board)
        return board.toResponseDTO(postCount)
    }

    /**
     * Get board by context (e.g., country code)
     * Used to find the board for a specific country/city
     */
    fun getBoardByContext(context: String): BoardResponseDTO? {
        val board = boardRepository.findByContextAndStatus(context, BoardStatus.ACTIVE)
            ?: return null

        val postCount = postRepository.countByBoard(board)
        return board.toResponseDTO(postCount)
    }

    /**
     * Create board request (user-initiated, status=PENDING)
     * Requires admin approval
     */
    fun requestBoardCreation(request: BoardCreateRequestDTO): BoardResponseDTO {
        val userId = securityUtils.getCurrentUserId()
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found") }

        // Check if board with same context already exists
        val existing = boardRepository.findByContextAndStatus(
            request.context ?: "",
            BoardStatus.ACTIVE
        )
        if (existing != null) {
            throw IllegalArgumentException("Board with this context already exists")
        }

        val board = Board(
            name = request.name,
            description = request.description,
            boardType = request.boardType,
            context = request.context,
            status = BoardStatus.PENDING, // Awaiting admin approval
            boardCreatedBy = user
        )

        val saved = boardRepository.save(board)
        return saved.toResponseDTO()
    }

    /**
     * Admin: Approve board (status PENDING -> ACTIVE)
     */
    fun approveBoard(boardId: Long): BoardResponseDTO {
        val board = boardRepository.findById(boardId)
            .orElseThrow { EntityNotFoundException("Board not found") }

        if (board.status != BoardStatus.PENDING) {
            throw IllegalStateException("Board is not pending approval")
        }

        val approved = board.copy(status = BoardStatus.ACTIVE)
        val saved = boardRepository.save(approved)
        return saved.toResponseDTO()
    }

    /**
     * Admin: Reject board (status PENDING -> REJECTED)
     */
    fun rejectBoard(boardId: Long): BoardResponseDTO {
        val board = boardRepository.findById(boardId)
            .orElseThrow { EntityNotFoundException("Board not found") }

        if (board.status != BoardStatus.PENDING) {
            throw IllegalStateException("Board is not pending approval")
        }

        val rejected = board.copy(status = BoardStatus.REJECTED)
        val saved = boardRepository.save(rejected)
        return saved.toResponseDTO()
    }

    /**
     * Admin: Get all boards (including PENDING and REJECTED)
     */
    fun getAllBoards(pageable: Pageable): Page<BoardResponseDTO> {
        return boardRepository.findAll(pageable)
            .map { board ->
                val postCount = try {
                    postRepository.countByBoard(board)
                } catch (e: Exception) {
                    0L
                }
                board.toResponseDTO(postCount)
            }
    }
}
