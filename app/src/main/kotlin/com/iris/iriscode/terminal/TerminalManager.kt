package com.iris.iriscode.terminal

import com.termux.terminal.TerminalSession
import java.util.UUID

class TerminalManager {

    var currentSession: TerminalSession? = null
        private set

    private var sessionClient: TerminalSessionClientImpl = TerminalSessionClientImpl()

    fun createSession(
        shellPath: String = "/data/data/com.termux/files/usr/bin/bash",
        cwd: String = "/data/data/com.termux/files/home",
        env: Array<String> = defaultEnv(),
    ): TerminalSession {
        val session = TerminalSession(
            shellPath,
            cwd,
            arrayOf(shellPath, "-l"),
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
        fun defaultEnv(): Array<String> {
            return arrayOf(
                "TERM=xterm-256color",
                "HOME=/data/data/com.termux/files/home",
                "SHELL=/data/data/com.termux/files/usr/bin/bash",
                "USER=" + System.getProperty("user.name", "u0_a"),
                "PATH=/data/data/com.termux/files/usr/bin:/data/data/com.termux/files/usr/bin/applets:/system/bin:/system/xbin",
                "LANG=en_US.UTF-8",
                "TMPDIR=/data/data/com.termux/files/usr/tmp",
                "PREFIX=/data/data/com.termux/files/usr",
                "LD_PRELOAD=",
            )
        }
    }
}
