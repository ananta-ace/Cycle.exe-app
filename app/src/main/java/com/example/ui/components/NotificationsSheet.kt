package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNotification
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LocalCyclePalette
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    notifications: List<AppNotification>,
    busyId: String?,
    onDismiss: () -> Unit,
    onMarkRead: (AppNotification) -> Unit,
    onMarkAllRead: () -> Unit,
    onSendReply: (notice: AppNotification, text: String, onClear: () -> Unit) -> Unit,
    onNavigateFromNotification: (AppNotification) -> Unit
) {
    val palette = LocalCyclePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expandedId by remember { mutableStateOf<String?>(null) }
    val replyInputs = remember { mutableStateMapOf<String, String>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = GoldAccent,
                        border = BorderStroke(2.dp, CharcoalInk),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                tint = CharcoalInk,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Notifications & Notices",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Direct messages, status updates & replies",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close notifications")
                }
            }

            if (notifications.any { !it.isRead }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onMarkAllRead,
                        modifier = Modifier.testTag("mark_all_read_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MarkEmailRead,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Mark all read", fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = palette.dashedBorder, modifier = Modifier.padding(vertical = 8.dp))

            if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsOff,
                        contentDescription = null,
                        tint = palette.textMuted,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No notifications yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textSecondary
                    )
                    Text(
                        text = "You're completely caught up!",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        val isDirect = notif.isDirectNotice || notif.type == "DIRECT_NOTICE"
                        val isExpanded = expandedId == notif.id
                        val cardBg = when {
                            isDirect && notif.isUrgent -> palette.errorBg
                            isDirect || !notif.isRead -> palette.alertBg
                            else -> palette.cardSurface
                        }
                        val cardBorder = when {
                            isDirect && notif.isUrgent -> palette.errorFg
                            isDirect -> palette.border
                            !notif.isRead -> palette.alertBorder
                            else -> palette.dashedBorder
                        }

                        NeoCard(
                            containerColor = cardBg,
                            borderColor = cardBorder,
                            shadowColor = cardBorder,
                            shadowOffset = 3.dp,
                            contentPadding = PaddingValues(14.dp),
                            onClick = {
                                onMarkRead(notif)
                                if (isDirect || notif.replies.isNotEmpty()) {
                                    expandedId = if (isExpanded) null else notif.id
                                } else {
                                    onNavigateFromNotification(notif)
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (notif.isUrgent) palette.errorFg else CharcoalInk,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when {
                                                notif.isUrgent -> Icons.Filled.Warning
                                                isDirect -> Icons.Filled.Campaign
                                                notif.type == "APPLICATION_APPROVED" -> Icons.Filled.CheckCircle
                                                else -> Icons.Filled.Notifications
                                            },
                                            contentDescription = null,
                                            tint = if (notif.isUrgent) androidx.compose.ui.graphics.Color.White else GoldAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = notif.title.ifBlank { "Cycle.exe Notice" },
                                            style = MaterialTheme.typography.titleSmall,
                                            color = palette.textPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (!notif.isRead) {
                                            Surface(
                                                shape = CircleShape,
                                                color = palette.accentBronze,
                                                modifier = Modifier.size(9.dp)
                                            ) {}
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = notif.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = DateUtils.formatRelativeOrDate(notif.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textMuted
                                        )
                                        if (isDirect || notif.replies.isNotEmpty()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                                    contentDescription = null,
                                                    tint = palette.accentBronze,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = if (isExpanded) "Hide thread" else "Replies (${notif.replies.size})",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = palette.accentBronze
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (isExpanded) {
                                HorizontalDivider(
                                    color = palette.dashedBorder,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    notif.replies.forEach { reply ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = palette.cardSurface,
                                            border = BorderStroke(1.dp, palette.dashedBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "${reply.senderName} (${reply.senderRole})",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = palette.textPrimary
                                                    )
                                                    Text(
                                                        text = DateUtils.formatRelativeOrDate(reply.createdAt),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.textMuted
                                                    )
                                                }
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = reply.text,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = palette.textSecondary
                                                )
                                            }
                                        }
                                    }

                                    val draft = replyInputs[notif.id].orEmpty()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = draft,
                                            onValueChange = { replyInputs[notif.id] = it },
                                            placeholder = { Text("Write a reply...") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            enabled = draft.isNotBlank() && busyId != notif.id,
                                            onClick = {
                                                onSendReply(notif, draft) {
                                                    replyInputs[notif.id] = ""
                                                }
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Send reply",
                                                tint = if (draft.isNotBlank()) palette.accentBronze else palette.textMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
