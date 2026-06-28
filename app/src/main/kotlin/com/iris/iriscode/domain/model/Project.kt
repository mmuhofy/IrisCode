package com.iris.iriscode.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val path: String,
    val lastSessionId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
