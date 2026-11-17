package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MissionReport
import com.example.data.model.MissionSegment
import com.example.ui.components.NeoPrimaryButton
import com.example.ui.components.NeoSecondaryButton
import com.example.ui.theme.LocalCyclePalette
import com.example.ui.viewmodel.CandidateRider
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MissionEditorSheet(
    editingMission: MissionReport?,
    nextSuggestedTag: String,
    candidateRiders: List<CandidateRider>,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (
        editingId: String?,
        tag: String,
        title: String,
        date: String,
        startTime: String,
        endTime: String,
        duration: String,
        avgSpeed: String,
        totalDist: String,
        segments: List<MissionSegment>,
        crewIds: List<String>,
        notes: String,
        variant: String,
        customColor: String?,
        isDuo: Boolean
    ) -> Unit
) {
    val palette = LocalCyclePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var tag by remember(editingMission) { mutableStateOf(editingMission?.resolvedTag() ?: nextSuggestedTag) }
    var title by remember(editingMission) { mutableStateOf(editingMission?.resolvedTitle() ?: "") }
    var date by remember(editingMission) { mutableStateOf(editingMission?.date?.ifBlank { DateUtils.todayYmd() } ?: DateUtils.todayYmd()) }
    var startTime by remember(editingMission) { mutableStateOf(editingMission?.startTime ?: "03:30 PM") }
    var endTime by remember(editingMission) { mutableStateOf(editingMission?.endTime ?: "06:30 PM") }
    var duration by remember(editingMission) { mutableStateOf(editingMission?.duration ?: "") }
    var avgSpeed by remember(editingMission) { mutableStateOf(editingMission?.avgSpeed ?: "~12.5 km/h") }
    var totalDist by remember(editingMission) { mutableStateOf(editingMission?.totalDist ?: "0.0 km") }
    var notes by remember(editingMission) { mutableStateOf(editingMission?.resolvedNotes() ?: "") }
    var variant by remember(editingMission) { mutableStateOf(editingMission?.variant ?: "default") }
    var customColor by remember(editingMission) { mutableStateOf(editingMission?.customColor ?: "#7C3AED") }
    var isDuo by remember(editingMission) { mutableStateOf(editingMission?.isDuo ?: false) }

    val segments = remember(editingMission) {
        mutableStateListOf<MissionSegment>().apply {
            val existing = editingMission?.segments.orEmpty()
            if (existing.isEmpty()) {
                add(MissionSegment(1, "", "", ""))
            } else {
                addAll(existing)
            }
        }
    }

    val selectedCrew = remember(editingMission) {
        mutableStateListOf<String>().apply {
            addAll(editingMission?.crewIds.orEmpty())
        }
    }
    var customCrewName by remember { mutableStateOf("") }

    LaunchedEffect(startTime, endTime) {
        val computed = DateUtils.computeDurationDisplay(startTime, endTime)
        if (computed.isNotBlank()) duration = computed
    }

    LaunchedEffect(segments.toList()) {
        val sum = segments.sumOf { seg ->
            Regex("([0-9]+\\.?[0-9]*)").find(seg.distance)?.value?.toDoubleOrNull() ?: 0.0
        }
        if (sum > 0.0) {
            totalDist = String.format(java.util.Locale.US, "%.1f km", sum)
        }
    }

    val colorPresets = listOf("#7C3AED", "#0284C7", "#E11D48", "#D97706", "#059669", "#1E1E1E")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingMission == null) "Create Mission Report" else "Edit Mission ${editingMission.resolvedTag()}",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close editor")
                }
            }

            if (!error.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.errorBg,
                    border = BorderStroke(1.5.dp, palette.errorFg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = palette.errorFg,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Tag *") },
                    singleLine = true,
                    modifier = Modifier.weight(0.32f)
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Mission Title *") },
                    singleLine = true,
                    modifier = Modifier.weight(0.68f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = avgSpeed,
                    onValueChange = { avgSpeed = it },
                    label = { Text("Avg Speed") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Start Time") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("End Time") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = totalDist,
                    onValueChange = { totalDist = it },
                    label = { Text("Total Dist") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = palette.dashedBorder)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Route Segments (${segments.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary
                )
                NeoSecondaryButton(
                    text = "Add Leg",
                    onClick = {
                        segments.add(MissionSegment(segments.size + 1, "", "", ""))
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }

            segments.forEachIndexed { idx, seg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = seg.from,
                        onValueChange = { segments[idx] = seg.copy(from = it, num = idx + 1) },
                        label = { Text("From") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = seg.to,
                        onValueChange = { segments[idx] = seg.copy(to = it, num = idx + 1) },
                        label = { Text("To") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = seg.distance,
                        onValueChange = { segments[idx] = seg.copy(distance = it, num = idx + 1) },
                        label = { Text("Dist") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                    if (segments.size > 1) {
                        IconButton(
                            onClick = { segments.removeAt(idx) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Remove leg",
                                tint = palette.errorFg
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = palette.dashedBorder)
            Text(
                text = "Crew Roster (${selectedCrew.size})",
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary
            )
            if (candidateRiders.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    candidateRiders.forEach { rider ->
                        val chosen = selectedCrew.contains(rider.name)
                        FilterChip(
                            selected = chosen,
                            onClick = {
                                if (chosen) selectedCrew.remove(rider.name)
                                else selectedCrew.add(rider.name)
                            },
                            label = { Text(rider.name) }
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customCrewName,
                    onValueChange = { customCrewName = it },
                    label = { Text("Add custom rider name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                NeoSecondaryButton(
                    text = "Add",
                    enabled = customCrewName.isNotBlank(),
                    onClick = {
                        val clean = customCrewName.trim()
                        if (clean.isNotEmpty() && !selectedCrew.contains(clean)) {
                            selectedCrew.add(clean)
                        }
                        customCrewName = ""
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isDuo, onCheckedChange = { isDuo = it })
                Text("Duo Ride Highlight", style = MaterialTheme.typography.bodyMedium)
            }

            Text(
                text = "Card Style Variant",
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("default" to "Charcoal", "alt-accent" to "Amber", "green-accent" to "Green", "custom" to "Custom").forEach { (key, label) ->
                    FilterChip(
                        selected = variant == key,
                        onClick = { variant = key },
                        label = { Text(label) }
                    )
                }
            }

            if (variant == "custom") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorPresets.forEach { hex ->
                        val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { Color.Magenta }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { customColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (customColor.equals(hex, ignoreCase = true)) {
                                Icon(Icons.Filled.Check, contentDescription = "Selected color", tint = Color.White)
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Mission Notes / Disclaimer") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                NeoSecondaryButton(text = "Cancel", onClick = onDismiss)
                NeoPrimaryButton(
                    text = if (editingMission == null) "Publish Mission" else "Save Mission",
                    loading = saving,
                    onClick = {
                        onSave(
                            editingMission?.id,
                            tag,
                            title,
                            date,
                            startTime,
                            endTime,
                            duration,
                            avgSpeed,
                            totalDist,
                            segments.toList(),
                            selectedCrew.toList(),
                            notes,
                            variant,
                            if (variant == "custom") customColor else null,
                            isDuo
                        )
                    },
                    modifier = Modifier.testTag("save_mission_button")
                )
            }
        }
    }
}
