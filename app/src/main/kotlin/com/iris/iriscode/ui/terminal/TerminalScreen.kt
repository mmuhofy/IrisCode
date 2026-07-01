package com.iris.iriscode.ui.terminal

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.iris.iriscode.terminal.TerminalManager
import com.iris.iriscode.terminal.TerminalViewClientImpl
import com.termux.view.TerminalView

@Composable
fun TerminalScreen(
    modifier: Modifier = Modifier,
    terminalManager: TerminalManager = remember { TerminalManager() }
) {
    val viewClient = remember { TerminalViewClientImpl() }

    DisposableEffect(Unit) {
        val session = terminalManager.createSession()
        onDispose {
            terminalManager.destroy()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            TerminalView(context, null).apply {
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
