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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FeaturedMember
import com.example.ui.components.CycleAvatar
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoPrimaryButton
import com.example.ui.components.NeoSecondaryButton
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LocalCyclePalette
import com.example.ui.viewmodel.AppSessionState
import com.example.ui.viewmodel.CrewViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemberDetailSheet(
    member: FeaturedMember,
    onDismiss: () -> Unit,
    onRateClick: () -> Unit
) {
    val palette = LocalCyclePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    CycleAvatar(
                        photoUrl = member.img,
                        name = member.name,
                        size = 72.dp,
                        borderColor = GoldAccent
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = palette.textPrimary
                            )
                            if (member.active) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Active crew",
                                    tint = palette.accentGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.chipBg,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = member.title.ifBlank { "Cycle.exe Crew" },
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close rider details")
                }
            }

            // Rating & Status Row
            NeoCard(
                containerColor = palette.cardSurfaceAlt,
                shadowOffset = 3.dp,
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = if (member.ratingCount > 0) {
                                    String.format(Locale.US, "%.1f / 5.0 (%d ratings)", member.ratingAverage, member.ratingCount)
                                } else {
                                    "Not rated yet"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary
                            )
                            Text(
                                text = member.attend.ifBlank { if (member.active) "Active Rider" else "Squad Alumni" },
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                    NeoPrimaryButton(
                        text = "Rate",
                        onClick = onRateClick,
                        modifier = Modifier.testTag("sheet_rate_member_button")
                    )
                }
            }

            if (member.bio.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "RIDER BIO",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textMuted
                    )
                    Text(
                        text = member.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                }
            }

            if (member.badges.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "EARNED BADGES (${member.badges.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textMuted
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        member.badges.forEach { badge ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = GoldAccent.copy(alpha = 0.2f),
                                border = BorderStroke(1.5.dp, GoldAccent)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.EmojiEvents,
                                        contentDescription = null,
                                        tint = palette.accentBronze,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = badge,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (member.completedRaces.isNotEmpty() || member.completedRacesCount > 0L) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "COMPLETED EXPEDITIONS (${member.resolvedRaceCount()})",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textMuted
                    )
                    if (member.completedRaces.isEmpty()) {
                        Text(
                            text = "${member.completedRacesCount} logged group expeditions",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    } else {
                        member.completedRaces.forEach { race ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.cardSurface,
                                border = BorderStroke(1.dp, palette.dashedBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Flag,
                                        contentDescription = null,
                                        tint = palette.accentGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = race,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.textPrimary
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

@Composable
fun RatingDialog(
    member: FeaturedMember,
    session: AppSessionState,
    viewModel: CrewViewModel,
    onDismiss: () -> Unit
) {
    val palette = LocalCyclePalette.current
    val existing by viewModel.existingRating.collectAsStateWithLifecycle()
    val loading by viewModel.ratingLoading.collectAsStateWithLifecycle()
    val error by viewModel.ratingError.collectAsStateWithLifecycle()

    val uid = session.user?.uid.orEmpty()
    val isSelfRating = member.userId.isNotBlank() && member.userId == uid

    var stars by remember(member.id) { mutableIntStateOf(5) }
    var comment by remember(member.id) { mutableStateOf("") }
    var initialized by remember(member.id) { mutableStateOf(false) }

    LaunchedEffect(member.id, uid) {
        if (uid.isNotBlank() && !isSelfRating) {
            viewModel.loadExistingRating(member.id, uid)
        }
    }

    LaunchedEffect(existing) {
        if (!initialized && existing != null) {
            stars = existing!!.rating.coerceIn(1, 5)
            comment = existing!!.comment
            initialized = true
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            shadowOffset = 6.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = GoldAccent)
                    Text(
                        text = if (existing != null) "Update Rating" else "Rate Rider",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close rating dialog")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                CycleAvatar(photoUrl = member.img, name = member.name, size = 44.dp)
                Column {
                    Text(member.name, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Text(member.title, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                }
            }

            HorizontalDivider(color = palette.dashedBorder, modifier = Modifier.padding(vertical = 8.dp))

            if (isSelfRating) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.errorBg,
                    border = BorderStroke(1.dp, palette.errorFg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "You cannot rate your own featured rider profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.errorFg,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                NeoSecondaryButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Select Rating ($stars / 5)",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textPrimary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (1..5).forEach { value ->
                        IconButton(
                            onClick = { stars = value },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("rating_star_$value")
                        ) {
                            Icon(
                                imageVector = if (value <= stars) Icons.Filled.Star else Icons.Filled.StarOutline,
                                contentDescription = "$value stars",
                                tint = if (value <= stars) GoldAccent else palette.textMuted,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { if (it.length <= 300) comment = it },
                    label = { Text("Feedback (optional)") },
                    placeholder = { Text("Great paceline leader and steady tempo!") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rating_comment_input")
                )

                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.errorFg
                    )
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (existing != null) {
                        NeoSecondaryButton(
                            text = "Remove",
                            contentColor = palette.errorFg,
                            enabled = !loading,
                            onClick = {
                                viewModel.deleteRating(member, uid, onSuccess = onDismiss)
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = palette.errorFg, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                    NeoPrimaryButton(
                        text = if (existing != null) "Update Rating" else "Submit Rating",
                        loading = loading,
                        onClick = {
                            val raterName = session.profile?.displayName?.ifBlank { null }
                                ?: session.user?.displayName
                                ?: "Cyclist"
                            viewModel.submitRating(
                                targetMember = member,
                                raterId = uid,
                                raterName = raterName,
                                rating = stars,
                                comment = comment.trim(),
                                onSuccess = onDismiss
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_rating_button")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ApplicationSheet(
    session: AppSessionState,
    viewModel: CrewViewModel,
    onDismiss: () -> Unit
) {
    val palette = LocalCyclePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val existingApp by viewModel.myApplication.collectAsStateWithLifecycle()
    val busy by viewModel.appBusy.collectAsStateWithLifecycle()
    val error by viewModel.appError.collectAsStateWithLifecycle()
    val success by viewModel.appSuccess.collectAsStateWithLifecycle()

    val uid = session.user?.uid.orEmpty()
    var reason by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var requestedTitle by remember { mutableStateOf("Cycle.exe Admin Member") }
    var populated by remember { mutableStateOf(false) }

    val titleSuggestions = remember {
        listOf(
            "Cycle.exe Admin Member",
            "Chill Guy",
            "Road Captain",
            "Climbing Specialist",
            "Endurance Pacer"
        )
    }

    LaunchedEffect(uid) {
        if (uid.isNotBlank()) viewModel.loadMyApplication(uid)
    }

    LaunchedEffect(existingApp) {
        if (!populated && existingApp != null) {
            reason = existingApp!!.reason
            experience = existingApp!!.cyclingExperience
            if (existingApp!!.requestedTitle.isNotBlank()) {
                requestedTitle = existingApp!!.requestedTitle
            }
            populated = true
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Icon(Icons.Filled.PedalBike, contentDescription = null, tint = CharcoalInk)
                        }
                    }
                    Column {
                        Text(
                            text = "Admin Membership Application",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Unlock posting & community leadership perks",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close application sheet")
                }
            }

            existingApp?.let { app ->
                val (bg, border, icon, label) = when (app.status.uppercase()) {
                    "APPROVED" -> Quad(palette.accentGreenBg, palette.accentGreen, Icons.Filled.CheckCircle, "Application Approved")
                    "REJECTED" -> Quad(palette.errorBg, palette.errorFg, Icons.Filled.Warning, "Application Rejected (You can update & re-apply)")
                    else -> Quad(palette.alertBg, palette.alertBorder, Icons.Filled.HourglassTop, "Application Pending Review")
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = bg,
                    border = BorderStroke(1.5.dp, border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(icon, contentDescription = null, tint = border)
                        Column {
                            Text(label, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                            if (!app.rejectionReason.isNullOrBlank()) {
                                Text(
                                    text = "Reason: ${app.rejectionReason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            if (success) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.accentGreenBg,
                    border = BorderStroke(1.dp, palette.accentGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Your application has been submitted! Moderators and the Super Admin will review it shortly.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentGreen,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Why do you want to join Cycle.exe Admin Members?") },
                placeholder = { Text("Share how you want to contribute to rides & posts...") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("application_reason_input")
            )

            OutlinedTextField(
                value = experience,
                onValueChange = { experience = it },
                label = { Text("Cycling Background & Experience") },
                placeholder = { Text("Weekly mileage, group ride history, favorite routes...") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("application_experience_input")
            )

            OutlinedTextField(
                value = requestedTitle,
                onValueChange = { if (it.length <= 50) requestedTitle = it },
                label = { Text("Preferred Community Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("application_title_input")
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                titleSuggestions.forEach { preset ->
                    FilterChip(
                        selected = requestedTitle == preset,
                        onClick = { requestedTitle = preset },
                        label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldAccent,
                            selectedLabelColor = CharcoalInk
                        )
                    )
                }
            }

            if (!error.isNullOrBlank()) {
                Text(
                    text = error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.errorFg
                )
            }

            if (existingApp?.status?.uppercase() != "APPROVED") {
                NeoPrimaryButton(
                    text = if (existingApp != null) "Update & Re-Submit Application" else "Submit Membership Application",
                    loading = busy,
                    onClick = {
                        val name = session.profile?.displayName?.ifBlank { null }
                            ?: session.user?.displayName
                            ?: "Cyclist"
                        val email = session.user?.email.orEmpty()
                        val photo = session.profile?.photoURL?.ifBlank { null }
                            ?: session.user?.photoUrl?.toString()
                            ?: ""
                        viewModel.submitApplication(
                            uid = uid,
                            applicantName = name,
                            applicantEmail = email,
                            applicantPhotoURL = photo,
                            reason = reason,
                            cyclingExperience = experience,
                            requestedTitle = requestedTitle
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = CharcoalInk)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_application_button")
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
