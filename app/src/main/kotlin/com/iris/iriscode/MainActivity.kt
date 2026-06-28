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
import androidx.hilt.navigation.compose.hiltViewModel
import com.iris.iriscode.ui.onboarding.ApiKeyScreen
import com.iris.iriscode.ui.onboarding.OnboardingStep
import com.iris.iriscode.ui.onboarding.OnboardingViewModel
import com.iris.iriscode.ui.onboarding.ProjectSetupScreen
import com.iris.iriscode.ui.onboarding.WelcomeScreen
import com.iris.iriscode.ui.theme.IrisCodeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            IrisCodeTheme {
                val viewModel: OnboardingViewModel = hiltViewModel()
                val state by viewModel.state.collectAsState()

                AnimatedContent(
                    targetState = state.currentStep,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "onboarding"
                ) { step ->
                    when (step) {
                        OnboardingStep.Welcome -> {
                            WelcomeScreen(onNext = viewModel::nextStep)
                        }
                        OnboardingStep.ApiKey -> {
                            ApiKeyScreen(
                                apiKey = state.apiKey,
                                error = state.apiKeyError,
                                isValidating = state.isValidating,
                                onApiKeyChange = viewModel::updateApiKey,
                                onNext = viewModel::nextStep,
                                onSkip = viewModel::skipApiKey
                            )
                        }
                        OnboardingStep.ProjectSetup -> {
                            ProjectSetupScreen(
                                projectPath = state.projectPath,
                                onProjectPathSelected = viewModel::setProjectPath,
                                onNext = viewModel::nextStep
                            )
                        }
                    }
                }
            }
        }
    }
}
