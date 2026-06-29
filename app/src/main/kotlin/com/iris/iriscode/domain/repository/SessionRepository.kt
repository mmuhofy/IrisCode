package com.iris.iriscode.domain.repository

import com.iris.iriscode.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getSessionsByProject(projectId: Long): Flow<List<Session>>
    suspend fun getSessionById(id: String): Session?
    suspend fun createSession(session: Session)
    suspend fun deleteSession(id: String)
}
