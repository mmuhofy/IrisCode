package com.iris.iriscode.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.iris.iriscode.domain.model.WorkMode
import com.iris.iriscode.ui.chat.components.MessageBubble
import com.iris.iriscode.ui.chat.components.ModelSheet
import com.iris.iriscode.ui.chat.components.SlashMenu
import com.iris.iriscode.ui.theme.IrisAuto
import com.iris.iriscode.ui.theme.IrisBackground
import com.iris.iriscode.ui.theme.IrisBuild
import com.iris.iriscode.ui.theme.IrisOutline
import com.iris.iriscode.ui.theme.IrisPlan
import com.iris.iriscode.ui.theme.IrisPrimary
import com.iris.iriscode.ui.theme.IrisSurface
import com.iris.iriscode.ui.theme.IrisSurfaceVariant
import com.iris.iriscode.ui.theme.IrisTextSubtle

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = projectName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
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
                Spacer(modifier = Modifier.width(8.dp))
                ModelChip(
                    label = state.currentModel,
                    onClick = { viewModel.showModelSheet() }
                )
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
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 8.dp
                    )
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
                        item(key = "typing") {
                            TypingIndicator()
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
                placeholder = { Text("Type a message or / for commands...") },
                singleLine = false,
                maxLines = 4,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.sendMessage() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IrisPrimary,
                    unfocusedBorderColor = IrisOutline,
                    cursorColor = IrisPrimary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { viewModel.sendMessage() },
                enabled = state.inputText.isNotBlank() && !state.isProcessing
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = IrisPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Send,
                        contentDescription = "Send",
                        tint = if (state.inputText.isNotBlank()) IrisPrimary else IrisTextSubtle
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
private fun WorkModeChip(
    mode: WorkMode,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = when (mode) {
            WorkMode.PLAN -> IrisPlan
            WorkMode.BUILD -> IrisBuild
            WorkMode.AUTO -> IrisAuto
        },
        label = "modeColor"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = mode.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = bgColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ModelChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(IrisPrimary.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = IrisPrimary,
            fontWeight = FontWeight.SemiBold
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
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(IrisPrimary.copy(alpha = 0.6f))
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
