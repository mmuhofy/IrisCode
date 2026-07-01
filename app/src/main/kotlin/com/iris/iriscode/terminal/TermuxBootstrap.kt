package com.iris.iriscode.terminal

import android.content.Context
import android.os.Build
import android.system.Os
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

sealed class BootstrapState {
    data object Checking : BootstrapState()
    data object AlreadyInstalled : BootstrapState()
    data class Downloading(val progress: Float) : BootstrapState()
    data object Extracting : BootstrapState()
    data object Completed : BootstrapState()
    data class Failed(val message: String) : BootstrapState()
}

class TermuxBootstrap(private val context: Context) {

    private val prefixDir: File
        get() = File(context.filesDir, "termux/usr")

    private val homeDir: File
        get() = File(context.filesDir, "termux/home")

    val isInstalled: Boolean
        get() = File(prefixDir, "bin/bash").canExecute()

    val shellPath: String
        get() = File(prefixDir, "bin/bash").absolutePath

    val defaultCwd: String
        get() = homeDir.absolutePath

    private val archMap = mapOf(
        "arm64-v8a" to "aarch64",
        "armeabi-v7a" to "arm",
        "x86" to "i686",
        "x86_64" to "x86_64"
    )

    private val client = OkHttpClient()

    suspend fun install(onState: (BootstrapState) -> Unit) {
        onState(BootstrapState.Checking)
        runInstall(onState)
    }

    fun retry() {
        File(context.filesDir, "termux/staging").deleteRecursively()
        prefixDir.deleteRecursively()
    }

    private suspend fun runInstall(onState: (BootstrapState) -> Unit) {
        if (isInstalled) {
            onState(BootstrapState.AlreadyInstalled)
            return
        }

        try {
            withContext(Dispatchers.IO) {
                val arch = archMap[Build.SUPPORTED_ABIS[0]]
                    ?: throw RuntimeException("Unsupported architecture: ${Build.SUPPORTED_ABIS[0]}")

                val releaseUrl = "https://api.github.com/repos/termux/termux-packages/releases?per_page=1"
                val releaseRequest = Request.Builder()
                    .url(releaseUrl)
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", "IrisCode/1.0")
                    .build()
                val releaseResponse = client.newCall(releaseRequest).execute()
                if (!releaseResponse.isSuccessful) throw RuntimeException("Failed to fetch latest release")
                val releaseBody = releaseResponse.body!!.string()

                val tagName = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").find(releaseBody)?.groupValues?.get(1)
                    ?: throw RuntimeException("Could not parse latest release tag")
                val zipName = "bootstrap-$arch.zip"
                val downloadUrl = "https://github.com/termux/termux-packages/releases/download/$tagName/$zipName"

                val homeRequest = Request.Builder().url(downloadUrl).build()
                val response = client.newCall(homeRequest).execute()
                if (!response.isSuccessful) throw RuntimeException("Failed to download bootstrap: ${response.code}")
                val body = response.body ?: throw RuntimeException("Empty response body")
                val totalBytes = body.contentLength()
                val inputStream = body.byteStream()

                onState(BootstrapState.Downloading(0f))
                val zipBytes = inputStream.readBytes().also {
                    onState(BootstrapState.Downloading(1f))
                }

                onState(BootstrapState.Extracting)
                prefixDir.parentFile?.mkdirs()
                homeDir.mkdirs()

                val stagingDir = File(context.filesDir, "termux/staging")
                stagingDir.mkdirs()

                val symlinks = mutableListOf<Pair<String, String>>()
                val buffer = ByteArray(8192)

                ZipInputStream(zipBytes.inputStream()).use { zipInput ->
                    var entry = zipInput.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (name == "SYMLINKS.txt") {
                            val lines = zipInput.readBytes().decodeToString()
                            for (line in lines.lines()) {
                                if (line.isBlank()) continue
                                val parts = line.split("\u2190")
                                if (parts.size == 2) {
                                    symlinks.add(parts[0].trim() to "${stagingDir.absolutePath}/${parts[1].trim()}")
                                }
                            }
                        } else {
                            val target = File(stagingDir, name)
                            if (entry.isDirectory) {
                                target.mkdirs()
                            } else {
                                target.parentFile?.mkdirs()
                                FileOutputStream(target).use { out ->
                                    var read: Int
                                    while (zipInput.read(buffer).also { read = it } != -1) {
                                        out.write(buffer, 0, read)
                                    }
                                }
                                if (name.startsWith("bin/") || name.startsWith("libexec") ||
                                    name.startsWith("lib/apt/apt-helper") || name.startsWith("lib/apt/methods")
                                ) {
                                    target.setExecutable(true, false)
                                }
                            }
                        }
                        zipInput.closeEntry()
                        entry = zipInput.nextEntry
                    }
                }

                for ((oldPath, newPath) in symlinks) {
                    File(newPath).parentFile?.mkdirs()
                    Os.symlink(oldPath, newPath)
                }

                if (!stagingDir.renameTo(prefixDir)) {
                    stagingDir.copyRecursively(prefixDir, overwrite = true)
                    stagingDir.deleteRecursively()
                }

                prefixDir.walkTopDown().forEach { file ->
                    if (file.isFile && (file.name == "bash" || file.parent?.endsWith("/bin") == true ||
                        file.parent?.endsWith("/libexec") == true)
                    ) {
                        file.setExecutable(true, false)
                    }
                }

                onState(BootstrapState.Completed)
            }
        } catch (e: Exception) {
            Log.e("TermuxBootstrap", "Bootstrap failed", e)
            onState(BootstrapState.Failed("${e::class.simpleName}: ${e.message ?: "Unknown error"}"))
        }
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
        env.add("HOME=${homeDir.absolutePath}")
        env.add("PREFIX=${prefixDir.absolutePath}")
        env.add("TMPDIR=${File(context.filesDir, "termux/tmp").absolutePath}")
        env.add("SHELL=$shellPath")
        env.add("LD_PRELOAD=")
        return env.toTypedArray()
    }
}
