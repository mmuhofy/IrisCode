package com.iris.iriscode.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.iris.iriscode.ui.theme.*
import java.io.File

@Composable
fun FilesTab() {
    val context = LocalContext.current
    val baseDir = context.filesDir
    var currentDir by remember { mutableStateOf(baseDir) }
    var stack by remember { mutableStateOf(listOf<File>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IrisBackground)
    ) {
        // Breadcrumb / header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IrisSurfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentDir != baseDir) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val parent = currentDir.parentFile
                            if (parent != null && parent.absolutePath.startsWith(baseDir.absolutePath)) {
                                stack = stack.dropLast(1)
                                currentDir = parent
                            }
                        }
                        .background(IrisSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.ArrowLeft,
                        contentDescription = "Back",
                        tint = IrisTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = if (currentDir == baseDir) Lucide.HardDrive else Lucide.Folder,
                contentDescription = null,
                tint = IrisPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (currentDir == baseDir) "IRIS Files" else currentDir.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val files = remember(currentDir) { currentDir.listFiles()?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name }) ?: emptyList() }

        if (files.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Lucide.FolderOpen,
                        contentDescription = null,
                        tint = IrisTextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Empty folder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IrisTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(files, key = { it.absolutePath }) { file ->
                    FileRow(
                        file = file,
                        onClick = {
                            if (file.isDirectory) {
                                stack = stack + currentDir
                                currentDir = file
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileRow(file: File, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IrisSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (file.isDirectory) Lucide.Folder else getFileIcon(file),
                contentDescription = null,
                tint = if (file.isDirectory) IrisPrimary else IrisTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (file.isDirectory) "" else formatSize(file.length()),
                style = MaterialTheme.typography.bodySmall,
                color = IrisTextSecondary
            )
        }
    }
}

private fun getFileIcon(file: File) = when {
    file.name.endsWith(".kt") || file.name.endsWith(".kts") -> Lucide.FileCode
    file.name.endsWith(".xml") -> Lucide.FileCode
    file.name.endsWith(".json") -> Lucide.FileCode
    file.name.endsWith(".md") -> Lucide.FileText
    file.name.endsWith(".txt") -> Lucide.FileText
    file.name.endsWith(".png") || file.name.endsWith(".jpg") -> Lucide.Image
    else -> Lucide.File
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}
