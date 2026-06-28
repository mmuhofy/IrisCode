package com.iris.iriscode.ui.onboarding

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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreateNewFolder
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iris.iriscode.ui.theme.IrisBackground
import com.iris.iriscode.ui.theme.IrisOutline
import com.iris.iriscode.ui.theme.IrisPrimary
import com.iris.iriscode.ui.theme.IrisSuccess
import com.iris.iriscode.ui.theme.IrisSurfaceVariant
import com.iris.iriscode.ui.theme.IrisTextSubtle

@Composable
fun ProjectSetupScreen(
    projectPath: String?,
    onProjectPathSelected: (String) -> Unit,
    onNext: () -> Unit
) {
    var selectedPath by remember { mutableStateOf(projectPath) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val path = uri.path?.split(":")?.lastOrNull()
            if (path != null) {
                selectedPath = "/storage/emulated/0/$path"
                onProjectPathSelected(selectedPath!!)
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
        Spacer(modifier = Modifier.height(64.dp))

        Icon(
            imageVector = Icons.Outlined.CreateNewFolder,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = IrisPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Your First Project",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Pick a folder for your code.\nYou can change it later in settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = IrisTextSubtle,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedButton(
            onClick = { folderPicker.launch(null) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(),
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Icon(
                imageVector = if (selectedPath != null) Icons.Outlined.CheckCircle else Icons.Outlined.CreateNewFolder,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (selectedPath != null) IrisSuccess else IrisPrimary
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = if (selectedPath != null) "Change Folder" else "Choose Folder",
                fontWeight = FontWeight.Medium,
                color = if (selectedPath != null) IrisSuccess else IrisPrimary
            )
        }

        if (selectedPath != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = selectedPath!!,
                style = MaterialTheme.typography.bodySmall,
                color = IrisTextSubtle,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        IrisSurfaceVariant,
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(12.dp)
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
            Text("Start Coding", fontWeight = FontWeight.SemiBold)
        }
    }
}
