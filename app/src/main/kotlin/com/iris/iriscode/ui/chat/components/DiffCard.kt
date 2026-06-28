package com.iris.iriscode.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iris.iriscode.domain.model.ChatMessage
import com.iris.iriscode.ui.theme.IrisBackground
import com.iris.iriscode.ui.theme.IrisError
import com.iris.iriscode.ui.theme.IrisPrimary
import com.iris.iriscode.ui.theme.IrisSuccess
import com.iris.iriscode.ui.theme.IrisSurface
import com.iris.iriscode.ui.theme.IrisTextSubtle
import com.iris.iriscode.ui.theme.IrisWarning

@Composable
fun DiffCard(
    message: ChatMessage.FileDiff
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 64.dp, top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(IrisSurface)
    ) {
        Text(
            text = message.filePath,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = IrisWarning,
            modifier = Modifier.padding(12.dp, 10.dp, 12.dp, 4.dp)
        )

        Text(
            text = message.diff,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = IrisTextSubtle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
        )

        if (message.isApproved == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Button(
                    onClick = { /* handled by parent */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = IrisSuccess)
                ) {
                    Text("Approve", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { /* handled by parent */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IrisError)
                ) {
                    Text("Reject", fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            val status = if (message.isApproved == true) "Approved" else "Rejected"
            val color = if (message.isApproved == true) IrisSuccess else IrisError
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
