package com.iris.iriscode.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey
    val id: String,
    val projectId: Long,
    val summary: String = "",
    val toolCallCount: Int = 0,
    val duration: Long = 0L,
    val cost: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
