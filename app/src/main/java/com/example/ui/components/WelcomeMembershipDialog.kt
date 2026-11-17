package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PedalBike
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserProfile
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LocalCyclePalette

@Composable
fun WelcomeMembershipDialog(
    uid: String,
    profile: UserProfile?,
    isSuperAdmin: Boolean,
    dismissedKeys: Set<String> = emptySet(),
    onDismissWithToken: (String) -> Unit
) {
    if (profile == null || uid.isBlank()) return

    val isAdminMember = (profile.isAdminMember || profile.adminMembershipStatus == "APPROVED") && !profile.isSuspended
    val isRegularAdmin = profile.role == "ADMIN" && !isSuperAdmin && !profile.isSuspended

    val adminMemberToken = "SEEN_ADMIN_MEMBER_v2"
    val adminToken = "SEEN_ADMIN_TEAM_v2"

    val (shouldShow, token, isTrophy, title, message) = when {
        isRegularAdmin &&
            profile.welcomeSeenForRole != adminToken &&
            "$uid:$adminToken" !in dismissedKeys -> WelcomeData(
            true,
            adminToken,
            true,
            "Welcome to the Cycle.exe Admin Team!",
            "Your Admin access has been granted by the Super Admin. Please use your moderation and community management permissions responsibly."
        )
        isAdminMember &&
            profile.welcomeSeenForRole != adminMemberToken &&
            "$uid:$adminMemberToken" !in dismissedKeys -> WelcomeData(
            true,
            adminMemberToken,
            false,
            "Welcome to Cycle.exe Admin Members!",
            "Congratulations! Your Admin Membership has been officially approved. You can now publish posts, join discussions, select your custom community title, and help shape our rides!"
        )
        else -> return
    }

    var locallyDismissed by remember(uid, token) { mutableStateOf(false) }
    if (!shouldShow || locallyDismissed) return
    val palette = LocalCyclePalette.current

    val handleDismiss = {
        locallyDismissed = true
        onDismissWithToken(token)
    }

    Dialog(onDismissRequest = handleDismiss) {
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            shadowOffset = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = GoldAccent,
                    border = BorderStroke(2.5.dp, CharcoalInk),
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isTrophy) Icons.Filled.EmojiEvents else Icons.Filled.PedalBike,
                            contentDescription = null,
                            tint = CharcoalInk,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center
                )
                if (isAdminMember) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.alertBg,
                        border = BorderStroke(1.5.dp, palette.alertBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Pro Tip: Choose your custom title!",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Open your Profile tab to customize your rider bio and showcase your title.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                NeoPrimaryButton(
                    text = "Continue to Community",
                    onClick = handleDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("welcome_continue_button"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = CharcoalInk
                        )
                    }
                )
            }
        }
    }
}

private data class WelcomeData(
    val show: Boolean,
    val token: String,
    val isTrophy: Boolean,
    val title: String,
    val message: String
)
