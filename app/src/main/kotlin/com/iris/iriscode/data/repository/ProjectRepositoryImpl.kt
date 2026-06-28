package com.iris.iriscode.data.repository

import com.iris.iriscode.data.local.ProjectDao
import com.iris.iriscode.domain.model.Project
import com.iris.iriscode.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val dao: ProjectDao
) : ProjectRepository {

    override fun getAllProjects(): Flow<List<Project>> = dao.getAllProjects()

    override suspend fun getProjectById(id: Long): Project? = dao.getProjectById(id)

    override suspend fun createProject(name: String, path: String): Long {
        val project = Project(name = name, path = path)
        return dao.insert(project)
    }

    override suspend fun deleteProject(id: Long) = dao.deleteById(id)

    override suspend fun updateLastSession(projectId: Long, sessionId: String) =
        dao.updateLastSession(projectId, sessionId)
}
