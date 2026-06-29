package com.iris.iriscode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iris.iriscode.ui.chat.ChatScreen
import com.iris.iriscode.ui.chat.ChatViewModel
import com.iris.iriscode.ui.onboarding.ApiKeyScreen
import com.iris.iriscode.ui.onboarding.CreateSessionScreen
import com.iris.iriscode.ui.onboarding.WelcomeScreen
import com.iris.iriscode.ui.onboarding.OnboardingEvent
import com.iris.iriscode.ui.onboarding.OnboardingStep
import com.iris.iriscode.ui.onboarding.OnboardingViewModel
import com.iris.iriscode.ui.projects.ProjectsScreen
import com.iris.iriscode.ui.projects.ProjectsViewModel
import com.iris.iriscode.ui.sessions.SessionListScreen
import com.iris.iriscode.ui.sessions.SessionListViewModel
import com.iris.iriscode.ui.theme.IrisCodeTheme
import com.iris.iriscode.ui.theme.IrisPrimary
import com.iris.iriscode.ui.theme.IrisSurfaceVariant
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.text.font.FontWeight

private sealed class Screen {
    object Onboarding : Screen()
    object Projects : Screen()
    data class SessionList(val projectName: String, val projectId: Long, val projectPath: String) : Screen()
    data class Chat(val projectName: String, val projectId: Long, val projectPath: String, val sessionId: String? = null) : Screen()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appDir = filesDir.resolve("projects").absolutePath

        setContent {
            IrisCodeTheme {
                val onboardingVm: OnboardingViewModel = hiltViewModel()
                val state by onboardingVm.state.collectAsState()
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Onboarding) }

                LaunchedEffect(Unit) {
                    onboardingVm.events.collect { event ->
                        when (event) {
                            is OnboardingEvent.ProjectCreated -> {
                                currentScreen = Screen.SessionList(event.name, event.id, event.path)
                            }
                            is OnboardingEvent.NextStep -> { }
                        }
                    }
                }

                if (state.onboardingComplete && currentScreen == Screen.Onboarding) {
                    currentScreen = Screen.Projects
                }

                when (val screen = currentScreen) {
                    is Screen.Onboarding -> {
                        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().navigationBarsPadding()) {
                            Box(modifier = Modifier.weight(1f)) {
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
                                        OnboardingStep.CreateSession -> {
                                            CreateSessionScreen(
                                                onCreate = { name, _ ->
                                                    val path = "$appDir/$name"
                                                    onboardingVm.createFirstProject(name, path)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OnboardingPageIndicator(
                                currentStep = state.currentStep,
                                modifier = Modifier.padding(bottom = 48.dp)
                            )
                        }
                    }

                    is Screen.Projects -> {
                        val projectsVm: ProjectsViewModel = hiltViewModel()
                        ProjectsScreen(
                            viewModel = projectsVm,
                            onProjectClick = { project ->
                                currentScreen = Screen.SessionList(project.name, project.id, project.path)
                            },
                            onCreateProject = { projectsVm.showCreateSheet() }
                        )
                    }

                    is Screen.SessionList -> {
                        val sessionVm: SessionListViewModel = hiltViewModel()
                        LaunchedEffect(screen.projectId) {
                            sessionVm.init(screen.projectName, screen.projectId, screen.projectPath)
                        }
                        SessionListScreen(
                            viewModel = sessionVm,
                            onSessionClick = { sessionId ->
                                currentScreen = Screen.Chat(screen.projectName, screen.projectId, screen.projectPath, sessionId)
                            },
                            onNewSession = {
                                val id = sessionVm.createSession()
                                currentScreen = Screen.Chat(screen.projectName, screen.projectId, screen.projectPath, id)
                            },
                            onBack = { currentScreen = Screen.Projects }
                        )
                    }

                    is Screen.Chat -> {
                        val chatVm: ChatViewModel = hiltViewModel()
                        ChatScreen(
                            viewModel = chatVm,
                            projectName = screen.projectName,
                            projectPath = screen.projectPath,
                            onBack = { currentScreen = Screen.SessionList(screen.projectName, screen.projectId, screen.projectPath) }
                        )
                    }

                }
            }
        }
    }
}

@Composable
private fun OnboardingPageIndicator(
    currentStep: OnboardingStep,
    modifier: Modifier = Modifier
) {
    val steps = listOf(OnboardingStep.Welcome, OnboardingStep.ApiKey, OnboardingStep.CreateSession)
    val currentIndex = steps.indexOf(currentStep)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, _ ->
            val isActive = index <= currentIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(if (isActive) 24.dp else 8.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isActive) IrisPrimary else IrisSurfaceVariant)
            )
        }
    }
}
