package com.example.backend.web.config

import com.example.backend.web.model.Board
import com.example.backend.web.model.BoardStatus
import com.example.backend.web.model.BoardType
import com.example.backend.web.model.Country
import com.example.backend.web.repository.BoardRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Initialize default Country boards on application startup
 * Creates one board per Country enum value
 */
@Component
class BoardDataInitializer(
    private val boardRepository: BoardRepository
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments?) {
        println("=== Initializing default Country boards ===")

        // Create a board for each Country
        Country.values().forEach { country ->
            val context = country.name
            val existing = boardRepository.findByContextAndStatus(context, BoardStatus.ACTIVE)

            if (existing == null) {
                val board = Board(
                    name = country.getDisplayName(),
                    description = "${country.getDisplayName()} community - Share experiences, tips, and connect with others interested in ${country.getDisplayName()}",
                    boardType = BoardType.COUNTRY,
                    context = context,
                    status = BoardStatus.ACTIVE, // System-created boards are auto-approved
                    boardCreatedBy = null // System-created
                )
                boardRepository.save(board)
                println("Created board: ${board.name} (${board.context})")
            } else {
                println("Board already exists: ${existing.name} (${existing.context})")
            }
        }

        println("=== Board initialization complete ===")
    }
}

// Extension function for Country display names
// Simply format the enum name (e.g., "SOUTH_KOREA" -> "South Korea")
fun Country.getDisplayName(): String {
    return this.name
        .split('_')
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
}
