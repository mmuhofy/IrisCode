package com.iris.iriscode.terminal

import android.content.Context
import android.system.Os
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

// Inspired by: github.com/Xed-Editor/Karbon-PackagesX (proot binary for Android NDK)
// and github.com/termux/termux-app (proot-loader)
class UbuntuBootstrap(private val context: Context) {

    private val baseDir: File get() = File(context.filesDir, "ubuntu")
    val prootFile: File get() = File(baseDir, "bin/proot")
    val libDir: File get() = File(baseDir, "lib")
    val rootfsDir: File get() = File(baseDir, "rootfs")
    private val tmpDir: File get() = File(baseDir, "tmp")

    val isInstalled: Boolean
        get() = prootFile.canExecute()
            && File(libDir, "libtalloc.so.2").canRead()
            && File(rootfsDir, "bin/bash").canExecute()
            && File(rootfsDir, "etc/apt/sources.list").exists()

    suspend fun install(onState: (UbuntuSetupState) -> Unit) {
        if (isInstalled) {
            onState(UbuntuSetupState.Ready)
            return
        }
        runInstall(onState)
    }

    private suspend fun runInstall(onState: (UbuntuSetupState) -> Unit) {
        try {
            withContext(Dispatchers.IO) {
                baseDir.mkdirs()
                File(baseDir, "bin").mkdirs()
                libDir.mkdirs()
                tmpDir.mkdirs()

                onState(UbuntuSetupState.Extracting)

                // proot binary
                context.assets.open("proot").use { input ->
                    prootFile.parentFile!!.mkdirs()
                    FileOutputStream(prootFile).use { out ->
                        input.copyTo(out)
                    }
                }
                prootFile.setExecutable(true, false)

                // libtalloc.so.2
                context.assets.open("libtalloc.so.2").use { input ->
                    libDir.mkdirs()
                    FileOutputStream(File(libDir, "libtalloc.so.2")).use { out ->
                        input.copyTo(out)
                    }
                }

                // Ubuntu rootfs
                rootfsDir.mkdirs()
                context.assets.open("ubuntu-base.tar.gz").use { input ->
                    extractTarGz(GZIPInputStream(input), rootfsDir)
                }

                onState(UbuntuSetupState.Configuring)
                configureRootfs()

                onState(UbuntuSetupState.Ready)
            }
        } catch (e: Exception) {
            Log.e("UbuntuBootstrap", "Setup failed", e)
            onState(UbuntuSetupState.Failed("${e::class.simpleName}: ${e.message ?: "Unknown error"}"))
        }
    }

    private fun configureRootfs() {
        val mirror = "ports.ubuntu.com/ubuntu-ports"

        File(rootfsDir, "etc/resolv.conf").writeText(
            "nameserver 8.8.8.8\nnameserver 8.8.4.4\n"
        )
        File(rootfsDir, "etc/hostname").writeText("iriscode-ubuntu\n")
        File(rootfsDir, "etc/hosts").writeText(
            "127.0.0.1 localhost iriscode-ubuntu\n::1 localhost ip6-localhost ip6-loopback\n"
        )
        File(rootfsDir, "etc/apt/sources.list").writeText(
            """
            deb http://ports.ubuntu.com/ubuntu-ports noble main restricted universe multiverse
            deb http://ports.ubuntu.com/ubuntu-ports noble-updates main restricted universe multiverse
            deb http://ports.ubuntu.com/ubuntu-ports noble-security main restricted universe multiverse
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
            export TMPDIR=/tmp
            PS1='\[\033[01;32m\]\u@iriscode\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ '
            alias ll='ls -la'
            alias la='ls -A'
            """.trimIndent() + "\n"
        )
        File(rootfsDir, "root/.bash_profile").writeText(
            """
            if [ -f ~/.bashrc ]; then
                . ~/.bashrc
            fi
            """.trimIndent() + "\n"
        )
    }

    fun retry() {
        baseDir.deleteRecursively()
    }

    // ─── tar.gz extraction (no system binary dependency) ─────────────────────

    private fun extractTarGz(gz: GZIPInputStream, destDir: File) {
        var pendingLongName: String? = null
        var pendingLongLink: String? = null

        val headerBuf = ByteArray(512)
        val dataBuf = ByteArray(32768)

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
}
