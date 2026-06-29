package com.iris.iriscode.util

import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iris.iriscode.ui.theme.IrisPrimary
import com.iris.iriscode.ui.theme.IrisTextSubtle

fun hasStoragePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else true
}

@Composable
fun StoragePermissionRequest(
    onGranted: () -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(true) }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (hasStoragePermission()) {
            onGranted()
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text("Storage Access Required", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Iris needs access to manage all files on your device to read and write project files.",
                    color = IrisTextSubtle
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        }
                        settingsLauncher.launch(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IrisPrimary)
                ) {
                    Text("Grant Access")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    onGranted()
                }) {
                    Text("Skip")
                }
            }
        )
    }
}
