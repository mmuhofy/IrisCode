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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.iris.iriscode.terminal.TerminalManager
import com.iris.iriscode.terminal.TerminalViewClientImpl
import com.iris.iriscode.terminal.UbuntuSetupState
import com.termux.view.TerminalView
import kotlin.math.roundToInt

@Composable
fun TerminalScreen(
    modifier: Modifier = Modifier,
    terminalManager: TerminalManager,
    ubuntuSetupState: UbuntuSetupState,
    onRetry: () -> Unit = {}
) {
    val fontSize = remember { mutableIntStateOf(12) }
    val terminalViewRef = remember { mutableStateOf<TerminalView?>(null) }

    val viewClient = remember {
        TerminalViewClientImpl { scale ->
            val view = terminalViewRef.value ?: return@TerminalViewClientImpl 1.0f
            val base = fontSize.intValue
            val newSize = (base * scale).roundToInt().coerceIn(6, 30)
            if (newSize != base) {
                fontSize.intValue = newSize
                view.setTextSize(newSize)
            }
            newSize.toFloat() / base
        }
    }

    when (ubuntuSetupState) {
        is UbuntuSetupState.Checking, 
        is UbuntuSetupState.DownloadingProot, 
        is UbuntuSetupState.DownloadingRootfs, 
        is UbuntuSetupState.Extracting, 
        is UbuntuSetupState.Configuring -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                val msg = when (ubuntuSetupState) {
                    is UbuntuSetupState.Checking -> "Checking Ubuntu environment..."
                    is UbuntuSetupState.DownloadingProot -> 
                        "Downloading PRoot (${(ubuntuSetupState.progress * 100).toInt()}%)..."
                    is UbuntuSetupState.DownloadingRootfs -> 
                        "Downloading Ubuntu rootfs (${(ubuntuSetupState.progress * 100).toInt()}%)..."
                    is UbuntuSetupState.Extracting -> "Extracting Ubuntu rootfs..."
                    is UbuntuSetupState.Configuring -> "Configuring Ubuntu rootfs..."
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

        is UbuntuSetupState.Ready -> {
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                if (terminalManager.currentSession == null) {
                    terminalManager.createSession()
                }
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            AndroidView(
                modifier = modifier.focusRequester(focusRequester),
                factory = { ctx ->
                    TerminalView(ctx, null).apply {
                        setTextSize(12)
                        setTerminalViewClient(viewClient)
                        isFocusable = true
                        isFocusableInTouchMode = true
                        terminalManager.currentSession?.let { session ->
                            attachSession(session)
                        }
                        terminalManager.registerTerminalView(this)
                        terminalViewRef.value = this
                    }
                },
                update = { view ->
                    terminalManager.currentSession?.let { session ->
                        if (view.mTermSession != session) {
                            view.attachSession(session)
                        }
                    }
                    terminalManager.registerTerminalView(view)
                    terminalViewRef.value = view
                    view.requestFocus()
                }
            )
        }

        is UbuntuSetupState.Failed -> {
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
                        text = ubuntuSetupState.error,
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

        is UbuntuSetupState.Idle -> { }
    }
}
