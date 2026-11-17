package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.FeaturedMember
import com.example.data.model.UserProfile
import com.example.ui.components.CycleAvatar
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoPrimaryButton
import com.example.ui.components.NeoSecondaryButton
import com.example.ui.components.RoleBadge
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LocalCyclePalette
import com.example.util.DateUtils
import java.util.Locale

@Composable
fun UserInspectorDialog(
    user: UserProfile,
    onDismiss: () -> Unit,
    onFeatureOnHome: () -> Unit,
    onChangeRole: (String) -> Unit,
    onToggleSuspension: () -> Unit,
    onSendNotice: () -> Unit
) {
    val palette = LocalCyclePalette.current
    var roleMenuOpen by remember { mutableStateOf(false) }

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
                Text("Inspect User", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close inspector")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CycleAvatar(photoUrl = user.photoURL, name = user.displayName, size = 56.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.displayName.ifBlank { "Cyclist" }, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    RoleBadge(role = user.role)
                }
            }

            HorizontalDivider(color = palette.dashedBorder, modifier = Modifier.padding(vertical = 10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Custom Title: ${user.customTitle?.ifBlank { "None" } ?: "None"}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                Text("Membership: ${user.adminMembershipStatus} | AdminMember=${user.isAdminMember}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                Text("Status: ${if (user.isSuspended) "SUSPENDED" else "ACTIVE"}", style = MaterialTheme.typography.bodySmall, color = if (user.isSuspended) palette.errorFg else palette.accentGreen)
                val userRatingLabel = if (user.ratingAverage > 0.0 || user.ratingCount > 0L) {
                    String.format(Locale.US, "%.1f / 5.0 (%d reviews)", user.ratingAverage, user.ratingCount)
                } else {
                    "No ratings (${user.ratingCount} reviews)"
                }
                Text("Community Rating: $userRatingLabel", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                Text("Joined: ${DateUtils.formatShortDate(user.createdAt)}", style = MaterialTheme.typography.bodySmall, color = palette.textMuted)
                if (!user.bio.isNullOrBlank()) {
                    Text("Bio: ${user.bio}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                }
            }

            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NeoPrimaryButton(
                    text = "Feature on Home / Crew",
                    onClick = onFeatureOnHome,
                    leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, tint = CharcoalInk, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.fillMaxWidth()
                )
                NeoSecondaryButton(
                    text = "Send Direct Notice",
                    onClick = onSendNotice,
                    leadingIcon = { Icon(Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (user.role != "SUPER_ADMIN") {
                    Box {
                        NeoSecondaryButton(
                            text = "Change Role (${user.role})",
                            onClick = { roleMenuOpen = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(expanded = roleMenuOpen, onDismissRequest = { roleMenuOpen = false }) {
                            listOf("PUBLIC", "MEMBER", "ADMIN").forEach { roleOption ->
                                DropdownMenuItem(
                                    text = { Text(roleOption) },
                                    onClick = {
                                        roleMenuOpen = false
                                        onChangeRole(roleOption)
                                    }
                                )
                            }
                        }
                    }

                    NeoSecondaryButton(
                        text = if (user.isSuspended) "Reactivate Account" else "Suspend Account",
                        contentColor = if (user.isSuspended) palette.accentGreen else palette.errorFg,
                        onClick = onToggleSuspension,
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Block,
                                contentDescription = null,
                                tint = if (user.isSuspended) palette.accentGreen else palette.errorFg,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomTitleDialog(
    user: UserProfile,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val palette = LocalCyclePalette.current
    var title by remember(user.id) { mutableStateOf(user.customTitle.orEmpty()) }
    val presets = listOf("Chill Guy", "Road Warrior", "Road Captain", "Climbing Specialist", "Cycling Enthusiast")

    Dialog(onDismissRequest = onDismiss) {
        NeoCard(modifier = Modifier.fillMaxWidth(), shadowOffset = 6.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Custom Title", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close title editor")
                }
            }
            Text("Rider: ${user.displayName}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 50) title = it },
                label = { Text("Community Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { preset ->
                    FilterChip(
                        selected = title == preset,
                        onClick = { title = preset },
                        label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldAccent,
                            selectedLabelColor = CharcoalInk
                        )
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            NeoPrimaryButton(
                text = "Save Custom Title",
                onClick = { onSave(title) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeaturedMemberEditorSheet(
    editing: FeaturedMember?,
    linkedUser: UserProfile?,
    uploadProgress: Int?,
    busy: Boolean,
    onUploadPhoto: (android.net.Uri, (String) -> Unit) -> Unit,
    onSave: (
        existingId: String?,
        userId: String,
        name: String,
        title: String,
        img: String,
        active: Boolean,
        attend: String,
        perfect: Boolean,
        bio: String,
        badges: List<String>,
        completedRaces: List<String>,
        order: Long
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalCyclePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var title by remember(editing) { mutableStateOf(editing?.title ?: "Cycle.exe Crew Leader") }
    var img by remember(editing) { mutableStateOf(editing?.img.orEmpty()) }
    var attend by remember(editing) { mutableStateOf(editing?.attend ?: "Active Rider") }
    var orderText by remember(editing) { mutableStateOf((editing?.order ?: 1L).toString()) }
    var active by remember(editing) { mutableStateOf(editing?.active ?: true) }
    var perfect by remember(editing) { mutableStateOf(editing?.perfect ?: false) }
    var bio by remember(editing) { mutableStateOf(editing?.bio.orEmpty()) }

    val badges = remember(editing) { mutableStateListOf<String>().apply { addAll(editing?.badges.orEmpty()) } }
    val races = remember(editing) { mutableStateListOf<String>().apply { addAll(editing?.completedRaces.orEmpty()) } }
    var badgeDraft by remember { mutableStateOf("") }
    var raceDraft by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUploadPhoto(uri) { url -> img = url }
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
                Text(
                    text = if (editing != null) "Edit Featured Rider" else "Add Custom Crew Leader",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close editor")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CycleAvatar(photoUrl = img, name = name.ifBlank { "Rider" }, size = 64.dp, borderColor = GoldAccent)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    NeoSecondaryButton(
                        text = if (uploadProgress != null) "Uploading $uploadProgress%..." else "Pick Rider Photo",
                        enabled = uploadProgress == null,
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        leadingIcon = { Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!linkedUser?.photoURL.isNullOrBlank()) {
                        NeoSecondaryButton(
                            text = "Use Rider's Profile Pic",
                            onClick = { img = linkedUser!!.photoURL!! },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            if (uploadProgress != null) {
                LinearProgressIndicator(
                    progress = { (uploadProgress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = GoldAccent
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Rider Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Role / Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = attend,
                    onValueChange = { attend = it },
                    label = { Text("Attendance Label") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = orderText,
                    onValueChange = { orderText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Order") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(100.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { active = !active }) {
                    Checkbox(checked = active, onCheckedChange = { active = it })
                    Text("Active Crew", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { perfect = !perfect }) {
                    Checkbox(checked = perfect, onCheckedChange = { perfect = it })
                    Text("Perfect Attendance", style = MaterialTheme.typography.bodyMedium)
                }
            }

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Rider Bio") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = palette.dashedBorder)
            Text("BADGES", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = badgeDraft,
                    onValueChange = { badgeDraft = it },
                    label = { Text("Add Badge") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                NeoPrimaryButton(
                    text = "Add",
                    onClick = {
                        val clean = badgeDraft.trim()
                        if (clean.isNotBlank() && clean !in badges) {
                            badges.add(clean)
                            badgeDraft = ""
                        }
                    }
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Founding Captain", "Road Warrior", "Century Finisher", "Pace Setter").forEach { chip ->
                    if (chip !in badges) {
                        FilterChip(
                            selected = false,
                            onClick = { badges.add(chip) },
                            label = { Text("+ $chip", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                badges.forEach { b ->
                    FilterChip(
                        selected = true,
                        onClick = { badges.remove(b) },
                        label = { Text("$b ×", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldAccent, selectedLabelColor = CharcoalInk)
                    )
                }
            }

            HorizontalDivider(color = palette.dashedBorder)
            Text("COMPLETED RACES", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = raceDraft,
                    onValueChange = { raceDraft = it },
                    label = { Text("Add Completed Race") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                NeoPrimaryButton(
                    text = "Add",
                    onClick = {
                        val clean = raceDraft.trim()
                        if (clean.isNotBlank() && clean !in races) {
                            races.add(clean)
                            raceDraft = ""
                        }
                    }
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Rajendrapur Loop", "Nestlé ↔ Nourish Poultry", "Bhakowadi Duo Ride").forEach { r ->
                    if (r !in races) {
                        FilterChip(
                            selected = false,
                            onClick = { races.add(r) },
                            label = { Text("+ $r", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                races.forEach { r ->
                    FilterChip(
                        selected = true,
                        onClick = { races.remove(r) },
                        label = { Text("$r ×", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CharcoalInk, selectedLabelColor = GoldAccent)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            NeoPrimaryButton(
                text = "Save Featured Profile",
                loading = busy,
                onClick = {
                    onSave(
                        editing?.id,
                        editing?.userId.orEmpty(),
                        name,
                        title,
                        img,
                        active,
                        attend,
                        perfect,
                        bio,
                        badges.toList(),
                        races.toList(),
                        orderText.toLongOrNull() ?: 1L
                    )
                },
                leadingIcon = { Icon(Icons.Filled.Save, contentDescription = null, tint = CharcoalInk) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_featured_member_button")
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DirectNoticeComposerSheet(
    users: List<UserProfile>,
    initialRecipient: UserProfile?,
    busy: Boolean,
    onSend: (recipient: UserProfile, title: String, message: String, isUrgent: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalCyclePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedRecipient by remember(initialRecipient) {
        mutableStateOf(initialRecipient ?: users.firstOrNull())
    }
    var pickerOpen by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("Cycle.exe Official Notice") }
    var message by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(false) }

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
                Text("Send Direct Notice", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close notice composer")
                }
            }

            Box {
                NeoSecondaryButton(
                    text = selectedRecipient?.let { "To: ${it.displayName} (${it.email})" } ?: "Select Recipient",
                    onClick = { pickerOpen = true },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = pickerOpen,
                    onDismissRequest = { pickerOpen = false },
                    modifier = Modifier.heightIn(max = 320.dp)
                ) {
                    users.forEach { u ->
                        DropdownMenuItem(
                            text = { Text("${u.displayName.ifBlank { "Cyclist" }} (${u.email})") },
                            onClick = {
                                selectedRecipient = u
                                pickerOpen = false
                            }
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val templates = listOf(
                    "Ride Briefing" to "Heads up! Please confirm your availability for this weekend's Cycle.exe group ride.",
                    "Welcome Aboard" to "Welcome to the Cycle.exe squad! Feel free to reply here if you have any questions.",
                    "Policy Reminder" to "Please review our community safety & posting guidelines before publishing new ride posts."
                )
                templates.forEach { (label, body) ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            title = label
                            message = body
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Notice Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message *") },
                placeholder = { Text("Write a direct notice to this rider...") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mark as Urgent Alert", style = MaterialTheme.typography.bodyMedium, color = palette.errorFg)
                Switch(checked = isUrgent, onCheckedChange = { isUrgent = it })
            }

            NeoPrimaryButton(
                text = "Send Notice",
                loading = busy,
                enabled = selectedRecipient != null && message.isNotBlank(),
                onClick = {
                    selectedRecipient?.let { recipient ->
                        onSend(recipient, title, message, isUrgent)
                    }
                },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = CharcoalInk) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("send_direct_notice_button")
            )
        }
    }
}
