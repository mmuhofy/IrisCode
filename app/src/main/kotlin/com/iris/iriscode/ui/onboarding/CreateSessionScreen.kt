package com.iris.iriscode.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Sparkles
import com.iris.iriscode.ui.theme.IrisBackground
import com.iris.iriscode.ui.theme.IrisOutline
import com.iris.iriscode.ui.theme.IrisPrimary
import com.iris.iriscode.ui.theme.IrisTextSubtle
import com.iris.iriscode.util.StoragePermissionRequest
import com.iris.iriscode.util.hasStoragePermission

@Composable
fun CreateSessionScreen(
    onCreate: (projectName: String, prompt: String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var needsPermission by remember { mutableStateOf(false) }
    var pendingName by remember { mutableStateOf("") }
    var pendingPrompt by remember { mutableStateOf("") }

    if (needsPermission) {
        StoragePermissionRequest(
            onGranted = {
                needsPermission = false
                onCreate(pendingName, pendingPrompt)
            }
        )
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
            imageVector = Lucide.Sparkles,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = IrisPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "What do you want to build?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Describe your app idea and Iris will start building.",
            style = MaterialTheme.typography.bodyMedium,
            color = IrisTextSubtle,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            placeholder = { Text("e.g. Build a todo app with Jetpack Compose") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IrisPrimary,
                unfocusedBorderColor = IrisOutline,
                cursorColor = IrisPrimary
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val name = prompt.take(40).trim().replace("\n", " ").ifBlank { "My App" }
                if (hasStoragePermission()) {
                    onCreate(name, prompt)
                } else {
                    pendingName = name
                    pendingPrompt = prompt
                    needsPermission = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = prompt.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IrisPrimary)
        ) {
            Icon(
                imageVector = Lucide.Sparkles,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Building", fontWeight = FontWeight.SemiBold)
        }
    }
}
