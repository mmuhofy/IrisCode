package com.iris.iriscode.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.iris.iriscode.terminal.TerminalManager
import com.iris.iriscode.terminal.TerminalViewClientImpl
import com.termux.view.TerminalView

@Composable
fun TerminalScreen(
    modifier: Modifier = Modifier,
    terminalManager: TerminalManager = remember { TerminalManager() }
) {
    val context = LocalContext.current
    val viewClient = remember { TerminalViewClientImpl() }

    DisposableEffect(Unit) {
        val session = terminalManager.createSession()
        onDispose {
            terminalManager.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TerminalView(ctx, null).apply {
                setTerminalViewClient(viewClient)
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
