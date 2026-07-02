package com.iris.iriscode.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iris.iriscode.domain.model.Project
import com.iris.iriscode.domain.model.Session
import com.iris.iriscode.domain.repository.ProjectRepository
import com.iris.iriscode.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class HomeTab { Chats, Workspaces }

data class SessionWithWorkspace(
    val session: Session,
    val projectName: String,
    val projectId: Long,
    val projectPath: String
)

data class HomeWorkspaceGroup(
    val project: Project,
    val sessions: List<Session>
)

data class HomeUiState(
    val workspaceGroups: List<HomeWorkspaceGroup> = emptyList(),
    val allSessions: List<SessionWithWorkspace> = emptyList(),
    val selectedTab: HomeTab = HomeTab.Chats,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            projectRepository.getAllProjects().collect { projects ->
                val groups = projects.map { project ->
                    val sessions = sessionRepository.getSessionsByProject(project.id).first()
                    HomeWorkspaceGroup(project, sessions)
                }
                val allSessions = groups.flatMap { group ->
                    group.sessions.map { session ->
                        SessionWithWorkspace(
                            session = session,
                            projectName = group.project.name,
                            projectId = group.project.id,
                            projectPath = group.project.path
                        )
                    }
                }.sortedByDescending { it.session.createdAt }

                _state.value = _state.value.copy(
                    workspaceGroups = groups,
                    allSessions = allSessions,
                    isLoading = false
                )
            }
        }
    }

    fun selectTab(tab: HomeTab) {
        _state.value = _state.value.copy(selectedTab = tab)
    }

    fun quickCreateChat(
        onCreated: (projectName: String, projectId: Long, projectPath: String, sessionId: String) -> Unit
    ) {
        viewModelScope.launch {
            val projectId = projectRepository.createProject("New Chat", "")
            val sessionId = UUID.randomUUID().toString()
            sessionRepository.createSession(
                Session(
                    id = sessionId,
                    projectId = projectId,
                    summary = "New chat"
                )
            )
            onCreated("New Chat", projectId, "", sessionId)
        }
    }
}
