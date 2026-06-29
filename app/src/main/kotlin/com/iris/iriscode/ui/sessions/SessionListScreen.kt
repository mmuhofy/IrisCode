package com.iris.iriscode.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.iris.iriscode.domain.model.Session
import com.iris.iriscode.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SessionListScreen(
    viewModel: SessionListViewModel,
    onSessionClick: (sessionId: String) -> Unit,
    onNewSession: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val grouped = remember(state.sessions) { groupSessions(state.sessions) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IrisBackground)
            .statusBarsPadding()
    ) {
        // Header
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
                    .background(IrisPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.History,
                    contentDescription = null,
                    tint = IrisPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = state.projectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Sessions",
                    style = MaterialTheme.typography.labelSmall,
                    color = IrisTextSecondary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            FilledTonalButton(
                onClick = onNewSession,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = IrisPrimary.copy(alpha = 0.15f)
                )
            ) {
                Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New", fontWeight = FontWeight.SemiBold)
            }
        }

        if (state.sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(IrisSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.MessageSquarePlus,
                            contentDescription = null,
                            tint = IrisTextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No sessions yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = IrisTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap + to start a new session",
                        style = MaterialTheme.typography.bodySmall,
                        color = IrisTextSecondary.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                grouped.forEach { (label, sessions) ->
                    item(key = "header_$label") {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = IrisTextSecondary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
                        )
                    }
                    items(sessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            onClick = { onSessionClick(session.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: Session,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(IrisSurfaceContainer)
            .padding(start = 3.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(IrisPrimary)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(IrisPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.MessageSquareText,
                contentDescription = null,
                tint = IrisPrimary,
                modifier = Modifier.size(22.dp)
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
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatTimestamp(session.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = IrisTextSecondary.copy(alpha = 0.6f)
                )
                if (session.toolCallCount > 0) {
                    Text(text = " · ", style = MaterialTheme.typography.labelSmall, color = IrisTextSecondary.copy(alpha = 0.3f))
                    Icon(Lucide.Terminal, contentDescription = null, tint = IrisTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("${session.toolCallCount}", style = MaterialTheme.typography.labelSmall, color = IrisTextSecondary.copy(alpha = 0.6f))
                }
                if (session.duration > 0) {
                    Text(text = " · ", style = MaterialTheme.typography.labelSmall, color = IrisTextSecondary.copy(alpha = 0.3f))
                    Icon(Lucide.Clock, contentDescription = null, tint = IrisTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(formatDuration(session.duration), style = MaterialTheme.typography.labelSmall, color = IrisTextSecondary.copy(alpha = 0.6f))
                }
                if (session.cost > 0) {
                    Text(text = " · ", style = MaterialTheme.typography.labelSmall, color = IrisTextSecondary.copy(alpha = 0.3f))
                    Icon(Lucide.Coins, contentDescription = null, tint = IrisTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("$${String.format("%.4f", session.cost)}", style = MaterialTheme.typography.labelSmall, color = IrisTextSecondary.copy(alpha = 0.6f))
                }
            }
        }

        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = IrisTextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
    }
}

private data class SessionGroup(val label: String, val sessions: List<Session>)

private fun groupSessions(sessions: List<Session>): List<SessionGroup> {
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    val todaySessions = mutableListOf<Session>()
    val yesterdaySessions = mutableListOf<Session>()
    val olderSessions = mutableListOf<Session>()

    sessions.forEach { session ->
        val cal = Calendar.getInstance().apply { timeInMillis = session.createdAt }
        when {
            isSameDay(cal, today) -> todaySessions.add(session)
            isSameDay(cal, yesterday) -> yesterdaySessions.add(session)
            else -> olderSessions.add(session)
        }
    }

    return listOfNotNull(
        SessionGroup("Today", todaySessions).takeIf { it.sessions.isNotEmpty() },
        SessionGroup("Yesterday", yesterdaySessions).takeIf { it.sessions.isNotEmpty() },
        SessionGroup("Older", olderSessions).takeIf { it.sessions.isNotEmpty() }
    )
}

private fun isSameDay(c1: Calendar, c2: Calendar): Boolean =
    c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
    c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)

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
