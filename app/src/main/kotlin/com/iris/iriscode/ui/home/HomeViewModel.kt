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

data class HomeWorkspaceGroup(
    val project: Project,
    val sessions: List<Session>
)

data class HomeUiState(
    val workspaceGroups: List<HomeWorkspaceGroup> = emptyList(),
    val showNewChatSheet: Boolean = false,
    val newChatName: String = "",
    val newChatPath: String = "",
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
                _state.value = _state.value.copy(
                    workspaceGroups = groups,
                    isLoading = false
                )
            }
        }
    }

    fun showNewChatSheet() {
        _state.value = _state.value.copy(
            showNewChatSheet = true,
            newChatName = "",
            newChatPath = ""
        )
    }

    fun hideNewChatSheet() {
        _state.value = _state.value.copy(showNewChatSheet = false)
    }

    fun updateNewChatName(name: String) {
        _state.value = _state.value.copy(newChatName = name)
    }

    fun updateNewChatPath(path: String) {
        _state.value = _state.value.copy(newChatPath = path)
    }

    fun createChat(
        onCreated: (projectName: String, projectId: Long, projectPath: String, sessionId: String) -> Unit
    ) {
        val name = _state.value.newChatName.trim()
        val path = _state.value.newChatPath.trim()
        if (name.isEmpty() || path.isEmpty()) return

        viewModelScope.launch {
            val projectId = projectRepository.createProject(name, path)
            val sessionId = UUID.randomUUID().toString()
            sessionRepository.createSession(
                Session(
                    id = sessionId,
                    projectId = projectId,
                    summary = "New session"
                )
            )
            _state.value = _state.value.copy(showNewChatSheet = false)
            onCreated(name, projectId, path, sessionId)
        }
    }
}
