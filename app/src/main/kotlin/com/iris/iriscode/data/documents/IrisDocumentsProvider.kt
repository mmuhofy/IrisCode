package com.iris.iriscode.data.documents

import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import com.iris.iriscode.R
import java.io.File
import java.io.FileNotFoundException
import java.util.LinkedList

class IrisDocumentsProvider : DocumentsProvider() {

    companion object {
        private const val ALL_MIME_TYPES = "*/*"
    }

    private val baseDir: File
        get() = context!!.filesDir

    private val DEFAULT_ROOT_PROJECTION = arrayOf(
        Root.COLUMN_ROOT_ID,
        Root.COLUMN_MIME_TYPES,
        Root.COLUMN_FLAGS,
        Root.COLUMN_ICON,
        Root.COLUMN_TITLE,
        Root.COLUMN_SUMMARY,
        Root.COLUMN_DOCUMENT_ID,
        Root.COLUMN_AVAILABLE_BYTES,
    )

    private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
        Document.COLUMN_DOCUMENT_ID,
        Document.COLUMN_MIME_TYPE,
        Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_LAST_MODIFIED,
        Document.COLUMN_FLAGS,
        Document.COLUMN_SIZE,
    )

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
        val row = result.newRow()
        row.add(Root.COLUMN_ROOT_ID, getDocIdForFile(baseDir))
        row.add(Root.COLUMN_DOCUMENT_ID, getDocIdForFile(baseDir))
        row.add(Root.COLUMN_SUMMARY, null)
        row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE or Root.FLAG_SUPPORTS_SEARCH or Root.FLAG_SUPPORTS_IS_CHILD)
        row.add(Root.COLUMN_TITLE, "IRIS Files")
        row.add(Root.COLUMN_MIME_TYPES, ALL_MIME_TYPES)
        row.add(Root.COLUMN_AVAILABLE_BYTES, baseDir.freeSpace)
        row.add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
        return result
    }

    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        includeFile(result, documentId, null)
        return result
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<String>?,
        sortOrder: String?,
    ): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent = getFileForDocId(parentDocumentId)
        val files = parent.listFiles()
        if (files != null) {
            for (file in files) {
                includeFile(result, null, file)
            }
        }
        return result
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor {
        val file = getFileForDocId(documentId)
        val accessMode = ParcelFileDescriptor.parseMode(mode)
        return ParcelFileDescriptor.open(file, accessMode)
    }

    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point?,
        signal: CancellationSignal?,
    ): AssetFileDescriptor {
        val file = getFileForDocId(documentId)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, file.length())
    }

    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String,
    ): String {
        val parent = getFileForDocId(parentDocumentId)
        var newFile = File(parent, displayName)
        var noConflictId = 2
        while (newFile.exists()) {
            newFile = File(parent, "$displayName ($noConflictId)")
            noConflictId++
        }
        val succeeded = if (Document.MIME_TYPE_DIR == mimeType) {
            newFile.mkdir()
        } else {
            newFile.createNewFile()
        }
        if (!succeeded) throw FileNotFoundException("Failed to create $newFile")
        return newFile.absolutePath
    }

    override fun deleteDocument(documentId: String) {
        val file = getFileForDocId(documentId)
        if (!file.delete()) throw FileNotFoundException("Failed to delete $documentId")
    }

    override fun getDocumentType(documentId: String): String {
        val file = getFileForDocId(documentId)
        return getMimeType(file)
    }

    override fun querySearchDocuments(
        rootId: String,
        query: String,
        projection: Array<String>?,
    ): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent = getFileForDocId(rootId)
        val pending = LinkedList<File>()
        pending.add(parent)
        val MAX_SEARCH_RESULTS = 50
        while (pending.isNotEmpty() && result.count < MAX_SEARCH_RESULTS) {
            val file = pending.removeFirst()
            val isInside = try {
                file.canonicalPath.startsWith(baseDir.absolutePath)
            } catch (_: Exception) {
                true
            }
            if (isInside) {
                if (file.isDirectory) {
                    file.listFiles()?.let { pending.addAll(it.asList()) }
                } else {
                    if (file.name.lowercase().contains(query)) {
                        includeFile(result, null, file)
                    }
                }
            }
        }
        return result
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        return documentId.startsWith(parentDocumentId)
    }

    private fun getDocIdForFile(file: File): String = file.absolutePath

    private fun getFileForDocId(docId: String): File {
        val f = File(docId)
        if (!f.exists()) throw FileNotFoundException("${f.absolutePath} not found")
        return f
    }

    private fun getMimeType(file: File): String {
        if (file.isDirectory) return Document.MIME_TYPE_DIR
        val name = file.name
        val lastDot = name.lastIndexOf('.')
        if (lastDot >= 0) {
            val ext = name.substring(lastDot + 1).lowercase()
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            if (mime != null) return mime
        }
        return "application/octet-stream"
    }

    private fun includeFile(result: MatrixCursor, docId: String?, fileArg: File?) {
        val docIdResolved: String
        val fileResolved: File
        if (docId == null) {
            fileResolved = fileArg!!
            docIdResolved = getDocIdForFile(fileResolved)
        } else {
            docIdResolved = docId
            fileResolved = getFileForDocId(docId)
        }

        var flags = 0
        if (fileResolved.isDirectory) {
            if (fileResolved.canWrite()) flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
        } else if (fileResolved.canWrite()) {
            flags = flags or Document.FLAG_SUPPORTS_WRITE
        }
        if (fileResolved.parentFile?.canWrite() == true) {
            flags = flags or Document.FLAG_SUPPORTS_DELETE
        }

        val mimeType = getMimeType(fileResolved)
        if (mimeType.startsWith("image/")) flags = flags or Document.FLAG_SUPPORTS_THUMBNAIL

        val row = result.newRow()
        row.add(Document.COLUMN_DOCUMENT_ID, docIdResolved)
        row.add(Document.COLUMN_DISPLAY_NAME, fileResolved.name)
        row.add(Document.COLUMN_SIZE, fileResolved.length())
        row.add(Document.COLUMN_MIME_TYPE, mimeType)
        row.add(Document.COLUMN_LAST_MODIFIED, fileResolved.lastModified())
        row.add(Document.COLUMN_FLAGS, flags)
    }
}
