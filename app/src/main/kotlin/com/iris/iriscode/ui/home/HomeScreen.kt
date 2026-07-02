package com.iris.iriscode.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.*
import com.iris.iriscode.domain.model.Session
import com.iris.iriscode.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardGradient = Brush.horizontalGradient(
    colors = listOf(
        IrisPrimary.copy(alpha = 0.04f),
        IrisSurfaceContainer,
        IrisSurfaceContainer
    ),
    startX = 0f,
    endX = 400f
)

private val AccentGradient = Brush.horizontalGradient(
    colors = listOf(
        IrisPrimary.copy(alpha = 0.08f),
        IrisPrimary.copy(alpha = 0.02f),
        Color.Transparent
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onChatClick: (projectName: String, projectId: Long, projectPath: String, sessionId: String) -> Unit,
    onWorkspaceClick: (projectName: String, projectId: Long, projectPath: String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var renameTarget by remember { mutableStateOf<Session?>(null) }
    var renameText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IrisBackground)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar()

            Spacer(modifier = Modifier.height(4.dp))

            TabSelector(
                selectedTab = state.selectedTab,
                onTabSelect = viewModel::selectTab
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = IrisPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                when (state.selectedTab) {
                    HomeTab.Chats -> ChatsTab(
                        sessions = state.allSessions,
                        onChatClick = onChatClick,
                        onRenameRequest = { session ->
                            renameTarget = session
                            renameText = session.summary.ifEmpty { "New chat" }
                        },
                        onNewChat = { viewModel.quickCreateChat(onChatClick) }
                    )
                    HomeTab.Workspaces -> WorkspacesTab(
                        groups = state.workspaceGroups,
                        onWorkspaceClick = onWorkspaceClick,
                        onNewChat = { viewModel.quickCreateChat(onChatClick) }
                    )
                }
            }
        }

        NewChatFAB(
            onClick = { viewModel.quickCreateChat(onChatClick) }
        )
    }

    if (renameTarget != null) {
        RenameDialog(
            currentName = renameText,
            onNameChange = { renameText = it },
            onConfirm = { renameTarget = null },
            onDismiss = { renameTarget = null }
        )
    }
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(IrisPrimary)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Iris Code",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = IrisText,
                letterSpacing = (-0.3).sp
            )
        }

        IconButton(onClick = { }) {
            Icon(
                imageVector = Lucide.Settings,
                contentDescription = "Settings",
                tint = IrisTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TabSelector(
    selectedTab: HomeTab,
    onTabSelect: (HomeTab) -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selectedTab == HomeTab.Chats) IrisPrimary.copy(alpha = 0.15f)
            else IrisSurfaceVariant,
        animationSpec = tween(250),
        label = "tabBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(IrisSurfaceVariant)
            .padding(3.dp)
    ) {
        HomeTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val textColor by animateColorAsState(
                targetValue = if (isSelected) IrisPrimary else IrisTextMuted,
                animationSpec = tween(200),
                label = "tabText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) IrisPrimary.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .clickable { onTabSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (tab == HomeTab.Chats) Lucide.MessageSquareText
                                else Lucide.FolderKanban,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = tab.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatsTab(
    sessions: List<SessionWithWorkspace>,
    onChatClick: (projectName: String, projectId: Long, projectPath: String, sessionId: String) -> Unit,
    onRenameRequest: (Session) -> Unit,
    onNewChat: () -> Unit
) {
    if (sessions.isEmpty()) {
        EmptyState(
            icon = Lucide.MessageSquarePlus,
            title = "No chats yet",
            subtitle = "Start a chat and let Iris help you code",
            onAction = onNewChat
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 0.dp,
                bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(sessions, key = { it.session.id }) { item ->
                ChatCard(
                    session = item.session,
                    workspaceName = item.projectName,
                    workspacePath = item.projectPath,
                    onClick = {
                        onChatClick(
                            item.projectName,
                            item.projectId,
                            item.projectPath,
                            item.session.id
                        )
                    },
                    onLongClick = { onRenameRequest(item.session) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatCard(
    session: Session,
    workspaceName: String,
    workspacePath: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardGradient)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(start = 0.dp, end = 14.dp, top = 13.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccentBar()
        Spacer(modifier = Modifier.width(14.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IrisPrimary.copy(alpha = 0.1f)),
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
                    text = workspaceName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = IrisPrimary.copy(alpha = 0.8f)
                )
                Text(
                    text = "  ·  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = IrisTextMuted.copy(alpha = 0.3f)
                )
                Text(
                    text = formatTimestamp(session.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = IrisTextMuted
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
                        color = IrisTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspacesTab(
    groups: List<HomeWorkspaceGroup>,
    onWorkspaceClick: (projectName: String, projectId: Long, projectPath: String) -> Unit,
    onNewChat: () -> Unit
) {
    if (groups.isEmpty()) {
        EmptyState(
            icon = Lucide.FolderOpen,
            title = "No workspaces yet",
            subtitle = "Create a chat to get started",
            onAction = onNewChat
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 0.dp,
                bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(groups, key = { it.project.id }) { group ->
                WorkspaceCard(
                    name = group.project.name,
                    path = group.project.path,
                    chatCount = group.sessions.size,
                    onClick = {
                        onWorkspaceClick(
                            group.project.name,
                            group.project.id,
                            group.project.path
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun WorkspaceCard(
    name: String,
    path: String,
    chatCount: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AccentGradient)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(IrisPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.FolderKanban,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = IrisText
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = IrisPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$chatCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = IrisPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = path.ifEmpty { "No directory set" },
                style = MaterialTheme.typography.bodySmall,
                color = IrisTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = IrisTextMuted.copy(alpha = 0.25f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AccentBar() {
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(IrisPrimary)
    )
}

@Composable
private fun NewChatFAB(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            IrisPrimary,
                            IrisPrimaryVariant
                        )
                    )
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 13.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Lucide.Plus,
                    contentDescription = null,
                    tint = IrisBackground,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New Chat",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = IrisBackground
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onAction: () -> Unit
) {
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = IrisPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = IrisText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = IrisTextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IrisBackground,
        titleContentColor = IrisText,
        textContentColor = IrisTextSecondary,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Lucide.Pencil,
                    contentDescription = null,
                    tint = IrisPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Rename Chat", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IrisPrimary,
                    unfocusedBorderColor = IrisOutline,
                    cursorColor = IrisPrimary,
                    focusedContainerColor = IrisSurface,
                    unfocusedContainerColor = IrisSurface
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Rename", color = IrisPrimary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = IrisTextMuted)
            }
        }
    )
}

private fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}
