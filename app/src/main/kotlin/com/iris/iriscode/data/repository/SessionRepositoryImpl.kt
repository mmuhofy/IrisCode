package com.iris.iriscode.data.repository

import com.iris.iriscode.data.local.SessionDao
import com.iris.iriscode.domain.model.Session
import com.iris.iriscode.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val dao: SessionDao
) : SessionRepository {

    override fun getSessionsByProject(projectId: Long): Flow<List<Session>> =
        dao.getSessionsByProject(projectId)

    override suspend fun getSessionById(id: String): Session? =
        dao.getSessionById(id)

    override suspend fun createSession(session: Session) =
        dao.insert(session)

    override suspend fun deleteSession(id: String) =
        dao.deleteById(id)
}
