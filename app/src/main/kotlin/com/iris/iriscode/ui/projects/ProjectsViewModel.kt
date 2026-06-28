package com.iris.iriscode.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iris.iriscode.domain.model.Project
import com.iris.iriscode.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val showCreateSheet: Boolean = false,
    val showDeleteConfirm: Project? = null,
    val newProjectName: String = "",
    val newProjectPath: String? = null
)

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectsUiState())
    val state: StateFlow<ProjectsUiState> = _state.asStateFlow()

    val projects: StateFlow<List<Project>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun showCreateSheet() {
        _state.value = _state.value.copy(
            showCreateSheet = true,
            newProjectName = "",
            newProjectPath = null
        )
    }

    fun hideCreateSheet() {
        _state.value = _state.value.copy(showCreateSheet = false)
    }

    fun updateNewProjectName(name: String) {
        _state.value = _state.value.copy(newProjectName = name)
    }

    fun updateNewProjectPath(path: String) {
        _state.value = _state.value.copy(newProjectPath = path)
    }

    fun createProject() {
        val name = _state.value.newProjectName.trim()
        val path = _state.value.newProjectPath
        if (name.isEmpty() || path == null) return

        viewModelScope.launch {
            repository.createProject(name, path)
            _state.value = _state.value.copy(
                showCreateSheet = false,
                newProjectName = "",
                newProjectPath = null
            )
        }
    }

    fun requestDelete(project: Project) {
        _state.value = _state.value.copy(showDeleteConfirm = project)
    }

    fun cancelDelete() {
        _state.value = _state.value.copy(showDeleteConfirm = null)
    }

    fun confirmDelete() {
        val project = _state.value.showDeleteConfirm ?: return
        viewModelScope.launch {
            repository.deleteProject(project.id)
            _state.value = _state.value.copy(showDeleteConfirm = null)
        }
    }
}
