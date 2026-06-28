package com.iris.iriscode.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iris.iriscode.domain.model.ChatMessage
import com.iris.iriscode.ui.theme.IrisBackground
import com.iris.iriscode.ui.theme.IrisPrimary
import com.iris.iriscode.ui.theme.IrisSurface
import com.iris.iriscode.ui.theme.IrisTextSubtle

@Composable
fun MessageBubble(
    message: ChatMessage
) {
    when (message) {
        is ChatMessage.UserText -> UserBubble(message)
        is ChatMessage.AgentText -> AgentBubble(message)
        is ChatMessage.BashCommand -> BashCard(message)
        is ChatMessage.FileDiff -> DiffCard(message)
        is ChatMessage.AskUser -> AskCard(message)
        is ChatMessage.ReadFile -> ReadFileCard(message)
    }
}

@Composable
private fun UserBubble(message: ChatMessage.UserText) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
            .background(IrisPrimary)
            .padding(12.dp)
    ) {
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

@Composable
private fun AgentBubble(message: ChatMessage.AgentText) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 64.dp, top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
            .background(IrisSurface)
            .padding(12.dp)
    ) {
        Text(
            text = "Iris",
            style = MaterialTheme.typography.labelSmall,
            color = IrisPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ReadFileCard(message: ChatMessage.ReadFile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 64.dp, top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(IrisSurface)
    ) {
        Text(
            text = "Read: ${message.filePath}",
            style = MaterialTheme.typography.labelSmall,
            color = IrisTextSubtle,
            modifier = Modifier.padding(12.dp, 8.dp, 12.dp, 4.dp)
        )
        if (message.content.isNotEmpty()) {
            Text(
                text = message.content.take(200),
                style = MaterialTheme.typography.bodySmall,
                color = IrisTextSubtle,
                modifier = Modifier.padding(12.dp, 4.dp, 12.dp, 8.dp)
            )
        }
    }
}
