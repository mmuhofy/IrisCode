package com.iris.iriscode.terminal

import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView

class TerminalManager(
    private val termuxBootstrap: TermuxBootstrap,
    private val ubuntuBootstrap: UbuntuBootstrap
) {
    var currentSession: TerminalSession? = null
        private set

    val sessionClient: TerminalSessionClientImpl = TerminalSessionClientImpl()

    private var terminalViewRef: TerminalView? = null

    private val prootRunner: ProotRunner by lazy { ProotRunner(ubuntuBootstrap) }

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
        if (ubuntuBootstrap.isInstalled) {
            val cmd = prootRunner.build()
            val session = TerminalSession(
                cmd.executable,
                cmd.cwd,
                cmd.argv.toTypedArray(),
                cmd.environment.toTypedArray(),
                3000,
                sessionClient
            )
            currentSession = session
            return session
        }

        val shellPath = termuxBootstrap.shellPath
        val cwd = termuxBootstrap.defaultCwd
        val env = termuxBootstrap.buildEnv()

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
