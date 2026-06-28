// UNTESTED — verify before use
package com.iris.iriscode.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.iris.iriscode.ui.theme.*

private data class SlashCommand(
    val command: String,
    val description: String,
    val icon: ImageVector
)

private val commands = listOf(
    SlashCommand("plan",     "Read-only analysis mode",      Lucide.Eye),
    SlashCommand("build",    "Full tool use (default)",      Lucide.Hammer),
    SlashCommand("auto",     "Full autonomy, no approval",   Lucide.Zap),
    SlashCommand("models",   "Switch AI model",              Lucide.BrainCircuit),
    SlashCommand("new",      "Start a new session",          Lucide.RotateCcw),
    SlashCommand("info",     "Token & session info",         Lucide.Info),
    SlashCommand("history",  "Session history",              Lucide.History),
    SlashCommand("git",      "Git operations",               Lucide.GitBranch),
    SlashCommand("mcp",      "MCP server list",              Lucide.Plug),
    SlashCommand("settings", "Open settings",                Lucide.Settings),
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
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(IrisSurface)
    ) {
        Text(
            text = "COMMANDS",
            style = MaterialTheme.typography.labelSmall,
            color = IrisTextSubtle.copy(alpha = 0.5f),
            letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp),
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 2.dp)
        )

        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
            items(filtered) { cmd ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCommandSelected(cmd.command) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(IrisPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = cmd.icon,
                            contentDescription = null,
                            tint = IrisPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "/${cmd.command}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IrisPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(10.dp))
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