// UNTESTED — verify before use
package com.iris.iriscode.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.iris.iriscode.domain.model.ChatMessage
import com.iris.iriscode.ui.theme.*

@Composable
fun AskCard(
    message: ChatMessage.AskUser,
    onAnswer: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 56.dp, top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(IrisSurface)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IrisWarning.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.MessageCircleQuestion,
                    contentDescription = null,
                    tint = IrisWarning,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message.question,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (message.answer != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Lucide.CornerDownRight,
                    contentDescription = null,
                    tint = IrisTextSubtle.copy(alpha = 0.5f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = message.answer,
                    style = MaterialTheme.typography.bodySmall,
                    color = IrisTextSubtle
                )
            }
        } else if (message.options.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                message.options.forEach { option ->
                    OutlinedButton(
                        onClick = { onAnswer(option) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = IrisPrimary)
                    ) {
                        Text(option, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        } else {
            var textAnswer by remember { mutableStateOf("") }
            OutlinedTextField(
                value = textAnswer,
                onValueChange = { textAnswer = it },
                placeholder = { Text("Type your answer…", color = IrisTextSubtle) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IrisPrimary,
                    unfocusedBorderColor = IrisOutline,
                    cursorColor = IrisPrimary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onAnswer(textAnswer.trim()) },
                enabled = textAnswer.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IrisPrimary)
            ) {
                Icon(Lucide.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}