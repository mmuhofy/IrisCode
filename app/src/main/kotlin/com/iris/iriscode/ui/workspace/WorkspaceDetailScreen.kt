package com.iris.iriscode.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import com.iris.iriscode.domain.model.Session
import com.iris.iriscode.ui.components.ModernCard
import com.iris.iriscode.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkspaceDetailScreen(
    viewModel: WorkspaceDetailViewModel,
    projectName: String,
    projectId: Long,
    projectPath: String,
    onBack: () -> Unit,
    onChatClick: (projectName: String, projectId: Long, projectPath: String, sessionId: String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.init(projectName, projectId, projectPath)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IrisBackground)
            .statusBarsPadding()
    ) {
        TopBar(
            name = state.projectName,
            path = state.projectPath,
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ContextFilesSection()
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chats",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = IrisTextMuted,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            if (state.sessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Lucide.MessageSquarePlus,
                                contentDescription = null,
                                tint = IrisTextMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No chats yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = IrisTextMuted
                            )
                        }
                    }
                }
            } else {
                items(state.sessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        onClick = {
                            onChatClick(
                                state.projectName,
                                projectId,
                                state.projectPath,
                                session.id
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    name: String,
    path: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Lucide.ArrowLeft,
                contentDescription = "Back",
                tint = IrisText,
                modifier = Modifier.size(20.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IrisPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.FolderKanban,
                contentDescription = null,
                tint = IrisPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = IrisText
            )
            Text(
                text = path,
                style = MaterialTheme.typography.labelSmall,
                color = IrisTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ContextFilesSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(IrisSurfaceContainer)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IrisPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.BookOpenText,
                    contentDescription = null,
                    tint = IrisPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Context Files",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(IrisSurfaceVariant)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Lucide.FilePlus,
                    contentDescription = null,
                    tint = IrisTextMuted.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No context files yet",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = IrisTextMuted
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Agent can read these on every turn",
                    style = MaterialTheme.typography.labelSmall,
                    color = IrisTextMuted.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = IrisPrimary),
            border = BorderStroke(1.dp, IrisPrimary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Lucide.Plus,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add File", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SessionCard(
    session: Session,
    onClick: () -> Unit
) {
    ModernCard(
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(IrisPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.MessageSquareText,
                contentDescription = null,
                tint = IrisPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = session.summary.ifEmpty { "New session" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = IrisText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatTimestamp(session.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = IrisTextMuted.copy(alpha = 0.7f)
                )
                if (session.toolCallCount > 0) {
                    Text(
                        text = "  ·  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = IrisTextMuted.copy(alpha = 0.3f)
                    )
                    Icon(
                        imageVector = Lucide.BrainCircuit,
                        contentDescription = null,
                        tint = IrisTextMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${session.toolCallCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = IrisTextMuted.copy(alpha = 0.7f)
                    )
                }
                if (session.duration > 0) {
                    Text(
                        text = "  ·  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = IrisTextMuted.copy(alpha = 0.3f)
                    )
                    Icon(
                        imageVector = Lucide.Clock,
                        contentDescription = null,
                        tint = IrisTextMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = formatDuration(session.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = IrisTextMuted.copy(alpha = 0.7f)
                    )
                }
                if (session.cost > 0) {
                    Text(
                        text = "  ·  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = IrisTextMuted.copy(alpha = 0.3f)
                    )
                    Icon(
                        imageVector = Lucide.Coins,
                        contentDescription = null,
                        tint = IrisTextMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$${String.format("%.4f", session.cost)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = IrisTextMuted.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = IrisTextMuted.copy(alpha = 0.2f),
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatDuration(ms: Long): String {
    val sec = ms / 1000
    return when {
        sec < 60 -> "${sec}s"
        sec < 3600 -> "${sec / 60}m ${sec % 60}s"
        else -> "${sec / 3600}h ${(sec % 3600) / 60}m"
    }
}
