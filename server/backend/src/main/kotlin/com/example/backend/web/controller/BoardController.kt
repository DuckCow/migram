package com.example.backend.web.controller

import com.example.backend.web.model.BoardCreateRequestDTO
import com.example.backend.web.model.BoardResponseDTO
import com.example.backend.web.model.BoardType
import com.example.backend.web.service.BoardService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/boards")
class BoardController(
    private val boardService: BoardService
) {

    /**
     * GET /boards
     * Get all ACTIVE boards
     */
    @GetMapping
    fun getAllBoards(pageable: Pageable): Page<BoardResponseDTO> {
        return boardService.getAllActiveBoards(pageable)
    }

    /**
     * GET /boards/type/{boardType}
     * Get boards by type (COUNTRY, CITY, TOPIC)
     */
    @GetMapping("/type/{boardType}")
    fun getBoardsByType(
        @PathVariable boardType: BoardType,
        pageable: Pageable
    ): Page<BoardResponseDTO> {
        return boardService.getBoardsByType(boardType, pageable)
    }

    /**
     * GET /boards/{id}
     * Get board by ID
     */
    @GetMapping("/{id}")
    fun getBoardById(@PathVariable id: Long): BoardResponseDTO {
        return boardService.getBoardById(id)
    }

    /**
     * GET /boards/context/{context}
     * Get board by context (e.g., country code like "KR", "US")
     */
    @GetMapping("/context/{context}")
    fun getBoardByContext(@PathVariable context: String): ResponseEntity<BoardResponseDTO> {
        val board = boardService.getBoardByContext(context)
        return if (board != null) {
            ResponseEntity.ok(board)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * POST /boards
     * Request board creation (user-initiated, requires admin approval)
     */
    @PostMapping
    fun requestBoardCreation(@RequestBody request: BoardCreateRequestDTO): BoardResponseDTO {
        return boardService.requestBoardCreation(request)
    }

    /**
     * POST /boards/{id}/approve
     * Admin: Approve board (PENDING -> ACTIVE)
     */
    @PostMapping("/{id}/approve")
    fun approveBoard(@PathVariable id: Long): BoardResponseDTO {
        return boardService.approveBoard(id)
    }

    /**
     * POST /boards/{id}/reject
     * Admin: Reject board (PENDING -> REJECTED)
     */
    @PostMapping("/{id}/reject")
    fun rejectBoard(@PathVariable id: Long): BoardResponseDTO {
        return boardService.rejectBoard(id)
    }

    /**
     * GET /admin/boards
     * Admin: Get all boards (including PENDING and REJECTED)
     */
    @GetMapping("/admin/all")
    fun getAllBoardsForAdmin(pageable: Pageable): Page<BoardResponseDTO> {
        return boardService.getAllBoards(pageable)
    }
}
