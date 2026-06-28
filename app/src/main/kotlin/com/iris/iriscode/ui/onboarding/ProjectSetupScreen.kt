package com.iris.iriscode.ui.onboarding

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iris.iriscode.ui.theme.IrisBackground
import com.iris.iriscode.ui.theme.IrisOutline
import com.iris.iriscode.ui.theme.IrisPrimary
import com.iris.iriscode.ui.theme.IrisTextSubtle
import java.io.File

@Composable
fun ProjectSetupScreen(
    projectPath: String?,
    onProjectPathSelected: (String) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    var selectedPath by remember { mutableStateOf(projectPath) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val path = getPathFromUri(context, it)
            if (path != null) {
                selectedPath = path
                onProjectPathSelected(path)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IrisBackground)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Your First Project",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Pick a folder for your code.\nYou can change it later.",
            style = MaterialTheme.typography.bodyMedium,
            color = IrisTextSubtle,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = { folderPicker.launch(null) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                width = if (selectedPath != null) 2.dp else 1.dp
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (selectedPath != null) IrisPrimary else IrisOutline
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = if (selectedPath != null) "Change Folder" else "Choose Folder",
                color = if (selectedPath != null) IrisPrimary else IrisTextSubtle
            )
        }

        if (selectedPath != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = selectedPath!!,
                style = MaterialTheme.typography.bodySmall,
                color = IrisTextSubtle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IrisPrimary)
        ) {
            Text(
                text = "Start Coding",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun getPathFromUri(context: Context, uri: Uri): String? {
    val docId = try {
        androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)?.uri?.lastPathSegment
    } catch (e: Exception) {
        uri.path?.split(":")?.lastOrNull()
    }
    return docId?.let { "/storage/emulated/0/$it" }
}
