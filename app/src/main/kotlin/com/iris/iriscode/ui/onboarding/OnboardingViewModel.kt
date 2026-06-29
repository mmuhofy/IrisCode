package com.iris.iriscode.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iris.iriscode.data.local.OnboardingPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.iris.iriscode.data.remote.gemini.GeminiApi
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject

sealed class OnboardingStep {
    data object Welcome : OnboardingStep()
    data object ApiKey : OnboardingStep()
    data object ProjectSetup : OnboardingStep()
}

data class OnboardingState(
    val currentStep: OnboardingStep = OnboardingStep.Welcome,
    val apiKey: String = "",
    val apiKeyError: String? = null,
    val isValidating: Boolean = false,
    val projectPath: String? = null,
    val onboardingComplete: Boolean = false
)

sealed class OnboardingEvent {
    data object NextStep : OnboardingEvent()
    data object Complete : OnboardingEvent()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: OnboardingPreferences
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
                    _events.emit(OnboardingEvent.Complete)
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
            OnboardingStep.ProjectSetup -> {
                completeOnboarding()
            }
        }
    }

    fun updateApiKey(key: String) {
        _state.value = _state.value.copy(apiKey = key, apiKeyError = null)
    }

    fun setProjectPath(path: String) {
        _state.value = _state.value.copy(projectPath = path)
    }

    private fun validateApiKey(key: String) {
        _state.value = _state.value.copy(isValidating = true, apiKeyError = null)

        viewModelScope.launch {
            try {
                val request = Request.Builder()
                    .url("${GeminiApi.BASE_URL}/models")
                    .header("x-goog-api-key", key)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    preferences.saveApiKey(key)
                    _state.value = _state.value.copy(
                        isValidating = false,
                        currentStep = OnboardingStep.ProjectSetup
                    )
                } else {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    _state.value = _state.value.copy(
                        isValidating = false,
                        apiKeyError = "Invalid API key: $errorBody"
                    )
                }
            } catch (e: Exception) {
                val msg = e.message ?: e::class.simpleName ?: "Unknown error"
                _state.value = _state.value.copy(
                    isValidating = false,
                    apiKeyError = "Connection error: $msg"
                )
            }
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            preferences.setOnboardingCompleted()
            _state.value = _state.value.copy(onboardingComplete = true)
            _events.emit(OnboardingEvent.Complete)
        }
    }

    fun skipApiKey() {
        _state.value = _state.value.copy(
            apiKey = "",
            apiKeyError = null,
            currentStep = OnboardingStep.ProjectSetup
        )
    }
}
