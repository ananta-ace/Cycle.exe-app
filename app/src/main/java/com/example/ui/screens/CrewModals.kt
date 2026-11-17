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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FeaturedMember
import com.example.data.model.MemberRating
import com.example.ui.components.CycleAvatar
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoPrimaryButton
import com.example.ui.components.NeoSecondaryButton
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LocalCyclePalette
import com.example.ui.viewmodel.AppSessionState
import com.example.ui.viewmodel.CrewViewModel
import com.example.util.DateUtils
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemberDetailSheet(
    member: FeaturedMember,
    ratings: List<MemberRating> = emptyList(),
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
                            val effectiveCount = if (member.ratingCount > 0L) member.ratingCount else ratings.size.toLong()
                            val effectiveAverage = when {
                                member.ratingAverage > 0.0 -> member.ratingAverage
                                ratings.isNotEmpty() -> ((ratings.map { it.rating }.average()) * 10.0).roundToInt() / 10.0
                                else -> 0.0
                            }
                            val hasRating = effectiveAverage > 0.0 || effectiveCount > 0L
                            val countText = if (effectiveCount == 1L) "1 rating" else "$effectiveCount ratings"
                            Text(
                                text = if (hasRating) {
                                    if (effectiveCount > 0L) {
                                        String.format(Locale.US, "%.1f / 5.0 (%s)", effectiveAverage, countText)
                                    } else {
                                        String.format(Locale.US, "%.1f / 5.0", effectiveAverage)
                                    }
                                } else {
                                    "Not rated yet"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontStyle = if (hasRating) FontStyle.Normal else FontStyle.Italic,
                                color = if (hasRating) palette.textPrimary else palette.textMuted
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

            if (ratings.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "COMMUNITY REVIEWS (${ratings.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textMuted
                    )
                    ratings.take(10).forEach { review ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.cardSurface,
                            border = BorderStroke(1.dp, palette.dashedBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = review.raterName.ifBlank { "Community Rider" },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.textPrimary
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        (1..5).forEach { idx ->
                                            Icon(
                                                imageVector = if (idx <= review.rating) Icons.Filled.Star else Icons.Filled.StarOutline,
                                                contentDescription = null,
                                                tint = if (idx <= review.rating) GoldAccent else palette.textMuted,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                                if (review.comment.isNotBlank()) {
                                    Text(
                                        text = "\"${review.comment}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                }
                                val dateStr = DateUtils.formatShortDate(review.updatedAt.ifBlank { review.createdAt })
                                if (dateStr.isNotBlank()) {
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textMuted
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
    onDismiss: () -> Unit,
    onOpenSignIn: () -> Unit = {}
) {
    val palette = LocalCyclePalette.current
    val ratingUi by viewModel.ratingUiState.collectAsStateWithLifecycle()

    val uid = session.user?.uid.orEmpty()
    val isSelfRating = uid.isNotBlank() && (member.id == uid || member.userId == uid)
    val stateMatchesMember = ratingUi.targetMemberId == member.id
    val existing = if (stateMatchesMember) ratingUi.existingRating else null
    val isFetching = stateMatchesMember && ratingUi.isFetching
    val isSubmitting = stateMatchesMember && ratingUi.isSubmitting
    val error = if (stateMatchesMember) ratingUi.error else null

    var stars by remember(member.id, uid) { mutableIntStateOf(5) }
    var comment by remember(member.id, uid) { mutableStateOf("") }
    var hasUserEdited by remember(member.id, uid) { mutableStateOf(false) }
    var appliedGeneration by remember(member.id, uid) { mutableLongStateOf(-1L) }
    var confirmRemoveOpen by remember(member.id) { mutableStateOf(false) }

    LaunchedEffect(member.id, uid) {
        if (uid.isNotBlank() && !isSelfRating) {
            viewModel.loadExistingRating(member, uid)
        }
    }

    LaunchedEffect(
        ratingUi.targetMemberId,
        ratingUi.fetchGeneration,
        ratingUi.isFetching,
        ratingUi.existingRating
    ) {
        if (
            ratingUi.targetMemberId == member.id &&
            !ratingUi.isFetching &&
            ratingUi.fetchGeneration > 0L &&
            appliedGeneration != ratingUi.fetchGeneration
        ) {
            if (!hasUserEdited) {
                val found = ratingUi.existingRating
                if (found != null) {
                    stars = found.rating.coerceIn(1, 5)
                    comment = found.comment
                } else {
                    stars = 5
                    comment = ""
                }
            }
            appliedGeneration = ratingUi.fetchGeneration
        }
    }

    val starDescriptor = when (stars) {
        1 -> "1 Star — Needs Improvement"
        2 -> "2 Stars — Fair Leader"
        3 -> "3 Stars — Good Rider"
        4 -> "4 Stars — Great Team Leader!"
        else -> "5 Stars — Outstanding Cycling Leader!"
    }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        NeoCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .widthIn(max = 460.dp),
            shadowOffset = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = { if (!isSubmitting) onDismiss() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(48.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close rating dialog")
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CycleAvatar(
                            photoUrl = member.img,
                            name = member.name,
                            size = 64.dp,
                            borderColor = GoldAccent
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Rate ${member.name}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = palette.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = member.title.ifBlank { "Admin Member" },
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.accentBronze,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Community feedback helps highlight friendly, supportive group leaders!",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                HorizontalDivider(color = palette.dashedBorder, modifier = Modifier.padding(vertical = 12.dp))

                if (uid.isBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.alertBg,
                        border = BorderStroke(1.5.dp, palette.alertBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Sign in with Google to rate ${member.name} and share feedback with the crew.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeoSecondaryButton(
                            text = "Cancel",
                            onClick = onDismiss,
                            modifier = Modifier.weight(0.9f)
                        )
                        NeoPrimaryButton(
                            text = "Sign In",
                            onClick = {
                                onDismiss()
                                onOpenSignIn()
                            },
                            modifier = Modifier.weight(1.4f)
                        )
                    }
                } else if (isSelfRating) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.alertBg,
                        border = BorderStroke(1.5.dp, palette.alertBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = palette.accentBronze,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "You cannot rate yourself. Ratings are submitted by other registered community members.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    NeoSecondaryButton(
                        text = "Close",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    if (isFetching) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            color = GoldAccent
                        )
                    }

                    Text(
                        text = "SELECT RATING (1 TO 5 STARS)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        (1..5).forEach { value ->
                            IconButton(
                                onClick = {
                                    stars = value
                                    hasUserEdited = true
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("rating_star_$value")
                            ) {
                                Icon(
                                    imageVector = if (value <= stars) Icons.Filled.Star else Icons.Filled.StarOutline,
                                    contentDescription = "$value stars",
                                    tint = if (value <= stars) GoldAccent else palette.textMuted,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = starDescriptor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = comment,
                        onValueChange = {
                            if (it.length <= 300) {
                                comment = it
                                hasUserEdited = true
                            }
                        },
                        enabled = !isSubmitting,
                        label = { Text("Optional Feedback / Comment") },
                        placeholder = {
                            Text("Share a short note about this member's ride support or community leadership...")
                        },
                        supportingText = {
                            Text("${comment.length} / 300")
                        },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rating_comment_input")
                    )

                    if (!error.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.errorBg,
                            border = BorderStroke(1.5.dp, palette.errorFg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.errorFg,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeoSecondaryButton(
                            text = "Cancel",
                            enabled = !isSubmitting,
                            onClick = onDismiss,
                            modifier = Modifier.weight(0.9f)
                        )
                        NeoPrimaryButton(
                            text = when {
                                isSubmitting -> "Saving..."
                                existing != null -> "Update Rating"
                                else -> "Submit Rating"
                            },
                            enabled = !isSubmitting,
                            loading = isSubmitting,
                            leadingIcon = if (!isSubmitting) {
                                {
                                    Icon(
                                        imageVector = if (existing != null) Icons.Filled.CheckCircle else Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = CharcoalInk,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                null
                            },
                            onClick = {
                                val raterName = session.profile?.displayName?.ifBlank { null }
                                    ?: session.user?.displayName
                                    ?: "Rider"
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
                                .weight(1.5f)
                                .testTag("submit_rating_button")
                        )
                    }

                    if (existing != null && !isFetching) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(
                                enabled = !isSubmitting,
                                onClick = { confirmRemoveOpen = true },
                                modifier = Modifier.testTag("remove_rating_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = palette.errorFg,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Remove My Rating",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.errorFg
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmRemoveOpen) {
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) confirmRemoveOpen = false },
            title = { Text("Remove Rating?") },
            text = { Text("Are you sure you want to remove your rating for ${member.name}?") },
            confirmButton = {
                TextButton(
                    enabled = !isSubmitting,
                    onClick = {
                        confirmRemoveOpen = false
                        viewModel.deleteRating(member, uid, onSuccess = onDismiss)
                    }
                ) {
                    Text("Remove", color = palette.errorFg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSubmitting,
                    onClick = { confirmRemoveOpen = false }
                ) {
                    Text("Keep Rating")
                }
            }
        )
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
