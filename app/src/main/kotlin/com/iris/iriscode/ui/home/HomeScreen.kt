package com.iris.iriscode.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private val SectionGradient = Brush.horizontalGradient(
    colors = listOf(
        IrisPrimary.copy(alpha = 0.08f),
        IrisPrimary.copy(alpha = 0.02f),
        Color.Transparent
    )
)

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
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
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
                        top = 4.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.workspaceGroups.forEach { group ->
                        item(key = "header_${group.project.id}") {
                            WorkspaceHeader(
                                name = group.project.name,
                                count = group.sessions.size,
                                path = group.project.path,
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
            contentColor = IrisBackground,
            shape = RoundedCornerShape(16.dp),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)
    ) {
        Text(
            text = "Iris Code",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = IrisText,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Code anywhere. Agent-powered.",
            style = MaterialTheme.typography.bodySmall,
            color = IrisTextMuted,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun WorkspaceHeader(
    name: String,
    count: Int,
    path: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.99f else 1f,
        animationSpec = tween(80),
        label = "wsScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 6.dp)
            .scale(pressScale)
            .clip(RoundedCornerShape(12.dp))
            .background(SectionGradient)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(IrisPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.FolderKanban,
                contentDescription = null,
                tint = IrisPrimary,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
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
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = IrisPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = path,
                style = MaterialTheme.typography.labelSmall,
                color = IrisTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = IrisTextMuted.copy(alpha = 0.3f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ChatItem(
    session: Session,
    projectPath: String,
    onClick: () -> Unit
) {
    ModernCard(
        onClick = onClick
    ) {
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
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
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
private fun EmptyState(onNewChat: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(IrisSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Sparkles,
                    contentDescription = null,
                    tint = IrisPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "What do you want to build?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = IrisText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Start a chat and let Iris help you code",
                style = MaterialTheme.typography.bodyMedium,
                color = IrisTextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            FilledTonalButton(
                onClick = onNewChat,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = IrisPrimary.copy(alpha = 0.12f)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "New Chat",
                    fontWeight = FontWeight.SemiBold,
                    color = IrisPrimary
                )
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
        contentColor = IrisText,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "New Chat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose a working directory for this chat",
                style = MaterialTheme.typography.bodySmall,
                color = IrisTextMuted
            )

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(14.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onCreate,
                enabled = name.isNotBlank() && path.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IrisPrimary,
                    contentColor = IrisBackground,
                    disabledContainerColor = IrisSurfaceVariant,
                    disabledContentColor = IrisTextDisabled
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Text(
                    text = "Start Chat",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}
