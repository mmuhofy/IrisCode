package com.iris.iriscode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.iris.iriscode.domain.model.Project
import com.iris.iriscode.ui.chat.ChatScreen
import com.iris.iriscode.ui.chat.ChatViewModel
import com.iris.iriscode.ui.onboarding.ApiKeyScreen
import com.iris.iriscode.ui.onboarding.OnboardingStep
import com.iris.iriscode.ui.onboarding.OnboardingViewModel
import com.iris.iriscode.ui.onboarding.ProjectSetupScreen
import com.iris.iriscode.ui.onboarding.WelcomeScreen
import com.iris.iriscode.ui.projects.ProjectsScreen
import com.iris.iriscode.ui.projects.ProjectsViewModel
import com.iris.iriscode.ui.theme.IrisCodeTheme
import dagger.hilt.android.AndroidEntryPoint

private sealed class Screen {
    data object Onboarding : Screen()
    data object Projects : Screen()
    data class Chat(val projectName: String, val projectId: Long) : Screen()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            IrisCodeTheme {
                val onboardingVm: OnboardingViewModel = hiltViewModel()
                val state by onboardingVm.state.collectAsState()
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Onboarding) }

                if (state.onboardingComplete && currentScreen == Screen.Onboarding) {
                    currentScreen = Screen.Projects
                }

                when (val screen = currentScreen) {
                    is Screen.Onboarding -> {
                        AnimatedContent(
                            targetState = state.currentStep,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "onboarding"
                        ) { step ->
                            when (step) {
                                OnboardingStep.Welcome -> {
                                    WelcomeScreen(onNext = onboardingVm::nextStep)
                                }
                                OnboardingStep.ApiKey -> {
                                    ApiKeyScreen(
                                        apiKey = state.apiKey,
                                        error = state.apiKeyError,
                                        isValidating = state.isValidating,
                                        onApiKeyChange = onboardingVm::updateApiKey,
                                        onNext = onboardingVm::nextStep,
                                        onSkip = onboardingVm::skipApiKey
                                    )
                                }
                                OnboardingStep.ProjectSetup -> {
                                    ProjectSetupScreen(
                                        projectPath = state.projectPath,
                                        onProjectPathSelected = onboardingVm::setProjectPath,
                                        onNext = onboardingVm::nextStep
                                    )
                                }
                            }
                        }
                    }

                    is Screen.Projects -> {
                        val projectsVm: ProjectsViewModel = hiltViewModel()
                        ProjectsScreen(
                            viewModel = projectsVm,
                            onProjectClick = { project ->
                                currentScreen = Screen.Chat(project.name, project.id)
                            },
                            onCreateProject = { projectsVm.showCreateSheet() }
                        )
                    }

                    is Screen.Chat -> {
                        val chatVm: ChatViewModel = hiltViewModel()
                        ChatScreen(
                            viewModel = chatVm,
                            projectName = screen.projectName,
                            onBack = { currentScreen = Screen.Projects }
                        )
                    }
                }
            }
        }
    }
}
