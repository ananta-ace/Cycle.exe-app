package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.FeaturedMember
import com.example.data.model.MembershipApplication
import com.example.data.model.UserProfile
import com.example.ui.components.CycleAvatar
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoPrimaryButton
import com.example.ui.components.NeoSecondaryButton
import com.example.ui.components.RoleBadge
import com.example.ui.components.SuperAdminBootstrapBanner
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LocalCyclePalette
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AppSessionState
import java.util.Locale

private enum class SuperAdminTab(val label: String, val icon: ImageVector) {
    FEATURED("Featured", Icons.Filled.Star),
    USERS("Users", Icons.Filled.ManageAccounts),
    NOTICES("Notices", Icons.Filled.Campaign),
    APPLICATIONS("Apps", Icons.Filled.HowToReg),
    ANALYTICS("Analytics", Icons.Filled.Analytics),
    INVITATIONS("Invites", Icons.Filled.Key),
    LOGS("Logs", Icons.Filled.History)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuperAdminDashboardScreen(
    session: AppSessionState,
    viewModel: AdminViewModel,
    onSyncSuperAdmin: suspend () -> Pair<Boolean, String>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val palette = LocalCyclePalette.current
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val featured by viewModel.featuredMembers.collectAsStateWithLifecycle()
    val applications by viewModel.applications.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val allNotifications by viewModel.allNotifications.collectAsStateWithLifecycle()
    val codes by viewModel.invitationCodes.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val latestCode by viewModel.latestGeneratedCode.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.featUploadProgress.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(SuperAdminTab.FEATURED) }
    var userSearch by remember { mutableStateOf("") }
    var appFilter by remember { mutableStateOf("ALL") }
    var featurePickerOpen by remember { mutableStateOf(false) }

    var editingFeatured by remember { mutableStateOf<FeaturedMember?>(null) }
    var featuredEditorOpen by remember { mutableStateOf(false) }
    var inspectingUser by remember { mutableStateOf<UserProfile?>(null) }
    var editingTitleUser by remember { mutableStateOf<UserProfile?>(null) }
    var noticeComposerOpen by remember { mutableStateOf(false) }
    var noticeRecipient by remember { mutableStateOf<UserProfile?>(null) }
    var rejectingApp by remember { mutableStateOf<MembershipApplication?>(null) }
    var rejectReasonInput by remember { mutableStateOf("") }

    val filteredUsers = remember(users, userSearch) {
        val q = userSearch.trim().lowercase()
        if (q.isBlank()) users
        else users.filter {
            it.displayName.lowercase().contains(q) ||
                it.email.lowercase().contains(q) ||
                it.role.lowercase().contains(q)
        }
    }

    val filteredApps = remember(applications, appFilter) {
        when (appFilter) {
            "ALL" -> applications
            "SUSPENDED" -> {
                val suspendedIds = users.filter { it.isSuspended }.map { it.id }.toSet()
                applications.filter { it.userId in suspendedIds }
            }
            else -> applications.filter { it.status == appFilter }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NeoCard(
                containerColor = CharcoalInk,
                borderColor = GoldAccent,
                shadowColor = GoldAccent,
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
                            .testTag("super_admin_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldAccent)
                    }
                    Surface(shape = CircleShape, color = GoldAccent, modifier = Modifier.size(42.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = CharcoalInk)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Super Admin Control Hub", style = MaterialTheme.typography.headlineSmall, color = GoldAccent)
                        Text(
                            text = "${users.size} riders • ${featured.size} featured • ${applications.count { it.status == "PENDING" }} pending apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD4D4D8)
                        )
                    }
                }
            }
        }

        item {
            SuperAdminBootstrapBanner(
                email = session.user?.email,
                emailVerified = session.user?.isEmailVerified == true,
                profile = session.profile,
                firestoreSuperAdminSynced = session.firestoreSuperAdminSynced,
                onSyncClick = onSyncSuperAdmin
            )
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
                        NeoSecondaryButton(text = "OK", onClick = { viewModel.clearStatusMessage() })
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuperAdminTab.entries.forEach { tab ->
                    FilterChip(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        label = { Text(tab.label) },
                        leadingIcon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldAccent,
                            selectedLabelColor = CharcoalInk
                        ),
                        modifier = Modifier.testTag("super_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        when (activeTab) {
            SuperAdminTab.FEATURED -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val unfeaturedUsers = users.filter { u -> featured.none { f -> f.userId == u.id } }
                        Box {
                            NeoSecondaryButton(
                                text = "Feature a Registered User (${unfeaturedUsers.size})",
                                onClick = { featurePickerOpen = true },
                                leadingIcon = { Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = featurePickerOpen,
                                onDismissRequest = { featurePickerOpen = false },
                                modifier = Modifier.heightIn(max = 320.dp)
                            ) {
                                if (unfeaturedUsers.isEmpty()) {
                                    DropdownMenuItem(text = { Text("All registered users are already featured") }, onClick = { featurePickerOpen = false })
                                } else {
                                    unfeaturedUsers.forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text("${u.displayName.ifBlank { "Cyclist" }} (${u.email})") },
                                            onClick = {
                                                featurePickerOpen = false
                                                viewModel.featureRegisteredUser(u) { created ->
                                                    editingFeatured = created
                                                    featuredEditorOpen = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        NeoPrimaryButton(
                            text = "Add Custom Crew Leader",
                            onClick = {
                                editingFeatured = null
                                featuredEditorOpen = true
                            },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, tint = CharcoalInk) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                items(featured, key = { it.id }) { member ->
                    NeoCard(shadowOffset = 4.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CycleAvatar(photoUrl = member.img, name = member.name, size = 48.dp, borderColor = GoldAccent)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("#${member.order} • ${member.name}", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                                Text(member.title, style = MaterialTheme.typography.bodySmall, color = palette.accentBronze)
                                val ratingSummary = if (member.ratingAverage > 0.0 || member.ratingCount > 0L) {
                                    String.format(Locale.US, "★ %.1f (%d)", member.ratingAverage, member.ratingCount)
                                } else {
                                    "No ratings yet"
                                }
                                Text("${member.badges.size} badges • ${member.resolvedRaceCount()} races • $ratingSummary", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                            }
                            IconButton(
                                onClick = {
                                    editingFeatured = member
                                    featuredEditorOpen = true
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit featured member", tint = palette.accentBronze)
                            }
                            IconButton(
                                onClick = { viewModel.deleteFeaturedMember(member.id, member.name) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove featured member", tint = palette.errorFg)
                            }
                        }
                    }
                }
            }

            SuperAdminTab.USERS -> {
                item {
                    OutlinedTextField(
                        value = userSearch,
                        onValueChange = { userSearch = it },
                        placeholder = { Text("Search users by name, email, or role...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(filteredUsers, key = { it.id }) { user ->
                    NeoCard(shadowOffset = 4.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CycleAvatar(photoUrl = user.photoURL, name = user.displayName, size = 44.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.displayName.ifBlank { "Cyclist" }, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                                Text(user.email, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                                Spacer(Modifier.height(4.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    RoleBadge(role = user.role)
                                    if (!user.customTitle.isNullOrBlank()) {
                                        Surface(shape = RoundedCornerShape(50), color = palette.chipBg, border = BorderStroke(1.dp, palette.border)) {
                                            Text(user.customTitle!!, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                        }
                                    }
                                    if (user.isSuspended) {
                                        Surface(shape = RoundedCornerShape(50), color = palette.errorBg, border = BorderStroke(1.dp, palette.errorFg)) {
                                            Text("SUSPENDED", style = MaterialTheme.typography.labelSmall, color = palette.errorFg, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            NeoSecondaryButton(
                                text = "Inspect",
                                onClick = { inspectingUser = user },
                                leadingIcon = { Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(15.dp)) }
                            )
                            NeoSecondaryButton(
                                text = "Title",
                                onClick = { editingTitleUser = user },
                                leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null, modifier = Modifier.size(15.dp)) }
                            )
                            NeoSecondaryButton(
                                text = "Notice",
                                onClick = {
                                    noticeRecipient = user
                                    noticeComposerOpen = true
                                },
                                leadingIcon = { Icon(Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(15.dp)) }
                            )
                        }
                    }
                }
            }

            SuperAdminTab.NOTICES -> {
                item {
                    SuperAdminNoticesSection(
                        session = session,
                        viewModel = viewModel,
                        allNotifications = allNotifications,
                        onOpenComposer = {
                            noticeRecipient = null
                            noticeComposerOpen = true
                        }
                    )
                }
            }

            SuperAdminTab.APPLICATIONS -> {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ALL", "PENDING", "APPROVED", "REJECTED", "SUSPENDED").forEach { f ->
                            FilterChip(
                                selected = appFilter == f,
                                onClick = { appFilter = f },
                                label = { Text(f, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CharcoalInk, selectedLabelColor = GoldAccent)
                            )
                        }
                    }
                }
                items(filteredApps, key = { it.id }) { app ->
                    val targetUser = users.firstOrNull { it.id == app.userId }
                    NeoCard(shadowOffset = 4.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                CycleAvatar(photoUrl = app.applicantPhotoURL, name = app.applicantName.ifBlank { "Rider" }, size = 42.dp)
                                Column {
                                    Text(app.applicantName.ifBlank { "Cyclist" }, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                                    Text(app.applicantEmail, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                                }
                            }
                            RoleBadge(role = app.status)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Title: ${app.requestedTitle.ifBlank { "Admin Member" }}", style = MaterialTheme.typography.labelMedium, color = palette.accentBronze)
                        Text(app.reason, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary, modifier = Modifier.padding(top = 4.dp))
                        if (app.adminRecommendations.isNotEmpty()) {
                            Text(
                                text = "Moderator recs: ${app.adminRecommendations.joinToString { "${it.adminName} (${it.recommendation})" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (app.status == "PENDING" || app.status == "REJECTED") {
                                NeoPrimaryButton(
                                    text = "Approve",
                                    loading = busy,
                                    containerColor = palette.accentGreen,
                                    contentColor = Color.White,
                                    onClick = {
                                        viewModel.approveApplication(
                                            app = app,
                                            reviewerUid = session.user?.uid.orEmpty(),
                                            reviewerEmail = session.user?.email.orEmpty()
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp)) }
                                )
                            }
                            if (app.status == "PENDING") {
                                NeoSecondaryButton(
                                    text = "Reject",
                                    contentColor = palette.errorFg,
                                    onClick = {
                                        rejectReasonInput = ""
                                        rejectingApp = app
                                    }
                                )
                            }
                            if (targetUser != null && targetUser.role != "SUPER_ADMIN") {
                                NeoSecondaryButton(
                                    text = if (targetUser.isSuspended) "Reinstate" else "Suspend",
                                    contentColor = if (targetUser.isSuspended) palette.accentGreen else palette.errorFg,
                                    onClick = {
                                        viewModel.toggleUserSuspension(
                                            targetUser = targetUser,
                                            actorUid = session.user?.uid.orEmpty(),
                                            actorEmail = session.user?.email.orEmpty()
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(15.dp)) }
                                )
                            }
                            if (app.status == "APPROVED") {
                                NeoSecondaryButton(
                                    text = "Remove Admin",
                                    contentColor = palette.errorFg,
                                    onClick = {
                                        viewModel.removeFromAdminPanel(
                                            targetUserId = app.userId,
                                            name = app.applicantName,
                                            actorUid = session.user?.uid.orEmpty(),
                                            actorEmail = session.user?.email.orEmpty()
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            SuperAdminTab.ANALYTICS -> {
                item {
                    SuperAdminAnalyticsSection(
                        users = users,
                        applications = applications,
                        reports = reports,
                        featuredMembers = featured
                    )
                }
            }

            SuperAdminTab.INVITATIONS -> {
                item {
                    SuperAdminInvitationsSection(
                        session = session,
                        viewModel = viewModel,
                        codes = codes,
                        latestGeneratedCode = latestCode,
                        busy = busy
                    )
                }
            }

            SuperAdminTab.LOGS -> {
                item {
                    SuperAdminAuditLogsSection(logs = auditLogs)
                }
            }
        }
    }

    inspectingUser?.let { target ->
        val fresh = users.firstOrNull { it.id == target.id } ?: target
        UserInspectorDialog(
            user = fresh,
            onDismiss = { inspectingUser = null },
            onFeatureOnHome = {
                inspectingUser = null
                viewModel.featureRegisteredUser(fresh) { created ->
                    editingFeatured = created
                    featuredEditorOpen = true
                }
            },
            onChangeRole = { newRole ->
                viewModel.changeUserRole(fresh, newRole, session.user?.uid.orEmpty(), session.user?.email.orEmpty())
            },
            onToggleSuspension = {
                viewModel.toggleUserSuspension(fresh, session.user?.uid.orEmpty(), session.user?.email.orEmpty())
            },
            onSendNotice = {
                inspectingUser = null
                noticeRecipient = fresh
                noticeComposerOpen = true
            }
        )
    }

    editingTitleUser?.let { target ->
        CustomTitleDialog(
            user = target,
            onDismiss = { editingTitleUser = null },
            onSave = { newTitle ->
                viewModel.updateCustomTitle(
                    targetUserId = target.id,
                    newTitle = newTitle,
                    actorUid = session.user?.uid.orEmpty(),
                    actorEmail = session.user?.email.orEmpty(),
                    onDone = { editingTitleUser = null }
                )
            }
        )
    }

    if (featuredEditorOpen) {
        val linkedUser = users.firstOrNull { it.id == editingFeatured?.userId }
        FeaturedMemberEditorSheet(
            editing = editingFeatured,
            linkedUser = linkedUser,
            uploadProgress = uploadProgress,
            busy = busy,
            onUploadPhoto = { uri, onReady -> viewModel.uploadFeaturedPhoto(uri, onReady) },
            onSave = { id, uid, name, title, img, active, attend, perfect, bio, badges, races, order ->
                viewModel.saveFeaturedMember(
                    existingId = id,
                    userId = uid,
                    name = name,
                    title = title,
                    img = img,
                    active = active,
                    attend = attend,
                    perfect = perfect,
                    bio = bio,
                    badges = badges,
                    completedRaces = races,
                    order = order,
                    onSuccess = { featuredEditorOpen = false }
                )
            },
            onDismiss = { featuredEditorOpen = false }
        )
    }

    if (noticeComposerOpen) {
        DirectNoticeComposerSheet(
            users = users,
            initialRecipient = noticeRecipient,
            busy = busy,
            onSend = { recipient, title, msg, urgent ->
                viewModel.sendDirectNotice(
                    recipient = recipient,
                    senderId = session.user?.uid.orEmpty(),
                    senderName = session.profile?.displayName?.ifBlank { null } ?: "Super Admin",
                    senderEmail = session.user?.email.orEmpty(),
                    title = title,
                    message = msg,
                    isUrgent = urgent,
                    onSuccess = { noticeComposerOpen = false }
                )
            },
            onDismiss = { noticeComposerOpen = false }
        )
    }

    rejectingApp?.let { target ->
        AlertDialog(
            onDismissRequest = { rejectingApp = null },
            title = { Text("Reject Application") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Provide optional feedback for ${target.applicantName}:")
                    OutlinedTextField(
                        value = rejectReasonInput,
                        onValueChange = { rejectReasonInput = it },
                        label = { Text("Rejection reason") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                NeoPrimaryButton(
                    text = "Confirm Reject",
                    containerColor = palette.errorFg,
                    contentColor = Color.White,
                    onClick = {
                        viewModel.rejectApplication(
                            app = target,
                            reason = rejectReasonInput,
                            reviewerUid = session.user?.uid.orEmpty(),
                            reviewerEmail = session.user?.email.orEmpty()
                        )
                        rejectingApp = null
                    }
                )
            },
            dismissButton = {
                NeoSecondaryButton(text = "Cancel", onClick = { rejectingApp = null })
            }
        )
    }
}
