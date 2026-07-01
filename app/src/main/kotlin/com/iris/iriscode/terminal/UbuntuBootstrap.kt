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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

// Inspired by: github.com/termux/termux-app termux-bootstrap and proot-me/proot
// Uses Termux's Android-compatible PRoot (5.1.107.81) with libandroid-shmem + libtalloc
class UbuntuBootstrap(private val context: Context) {

    companion object {
        private const val UBUNTU_VERSION = "24.04.4"
        private const val TERMUX_PROOT_VERSION = "5.1.107.81"
        private const val LIBANDROID_SHMEM_VERSION = "0.7"
        private const val LIBTALLOC_VERSION = "2.4.3"
        private const val TERMUX_REPO = "https://packages.termux.dev/apt/termux-main"

        private val TERMUX_ARCH_MAP = mapOf(
            "arm64-v8a" to "aarch64",
            "armeabi-v7a" to "arm",
            "x86_64" to "x86_64",
            "x86" to "i686"
        )

        private val ROOTFS_ARCH_MAP = mapOf(
            "arm64-v8a" to "arm64",
            "armeabi-v7a" to "armhf",
            "x86_64" to "amd64",
            "x86" to "i386"
        )
    }

    private val baseDir: File get() = File(context.filesDir, "ubuntu")
    val prootFile: File get() = File(baseDir, "bin/proot")
    val prootLoaderDir: File get() = File(baseDir, "libexec/proot")
    val libDir: File get() = File(baseDir, "lib")
    val rootfsDir: File get() = File(baseDir, "rootfs")
    private val homeDir: File get() = File(baseDir, "home")
    private val tmpDir: File get() = File(baseDir, "tmp")

    val isInstalled: Boolean
        get() = prootFile.canExecute() && File(prootLoaderDir, "loader").canExecute() && File(rootfsDir, "bin/bash").canExecute()

    val prootPath: String get() = prootFile.absolutePath
    val prootLoaderPath: String get() = File(prootLoaderDir, "loader").absolutePath
    val libPath: String get() = libDir.absolutePath

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    suspend fun install(onState: (UbuntuSetupState) -> Unit) {
        onState(UbuntuSetupState.Checking)
        if (isInstalled) {
            onState(UbuntuSetupState.Ready)
            return
        }
        runInstall(onState)
    }

    private suspend fun runInstall(onState: (UbuntuSetupState) -> Unit) {
        val arch = Build.SUPPORTED_ABIS[0]
        val termuxArch = TERMUX_ARCH_MAP[arch]
            ?: return onState(UbuntuSetupState.Failed("Unsupported arch: $arch"))
        val rootfsArch = ROOTFS_ARCH_MAP[arch]
            ?: return onState(UbuntuSetupState.Failed("Unsupported arch: $arch"))

        try {
            withContext(Dispatchers.IO) {
                File(context.filesDir, "termux").deleteRecursively()

                baseDir.mkdirs()
                homeDir.mkdirs()
                tmpDir.mkdirs()

                // 1. Download Termux PRoot package
                onState(UbuntuSetupState.DownloadingProot(0f))
                val prootUrl = "$TERMUX_REPO/pool/main/p/proot/proot_${TERMUX_PROOT_VERSION}_${termuxArch}.deb"
                val prootDeb = File(tmpDir, "proot.deb")
                downloadFile(prootUrl, prootDeb) { progress ->
                    onState(UbuntuSetupState.DownloadingProot(progress))
                }
                extractDeb(prootDeb, baseDir, setOf("bin/proot", "libexec/proot/loader"))
                prootFile.setExecutable(true, false)
                File(prootLoaderDir, "loader").setExecutable(true, false)
                prootDeb.delete()

                // 2. Download libandroid-shmem
                onState(UbuntuSetupState.DownloadingLibandroidShmem(0f))
                val shmemUrl = "$TERMUX_REPO/pool/main/liba/libandroid-shmem/libandroid-shmem_${LIBANDROID_SHMEM_VERSION}_${termuxArch}.deb"
                val shmemDeb = File(tmpDir, "libandroid-shmem.deb")
                downloadFile(shmemUrl, shmemDeb) { progress ->
                    onState(UbuntuSetupState.DownloadingLibandroidShmem(progress))
                }
                extractDeb(shmemDeb, baseDir, setOf("lib/libandroid-shmem.so"))
                shmemDeb.delete()

                // 3. Download libtalloc
                onState(UbuntuSetupState.DownloadingLibtalloc(0f))
                val tallocUrl = "$TERMUX_REPO/pool/main/libt/libtalloc/libtalloc_${LIBTALLOC_VERSION}_${termuxArch}.deb"
                val tallocDeb = File(tmpDir, "talloc.deb")
                downloadFile(tallocUrl, tallocDeb) { progress ->
                    onState(UbuntuSetupState.DownloadingLibtalloc(progress))
                }
                extractDeb(tallocDeb, baseDir, setOf("lib/libtalloc.so.2", "lib/libtalloc.so.2.4.3"))
                tallocDeb.delete()

                // 4. Download Ubuntu rootfs
                onState(UbuntuSetupState.DownloadingRootfs(0f))
                val rootfsUrl = "https://cdimage.ubuntu.com/ubuntu-base/releases/$UBUNTU_VERSION/release/ubuntu-base-$UBUNTU_VERSION-base-$rootfsArch.tar.gz"
                val rootfsTmp = File(tmpDir, "ubuntu-base.tar.gz")
                downloadFile(rootfsUrl, rootfsTmp) { progress ->
                    onState(UbuntuSetupState.DownloadingRootfs(progress))
                }

                onState(UbuntuSetupState.Extracting)
                rootfsDir.mkdirs()
                extractTarGz(rootfsTmp, rootfsDir)
                rootfsTmp.delete()

                onState(UbuntuSetupState.Configuring)
                configureRootfs(rootfsArch)

                onState(UbuntuSetupState.Ready)
            }
        } catch (e: Exception) {
            Log.e("UbuntuBootstrap", "Setup failed", e)
            onState(UbuntuSetupState.Failed("${e::class.simpleName}: ${e.message ?: "Unknown error"}"))
        }
    }

    private fun configureRootfs(rootfsArch: String) {
        val mirror = if (rootfsArch == "amd64") "archive.ubuntu.com/ubuntu" else "ports.ubuntu.com/ubuntu-ports"

        File(rootfsDir, "etc/resolv.conf").writeText(
            "nameserver 8.8.8.8\nnameserver 8.8.4.4\n"
        )
        File(rootfsDir, "etc/hostname").writeText("iriscode-ubuntu\n")
        File(rootfsDir, "etc/hosts").writeText(
            "127.0.0.1 localhost iriscode-ubuntu\n::1 localhost ip6-localhost ip6-loopback\n"
        )
        File(rootfsDir, "etc/apt/sources.list").writeText(
            """
            deb http://$mirror noble main restricted universe multiverse
            deb http://$mirror noble-updates main restricted universe multiverse
            deb http://$mirror noble-security main restricted universe multiverse
            """.trimIndent() + "\n"
        )
        File(rootfsDir, "root").mkdirs()
        File(rootfsDir, "tmp").mkdirs()
        File(rootfsDir, "root/.bashrc").writeText(
            """
            export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
            export HOME=/root
            export TERM=xterm-256color
            export LANG=C.UTF-8
            export PROOT_TMP_DIR=/tmp
            export TMPDIR=/tmp
            export TEMP=/tmp
            PS1='\[\033[01;32m\]\u@iriscode\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ '
            alias ll='ls -la'
            alias la='ls -A'
            """.trimIndent() + "\n"
        )
        // bash --login sources .bash_profile, not .bashrc directly
        File(rootfsDir, "root/.bash_profile").writeText(
            """
            # Source .bashrc for interactive login shells
            if [ -f ~/.bashrc ]; then
                . ~/.bashrc
            fi
            """.trimIndent() + "\n"
        )
        
        // Ensure apt is available - install if missing
        installAptIfMissing()
    }

    fun retry() {
        baseDir.deleteRecursively()
    }

    // ─── File download ────────────────────────────────────────────────────────

    private fun downloadFile(url: String, dest: File, onProgress: (Float) -> Unit) {
        val request = Request.Builder().url(url).addHeader("User-Agent", "IrisCode/1.0").build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful)
            throw RuntimeException("Download failed: ${response.code} for $url")
        val body = response.body!!
        val total = body.contentLength()
        val input = body.byteStream()
        var read = 0L
        val buffer = ByteArray(8192)
        FileOutputStream(dest).use { out ->
            var count: Int
            while (input.read(buffer).also { count = it } != -1) {
                out.write(buffer, 0, count)
                read += count
                if (total > 0) onProgress(read.toFloat() / total)
            }
        }
    }

    // ─── DEB extraction (ar + tar.xz) ────────────────────────────────────────

    private fun extractDeb(debFile: File, destDir: File, targetPaths: Set<String>) {
        val arHeader = ByteArray(60)
        FileInputStream(debFile).use { fis ->
            // Skip ar magic
            fis.readNBytes(8)

            while (true) {
                val bytesRead = fis.read(arHeader)
                if (bytesRead < 60) break // EOF

                val name = arHeader.copyOfRange(0, 16)
                    .takeWhile { it != 0x20.toByte() && it != 0.toByte() }
                    .toByteArray()
                    .decodeToString()
                val sizeStr = arHeader.copyOfRange(48, 58)
                    .takeWhile { it != 0x20.toByte() && it != 0.toByte() }
                    .toByteArray()
                    .decodeToString()
                    .trim()
                val size = sizeStr.toLongOrNull() ?: 0L

                if (name == "data.tar.xz" || name == "data.tar.gz" || name == "data.tar") {
                    extractTarFromDeb(fis, destDir, targetPaths, size)
                } else {
                    fis.skip(size)
                }

                // Align to even boundary
                if (size % 2L != 0L) fis.skip(1)
            }
        }
    }

    private fun extractTarFromDeb(input: java.io.InputStream, destDir: File, targetPaths: Set<String>, dataSize: Long) {
        val headerBuf = ByteArray(512)
        val dataBuf = ByteArray(32768)
        var pendingLongName: String? = null
        var pendingLongLink: String? = null
        var remaining = dataSize

        while (remaining > 0) {
            val read = readFully(input, headerBuf, minOf(512, remaining).toInt())
            if (read < 512) break
            remaining -= 512
            if (headerBuf.all { it == 0.toByte() }) {
                // Skip remaining padding blocks
                break
            }

            val name = parseString(headerBuf, 0, 100)
            val size = parseOctal(headerBuf, 124, 12)
            val type = headerBuf[156].toInt().toChar()
            val linkName = parseString(headerBuf, 157, 100)

            if (type == 'L') {
                pendingLongName = readStringData(input, size, dataBuf)
                skipPadding(input, size)
                remaining -= (512 + size + (512 - size % 512) % 512)
                continue
            }
            if (type == 'K') {
                pendingLongLink = readStringData(input, size, dataBuf)
                skipPadding(input, size)
                remaining -= (512 + size + (512 - size % 512) % 512)
                continue
            }

            val finalName = pendingLongName ?: name
            pendingLongName = null
            val finalLink = pendingLongLink ?: linkName
            pendingLongLink = null

            if (type == '5') {
                // Directory - skip
                skipPadding(input, size)
                remaining -= (512 + size + (512 - size % 512) % 512)
            } else if (type == '2') {
                // Symlink - skip
                skipPadding(input, size)
                remaining -= (512 + size + (512 - size % 512) % 512)
            } else {
                // Regular file - check if we need it
                val needsExtract = targetPaths.contains(finalName)
                val entry = File(destDir, finalName)

                if (needsExtract) {
                    entry.parentFile?.mkdirs()
                    FileOutputStream(entry).use { out ->
                        var left = size
                        while (left > 0) {
                            val toRead = minOf(dataBuf.size.toLong(), left).toInt()
                            val read = readFully(input, dataBuf, toRead)
                            if (read < 0) throw RuntimeException("Unexpected EOF in $finalName")
                            out.write(dataBuf, 0, read)
                            left -= read
                        }
                    }
                    if (finalName.endsWith(".so") || finalName.contains("proot") || finalName.contains("loader")) {
                        entry.setExecutable(true, false)
                    }
                } else {
                    // Skip file data
                    var left = size
                    val skipBuf = ByteArray(8192)
                    while (left > 0) {
                        val toSkip = minOf(skipBuf.size.toLong(), left).toInt()
                        readFully(input, skipBuf, toSkip)
                        left -= toSkip
                    }
                }
                skipPadding(input, size)
                remaining -= (512 + size + (512 - size % 512) % 512)
            }
        }
    }

    // ─── Manual tar.gz extraction (no system binary dependency) ──────────────

    private fun extractTarGz(tarGzFile: File, destDir: File) {
        var pendingLongName: String? = null
        var pendingLongLink: String? = null

        val headerBuf = ByteArray(512)
        val dataBuf = ByteArray(32768)

        GZIPInputStream(FileInputStream(tarGzFile)).use { gz ->
            while (true) {
                if (readFully(gz, headerBuf) < 0) return
                if (headerBuf.all { it == 0.toByte() }) return

                val name = parseString(headerBuf, 0, 100)
                val size = parseOctal(headerBuf, 124, 12)
                val type = headerBuf[156].toInt().toChar()
                val linkName = parseString(headerBuf, 157, 100)

                if (type == 'L') {
                    pendingLongName = readStringData(gz, size, dataBuf)
                    skipPadding(gz, size)
                    continue
                }
                if (type == 'K') {
                    pendingLongLink = readStringData(gz, size, dataBuf)
                    skipPadding(gz, size)
                    continue
                }

                val finalName = pendingLongName ?: name
                pendingLongName = null
                val finalLink = pendingLongLink ?: linkName
                pendingLongLink = null

                if (type == '5') {
                    File(destDir, finalName).mkdirs()
                    skipPadding(gz, size)
                } else if (type == '2') {
                    val entry = File(destDir, finalName)
                    entry.parentFile?.mkdirs()
                    entry.delete()
                    Os.symlink(finalLink, entry.absolutePath)
                    skipPadding(gz, size)
                } else {
                    val entry = File(destDir, finalName)
                    entry.parentFile?.mkdirs()
                    var remaining = size
                    FileOutputStream(entry).use { out ->
                        while (remaining > 0) {
                            val toRead = minOf(dataBuf.size.toLong(), remaining).toInt()
                            val read = readFully(gz, dataBuf, toRead)
                            if (read < 0) throw RuntimeException("Unexpected EOF in $finalName")
                            out.write(dataBuf, 0, read)
                            remaining -= read
                        }
                    }
                    skipPadding(gz, size)

                    val mode = parseOctal(headerBuf, 100, 8).toInt()
                    if (mode and 64 != 0) entry.setExecutable(true, false)
                }
            }
        }
    }

    private fun readFully(input: java.io.InputStream, buf: ByteArray, length: Int = buf.size): Int {
        var offset = 0
        while (offset < length) {
            val read = input.read(buf, offset, length - offset)
            if (read < 0) return if (offset == 0) -1 else offset
            offset += read
        }
        return offset
    }

    private fun skipPadding(input: java.io.InputStream, dataSize: Long) {
        val padding = (512 - (dataSize % 512)) % 512
        var skipped = 0L
        while (skipped < padding) {
            val toSkip = minOf(padding - skipped, 4096L)
            val n = input.skip(toSkip)
            if (n <= 0) {
                val buf = ByteArray(minOf(padding - skipped, 4096L).toInt())
                readFully(input, buf, buf.size)
                skipped += buf.size
            } else {
                skipped += n
            }
        }
    }

    private fun readStringData(input: java.io.InputStream, size: Long, buf: ByteArray): String {
        val toRead = minOf(buf.size.toLong(), size).toInt()
        readFully(input, buf, toRead)
        if (size > buf.size) {
            var remaining = size - buf.size
            val discardBuf = ByteArray(4096)
            while (remaining > 0) {
                val chunk = minOf(discardBuf.size.toLong(), remaining).toInt()
                readFully(input, discardBuf, chunk)
                remaining -= chunk
            }
        }
        val end = buf.indexOfFirst { it == 0.toByte() }.let { if (it < 0) toRead else it }
        return buf.copyOfRange(0, end).decodeToString()
    }

    private fun parseOctal(data: ByteArray, offset: Int, length: Int): Long {
        val end = minOf(offset + length, data.size)
        var i = offset
        while (i < end && (data[i] == 0x20.toByte() || data[i] == 0x30.toByte() || data[i] == 0.toByte())) i++
        if (i >= end) return 0
        var j = i
        while (j < end && data[j] != 0x20.toByte() && data[j] != 0.toByte()) j++
        if (i == j) return 0
        val str = data.copyOfRange(i, j).decodeToString()
        return str.toLong(8)
    }

    private fun parseString(data: ByteArray, offset: Int, length: Int): String {
        val end = minOf(offset + length, data.size)
        return data.copyOfRange(offset, end)
            .takeWhile { it != 0.toByte() }
            .toByteArray()
            .decodeToString()
    }

    // ─── Ensure apt is available ───────────────────────────────────────────────

    private fun installAptIfMissing() {
        val aptBinary = File(rootfsDir, "usr/bin/apt")
        if (!aptBinary.exists()) {
            try {
                // Run apt update && apt install -y apt inside PRoot
                val prootCmd = listOf(
                    prootFile.absolutePath,
                    "--link2symlink",
                    "-r", rootfsDir.absolutePath,
                    "-b", "/system:/system",
                    "-b", "/vendor:/vendor",
                    "-b", "/apex:/apex",
                    "-b", "/dev:/dev",
                    "-b", "/proc:/proc",
                    "-b", "/sys:/sys",
                    "-b", "/storage/self/primary:/sdcard",
                    "-w", "/root",
                    "/bin/bash", "-c", "apt update && apt install -y apt"
                )
                val process = ProcessBuilder(prootCmd).redirectErrorStream(true).start()
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    val output = process.inputStream.readBytes().decodeToString()
                    Log.w("UbuntuBootstrap", "apt install failed (exit $exitCode): $output")
                }
            } catch (e: Exception) {
                Log.w("UbuntuBootstrap", "Failed to install apt: $e")
            }
        }
    }
}