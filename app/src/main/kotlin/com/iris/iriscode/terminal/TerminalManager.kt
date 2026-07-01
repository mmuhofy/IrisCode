package com.iris.iriscode.terminal

import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView

class TerminalManager(private val bootstrap: TermuxBootstrap) {

    var currentSession: TerminalSession? = null
        private set

    val sessionClient: TerminalSessionClientImpl = TerminalSessionClientImpl()

    private var terminalViewRef: TerminalView? = null

    fun registerTerminalView(view: TerminalView) {
        terminalViewRef = view
        sessionClient.onTextChanged = { session ->
            view.onScreenUpdated()
        }
    }

    fun unregisterTerminalView() {
        sessionClient.onTextChanged = null
        terminalViewRef = null
    }

    fun createSession(): TerminalSession {
        val shellPath = bootstrap.shellPath
        val cwd = bootstrap.defaultCwd
        val env = bootstrap.buildEnv()

        val session = TerminalSession(
            shellPath,
            cwd,
            arrayOf(shellPath),
            env,
            3000,
            sessionClient
        )
        currentSession = session
        return session
    }

    fun updateSessionClient() {
        currentSession?.updateTerminalSessionClient(sessionClient)
    }

    fun onSessionFinished(finishedSession: TerminalSession) {
        if (currentSession == finishedSession) {
            currentSession = null
        }
    }

    fun destroy() {
        currentSession?.finishIfRunning()
        currentSession = null
    }
}
