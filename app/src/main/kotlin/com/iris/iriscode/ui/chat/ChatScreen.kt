package com.iris.iriscode.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.iris.iriscode.domain.model.WorkMode
import com.iris.iriscode.ui.chat.components.MessageBubble
import com.iris.iriscode.ui.chat.components.ModelSheet
import com.iris.iriscode.ui.chat.components.SlashMenu
import com.iris.iriscode.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    projectName: String,
    onBack: () -> Unit,
    onSettings: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IrisBackground)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = projectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(IrisOutline)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "main",
                            style = MaterialTheme.typography.labelSmall,
                            color = IrisTextSecondary
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Lucide.ArrowLeft,
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            actions = {
                WorkModeChip(
                    mode = state.workMode,
                    onClick = {
                        val next = when (state.workMode) {
                            WorkMode.PLAN -> WorkMode.BUILD
                            WorkMode.BUILD -> WorkMode.AUTO
                            WorkMode.AUTO -> WorkMode.PLAN
                        }
                        viewModel.setWorkMode(next)
                    }
                )
                Spacer(modifier = Modifier.width(6.dp))
                ModelChip(
                    label = state.currentModel,
                    onClick = { viewModel.showModelSheet() }
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { /* new session */ }) {
                    Icon(
                        imageVector = Lucide.Plus,
                        contentDescription = "New Session",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onSettings) {
                    Icon(
                        imageVector = Lucide.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = IrisSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        TabRow(
            selectedTabIndex = state.selectedTab.ordinal,
            containerColor = IrisSurface,
            contentColor = IrisPrimary,
            divider = { HorizontalDivider(color = IrisOutline, thickness = 0.5.dp) }
        ) {
            Tab(
                selected = state.selectedTab == ChatTab.Chat,
                onClick = { viewModel.setTab(ChatTab.Chat) },
                text = { Text("Chat", fontWeight = FontWeight.Medium) },
                selectedContentColor = IrisPrimary,
                unselectedContentColor = IrisTextSecondary
            )
            Tab(
                selected = state.selectedTab == ChatTab.Terminal,
                onClick = { viewModel.setTab(ChatTab.Terminal) },
                text = { Text("Terminal", fontWeight = FontWeight.Medium) },
                selectedContentColor = IrisPrimary,
                unselectedContentColor = IrisTextSecondary
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (state.selectedTab == ChatTab.Terminal) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Lucide.SquareTerminal,
                            contentDescription = null,
                            tint = IrisTextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Terminal ready",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IrisTextSecondary
                        )
                    }
                }
            } else {
                if (state.messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Lucide.Sparkles,
                                contentDescription = null,
                                tint = IrisPrimary.copy(alpha = 0.3f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "What do you want Iris to do?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = IrisTextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp)
                    ) {
                        items(state.messages, key = { it.id }) { message ->
                            MessageBubble(message = message)
                        }
                        if (state.isTyping) {
                            item(key = "typing") { TypingIndicator() }
                        }
                    }
                }
            }
        }

        if (state.showSlashMenu) {
            SlashMenu(
                query = state.slashQuery,
                onCommandSelected = viewModel::handleSlashCommand
            )
        }

        AnimatedVisibility(
            visible = state.showExpandedPanel,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            ExpandedPanel(
                workMode = state.workMode,
                effortLevel = state.effortLevel,
                thinkingEnabled = state.thinkingEnabled,
                webSearchEnabled = state.webSearchEnabled,
                onModeChange = viewModel::setWorkMode,
                onEffortChange = viewModel::setEffortLevel,
                onThinkingChange = viewModel::setThinking,
                onWebSearchChange = viewModel::setWebSearch
            )
        }

        InputBar(
            inputText = state.inputText,
            isProcessing = state.isProcessing,
            showExpanded = state.showExpandedPanel,
            onTextChange = { text ->
                viewModel.updateInput(text)
                if (text.startsWith("/")) {
                    viewModel.toggleSlashMenu(true)
                    viewModel.updateSlashQuery(text.removePrefix("/"))
                } else if (!text.startsWith("/") && state.showSlashMenu) {
                    viewModel.toggleSlashMenu(false)
                }
            },
            onSend = viewModel::sendMessage,
            onToggleExpanded = viewModel::toggleExpandedPanel
        )
    }

    if (state.showModelSheet) {
        ModelSheet(
            currentModel = state.currentModel,
            onModelSelected = viewModel::selectModel,
            onDismiss = viewModel::hideModelSheet
        )
    }
}

@Composable
private fun InputBar(
    inputText: String,
    isProcessing: Boolean,
    showExpanded: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleExpanded: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(IrisSurface)
            .navigationBarsPadding()
            .imePadding()
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onTextChange,
            placeholder = {
                Text(
                    "Message Iris…",
                    color = IrisTextSecondary
                )
            },
            singleLine = false,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IrisPrimary,
                unfocusedBorderColor = IrisOutline,
                cursorColor = IrisPrimary
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(icon = Lucide.Slash, label = "Commands", onClick = { onTextChange("/") })
            Spacer(modifier = Modifier.width(2.dp))
            ActionButton(icon = Lucide.Plus, label = "More", onClick = onToggleExpanded)
            Spacer(modifier = Modifier.width(2.dp))
            ActionButton(icon = Lucide.Mic, label = "Voice", onClick = {})

            Spacer(modifier = Modifier.weight(1f))

            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = IrisPrimary
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank()) IrisPrimary else IrisSurfaceVariant
                        )
                        .clickable(enabled = inputText.isNotBlank(), onClick = onSend),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.ArrowUp,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) IrisBackground else IrisTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = IrisTextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = IrisTextSecondary
        )
    }
}

@Composable
private fun ExpandedPanel(
    workMode: WorkMode,
    effortLevel: String,
    thinkingEnabled: Boolean,
    webSearchEnabled: Boolean,
    onModeChange: (WorkMode) -> Unit,
    onEffortChange: (String) -> Unit,
    onThinkingChange: (Boolean) -> Unit,
    onWebSearchChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(IrisBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = "MODE",
            style = MaterialTheme.typography.labelSmall,
            color = IrisTextMuted,
            letterSpacing = TextUnit(1.2f, TextUnitType.Sp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WorkMode.entries.forEach { mode ->
                val isActive = mode == workMode
                val color = when (mode) {
                    WorkMode.PLAN -> IrisPlan
                    WorkMode.BUILD -> IrisBuild
                    WorkMode.AUTO -> IrisAuto
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) color.copy(alpha = 0.2f) else IrisSurfaceVariant)
                        .border(
                            width = 1.dp,
                            color = if (isActive) color else IrisOutline,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onModeChange(mode) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isActive) color else IrisTextSecondary,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = IrisOutline, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Thinking", style = MaterialTheme.typography.bodySmall, color = IrisTextSecondary)
            Switch(
                checked = thinkingEnabled,
                onCheckedChange = onThinkingChange,
                colors = SwitchDefaults.colors(checkedTrackColor = IrisPrimary)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Web Search", style = MaterialTheme.typography.bodySmall, color = IrisTextSecondary)
            Switch(
                checked = webSearchEnabled,
                onCheckedChange = onWebSearchChange,
                colors = SwitchDefaults.colors(checkedTrackColor = IrisPrimary)
            )
        }
    }
}

@Composable
private fun WorkModeChip(mode: WorkMode, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = when (mode) {
            WorkMode.PLAN -> IrisPlan
            WorkMode.BUILD -> IrisBuild
            WorkMode.AUTO -> IrisAuto
        },
        label = "modeColor"
    )
    val icon = when (mode) {
        WorkMode.PLAN -> Lucide.Eye
        WorkMode.BUILD -> Lucide.Hammer
        WorkMode.AUTO -> Lucide.Zap
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = bgColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = mode.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = bgColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ModelChip(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(IrisPrimary.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Lucide.BrainCircuit,
            contentDescription = null,
            tint = IrisPrimary,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = IrisPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Icon(
            imageVector = Lucide.ChevronDown,
            contentDescription = null,
            tint = IrisPrimary.copy(alpha = 0.6f),
            modifier = Modifier.size(11.dp)
        )
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(IrisSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.Sparkles,
                contentDescription = null,
                tint = IrisPrimary.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(IrisSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(IrisTextSecondary.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}
