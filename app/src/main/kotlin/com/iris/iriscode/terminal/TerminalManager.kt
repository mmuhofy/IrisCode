package com.iris.iriscode.terminal

import android.app.Application
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView

class TerminalManager(
    private val ubuntuBootstrap: UbuntuBootstrap,
    application: Application
) {
    var currentSession: TerminalSession? = null
        private set

    val sessionClient: TerminalSessionClientImpl = TerminalSessionClientImpl()

    private var terminalViewRef: TerminalView? = null

    private val prootRunner: ProotRunner by lazy {
        ProotRunner(ubuntuBootstrap, application.applicationInfo.nativeLibraryDir)
    }

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

        val session = TerminalSession(
            "/system/bin/sh",
            "/",
            arrayOf("sh"),
            arrayOf("PATH=/system/bin:/system/xbin", "HOME=/", "TERM=vt100"),
            3000,
            sessionClient
        )
        currentSession = session
        return session
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
