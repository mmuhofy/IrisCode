package com.iris.iriscode.ui.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iris.iriscode.ui.theme.IrisPrimary
import com.iris.iriscode.ui.theme.IrisTextSubtle

private val models = listOf(
    "flash" to "Gemini 2.5 Flash (default)",
    "pro" to "Gemini 2.5 Pro",
    "claude-sonnet" to "Claude Sonnet (v1.1+)",
    "gpt-4o" to "OpenAI GPT-4o (v1.1+)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSheet(
    currentModel: String,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Select Model",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            models.forEach { (id, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onModelSelected(id) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (id == currentModel) "●" else "○",
                        style = MaterialTheme.typography.titleMedium,
                        color = IrisPrimary,
                        modifier = Modifier.width(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = id,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (id == currentModel) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            color = IrisTextSubtle
                        )
                    }
                }
            }
        }
    }
}
