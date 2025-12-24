package com.example.backend.web.repository

import com.example.backend.web.model.Block
import com.example.backend.web.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BlockRepository : JpaRepository<Block, Long> {
    fun findByBlockerAndBlocked(blocker: User, blocked: User): Block?

    fun existsByBlockerAndBlocked(blocker: User, blocked: User): Boolean

    fun findByBlocker(blocker: User): List<Block>

    fun deleteByBlockerAndBlocked(blocker: User, blocked: User)
}
