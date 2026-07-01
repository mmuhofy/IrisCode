package com.iris.iriscode.ui.terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.iris.iriscode.terminal.TerminalManager
import com.iris.iriscode.terminal.TerminalViewClientImpl
import com.termux.view.TerminalView

@Composable
fun TerminalScreen(
    modifier: Modifier = Modifier,
    terminalManager: TerminalManager
) {
    val viewClient = TerminalViewClientImpl()

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
