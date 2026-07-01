package com.iris.iriscode.terminal

import com.termux.terminal.TerminalSession

class TerminalManager(private val bootstrap: TermuxBootstrap) {

    var currentSession: TerminalSession? = null
        private set

    private var sessionClient: TerminalSessionClientImpl = TerminalSessionClientImpl()

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
