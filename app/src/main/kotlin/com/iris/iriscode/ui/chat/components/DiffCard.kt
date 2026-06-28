// UNTESTED — verify before use
package com.iris.iriscode.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.iris.iriscode.domain.model.ChatMessage
import com.iris.iriscode.ui.theme.*

@Composable
fun DiffCard(message: ChatMessage.FileDiff) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 56.dp, top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(IrisSurface)
    ) {
        // File header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IrisBackground)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.FileDiff,
                contentDescription = null,
                tint = IrisWarning,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message.filePath,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = IrisWarning,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
        }

        // Diff content
        Text(
            text = message.diff,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = IrisTextSubtle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
        )

        // Action row
        if (message.isApproved == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* handled by parent */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IrisSuccess)
                ) {
                    Icon(
                        Lucide.Check,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Approve", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { /* handled by parent */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IrisError)
                ) {
                    Icon(
                        Lucide.X,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = IrisError
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reject", fontWeight = FontWeight.SemiBold, color = IrisError)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val approved = message.isApproved == true
                val icon = if (approved) Lucide.CircleCheck else Lucide.CircleX
                val color = if (approved) IrisSuccess else IrisError
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (approved) "Approved" else "Rejected",
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}