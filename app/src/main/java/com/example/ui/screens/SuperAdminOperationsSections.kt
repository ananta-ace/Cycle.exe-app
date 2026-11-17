package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNotification
import com.example.data.model.AuditLog
import com.example.data.model.ContentReport
import com.example.data.model.FeaturedMember
import com.example.data.model.InvitationCode
import com.example.data.model.MembershipApplication
import com.example.data.model.UserProfile
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoPrimaryButton
import com.example.ui.components.NeoSecondaryButton
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCyclePalette
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AppSessionState
import com.example.util.DateUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuperAdminNoticesSection(
    session: AppSessionState,
    viewModel: AdminViewModel,
    allNotifications: List<AppNotification>,
    onOpenComposer: () -> Unit
) {
    val palette = LocalCyclePalette.current
    val directNotices = remember(allNotifications) {
        allNotifications.filter { it.isDirectNotice || it.type == "DIRECT_NOTICE" }
    }
    var statusFilter by remember { mutableStateOf("ALL") }
    var search by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<String?>(null) }
    val replyDrafts = remember { mutableStateMapOf<String, String>() }

    val totalSent = directNotices.size
    val unreadCount = directNotices.count { !it.isRead }
    val readPct = if (totalSent == 0) 0 else ((totalSent - unreadCount) * 100) / totalSent
    val repliesCount = directNotices.sumOf { it.replies.size }

    val filtered = remember(directNotices, statusFilter, search) {
        directNotices.filter { n ->
            val matchFilter = when (statusFilter) {
                "UNREAD" -> !n.isRead
                "READ" -> n.isRead
                "URGENT" -> n.isUrgent
                "REPLIED" -> n.replies.isNotEmpty()
                else -> true
            }
            val q = search.trim().lowercase()
            val matchSearch = q.isBlank() ||
                n.recipientName.lowercase().contains(q) ||
                n.recipientEmail.lowercase().contains(q) ||
                n.title.lowercase().contains(q) ||
                n.message.lowercase().contains(q)
            matchFilter && matchSearch
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NeoPrimaryButton(
            text = "Compose Direct Notice",
            onClick = onOpenComposer,
            leadingIcon = { Icon(Icons.Filled.Campaign, contentDescription = null, tint = CharcoalInk) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_notice_composer_button")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricMiniCard("Total Sent", totalSent.toString(), Modifier.weight(1f))
            MetricMiniCard("Unread", unreadCount.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricMiniCard("Read Rate", "$readPct%", Modifier.weight(1f))
            MetricMiniCard("Replies", repliesCount.toString(), Modifier.weight(1f))
        }

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search recipient, title, or message...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("ALL", "UNREAD", "READ", "URGENT", "REPLIED").forEach { f ->
                FilterChip(
                    selected = statusFilter == f,
                    onClick = { statusFilter = f },
                    label = { Text(f, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CharcoalInk, selectedLabelColor = GoldAccent)
                )
            }
        }

        if (filtered.isEmpty()) {
            NeoCard {
                Text("No direct notices match the current filter.", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
            }
        } else {
            filtered.forEach { notice ->
                val isExpanded = expandedId == notice.id
                NeoCard(
                    borderColor = if (notice.isUrgent) palette.errorFg else palette.border,
                    shadowOffset = 4.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = notice.title.ifBlank { "Direct Notice" },
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "To: ${notice.recipientName.ifBlank { notice.recipientEmail }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accentBronze
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteNotice(notice.id) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete notice", tint = palette.errorFg)
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(notice.message, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (notice.isRead) "Read ${DateUtils.formatRelativeOrDate(notice.readAt)}" else "Delivered • Unread",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (notice.isRead) palette.accentGreen else palette.accentBronze
                        )
                        NeoSecondaryButton(
                            text = if (isExpanded) "Hide Thread" else "Replies (${notice.replies.size})",
                            onClick = { expandedId = if (isExpanded) null else notice.id },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(15.dp)) }
                        )
                    }

                    if (isExpanded) {
                        HorizontalDivider(color = palette.dashedBorder, modifier = Modifier.padding(vertical = 8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            notice.replies.forEach { reply ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.cardSurfaceAlt,
                                    border = BorderStroke(1.dp, palette.dashedBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("${reply.senderName} (${reply.senderRole})", style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
                                            Text(DateUtils.formatRelativeOrDate(reply.createdAt), style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                                        }
                                        Text(reply.text, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                                    }
                                }
                            }
                            val draft = replyDrafts[notice.id].orEmpty()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = draft,
                                    onValueChange = { replyDrafts[notice.id] = it },
                                    placeholder = { Text("Reply as Super Admin...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    enabled = draft.isNotBlank(),
                                    onClick = {
                                        viewModel.sendAdminReplyToNotice(
                                            notice = notice,
                                            senderId = session.user?.uid.orEmpty(),
                                            senderName = session.profile?.displayName?.ifBlank { null } ?: "Super Admin",
                                            senderEmail = session.user?.email.orEmpty(),
                                            text = draft
                                        ) {
                                            replyDrafts[notice.id] = ""
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send admin reply", tint = palette.accentBronze)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuperAdminAnalyticsSection(
    users: List<UserProfile>,
    applications: List<MembershipApplication>,
    reports: List<ContentReport>,
    featuredMembers: List<FeaturedMember>
) {
    val palette = LocalCyclePalette.current
    val superAdmins = users.count { it.role == "SUPER_ADMIN" }
    val admins = users.count { it.role == "ADMIN" }
    val members = users.count { it.role == "MEMBER" || it.isAdminMember }
    val publicUsers = users.count { it.role == "PUBLIC" && !it.isAdminMember }
    val suspended = users.count { it.isSuspended }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricMiniCard("Registered Users", users.size.toString(), Modifier.weight(1f))
            MetricMiniCard("Featured Crew", featuredMembers.size.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricMiniCard("Pending Apps", applications.count { it.status == "PENDING" }.toString(), Modifier.weight(1f))
            MetricMiniCard("Pending Reports", reports.count { it.status == "PENDING" }.toString(), Modifier.weight(1f))
        }

        NeoCard(shadowOffset = 4.dp) {
            Text("ROLE DISTRIBUTION", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
            Spacer(Modifier.height(10.dp))
            AnalyticsBarRow("SUPER_ADMIN", superAdmins, users.size, GoldAccent)
            AnalyticsBarRow("ADMIN", admins, users.size, palette.accentBronze)
            AnalyticsBarRow("MEMBER", members, users.size, palette.accentGreen)
            AnalyticsBarRow("PUBLIC", publicUsers, users.size, palette.textSecondary)
            AnalyticsBarRow("SUSPENDED", suspended, users.size, palette.errorFg)
        }
    }
}

@Composable
fun SuperAdminInvitationsSection(
    session: AppSessionState,
    viewModel: AdminViewModel,
    codes: List<InvitationCode>,
    latestGeneratedCode: Pair<String, String>?,
    busy: Boolean
) {
    val palette = LocalCyclePalette.current
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NeoCard(shadowOffset = 4.dp) {
            Text("Generate Single-Use Invitation Code", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Text(
                text = "Generate single-use role codes and share them directly with trusted riders.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeoPrimaryButton(
                    text = "New MEMBER Code",
                    loading = busy,
                    onClick = {
                        viewModel.generateInvitationCode(
                            targetRole = "MEMBER",
                            actorUid = session.user?.uid.orEmpty(),
                            actorEmail = session.user?.email.orEmpty()
                        )
                    },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, tint = CharcoalInk, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
                NeoSecondaryButton(
                    text = "New ADMIN Code",
                    enabled = !busy,
                    onClick = {
                        viewModel.generateInvitationCode(
                            targetRole = "ADMIN",
                            actorUid = session.user?.uid.orEmpty(),
                            actorEmail = session.user?.email.orEmpty()
                        )
                    },
                    leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
            }

            latestGeneratedCode?.let { (code, role) ->
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.accentGreenBg,
                    border = BorderStroke(1.5.dp, palette.accentGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("NEW $role CODE (SAVE NOW)", style = MaterialTheme.typography.labelSmall, color = palette.accentGreen)
                            Text(code, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Cycle.exe Invitation Code", code))
                                Toast.makeText(context, "Copied invitation code!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy invitation code", tint = palette.accentGreen)
                        }
                    }
                }
            }
        }

        if (codes.isEmpty()) {
            NeoCard {
                Text("No invitation codes issued yet.", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
            }
        } else {
            codes.forEach { item ->
                val statusText = when {
                    item.isRevoked -> "REVOKED"
                    item.isRedeemed -> "REDEEMED"
                    else -> "ACTIVE"
                }
                NeoCard(shadowOffset = 3.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.displayCodeMasked, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("${item.targetRole} • $statusText • Created ${DateUtils.formatShortDate(item.createdAt)}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                        if (!item.isRevoked && !item.isRedeemed) {
                            IconButton(
                                onClick = {
                                    viewModel.revokeInvitationCode(
                                        codeId = item.id,
                                        actorUid = session.user?.uid.orEmpty(),
                                        actorEmail = session.user?.email.orEmpty()
                                    )
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Block, contentDescription = "Revoke code", tint = palette.errorFg)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuperAdminAuditLogsSection(logs: List<AuditLog>) {
    val palette = LocalCyclePalette.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (logs.isEmpty()) {
            NeoCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = palette.textMuted)
                    Text("No security audit logs recorded yet.", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                }
            }
        } else {
            logs.forEach { log ->
                NeoCard(shadowOffset = 3.dp, contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = CharcoalInk
                        ) {
                            Text(
                                text = log.action,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = DateUtils.formatRelativeOrDate(log.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textMuted
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(log.details, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                    Text(
                        text = "Actor: ${log.actorEmail.ifBlank { log.actorId }} • Target: ${log.targetType} (${log.targetId})",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricMiniCard(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = LocalCyclePalette.current
    NeoCard(modifier = modifier, shadowOffset = 3.dp, cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = palette.textPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
    }
}

@Composable
private fun AnalyticsBarRow(label: String, count: Int, total: Int, barColor: Color) {
    val palette = LocalCyclePalette.current
    val pct = if (total <= 0) 0f else (count.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontFamily = JetBrainsMonoFontFamily, style = MaterialTheme.typography.labelSmall, color = palette.textPrimary)
            Text("$count (${(pct * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        }
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = palette.chipBg,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = barColor,
                    modifier = Modifier
                        .fillMaxWidth(pct.coerceAtLeast(if (count > 0) 0.04f else 0f))
                        .height(8.dp)
                ) {}
            }
        }
    }
}
