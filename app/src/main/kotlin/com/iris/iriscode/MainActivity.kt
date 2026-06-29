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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.Lucide
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
import com.iris.iriscode.ui.theme.IrisPrimary
import com.iris.iriscode.ui.theme.IrisSurfaceVariant
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector

private sealed class Screen {
    data object Onboarding : Screen()
    data object Projects : Screen()
    data class Chat(val projectName: String, val projectId: Long) : Screen()
    data object Settings : Screen()
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
                        Column(modifier = Modifier.fillMaxWidth()) {
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
                            onBack = { currentScreen = Screen.Projects },
                            onSettings = { currentScreen = Screen.Settings }
                        )
                    }

                    is Screen.Settings -> {
                        PlaceholderScreen(
                            title = "Settings",
                            icon = Lucide.Settings,
                            onBack = { currentScreen = Screen.Projects }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    icon: ImageVector,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.iris.iriscode.ui.theme.IrisBackground)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Lucide.ArrowLeft,
                    contentDescription = "Back",
                    tint = com.iris.iriscode.ui.theme.IrisText,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = com.iris.iriscode.ui.theme.IrisTextSecondary
            )
        }
    }
}

@Composable
private fun OnboardingPageIndicator(
    currentStep: OnboardingStep,
    modifier: Modifier = Modifier
) {
    val steps = listOf(OnboardingStep.Welcome, OnboardingStep.ApiKey, OnboardingStep.ProjectSetup)
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
