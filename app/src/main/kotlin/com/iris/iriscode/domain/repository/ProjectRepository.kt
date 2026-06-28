package com.iris.iriscode.domain.repository

import com.iris.iriscode.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getAllProjects(): Flow<List<Project>>
    suspend fun getProjectById(id: Long): Project?
    suspend fun createProject(name: String, path: String): Long
    suspend fun deleteProject(id: Long)
    suspend fun updateLastSession(projectId: Long, sessionId: String)
}
