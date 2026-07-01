package com.iris.iriscode.terminal

import java.io.File

class ProotRunner(
    private val bootstrap: UbuntuBootstrap
) {
    val prootPath: String get() = bootstrap.prootFile.absolutePath
    val rootfsPath: String get() = bootstrap.rootfsDir.absolutePath
    private val homePath: String get() = File(bootstrap.rootfsDir.parentFile!!, "home").absolutePath
    private val tmpPath: String get() = File(bootstrap.rootfsDir.parentFile!!, "tmp").absolutePath

    data class ProotCommand(
        val executable: String,
        val argv: List<String>,
        val cwd: String,
        val environment: List<String>
    )

    fun build(): ProotCommand {
        File(tmpPath).mkdirs()
        File(homePath).mkdirs()

        // Ensure guest paths exist in rootfs for bind mounts
        File(rootfsPath, "root").mkdirs()
        File(rootfsPath, "tmp").mkdirs()

        val binds = mutableListOf<String>().apply {
            add("-b"); add("/system:/system")
            add("-b"); add("/vendor:/vendor")
            add("-b"); add("/apex:/apex")
            add("-b"); add("/dev:/dev")
            add("-b"); add("/proc:/proc")
            add("-b"); add("/sys:/sys")
            add("-b"); add("/storage/self/primary:/sdcard")
            add("-b"); add("$homePath:/root")
            add("-b"); add("$tmpPath:/tmp")
        }

        val argv = mutableListOf<String>().apply {
            add(prootPath)
            add("--link2symlink")
            add("-r"); add(rootfsPath)
            addAll(binds)
            add("-w"); add("/root")
            add("/bin/bash")
            add("--login")
        }

        val env = mutableListOf<String>().apply {
            add("PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")
            add("HOME=/root")
            add("TERM=xterm-256color")
            add("LANG=C.UTF-8")
            add("SHELL=/bin/bash")
            add("TMPDIR=/tmp")
            add("PROOT_TMP_DIR=/tmp")
            try {
                val systemEnv = System.getenv()
                for ((key, value) in systemEnv) {
                    when (key) {
                        "PATH", "HOME", "TERM", "LANG", "SHELL", "TMPDIR",
                        "LD_LIBRARY_PATH", "LD_PRELOAD", "PREFIX" -> {}
                        else -> add("$key=$value")
                    }
                }
            } catch (_: Exception) {}
        }

        return ProotCommand(
            executable = prootPath,
            argv = argv,
            cwd = "/root",
            environment = env
        )
    }

    fun buildBashCommand(command: String): List<String> {
        val base = build()
        return base.argv.dropLast(2) + listOf("/bin/bash", "-c", command)
    }
}