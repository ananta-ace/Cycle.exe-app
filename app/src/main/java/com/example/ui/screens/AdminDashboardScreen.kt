package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CycleAvatar
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoPrimaryButton
import com.example.ui.components.NeoSecondaryButton
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LocalCyclePalette
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AppSessionState
import com.example.util.DateUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardScreen(
    session: AppSessionState,
    viewModel: AdminViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val palette = LocalCyclePalette.current
    val applications by viewModel.applications.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("APPLICATIONS") }
    var reportFilter by remember { mutableStateOf("PENDING") }
    val commentInputs = remember { mutableStateMapOf<String, String>() }

    val pendingAppsCount = applications.count { it.status == "PENDING" }
    val pendingReportsCount = reports.count { it.status == "PENDING" }
    val filteredReports = remember(reports, reportFilter) {
        if (reportFilter == "ALL") reports else reports.filter { it.status == reportFilter }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NeoCard(
                containerColor = palette.cardSurfaceAlt,
                shadowOffset = 5.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("admin_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Surface(
                        shape = CircleShape,
                        color = CharcoalInk,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = GoldAccent)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Moderator Dashboard",
                            style = MaterialTheme.typography.headlineSmall,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Recommend membership applicants & review content reports",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        }

        if (!statusMessage.isNullOrBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.accentGreenBg,
                    border = BorderStroke(1.5.dp, palette.accentGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentGreen,
                            modifier = Modifier.weight(1f)
                        )
                        NeoSecondaryButton(
                            text = "OK",
                            onClick = { viewModel.clearStatusMessage() }
                        )
                    }
                }
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeTab == "APPLICATIONS",
                    onClick = { activeTab = "APPLICATIONS" },
                    label = { Text("Applications ($pendingAppsCount pending)") },
                    leadingIcon = { Icon(Icons.Filled.HowToReg, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldAccent,
                        selectedLabelColor = CharcoalInk
                    ),
                    modifier = Modifier.testTag("admin_tab_applications")
                )
                FilterChip(
                    selected = activeTab == "REPORTS",
                    onClick = { activeTab = "REPORTS" },
                    label = { Text("Content Reports ($pendingReportsCount pending)") },
                    leadingIcon = { Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldAccent,
                        selectedLabelColor = CharcoalInk
                    ),
                    modifier = Modifier.testTag("admin_tab_reports")
                )
            }
        }

        if (activeTab == "APPLICATIONS") {
            if (applications.isEmpty()) {
                item {
                    NeoCard {
                        Text(
                            text = "No membership applications in the queue.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                }
            } else {
                items(applications, key = { it.id }) { app ->
                    val statusBg = when (app.status) {
                        "APPROVED" -> palette.accentGreenBg
                        "REJECTED" -> palette.errorBg
                        else -> palette.alertBg
                    }
                    val statusFg = when (app.status) {
                        "APPROVED" -> palette.accentGreen
                        "REJECTED" -> palette.errorFg
                        else -> palette.accentBronze
                    }

                    NeoCard(shadowOffset = 4.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                CycleAvatar(
                                    photoUrl = app.applicantPhotoURL,
                                    name = app.applicantName.ifBlank { "Rider" },
                                    size = 44.dp
                                )
                                Column {
                                    Text(
                                        text = app.applicantName.ifBlank { "Cyclist" },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = app.applicantEmail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = statusBg,
                                border = BorderStroke(1.dp, statusFg)
                            ) {
                                Text(
                                    text = app.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusFg,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        if (app.requestedTitle.isNotBlank()) {
                            Text(
                                text = "Requested Title: ${app.requestedTitle}",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.accentBronze
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        Text(
                            text = "Motivation: ${app.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Experience: ${app.cyclingExperience}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )

                        if (app.adminRecommendations.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = palette.dashedBorder)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "ADMIN RECOMMENDATIONS (${app.adminRecommendations.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textMuted
                            )
                            app.adminRecommendations.forEach { rec ->
                                Text(
                                    text = "• ${rec.adminName}: ${rec.recommendation}${if (rec.comment.isNotBlank()) " — \"${rec.comment}\"" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        if (app.status == "PENDING") {
                            Spacer(Modifier.height(10.dp))
                            val draft = commentInputs[app.id].orEmpty()
                            OutlinedTextField(
                                value = draft,
                                onValueChange = { commentInputs[app.id] = it },
                                label = { Text("Moderator note for Super Admin (optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NeoPrimaryButton(
                                    text = "Recommend Approve",
                                    containerColor = palette.accentGreen,
                                    contentColor = Color.White,
                                    onClick = {
                                        val adminName = session.profile?.displayName?.ifBlank { null }
                                            ?: session.user?.displayName ?: "Moderator"
                                        viewModel.recommendApplication(
                                            app = app,
                                            adminId = session.user?.uid.orEmpty(),
                                            adminName = adminName,
                                            recommendation = "APPROVE",
                                            comment = draft
                                        )
                                        commentInputs[app.id] = ""
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Filled.ThumbUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                NeoSecondaryButton(
                                    text = "Recommend Reject",
                                    contentColor = palette.errorFg,
                                    onClick = {
                                        val adminName = session.profile?.displayName?.ifBlank { null }
                                            ?: session.user?.displayName ?: "Moderator"
                                        viewModel.recommendApplication(
                                            app = app,
                                            adminId = session.user?.uid.orEmpty(),
                                            adminName = adminName,
                                            recommendation = "REJECT",
                                            comment = draft
                                        )
                                        commentInputs[app.id] = ""
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Filled.ThumbDown, contentDescription = null, tint = palette.errorFg, modifier = Modifier.size(16.dp))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("PENDING", "REVIEWED", "DISMISSED", "ALL").forEach { filter ->
                        FilterChip(
                            selected = reportFilter == filter,
                            onClick = { reportFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CharcoalInk,
                                selectedLabelColor = GoldAccent
                            )
                        )
                    }
                }
            }

            if (filteredReports.isEmpty()) {
                item {
                    NeoCard {
                        Text(
                            text = "No content reports matching \"$reportFilter\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                }
            } else {
                items(filteredReports, key = { it.id }) { report ->
                    NeoCard(shadowOffset = 4.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = palette.chipBg,
                                border = BorderStroke(1.dp, palette.border)
                            ) {
                                Text(
                                    text = "${report.targetType} • ${report.status}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = DateUtils.formatRelativeOrDate(report.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textMuted
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Reason: ${report.reason}",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.errorFg
                        )
                        if (report.targetExcerpt.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "\"${report.targetExcerpt}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textPrimary
                            )
                        }
                        Text(
                            text = "Reported by ${report.reporterEmail.ifBlank { report.reporterId }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (report.status == "PENDING") {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NeoSecondaryButton(
                                    text = "Dismiss",
                                    onClick = { viewModel.updateReportStatus(report.id, "DISMISSED") },
                                    modifier = Modifier.weight(1f)
                                )
                                NeoPrimaryButton(
                                    text = "Mark Reviewed",
                                    onClick = { viewModel.updateReportStatus(report.id, "REVIEWED") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = CharcoalInk, modifier = Modifier.size(16.dp))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
