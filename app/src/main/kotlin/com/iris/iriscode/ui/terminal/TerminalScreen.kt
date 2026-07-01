package com.iris.iriscode.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.iris.iriscode.terminal.BootstrapState
import com.iris.iriscode.terminal.TerminalManager
import com.iris.iriscode.terminal.TerminalViewClientImpl
import com.termux.view.TerminalView

@Composable
fun TerminalScreen(
    modifier: Modifier = Modifier,
    terminalManager: TerminalManager,
    bootstrapState: BootstrapState,
    onRetry: () -> Unit = {}
) {
    val viewClient = TerminalViewClientImpl()

    when (bootstrapState) {
        is BootstrapState.Checking, is BootstrapState.Downloading, is BootstrapState.Extracting -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                val msg = when (bootstrapState) {
                    is BootstrapState.Checking -> "Checking Termux environment..."
                    is BootstrapState.Downloading -> "Downloading Termux environment (${(bootstrapState.progress * 100).toInt()}%)..."
                    is BootstrapState.Extracting -> "Extracting Termux environment..."
                    else -> ""
                }
                CircularProgressIndicator()
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        is BootstrapState.Completed, is BootstrapState.AlreadyInstalled -> {
            LaunchedEffect(Unit) {
                if (terminalManager.currentSession == null) {
                    terminalManager.createSession()
                }
            }

            AndroidView(
                modifier = modifier,
                factory = { ctx ->
                    TerminalView(ctx, null).apply {
                        setTextSize(12)
                        setTerminalViewClient(viewClient)
                        terminalManager.currentSession?.let { session ->
                            attachSession(session)
                        }
                    }
                },
                update = { view ->
                    terminalManager.currentSession?.let { session ->
                        if (view.mTermSession != session) {
                            view.attachSession(session)
                        }
                    }
                }
            )
        }

        is BootstrapState.Failed -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Failed to set up terminal",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = bootstrapState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
