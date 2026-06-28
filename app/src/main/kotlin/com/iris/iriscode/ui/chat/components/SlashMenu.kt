package com.iris.iriscode.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iris.iriscode.ui.theme.IrisPrimary
import com.iris.iriscode.ui.theme.IrisSurface
import com.iris.iriscode.ui.theme.IrisTextSubtle

private data class SlashCommand(
    val command: String,
    val description: String
)

private val commands = listOf(
    SlashCommand("plan", "Switch to PLAN mode (read-only)"),
    SlashCommand("build", "Switch to BUILD mode (default)"),
    SlashCommand("auto", "Switch to AUTO mode (full autonomy)"),
    SlashCommand("models", "Switch AI model"),
    SlashCommand("new", "Start a new session"),
    SlashCommand("info", "Token/session info"),
    SlashCommand("history", "Session history"),
    SlashCommand("git", "Git operations"),
    SlashCommand("settings", "Open settings"),
)

@Composable
fun SlashMenu(
    query: String,
    onCommandSelected: (String) -> Unit
) {
    val filtered = if (query.isBlank()) commands
    else commands.filter {
        it.command.contains(query, ignoreCase = true) ||
        it.description.contains(query, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(IrisSurface)
    ) {
        Text(
            text = "Commands",
            style = MaterialTheme.typography.labelSmall,
            color = IrisTextSubtle,
            modifier = Modifier.padding(12.dp, 8.dp)
        )

        LazyColumn(modifier = Modifier.height(300.dp)) {
            items(filtered) { cmd ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCommandSelected(cmd.command) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "/${cmd.command}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IrisPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = cmd.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = IrisTextSubtle
                    )
                }
            }
        }
    }
}
