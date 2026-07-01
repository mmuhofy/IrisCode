package com.iris.iriscode.ui.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.composables.icons.lucide.*
import com.iris.iriscode.ui.chat.components.FilesTab
import com.iris.iriscode.ui.chat.components.MessageBubble
import com.iris.iriscode.ui.chat.components.SlashMenu
import com.iris.iriscode.ui.terminal.TerminalScreen
import com.iris.iriscode.ui.theme.*

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    projectName: String,
    projectPath: String,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(projectPath) {
        viewModel.setProjectInfo(projectPath, projectName)
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IrisSurface)
            .statusBarsPadding()
    ) {
        TopBar(
            projectName = projectName,
            currentModel = state.currentModel,
            modelDropdownExpanded = state.modelDropdownExpanded,
            showMoreMenu = state.showMoreMenu,
            onBack = onBack,
            onModelClick = viewModel::toggleModelDropdown,
            onModelDismiss = viewModel::dismissModelDropdown,
            onModelSelect = viewModel::selectModel,
            onMoreClick = viewModel::toggleMoreMenu,
            onMoreDismiss = viewModel::dismissMoreMenu
        )

        PillTabs(
            selectedTab = state.selectedTab,
            onTabSelect = viewModel::setTab
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(IrisBackground)
        ) {
            when (state.selectedTab) {
                ChatTab.Terminal -> {
                    TerminalScreen(
                        modifier = Modifier.fillMaxSize(),
                        terminalManager = viewModel.terminalManager
                    )
                }
                ChatTab.Files -> {
                    FilesTab()
                }
                ChatTab.Chat -> {
                    if (state.messages.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Lucide.BrainCircuit,
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
                                MessageBubble(
                                    message = message,
                                    onAnswerAsk = { answer -> viewModel.answerAsk(message.id, answer) },
                                    onApproveDiff = { viewModel.approveDiff(message.id) },
                                    onRejectDiff = { viewModel.rejectDiff(message.id) }
                                )
                            }
                            if (state.isTyping) {
                                item(key = "typing") { TypingIndicator() }
                            }
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

        InputBar(
            inputText = state.inputText,
            isProcessing = state.isProcessing,
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
            onToggleExpanded = viewModel::toggleOptionsSheet,
            onSwipeLeft = { viewModel.toggleSlashMenu(true) }
        )
    }

    if (state.showOptionsSheet) {
        OptionsSheet(
            workMode = state.workMode,
            thinkingEnabled = state.thinkingEnabled,
            webSearchEnabled = state.webSearchEnabled,
            onModeChange = viewModel::setWorkMode,
            onThinkingChange = viewModel::setThinking,
            onWebSearchChange = viewModel::setWebSearch,
            onDismiss = viewModel::dismissOptionsSheet
        )
    }
}

@Composable
private fun TopBar(
    projectName: String,
    currentModel: String,
    modelDropdownExpanded: Boolean,
    showMoreMenu: Boolean,
    onBack: () -> Unit,
    onModelClick: () -> Unit,
    onModelDismiss: () -> Unit,
    onModelSelect: (String) -> Unit,
    onMoreClick: () -> Unit,
    onMoreDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
    ) {
        // Left: back + name + branch
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
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

        // Center: model chip
        Box(modifier = Modifier.align(Alignment.Center)) {
            ModelChip(
                modelName = currentModel,
                expanded = modelDropdownExpanded,
                onClick = onModelClick
            )

            DropdownMenu(
                expanded = modelDropdownExpanded,
                onDismissRequest = onModelDismiss,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.background(IrisSurface)
            ) {
                availableModels.forEach { model ->
                    val isSelected = model.id == currentModel
                    Surface(
                        onClick = { onModelSelect(model.id) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) IrisPrimary.copy(alpha = 0.12f)
                                else IrisSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) IrisPrimary else IrisText
                                )
                                Text(
                                    text = model.provider,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IrisTextSecondary.copy(alpha = 0.6f)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = null,
                                    tint = IrisPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right: more menu
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Lucide.Menu,
                    contentDescription = "More options",
                    tint = IrisText,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = onMoreDismiss,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.background(IrisSurface)
            ) {
                listOf(
                    "Settings" to Lucide.Settings,
                    "New Session" to Lucide.Plus,
                    "Export Session" to Lucide.Download
                ).forEachIndexed { index, (label, icon) ->
                    if (index == 2) {
                        HorizontalDivider(color = IrisOutline.copy(alpha = 0.3f))
                    }
                    Surface(
                        onClick = { onMoreDismiss() },
                        shape = RoundedCornerShape(10.dp),
                        color = IrisSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = IrisTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = IrisText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelChip(
    modelName: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(100),
        label = "chipScale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = if (expanded) 1.5.dp else 1.dp,
            color = if (expanded) IrisPrimary.copy(alpha = 0.5f)
                    else IrisText.copy(alpha = 0.18f),
        ),
        interactionSource = interactionSource,
        modifier = Modifier.scale(pressScale)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = modelName.split("-").joinToString(" ") { part ->
                    part.replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }
                },
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                ),
                color = IrisText.copy(alpha = 0.8f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Lucide.ChevronDown,
                contentDescription = "Change model",
                tint = IrisText.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer {
                        rotationX = if (expanded) 180f else 0f
                    }
            )
        }
    }
}

@Composable
private fun PillTabs(
    selectedTab: ChatTab,
    onTabSelect: (ChatTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ChatTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab

            val tabIcon = when (tab) {
                ChatTab.Chat -> Lucide.MessageSquare
                ChatTab.Terminal -> Lucide.SquareTerminal
                ChatTab.Files -> Lucide.Folder
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) IrisPrimary.copy(alpha = 0.15f)
                        else IrisSurfaceVariant
                    )
                    .clickable { onTabSelect(tab) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tabIcon,
                        contentDescription = null,
                        tint = if (isSelected) IrisPrimary else IrisTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) IrisPrimary else IrisTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    inputText: String,
    isProcessing: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleExpanded: () -> Unit,
    onSwipeLeft: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(IrisBackground)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, IrisOutline, RoundedCornerShape(24.dp))
                .background(IrisSurface)
                .pointerInput(Unit) {
                    var dragTotal = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            dragTotal += dragAmount
                        },
                        onDragEnd = {
                            if (dragTotal < -60f) onSwipeLeft()
                            dragTotal = 0f
                        },
                        onDragCancel = { dragTotal = 0f }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onTextChange,
                    placeholder = {
                        Text("Message Iris…", color = IrisTextSecondary)
                    },
                    singleLine = false,
                    maxLines = 3,
                    minLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IrisOutline.copy(alpha = 0f),
                        unfocusedBorderColor = IrisOutline.copy(alpha = 0f),
                        cursorColor = IrisPrimary,
                        focusedContainerColor = IrisSurface,
                        unfocusedContainerColor = IrisSurface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, bottom = 4.dp, top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleExpanded,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Plus,
                            contentDescription = "More options",
                            tint = IrisTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { /* voice input */ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Mic,
                                contentDescription = "Voice input",
                                tint = IrisTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                strokeWidth = 2.dp,
                                color = IrisPrimary
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (inputText.isNotBlank()) IrisPrimary
                                        else IrisSurfaceVariant
                                    )
                                    .clickable(
                                        enabled = inputText.isNotBlank(),
                                        onClick = onSend
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.ArrowUp,
                                    contentDescription = "Send",
                                    tint = if (inputText.isNotBlank()) IrisBackground
                                           else IrisTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionsSheet(
    workMode: com.iris.iriscode.domain.model.WorkMode,
    thinkingEnabled: Boolean,
    webSearchEnabled: Boolean,
    onModeChange: (com.iris.iriscode.domain.model.WorkMode) -> Unit,
    onThinkingChange: (Boolean) -> Unit,
    onWebSearchChange: (Boolean) -> Unit,
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
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "MODE",
                style = MaterialTheme.typography.labelSmall,
                color = IrisTextMuted,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                com.iris.iriscode.domain.model.WorkMode.entries.forEach { mode ->
                    val isActive = mode == workMode
                    val color = when (mode) {
                        com.iris.iriscode.domain.model.WorkMode.PLAN -> IrisPlan
                        com.iris.iriscode.domain.model.WorkMode.BUILD -> IrisBuild
                        com.iris.iriscode.domain.model.WorkMode.AUTO -> IrisAuto
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
            Spacer(modifier = Modifier.height(16.dp))
        }
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
                imageVector = Lucide.BrainCircuit,
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
