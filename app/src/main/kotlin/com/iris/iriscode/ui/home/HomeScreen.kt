package com.iris.iriscode.ui.home

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.iris.iriscode.domain.model.Project
import com.iris.iriscode.domain.model.Session
import com.iris.iriscode.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onChatClick: (projectName: String, projectId: Long, projectPath: String, sessionId: String) -> Unit,
    onWorkspaceClick: (projectName: String, projectId: Long, projectPath: String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IrisBackground)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header()

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = IrisPrimary,
                        strokeWidth = 2.dp
                    )
                }
            } else if (state.workspaceGroups.isEmpty()) {
                EmptyState(onNewChat = viewModel::showNewChatSheet)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.workspaceGroups.forEach { group ->
                        item(key = "header_${group.project.id}") {
                            WorkspaceHeader(
                                project = group.project,
                                sessionCount = group.sessions.size,
                                onClick = {
                                    onWorkspaceClick(
                                        group.project.name,
                                        group.project.id,
                                        group.project.path
                                    )
                                }
                            )
                        }
                        items(
                            items = group.sessions,
                            key = { "session_${it.id}" }
                        ) { session ->
                            ChatItem(
                                session = session,
                                projectPath = group.project.path,
                                onClick = {
                                    onChatClick(
                                        group.project.name,
                                        group.project.id,
                                        group.project.path,
                                        session.id
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = viewModel::showNewChatSheet,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .navigationBarsPadding(),
            containerColor = IrisPrimary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Lucide.Plus,
                contentDescription = "New Chat",
                modifier = Modifier.size(22.dp)
            )
        }
    }

    if (state.showNewChatSheet) {
        NewChatSheet(
            name = state.newChatName,
            path = state.newChatPath,
            onNameChange = viewModel::updateNewChatName,
            onPathChange = viewModel::updateNewChatPath,
            onCreate = { viewModel.createChat(onChatClick) },
            onDismiss = viewModel::hideNewChatSheet
        )
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Iris Code",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun WorkspaceHeader(
    project: Project,
    sessionCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(IrisPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.FolderKanban,
                contentDescription = null,
                tint = IrisPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = project.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = IrisText
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "($sessionCount)",
            style = MaterialTheme.typography.labelSmall,
            color = IrisTextMuted
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = IrisTextMuted.copy(alpha = 0.4f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun ChatItem(
    session: Session,
    projectPath: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(IrisSurfaceContainer)
            .padding(start = 3.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(IrisPrimary)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IrisPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.MessageSquareText,
                contentDescription = null,
                tint = IrisPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = session.summary.ifEmpty { "New chat" },
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
                    color = IrisTextMuted
                )
                Text(
                    text = " · ",
                    style = MaterialTheme.typography.labelSmall,
                    color = IrisTextMuted.copy(alpha = 0.3f)
                )
                Text(
                    text = projectPath,
                    style = MaterialTheme.typography.labelSmall,
                    color = IrisTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onNewChat: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
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
                text = "No chats yet",
                style = MaterialTheme.typography.titleMedium,
                color = IrisTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Start a new chat to begin coding",
                style = MaterialTheme.typography.bodySmall,
                color = IrisTextSecondary.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            FilledTonalButton(
                onClick = onNewChat,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = IrisPrimary.copy(alpha = 0.15f)
                )
            ) {
                Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Chat", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewChatSheet(
    name: String,
    path: String,
    onNameChange: (String) -> Unit,
    onPathChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = IrisBackground,
        contentColor = IrisText
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "New Chat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Set a working directory for this chat",
                style = MaterialTheme.typography.bodySmall,
                color = IrisTextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Name") },
                placeholder = { Text("e.g. My Project") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IrisPrimary,
                    unfocusedBorderColor = IrisOutline,
                    cursorColor = IrisPrimary,
                    focusedContainerColor = IrisSurface,
                    unfocusedContainerColor = IrisSurface
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = path,
                onValueChange = onPathChange,
                label = { Text("Working Directory") },
                placeholder = { Text("/Projects/My Project") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IrisPrimary,
                    unfocusedBorderColor = IrisOutline,
                    cursorColor = IrisPrimary,
                    focusedContainerColor = IrisSurface,
                    unfocusedContainerColor = IrisSurface
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onCreate,
                enabled = name.isNotBlank() && path.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IrisPrimary,
                    contentColor = IrisBackground
                )
            ) {
                Text(
                    text = "Start Chat",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}
