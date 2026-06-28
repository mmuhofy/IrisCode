package com.iris.iriscode.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iris.iriscode.domain.model.ChatMessage
import com.iris.iriscode.ui.theme.IrisOutline
import com.iris.iriscode.ui.theme.IrisPrimary
import com.iris.iriscode.ui.theme.IrisSurface
import com.iris.iriscode.ui.theme.IrisTextSubtle

@Composable
fun AskCard(
    message: ChatMessage.AskUser,
    onAnswer: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 64.dp, top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(IrisSurface)
            .padding(12.dp)
    ) {
        Text(
            text = message.question,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (message.answer != null) {
            Text(
                text = message.answer,
                style = MaterialTheme.typography.bodySmall,
                color = IrisTextSubtle
            )
        } else if (message.options.isNotEmpty()) {
            Row {
                message.options.forEach { option ->
                    OutlinedButton(
                        onClick = { onAnswer(option) },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(option)
                    }
                }
            }
        } else {
            var textAnswer by remember { mutableStateOf("") }
            OutlinedTextField(
                value = textAnswer,
                onValueChange = { textAnswer = it },
                placeholder = { Text("Type your answer...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
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
                colors = ButtonDefaults.buttonColors(containerColor = IrisPrimary)
            ) {
                Text("Send")
            }
        }
    }
}
