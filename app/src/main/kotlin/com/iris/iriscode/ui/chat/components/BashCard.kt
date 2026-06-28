// UNTESTED — verify before use
package com.iris.iriscode.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.iris.iriscode.domain.model.ChatMessage
import com.iris.iriscode.ui.theme.*

@Composable
fun BashCard(message: ChatMessage.BashCommand) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 56.dp, top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(IrisSurface)
    ) {
        // Command header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IrisBackground)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.SquareTerminal,
                contentDescription = null,
                tint = IrisSuccess,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message.command,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
        }

        // Output
        if (message.output.isNotEmpty()) {
            Text(
                text = message.output,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = IrisTextSubtle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .heightIn(max = 120.dp)
                    .verticalScroll(rememberScrollState())
            )
        }

        // Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (message.isRunning) {
                Icon(
                    imageVector = Lucide.LoaderCircle,
                    contentDescription = null,
                    tint = IrisWarning,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Running…",
                    style = MaterialTheme.typography.labelSmall,
                    color = IrisWarning,
                    fontWeight = FontWeight.Medium
                )
            } else {
                val icon = if (message.exitCode == 0) Lucide.CircleCheck else Lucide.CircleX
                val color = if (message.exitCode == 0) IrisSuccess else IrisError
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "exit ${message.exitCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}