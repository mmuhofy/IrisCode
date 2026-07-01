package com.iris.iriscode.terminal

import android.util.Log
import com.termux.terminal.TerminalSession
import java.io.File

class TerminalManager {

    var currentSession: TerminalSession? = null
        private set

    private var sessionClient: TerminalSessionClientImpl = TerminalSessionClientImpl()

    fun createSession(
        cwd: String = "/data/data/com.termux/files/home",
    ): TerminalSession {
        val shellPath = detectShell()
        val env = buildEnv()

        Log.i("TerminalManager", "Starting shell: $shellPath, cwd: $cwd")

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
            val termuxBash = "/data/data/com.termux/files/usr/bin/bash"
            if (File(termuxBash).exists()) {
                Log.i("TerminalManager", "Using Termux bash: $termuxBash")
                return termuxBash
            }
            val systemSh = "/system/bin/sh"
            Log.i("TerminalManager", "Termux not found, using system shell: $systemSh")
            return systemSh
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
