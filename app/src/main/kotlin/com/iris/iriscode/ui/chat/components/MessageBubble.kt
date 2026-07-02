package com.iris.iriscode.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.iris.iriscode.domain.model.ChatMessage
import com.iris.iriscode.ui.theme.IrisSurface
import com.iris.iriscode.ui.theme.IrisText
import com.iris.iriscode.ui.theme.IrisTextSecondary
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: ChatMessage,
    onAnswerAsk: (String) -> Unit = {},
    onApproveDiff: () -> Unit = {},
    onRejectDiff: () -> Unit = {}
) {
    when (message) {
        is ChatMessage.UserText -> UserBubble(message)
        is ChatMessage.AgentText -> AgentBubble(message)
        is ChatMessage.BashCommand -> BashCard(message)
        is ChatMessage.FileDiff -> DiffCard(message, onApprove = onApproveDiff, onReject = onRejectDiff)
        is ChatMessage.AskUser -> AskCard(message, onAnswer = onAnswerAsk)
        is ChatMessage.ReadFile -> ReadFileCard(message)
    }
}

@Composable
private fun UserBubble(message: ChatMessage.UserText) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
    ) {
        MarkdownText(
            markdown = message.text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = IrisText
            )
        )
        Text(
            text = formatTimestamp(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = IrisTextSecondary.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 2.dp)
        )
    }
}

@Composable
private fun AgentBubble(message: ChatMessage.AgentText) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
    ) {
        MarkdownText(
            markdown = message.text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = IrisText
            )
        )
        Text(
            text = formatTimestamp(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = IrisTextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ReadFileCard(message: ChatMessage.ReadFile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 48.dp, top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(IrisSurface)
    ) {
        Text(
            text = "Read: ${message.filePath}",
            style = MaterialTheme.typography.labelSmall,
            color = IrisTextSecondary,
            modifier = Modifier.padding(12.dp, 8.dp, 12.dp, 4.dp)
        )
        if (message.content.isNotEmpty()) {
            Text(
                text = message.content.take(200),
                style = MaterialTheme.typography.bodySmall,
                color = IrisTextSecondary,
                modifier = Modifier.padding(12.dp, 4.dp, 12.dp, 8.dp)
            )
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}
