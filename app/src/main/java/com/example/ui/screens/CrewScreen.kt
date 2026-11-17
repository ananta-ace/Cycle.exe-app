package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FeaturedMember
import com.example.ui.components.CycleAvatar
import com.example.ui.components.HeroSectionHeader
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoPrimaryButton
import com.example.ui.components.NeoSecondaryButton
import com.example.ui.components.SyncErrorCard
import com.example.ui.components.SyncLoadingCard
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LocalCyclePalette
import com.example.ui.viewmodel.AppSessionState
import com.example.ui.viewmodel.CrewViewModel
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CrewScreen(
    session: AppSessionState,
    viewModel: CrewViewModel,
    onOpenSignIn: () -> Unit,
    onOpenApplication: () -> Unit,
    onOpenSuperAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCyclePalette.current
    val membersState by viewModel.membersState.collectAsStateWithLifecycle()
    val members = membersState.items

    var selectedMember by remember { mutableStateOf<FeaturedMember?>(null) }
    var ratingMember by remember { mutableStateOf<FeaturedMember?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 900.dp -> 3
            maxWidth >= 580.dp -> 2
            else -> 1
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(span = { GridItemSpan(columns) }) {
                HeroSectionHeader(
                    badgeText = "Official Community Leadership Directory",
                    title = "The Crew & Featured Riders",
                    subtitle = "Meet the dedicated riders who pace the peloton, chart routes, and build Cycle.exe."
                )
            }

            if (!session.isMember && !session.isSuperAdmin && !session.isSuspended) {
                item(span = { GridItemSpan(columns) }) {
                    val isPending = session.profile?.adminMembershipStatus == "PENDING"
                    val isRejected = session.profile?.adminMembershipStatus == "REJECTED"
                    NeoCard(
                        containerColor = palette.alertBg,
                        borderColor = palette.alertBorder,
                        shadowColor = palette.alertBorder,
                        shadowOffset = 4.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = GoldAccent,
                                border = BorderStroke(2.dp, CharcoalInk),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.PedalBike, contentDescription = null, tint = CharcoalInk)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        isPending -> "Application Pending Review"
                                        isRejected -> "Application Status: Rejected"
                                        else -> "Want to become a Cycle.exe Admin Member?"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = when {
                                        isPending -> "Our moderators and Super Admin are reviewing your cycling background."
                                        isRejected -> "You can update your application details and re-apply anytime."
                                        else -> "Apply for Admin Membership to publish rides, photos, and earn featured crew placement."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        NeoPrimaryButton(
                            text = when {
                                session.user == null -> "Sign In to Apply"
                                isPending -> "View Application"
                                isRejected -> "Update & Re-Apply"
                                else -> "Apply for Membership"
                            },
                            onClick = {
                                if (session.user == null) onOpenSignIn() else onOpenApplication()
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Bolt, contentDescription = null, tint = CharcoalInk)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("crew_apply_membership_button")
                        )
                    }
                }
            }

            if (membersState.isLoading && members.isEmpty()) {
                item(span = { GridItemSpan(columns) }) {
                    SyncLoadingCard(title = "Loading Crew Directory...")
                }
            } else if (membersState.error != null && members.isEmpty()) {
                item(span = { GridItemSpan(columns) }) {
                    SyncErrorCard(
                        message = membersState.error.orEmpty(),
                        onRetry = { viewModel.refreshMembers() }
                    )
                }
            } else if (members.isEmpty()) {
                item(span = { GridItemSpan(columns) }) {
                    NeoCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Groups,
                                contentDescription = null,
                                tint = palette.textMuted,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Featured Crew Directory is Empty",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Super Admin can feature registered riders from the Control Hub.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            if (session.isSuperAdmin) {
                                Spacer(Modifier.height(4.dp))
                                NeoPrimaryButton(
                                    text = "Manage Crew in Control Hub",
                                    onClick = onOpenSuperAdmin,
                                    leadingIcon = {
                                        Icon(Icons.Filled.Settings, contentDescription = null, tint = CharcoalInk)
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                items(members, key = { it.id }) { member ->
                    NeoCard(
                        onClick = { selectedMember = member },
                        shadowOffset = 5.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (member.active) palette.accentGreenBg else palette.chipBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (member.active) palette.accentGreen else palette.border
                                )
                            ) {
                                Text(
                                    text = if (member.active) "ACTIVE CREW" else "ALUMNI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (member.active) palette.accentGreen else palette.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            if (member.perfect || member.attend.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (member.perfect) GoldAccent.copy(alpha = 0.25f) else palette.cardSurfaceAlt,
                                    border = BorderStroke(1.dp, if (member.perfect) GoldAccent else palette.dashedBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (member.perfect) {
                                            Icon(
                                                imageVector = Icons.Filled.EmojiEvents,
                                                contentDescription = null,
                                                tint = palette.accentBronze,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Text(
                                            text = member.attend.ifBlank { "Regular" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CycleAvatar(
                                photoUrl = member.img,
                                name = member.name,
                                size = 60.dp,
                                borderColor = if (member.active) GoldAccent else palette.border
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = member.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (member.active) {
                                        Icon(
                                            imageVector = Icons.Filled.Verified,
                                            contentDescription = "Verified leader",
                                            tint = palette.accentGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = member.title.ifBlank { "Cycle.exe Crew" },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.accentBronze,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${member.resolvedRaceCount()} expeditions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textMuted
                                )
                            }
                        }

                        if (member.bio.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = member.bio,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (member.badges.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                member.badges.take(3).forEach { badge ->
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = palette.chipBg,
                                        border = BorderStroke(1.dp, palette.dashedBorder)
                                    ) {
                                        Text(
                                            text = badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (member.badges.size > 3) {
                                    Text(
                                        text = "+${member.badges.size - 3} more",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textMuted,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (member.ratingCount > 0) {
                                        String.format(Locale.US, "%.1f (%d)", member.ratingAverage, member.ratingCount)
                                    } else {
                                        "No ratings"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.textPrimary
                                )
                            }

                            NeoSecondaryButton(
                                text = "Rate Rider",
                                onClick = {
                                    if (session.user == null) onOpenSignIn() else ratingMember = member
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("rate_member_${member.id}")
                            )
                        }
                    }
                }
            }
        }
    }

    selectedMember?.let { member ->
        val fresh = members.firstOrNull { it.id == member.id } ?: member
        MemberDetailSheet(
            member = fresh,
            onDismiss = { selectedMember = null },
            onRateClick = {
                selectedMember = null
                if (session.user == null) onOpenSignIn() else ratingMember = fresh
            }
        )
    }

    ratingMember?.let { member ->
        val fresh = members.firstOrNull { it.id == member.id } ?: member
        RatingDialog(
            member = fresh,
            session = session,
            viewModel = viewModel,
            onDismiss = { ratingMember = null }
        )
    }
}
