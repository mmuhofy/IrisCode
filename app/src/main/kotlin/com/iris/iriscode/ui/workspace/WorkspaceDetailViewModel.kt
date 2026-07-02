package com.iris.iriscode.ui.workspace

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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkspaceDetailUiState(
    val projectName: String = "",
    val projectPath: String = "",
    val sessions: List<Session> = emptyList(),
    val contextFiles: List<String> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class WorkspaceDetailViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WorkspaceDetailUiState())
    val state: StateFlow<WorkspaceDetailUiState> = _state.asStateFlow()

    fun init(projectName: String, projectId: Long, projectPath: String) {
        _state.value = _state.value.copy(
            projectName = projectName,
            projectPath = projectPath
        )
        viewModelScope.launch {
            val project = projectRepository.getProjectById(projectId)
            sessionRepository.getSessionsByProject(projectId).collect { sessions ->
                _state.value = _state.value.copy(
                    sessions = sessions,
                    contextFiles = emptyList(),
                    isLoading = false
                )
            }
        }
    }
}
