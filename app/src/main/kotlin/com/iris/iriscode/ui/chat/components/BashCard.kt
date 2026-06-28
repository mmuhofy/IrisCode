package com.iris.iriscode.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iris.iriscode.domain.model.ChatMessage
import com.iris.iriscode.ui.theme.IrisBackground
import com.iris.iriscode.ui.theme.IrisPrimary
import com.iris.iriscode.ui.theme.IrisSuccess
import com.iris.iriscode.ui.theme.IrisSurface
import com.iris.iriscode.ui.theme.IrisTextSubtle
import com.iris.iriscode.ui.theme.IrisWarning

@Composable
fun BashCard(
    message: ChatMessage.BashCommand
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 64.dp, top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(IrisSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IrisBackground)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = IrisSuccess,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message.command,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
        }

        if (message.output.isNotEmpty()) {
            Text(
                text = message.output,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = IrisTextSubtle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState())
                    .height(100.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (message.isRunning) {
                Text(
                    text = "Running...",
                    style = MaterialTheme.typography.labelSmall,
                    color = IrisWarning
                )
            } else {
                val icon = if (message.exitCode == 0) "✓" else "✗"
                val color = if (message.exitCode == 0) IrisSuccess else MaterialTheme.colorScheme.error
                Text(
                    text = "$icon exit code: ${message.exitCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
        }
    }
}
