// UNTESTED — verify before use
package com.iris.iriscode.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
    onBack: () -> Unit
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
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
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
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = IrisSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
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
                            color = IrisTextSubtle
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(tween(300))
                        ) {
                            MessageBubble(message = message)
                        }
                    }
                    if (state.isTyping) {
                        item(key = "typing") { TypingIndicator() }
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IrisSurface)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.inputText,
                onValueChange = { text ->
                    viewModel.updateInput(text)
                    if (text.startsWith("/")) {
                        viewModel.toggleSlashMenu(true)
                        viewModel.updateSlashQuery(text.removePrefix("/"))
                    } else if (!text.startsWith("/") && state.showSlashMenu) {
                        viewModel.toggleSlashMenu(false)
                    }
                },
                placeholder = {
                    Text(
                        "Message Iris… or / for commands",
                        color = IrisTextSubtle
                    )
                },
                singleLine = false,
                maxLines = 4,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.sendMessage() }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IrisPrimary,
                    unfocusedBorderColor = IrisOutline,
                    cursorColor = IrisPrimary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (state.inputText.isNotBlank() && !state.isProcessing)
                            IrisPrimary
                        else
                            IrisSurfaceVariant
                    )
                    .clickable(
                        enabled = state.inputText.isNotBlank() && !state.isProcessing,
                        onClick = { viewModel.sendMessage() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = IrisPrimary
                    )
                } else {
                    Icon(
                        imageVector = Lucide.ArrowUp,
                        contentDescription = "Send",
                        tint = if (state.inputText.isNotBlank()) IrisBackground else IrisTextSubtle,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
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
            .padding(start = 16.dp, end = 56.dp, top = 4.dp, bottom = 4.dp),
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
                            .background(IrisTextSubtle.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}