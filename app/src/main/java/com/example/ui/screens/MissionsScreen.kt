package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MissionReport
import com.example.ui.components.HeroSectionHeader
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoPrimaryButton
import com.example.ui.components.NeoSecondaryButton
import com.example.ui.components.SyncErrorCard
import com.example.ui.components.SyncLoadingCard
import com.example.ui.theme.AmberBronze
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCyclePalette
import com.example.ui.viewmodel.AppSessionState
import com.example.ui.viewmodel.MissionsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissionsScreen(
    session: AppSessionState,
    viewModel: MissionsViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalCyclePalette.current
    val missionsState by viewModel.missionsState.collectAsStateWithLifecycle()
    val missions = missionsState.items
    val candidateRiders by viewModel.candidateRiders.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var editorOpen by remember { mutableStateOf(false) }
    var editingMission by remember { mutableStateOf<MissionReport?>(null) }
    var confirmDeleteMission by remember { mutableStateOf<MissionReport?>(null) }

    val totalDistKm = remember(missions) {
        missions.sumOf { m ->
            Regex("([0-9]+\\.?[0-9]*)").find(m.totalDist)?.value?.toDoubleOrNull() ?: 0.0
        }
    }
    val avgFleetSpeed = remember(missions) {
        if (missions.isEmpty()) 0.0
        else missions.sumOf { m ->
            Regex("([0-9]+\\.?[0-9]*)").find(m.avgSpeed)?.value?.toDoubleOrNull() ?: 0.0
        } / missions.size
    }
    val totalSaddleHrs = remember(missions) {
        missions.sumOf { m ->
            val h = Regex("(\\d+)h").find(m.duration)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            val mins = Regex("(\\d+)m").find(m.duration)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            h + mins / 60.0
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroSectionHeader(
                badgeText = "Verified Ride Logs",
                title = "Mission Operations & Logs",
                subtitle = "Every ride, route segment, and pace record logged by Cycle.exe leadership."
            )
            if (session.isSuperAdmin) {
                Spacer(Modifier.height(10.dp))
                NeoPrimaryButton(
                    text = "Create New Mission",
                    onClick = {
                        viewModel.clearError()
                        editingMission = null
                        editorOpen = true
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = CharcoalInk)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_mission_button")
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MissionStatCard(
                        icon = Icons.Filled.EmojiEvents,
                        value = missions.size.toString(),
                        label = "total missions",
                        modifier = Modifier.weight(1f)
                    )
                    MissionStatCard(
                        icon = Icons.Filled.Route,
                        value = String.format(java.util.Locale.US, "%.1f km", totalDistKm),
                        label = "expedition dist",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MissionStatCard(
                        icon = Icons.Filled.Speed,
                        value = String.format(java.util.Locale.US, "%.1f km/h", avgFleetSpeed),
                        label = "avg fleet speed",
                        modifier = Modifier.weight(1f)
                    )
                    MissionStatCard(
                        icon = Icons.Filled.HourglassBottom,
                        value = String.format(java.util.Locale.US, "%.0f hrs", totalSaddleHrs),
                        label = "saddle time",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (missionsState.isLoading && missions.isEmpty()) {
            item {
                SyncLoadingCard(title = "Loading Mission Logs...")
            }
        } else if (missionsState.error != null && missions.isEmpty()) {
            item {
                SyncErrorCard(
                    message = missionsState.error.orEmpty(),
                    onRetry = { viewModel.refreshMissions() }
                )
            }
        } else if (missions.isEmpty()) {
            item {
                NeoCard(shadowOffset = 5.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = null,
                            tint = palette.textMuted,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = "No Missions Published Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Missions added on the website or by Super Admin appear here in real time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        } else {
            if (missionsState.error != null) {
                item {
                    SyncErrorCard(
                        message = missionsState.error.orEmpty(),
                        onRetry = { viewModel.refreshMissions() }
                    )
                }
            }
            items(missions, key = { it.id.ifBlank { it.resolvedTag() } }) { mission ->
            val accentColor = when {
                mission.variant == "custom" && !mission.customColor.isNullOrBlank() ->
                    runCatching { Color(android.graphics.Color.parseColor(mission.customColor)) }.getOrElse { AmberBronze }
                mission.variant == "alt-accent" -> AmberBronze
                mission.variant == "green-accent" -> palette.accentGreen
                else -> palette.border
            }
            val badgeBg = when (mission.variant) {
                "alt-accent" -> AmberBronze
                "green-accent" -> palette.accentGreen
                "custom" -> accentColor
                else -> CharcoalInk
            }
            val badgeFg = if (mission.variant == "default") GoldAccent else Color.White

            NeoCard(
                borderColor = accentColor,
                shadowColor = accentColor.copy(alpha = 0.7f),
                shadowOffset = 6.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = badgeBg,
                        border = BorderStroke(1.5.dp, palette.border)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Flag,
                                contentDescription = null,
                                tint = badgeFg,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Mission ${mission.resolvedTag()}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = badgeFg,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    if (session.isSuperAdmin) {
                        Row {
                            IconButton(
                                onClick = {
                                    viewModel.clearError()
                                    editingMission = mission
                                    editorOpen = true
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit mission", tint = palette.accentBronze)
                            }
                            IconButton(
                                onClick = { confirmDeleteMission = mission },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete mission", tint = palette.errorFg)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsBike,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = mission.resolvedTitle(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (mission.isDuo) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = badgeBg.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, accentColor)
                    ) {
                        Text(
                            text = "Duo Ride",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (mission.resolvedDate().isNotBlank()) {
                        MissionMetaChip(Icons.Filled.CalendarToday, mission.resolvedDate(), accentColor)
                    }
                    val timeLabel = when {
                        mission.startTime.isNotBlank() && mission.endTime.isNotBlank() ->
                            "${mission.startTime} – ${mission.endTime}"
                        mission.time.isNotBlank() -> mission.time
                        else -> ""
                    }
                    if (timeLabel.isNotBlank()) {
                        MissionMetaChip(Icons.Filled.Schedule, timeLabel, accentColor)
                    }
                    if (mission.duration.isNotBlank()) {
                        MissionMetaChip(Icons.Filled.HourglassBottom, mission.duration, accentColor)
                    }
                    if (mission.avgSpeed.isNotBlank()) {
                        MissionMetaChip(Icons.Filled.Speed, mission.avgSpeed, accentColor)
                    }
                }

                if (mission.segments.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.cardSurfaceAlt,
                        border = BorderStroke(1.5.dp, palette.dashedBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ROUTE SEGMENTS", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                                Text("DIST", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                            }
                            HorizontalDivider(
                                color = palette.dashedBorder,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                            mission.segments.forEachIndexed { idx, seg ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = accentColor.copy(alpha = 0.18f),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${seg.num.takeIf { it > 0 } ?: (idx + 1)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textPrimary
                                                )
                                            }
                                        }
                                        Text(
                                            text = seg.resolvedLabel(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.textPrimary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = badgeBg
                                    ) {
                                        Text(
                                            text = seg.distance.ifBlank { "—" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = badgeFg,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(
                                color = palette.dashedBorder,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOTAL EXPEDITION DISTANCE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = mission.totalDist.ifBlank { "—" },
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = accentColor
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Groups,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Crew (${mission.crewIds.size}):",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.textPrimary
                    )
                }
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (mission.crewIds.isEmpty()) {
                        Text(
                            text = "Full group paceline",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textMuted
                        )
                    } else {
                        mission.crewIds.forEach { memberName ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = palette.chipBg,
                                border = BorderStroke(1.dp, palette.border)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = CharcoalInk,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = memberName.take(1).uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GoldAccent
                                            )
                                        }
                                    }
                                    Text(
                                        text = memberName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                if (mission.resolvedNotes().isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.alertBg,
                        border = BorderStroke(1.dp, palette.alertBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = palette.accentBronze,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = mission.resolvedNotes(),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textPrimary
                            )
                        }
                    }
                }
            }
        }
        }
    }

    if (editorOpen) {
        val nextNum = String.format(java.util.Locale.US, "%03d", missions.size + 1)
        MissionEditorSheet(
            editingMission = editingMission,
            nextSuggestedTag = nextNum,
            candidateRiders = candidateRiders,
            saving = saving,
            error = error,
            onDismiss = { editorOpen = false },
            onSave = { editingId, tag, title, date, startTime, endTime, duration, avgSpeed, totalDist, segments, crewIds, notes, variant, customColor, isDuo ->
                viewModel.saveMission(
                    editingId = editingId,
                    tag = tag,
                    title = title,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    duration = duration,
                    avgSpeed = avgSpeed,
                    totalDist = totalDist,
                    segments = segments,
                    crewIds = crewIds,
                    notes = notes,
                    variant = variant,
                    customColor = customColor,
                    isDuo = isDuo,
                    createdBy = session.user?.uid ?: "super-admin"
                ) {
                    editorOpen = false
                }
            }
        )
    }

    confirmDeleteMission?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDeleteMission = null },
            title = { Text("Delete Mission?") },
            text = { Text("Permanently delete \"${target.resolvedTitle()}\"?") },
            confirmButton = {
                NeoPrimaryButton(
                    text = "Delete",
                    containerColor = palette.errorFg,
                    contentColor = Color.White,
                    onClick = {
                        viewModel.deleteMission(target)
                        confirmDeleteMission = null
                    }
                )
            },
            dismissButton = {
                NeoSecondaryButton(
                    text = "Cancel",
                    onClick = { confirmDeleteMission = null }
                )
            }
        )
    }
}

@Composable
private fun MissionStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalCyclePalette.current
    NeoCard(
        modifier = modifier,
        shadowOffset = 4.dp,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GoldAccent,
                border = BorderStroke(1.5.dp, CharcoalInk),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = CharcoalInk,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted
                )
            }
        }
    }
}

@Composable
private fun MissionMetaChip(icon: ImageVector, text: String, tint: Color) {
    val palette = LocalCyclePalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = palette.textSecondary
        )
    }
}
