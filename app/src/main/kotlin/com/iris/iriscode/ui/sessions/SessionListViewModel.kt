package com.iris.iriscode.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iris.iriscode.domain.model.Session
import com.iris.iriscode.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SessionListState(
    val sessions: List<Session> = emptyList(),
    val projectName: String = "",
    val projectId: Long = 0,
    val projectPath: String = ""
)

@HiltViewModel
class SessionListViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SessionListState())
    val state: StateFlow<SessionListState> = _state.asStateFlow()

    fun init(projectName: String, projectId: Long, projectPath: String) {
        _state.value = SessionListState(
            projectName = projectName,
            projectId = projectId,
            projectPath = projectPath
        )
        viewModelScope.launch {
            sessionRepository.getSessionsByProject(projectId).collect { sessions ->
                _state.value = _state.value.copy(sessions = sessions)
            }
        }
    }

    fun createSession(): String {
        val id = UUID.randomUUID().toString()
        viewModelScope.launch {
            sessionRepository.createSession(
                Session(
                    id = id,
                    projectId = _state.value.projectId,
                    summary = "New session"
                )
            )
        }
        return id
    }
}
