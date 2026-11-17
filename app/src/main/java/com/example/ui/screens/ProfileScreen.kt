package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.CycleAvatar
import com.example.ui.components.HeroSectionHeader
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoPrimaryButton
import com.example.ui.components.NeoSecondaryButton
import com.example.ui.components.RoleBadge
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCyclePalette
import com.example.ui.viewmodel.AppSessionState
import com.example.util.DateUtils

@Composable
fun ProfileScreen(
    session: AppSessionState,
    onSignInClick: () -> Unit,
    onBrowserSignInClick: () -> Unit,
    onUploadAvatar: (android.net.Uri) -> Unit,
    onSaveProfile: (displayName: String, bio: String, photoUrl: String?, customTitle: String?) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenApplication: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onOpenSuperAdminDashboard: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCyclePalette.current
    val user = session.user
    val profile = session.profile

    val displayName = profile?.displayName?.ifBlank { null } ?: user?.displayName ?: "Cyclist"
    val photoUrl = profile?.photoURL?.ifBlank { null } ?: user?.photoUrl?.toString().orEmpty()
    val email = user?.email.orEmpty()
    val bio = profile?.bio.orEmpty()
    val customTitle = profile?.customTitle.orEmpty()

    var editing by remember { mutableStateOf(false) }
    var nameInput by remember(displayName, editing) { mutableStateOf(displayName) }
    var bioInput by remember(bio, editing) { mutableStateOf(bio) }
    var photoInput by remember(photoUrl, editing) { mutableStateOf(photoUrl) }
    var titleInput by remember(customTitle, editing) { mutableStateOf(customTitle) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUploadAvatar(uri)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroSectionHeader(
                badgeText = "Rider Passport",
                title = "Your Profile & Settings",
                subtitle = if (user == null) {
                    "Sign in with Google to customize your profile, rate riders, post stories, and track applications."
                } else {
                    "Manage your rider bio, appearance, permissions, and account access."
                }
            )
        }

        if (user == null) {
            item {
                NeoCard(shadowOffset = 6.dp) {
                    Text(
                        text = "Sign in to Cycle.exe",
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.textPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Browsing Missions, Gallery, Feed & Crew is always open. Sign in with your Google account to unlock member actions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    if (!session.signInNotice.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.alertBg,
                            border = BorderStroke(1.5.dp, palette.alertBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Language, contentDescription = null, tint = palette.accentBronze)
                                Text(
                                    text = session.signInNotice,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    if (!session.signInError.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.errorBg,
                            border = BorderStroke(1.5.dp, palette.errorFg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = palette.errorFg)
                                Text(
                                    text = session.signInError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.errorFg,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    NeoPrimaryButton(
                        text = if (session.authBusy) "Signing in..." else "Continue with Google",
                        loading = session.authBusy,
                        onClick = onSignInClick,
                        leadingIcon = { Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = CharcoalInk) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_google_sign_in_button")
                    )
                    Spacer(Modifier.height(10.dp))
                    NeoSecondaryButton(
                        text = "Browser Google Sign-In",
                        enabled = !session.authBusy,
                        onClick = onBrowserSignInClick,
                        leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_browser_sign_in_button")
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Browser Sign-In works reliably across all Android devices and APK builds.",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textMuted
                    )
                }
            }
        } else {
            item {
                NeoCard(shadowOffset = 6.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        CycleAvatar(
                            photoUrl = photoUrl,
                            name = displayName,
                            size = 84.dp,
                            borderColor = GoldAccent
                        )
                        Surface(
                            onClick = {
                                avatarPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = CircleShape,
                            color = GoldAccent,
                            border = BorderStroke(2.dp, CharcoalInk),
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("change_avatar_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (session.avatarProgress != null) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = CharcoalInk
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.CameraAlt,
                                        contentDescription = "Change profile photo",
                                        tint = CharcoalInk,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = palette.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RoleBadge(role = session.role.key)
                            if (customTitle.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = palette.chipBg,
                                    border = BorderStroke(1.dp, palette.border)
                                ) {
                                    Text(
                                        text = customTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        if (!profile?.createdAt.isNullOrBlank()) {
                            Text(
                                text = "Joined ${DateUtils.formatShortDate(profile?.createdAt)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textMuted
                            )
                        }
                    }
                }

                if (session.avatarProgress != null) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (session.avatarProgress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = GoldAccent
                    )
                }

                if (!session.profileMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.accentGreenBg,
                        border = BorderStroke(1.dp, palette.accentGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = session.profileMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentGreen,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                if (!session.profileError.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.errorBg,
                        border = BorderStroke(1.dp, palette.errorFg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = session.profileError,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.errorFg,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.cardSurfaceAlt,
                    border = BorderStroke(1.dp, palette.dashedBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "RIDER BIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = bio.ifBlank { "No bio added yet. Tap Edit Profile to introduce yourself to the peloton!" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (bio.isBlank()) palette.textMuted else palette.textPrimary
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                if (!editing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeoPrimaryButton(
                            text = "Edit Profile",
                            onClick = { editing = true },
                            leadingIcon = {
                                Icon(Icons.Filled.Edit, contentDescription = null, tint = CharcoalInk, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_profile_button")
                        )
                        NeoSecondaryButton(
                            text = "Sign Out",
                            contentColor = palette.errorFg,
                            onClick = onSignOut,
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = palette.errorFg, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        )
                    }
                } else {
                    HorizontalDivider(color = palette.dashedBorder, modifier = Modifier.padding(vertical = 8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { if (it.length <= 80) nameInput = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_name_input")
                        )
                        OutlinedTextField(
                            value = bioInput,
                            onValueChange = { if (it.length <= 300) bioInput = it },
                            label = { Text("Rider Bio") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_bio_input")
                        )
                        if (session.isMember || session.isAdmin || session.isSuperAdmin) {
                            OutlinedTextField(
                                value = titleInput,
                                onValueChange = { if (it.length <= 50) titleInput = it },
                                label = { Text("Custom Community Title") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_title_input")
                            )
                        }
                        OutlinedTextField(
                            value = photoInput,
                            onValueChange = { photoInput = it },
                            label = { Text("Photo URL (or use camera button above)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NeoSecondaryButton(
                                text = "Cancel",
                                onClick = { editing = false },
                                modifier = Modifier.weight(1f)
                            )
                            NeoPrimaryButton(
                                text = "Save Changes",
                                onClick = {
                                    onSaveProfile(
                                        nameInput,
                                        bioInput,
                                        photoInput.trim().ifBlank { null },
                                        if (session.isMember || session.isAdmin || session.isSuperAdmin) titleInput else null
                                    )
                                    editing = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Save, contentDescription = null, tint = CharcoalInk, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_profile_button")
                            )
                        }
                    }
                }
            }
        }
        }

        // Theme Preference Card
        item {
            NeoCard(shadowOffset = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GoldAccent,
                            border = BorderStroke(1.5.dp, CharcoalInk),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (session.isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                    contentDescription = null,
                                    tint = CharcoalInk
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Night Ride Dark Mode",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = if (session.isDarkTheme) "Obsidian Asphalt theme active" else "Warm Paper Neo-Brutalist theme active",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                    Switch(
                        checked = session.isDarkTheme,
                        onCheckedChange = { onToggleTheme() },
                        modifier = Modifier.testTag("theme_switch")
                    )
                }
            }
        }

        // Role & Command Center Quick Links
        if (session.isSuperAdmin) {
            item {
                NeoCard(
                    containerColor = CharcoalInk,
                    borderColor = GoldAccent,
                    shadowColor = GoldAccent,
                    onClick = onOpenSuperAdminDashboard
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = GoldAccent,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = CharcoalInk)
                                }
                            }
                            Column {
                                Text(
                                    text = "Super Admin Control Hub",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GoldAccent
                                )
                                Text(
                                    text = "Users, Roles, Direct Notices, Featured Crew, Invites & Audit Logs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD4D4D8)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Super Admin Hub",
                            tint = GoldAccent
                        )
                    }
                }
            }
        }

        if (session.isAdmin) {
            item {
                NeoCard(
                    containerColor = palette.alertBg,
                    borderColor = palette.alertBorder,
                    shadowColor = palette.alertBorder,
                    onClick = onOpenAdminDashboard
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = CharcoalInk,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = GoldAccent)
                                }
                            }
                            Column {
                                Text(
                                    text = "Moderator Dashboard",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Review Admin Membership applications & content reports",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Moderator Dashboard",
                            tint = palette.textPrimary
                        )
                    }
                }
            }
        }

        if (user != null && !session.isMember && !session.isSuperAdmin && !session.isSuspended) {
            item {
                NeoCard(shadowOffset = 4.dp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = palette.accentBronze)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Membership Status: ${profile?.adminMembershipStatus ?: "NONE"}",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Apply for Admin Membership to post stories & join ride governance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    NeoPrimaryButton(
                        text = if (profile?.adminMembershipStatus == "PENDING") "View Application" else "Apply for Admin Membership",
                        onClick = onOpenApplication,
                        leadingIcon = {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = CharcoalInk)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_apply_membership_button")
                    )
                }
            }
        }

        if (session.isSuperAdmin) {
            item {
                NeoCard(containerColor = palette.cardSurfaceAlt, shadowOffset = 3.dp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = palette.accentGreen)
                        Text(
                            text = "Recognized Platform Super Admin ($email)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                    }
                }
            }
        }
    }
}
