package com.iris.iriscode.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iris.iriscode.data.local.OnboardingPreferences
import com.iris.iriscode.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.iris.iriscode.data.remote.gemini.GeminiApi
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject

sealed class OnboardingStep {
    data object Welcome : OnboardingStep()
    data object ApiKey : OnboardingStep()
    data object CreateSession : OnboardingStep()
}

data class OnboardingState(
    val currentStep: OnboardingStep = OnboardingStep.Welcome,
    val apiKey: String = "",
    val apiKeyError: String? = null,
    val isValidating: Boolean = false,
    val onboardingComplete: Boolean = false
)

sealed class OnboardingEvent {
    data object NextStep : OnboardingEvent()
    data class ProjectCreated(val name: String, val id: Long, val path: String) : OnboardingEvent()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: OnboardingPreferences,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>()
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    init {
        viewModelScope.launch {
            preferences.onboardingCompleted.collect { completed ->
                if (completed) {
                    _state.value = _state.value.copy(onboardingComplete = true)
                }
            }
        }
    }

    fun nextStep() {
        val current = _state.value.currentStep
        when (current) {
            OnboardingStep.Welcome -> {
                _state.value = _state.value.copy(currentStep = OnboardingStep.ApiKey)
            }
            OnboardingStep.ApiKey -> {
                val key = _state.value.apiKey
                if (key.isBlank()) {
                    _state.value = _state.value.copy(apiKeyError = "API key cannot be empty")
                    return
                }
                validateApiKey(key)
            }
            OnboardingStep.CreateSession -> {
                // CreateSessionScreen handles project creation directly
            }
        }
    }

    fun updateApiKey(key: String) {
        _state.value = _state.value.copy(apiKey = key, apiKeyError = null)
    }

    fun createFirstProject(name: String, path: String) {
        viewModelScope.launch {
            val id = projectRepository.createProject(name, path)
            preferences.setOnboardingCompleted()
            _state.value = _state.value.copy(onboardingComplete = true)
            _events.emit(OnboardingEvent.ProjectCreated(name, id, path))
        }
    }

    private fun validateApiKey(key: String) {
        _state.value = _state.value.copy(isValidating = true, apiKeyError = null)

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder()
                        .url("${GeminiApi.BASE_URL}/models")
                        .header("x-goog-api-key", key)
                        .get()
                        .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        preferences.saveApiKey(key)
                        Result.success(Unit)
                    } else {
                        val errorBody = response.body?.string() ?: "Unknown error"
                        Result.failure(Exception("Invalid API key: $errorBody"))
                    }
                } catch (e: Exception) {
                    val msg = e.message ?: e::class.simpleName ?: "Unknown error"
                    Result.failure(Exception("Connection error: $msg"))
                }
            }

            result.onSuccess {
                _state.value = _state.value.copy(
                    isValidating = false,
                    currentStep = OnboardingStep.CreateSession
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    isValidating = false,
                    apiKeyError = error.message
                )
            }
        }
    }

    fun skipApiKey() {
        _state.value = _state.value.copy(
            apiKey = "",
            apiKeyError = null,
            currentStep = OnboardingStep.CreateSession
        )
    }
}
