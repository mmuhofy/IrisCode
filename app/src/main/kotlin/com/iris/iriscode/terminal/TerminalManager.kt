package com.iris.iriscode.terminal

import com.termux.terminal.TerminalSession

class TerminalManager {

    var currentSession: TerminalSession? = null
        private set

    private var sessionClient: TerminalSessionClientImpl = TerminalSessionClientImpl()

    fun createSession(
        cwd: String = "/data/data/com.termux/files/home",
    ): TerminalSession {
        val shellPath = detectShell()
        val env = buildEnv()

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

    companion object {
        fun detectShell(): String {
            return "/data/data/com.termux/files/usr/bin/bash"
        }

        fun buildEnv(): Array<String> {
            val env = mutableListOf<String>()
            try {
                val systemEnv = System.getenv()
                for ((key, value) in systemEnv) {
                    env.add("$key=$value")
                }
            } catch (_: Exception) {}
            env.add("TERM=xterm-256color")
            return env.toTypedArray()
        }
    }
}
