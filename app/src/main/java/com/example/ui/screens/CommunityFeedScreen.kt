package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.ui.components.CycleAvatar
import com.example.ui.components.CycleImage
import com.example.ui.components.HeroSectionHeader
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoPrimaryButton
import com.example.ui.components.NeoSecondaryButton
import com.example.ui.components.ReportDialog
import com.example.ui.components.ReportTarget
import com.example.ui.components.SyncErrorCard
import com.example.ui.components.SyncLoadingCard
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LocalCyclePalette
import com.example.ui.viewmodel.AppSessionState
import com.example.ui.viewmodel.FeedViewModel
import com.example.util.DateUtils

@Composable
fun CommunityFeedScreen(
    session: AppSessionState,
    viewModel: FeedViewModel,
    onOpenSignIn: () -> Unit,
    onOpenApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCyclePalette.current
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val allPosts = postsState.items
    val userLikes by viewModel.userLikes.collectAsStateWithLifecycle()
    val commentsMap by viewModel.commentsMap.collectAsStateWithLifecycle()
    val expandedPostIds by viewModel.expandedPostIds.collectAsStateWithLifecycle()
    val isPosting by viewModel.isPosting.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val feedMessage by viewModel.feedMessage.collectAsStateWithLifecycle()

    val uid = session.user?.uid.orEmpty()
    val canModerate = session.isSuperAdmin || session.isAdmin
    val visiblePosts = remember(allPosts, canModerate) {
        if (canModerate) allPosts else allPosts.filter { !it.isHidden }
    }

    LaunchedEffect(visiblePosts, uid) {
        if (uid.isNotBlank() && visiblePosts.isNotEmpty()) {
            viewModel.observeLikesForPosts(visiblePosts.map { it.id }, uid)
        }
    }

    var newPostText by remember { mutableStateOf("") }
    var selectedPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val commentInputs = remember { mutableStateMapOf<String, String>() }
    var replyingToCommentId by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }
    var editingCommentId by remember { mutableStateOf<String?>(null) }
    var editCommentText by remember { mutableStateOf("") }

    var editingPost by remember { mutableStateOf<Post?>(null) }
    var editPostText by remember { mutableStateOf("") }
    var confirmDeletePost by remember { mutableStateOf<Post?>(null) }
    var reportTarget by remember { mutableStateOf<ReportTarget?>(null) }
    var fullScreenPhotoUrl by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedPhotoUris = (selectedPhotoUris + uris).distinct().take(4)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroSectionHeader(
                badgeText = "Live Rider Stream",
                title = "Community Feed",
                subtitle = "Ride logs, route updates, gear discussions, and photos shared by approved Admin Members."
            )
        }

        if (!session.isMember) {
            item {
                NeoCard(
                    containerColor = palette.alertBg,
                    borderColor = palette.alertBorder,
                    shadowColor = palette.border,
                    shadowOffset = 5.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CharcoalInk,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = GoldAccent
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uid.isBlank()) "Sign in to join the conversation" else "Posting reserved for Approved Admin Members",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary
                            )
                            Text(
                                text = if (uid.isBlank()) {
                                    "You can browse all community posts and comments as a guest. Sign in with Google to like, comment, or apply to post."
                                } else {
                                    "Public riders can read posts and rate crew leaders. Apply to unlock posting, liking, and comments."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    NeoPrimaryButton(
                        text = if (uid.isBlank()) "Sign In to Participate" else "Apply for Admin Member",
                        onClick = { if (uid.isBlank()) onOpenSignIn() else onOpenApply() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feed_apply_button")
                    )
                }
            }
        }

        if (!feedMessage.isNullOrBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.alertBg,
                    border = BorderStroke(1.5.dp, palette.alertBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = feedMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearMessage() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss message")
                        }
                    }
                }
            }
        }

        if (session.isMember) {
            item {
                NeoCard(shadowOffset = 6.dp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CycleAvatar(
                            photoUrl = session.profile?.photoURL ?: session.user?.photoUrl?.toString(),
                            name = session.profile?.displayName ?: session.user?.displayName ?: "Rider",
                            size = 44.dp
                        )
                        Column {
                            Text(
                                text = session.profile?.displayName ?: session.user?.displayName ?: "Cyclist",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = GoldAccent.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, GoldAccent)
                            ) {
                                Text(
                                    text = session.profile?.customTitle?.ifBlank { null }
                                        ?: if (session.isSuperAdmin) "Super Admin"
                                        else if (session.isAdmin) "Admin"
                                        else "Admin Member",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPostText,
                        onValueChange = { newPostText = it },
                        placeholder = { Text("Share a ride update, route tip, or sprint highlight...") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_composer_input")
                    )

                    if (selectedPhotoUris.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedPhotoUris.forEachIndexed { idx, uri ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(76.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(palette.chipBg)
                                ) {
                                    CycleImage(
                                        urlOrBase64 = uri.toString(),
                                        contentDescription = "Selected photo ${idx + 1}",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    IconButton(
                                        onClick = {
                                            selectedPhotoUris = selectedPhotoUris.filterIndexed { i, _ -> i != idx }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(28.dp)
                                            .padding(2.dp)
                                    ) {
                                        Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.7f)) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Remove photo",
                                                tint = Color.White,
                                                modifier = Modifier.padding(3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    uploadProgress?.let { pct ->
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { pct / 100f },
                            color = palette.accentGreen,
                            trackColor = palette.chipBg,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeoSecondaryButton(
                            text = "Photos (${selectedPhotoUris.size}/4)",
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.AddPhotoAlternate,
                                    contentDescription = "Add photos",
                                    tint = palette.accentGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("add_post_photos_button")
                        )

                        NeoPrimaryButton(
                            text = "Publish",
                            loading = isPosting,
                            enabled = !isPosting && (newPostText.isNotBlank() || selectedPhotoUris.isNotEmpty()),
                            onClick = {
                                viewModel.createPost(
                                    uid = uid,
                                    profile = session.profile,
                                    fallbackName = session.user?.displayName ?: "Rider",
                                    fallbackPhoto = session.user?.photoUrl?.toString().orEmpty(),
                                    isSuperAdmin = session.isSuperAdmin,
                                    isAdmin = session.isAdmin,
                                    content = newPostText,
                                    photoUris = selectedPhotoUris
                                ) {
                                    newPostText = ""
                                    selectedPhotoUris = emptyList()
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = CharcoalInk,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("publish_post_button")
                        )
                    }
                }
            }
        }

        if (postsState.isLoading && visiblePosts.isEmpty()) {
            item {
                SyncLoadingCard(title = "Loading Community Feed...")
            }
        } else if (postsState.error != null && visiblePosts.isEmpty()) {
            item {
                SyncErrorCard(
                    message = postsState.error.orEmpty(),
                    onRetry = { viewModel.refreshFeed() }
                )
            }
        } else if (visiblePosts.isEmpty()) {
            item {
                NeoCard(shadowOffset = 5.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PedalBike,
                            contentDescription = null,
                            tint = palette.textMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No Posts Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Ride updates published by Admin Members appear here in real time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        } else {
            items(visiblePosts, key = { it.id }) { post ->
                val isAuthor = uid.isNotBlank() && uid == post.authorId
                val isLiked = userLikes[post.id] == true
                val isExpanded = expandedPostIds.contains(post.id)
                val postComments = commentsMap[post.id].orEmpty()
                val topLevel = postComments.filter { it.parentId == null }
                val repliesMap = postComments.filter { it.parentId != null }.groupBy { it.parentId!! }

                NeoCard(
                    shadowOffset = 5.dp,
                    containerColor = if (post.isHidden) palette.chipBg else palette.cardSurface
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            CycleAvatar(
                                photoUrl = post.authorPhotoURL,
                                name = post.authorName.ifBlank { "Rider" },
                                size = 42.dp
                            )
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = post.authorName.ifBlank { "Rider" },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.textPrimary
                                    )
                                    if (post.authorTitle.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = GoldAccent.copy(alpha = 0.25f),
                                            border = BorderStroke(1.dp, GoldAccent)
                                        ) {
                                            Text(
                                                text = post.authorTitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textPrimary,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = buildString {
                                        append(DateUtils.formatRelativeOrDate(post.createdAt))
                                        if (post.isHidden) append(" • HIDDEN")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (post.isHidden) palette.errorFg else palette.textMuted
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isAuthor && uid.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        reportTarget = ReportTarget(
                                            type = "POST",
                                            id = post.id,
                                            excerpt = post.content.take(100),
                                            authorId = post.authorId
                                        )
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Flag,
                                        contentDescription = "Report post",
                                        tint = palette.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (canModerate) {
                                IconButton(
                                    onClick = { viewModel.toggleHiddenPost(post) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (post.isHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (post.isHidden) "Unhide post" else "Hide post",
                                        tint = palette.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (isAuthor) {
                                IconButton(
                                    onClick = {
                                        editingPost = post
                                        editPostText = post.content
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit post",
                                        tint = palette.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (isAuthor || session.isSuperAdmin) {
                                IconButton(
                                    onClick = { confirmDeletePost = post },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete post",
                                        tint = palette.errorFg,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (post.content.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = post.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary
                        )
                    }

                    if (post.photoURLs.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            post.photoURLs.chunked(2).forEach { rowUrls ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowUrls.forEach { imgUrl ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(if (post.photoURLs.size == 1) 220.dp else 140.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.Black)
                                                .clickable { fullScreenPhotoUrl = imgUrl }
                                        ) {
                                            CycleImage(
                                                urlOrBase64 = imgUrl,
                                                contentDescription = "Post attachment",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = palette.dashedBorder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                when {
                                    uid.isBlank() -> onOpenSignIn()
                                    session.isMember -> viewModel.toggleLike(post, uid)
                                    else -> onOpenApply()
                                }
                            },
                            modifier = Modifier.testTag("like_button_${post.id}")
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Like post",
                                tint = if (isLiked) palette.errorFg else palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${post.likesCount} ${if (post.likesCount == 1L) "Like" else "Likes"}",
                                color = if (isLiked) palette.errorFg else palette.textSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = { viewModel.toggleComments(post.id) },
                            modifier = Modifier.testTag("comments_button_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ModeComment,
                                contentDescription = "Toggle comments",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${post.commentsCount} ${if (post.commentsCount == 1L) "Comment" else "Comments"}",
                                color = palette.textSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isExpanded) {
                        HorizontalDivider(
                            color = palette.dashedBorder,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        if (session.isMember) {
                            val draft = commentInputs[post.id].orEmpty()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = draft,
                                    onValueChange = { commentInputs[post.id] = it },
                                    placeholder = { Text("Write a comment...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    enabled = draft.isNotBlank(),
                                    onClick = {
                                        viewModel.addComment(
                                            post = post,
                                            uid = uid,
                                            authorName = session.profile?.displayName ?: session.user?.displayName ?: "Rider",
                                            authorPhotoURL = session.profile?.photoURL ?: session.user?.photoUrl?.toString().orEmpty(),
                                            text = draft
                                        ) {
                                            commentInputs[post.id] = ""
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send comment",
                                        tint = if (draft.isNotBlank()) palette.accentBronze else palette.textMuted
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Apply for Admin Membership to join discussions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textMuted,
                                fontStyle = FontStyle.Italic
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        if (topLevel.isEmpty()) {
                            Text(
                                text = "No comments yet. Start the conversation!",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = palette.textMuted
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                topLevel.forEach { comment ->
                                    val isCommentAuthor = uid == comment.authorId
                                    val replies = repliesMap[comment.id].orEmpty()
                                    val isEditing = editingCommentId == comment.id
                                    val isReplying = replyingToCommentId == comment.id

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = palette.cardSurfaceAlt,
                                        border = BorderStroke(1.dp, palette.dashedBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                CycleAvatar(
                                                    photoUrl = comment.authorPhotoURL,
                                                    name = comment.authorName,
                                                    size = 28.dp
                                                )
                                                Text(
                                                    text = comment.authorName,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = palette.textPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = DateUtils.formatRelativeOrDate(comment.createdAt),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textMuted
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            if (isEditing) {
                                                OutlinedTextField(
                                                    value = editCommentText,
                                                    onValueChange = { editCommentText = it },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    TextButton(onClick = { editingCommentId = null }) {
                                                        Text("Cancel")
                                                    }
                                                    TextButton(onClick = {
                                                        viewModel.editComment(post.id, comment.id, editCommentText) {
                                                            editingCommentId = null
                                                        }
                                                    }) {
                                                        Text("Save", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            } else {
                                                Text(
                                                    text = comment.text,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = palette.textPrimary
                                                )
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (session.isMember) {
                                                    TextButton(onClick = {
                                                        replyingToCommentId = if (isReplying) null else comment.id
                                                        replyText = ""
                                                    }) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.Reply,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Reply")
                                                    }
                                                }
                                                if (isCommentAuthor && !isEditing) {
                                                    TextButton(onClick = {
                                                        editingCommentId = comment.id
                                                        editCommentText = comment.text
                                                    }) {
                                                        Text("Edit")
                                                    }
                                                }
                                                if (isCommentAuthor || session.isSuperAdmin) {
                                                    TextButton(onClick = { viewModel.deleteComment(post, comment) }) {
                                                        Text("Delete", color = palette.errorFg)
                                                    }
                                                }
                                                if (uid.isNotBlank() && !isCommentAuthor) {
                                                    TextButton(onClick = {
                                                        reportTarget = ReportTarget(
                                                            type = "COMMENT",
                                                            id = comment.id,
                                                            excerpt = comment.text.take(80),
                                                            authorId = comment.authorId
                                                        )
                                                    }) {
                                                        Text("Report")
                                                    }
                                                }
                                            }

                                            if (isReplying) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedTextField(
                                                        value = replyText,
                                                        onValueChange = { replyText = it },
                                                        placeholder = { Text("Reply to ${comment.authorName}...") },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    IconButton(
                                                        enabled = replyText.isNotBlank(),
                                                        onClick = {
                                                            viewModel.addComment(
                                                                post = post,
                                                                uid = uid,
                                                                authorName = session.profile?.displayName ?: session.user?.displayName ?: "Rider",
                                                                authorPhotoURL = session.profile?.photoURL ?: session.user?.photoUrl?.toString().orEmpty(),
                                                                text = replyText,
                                                                parentComment = comment
                                                            ) {
                                                                replyText = ""
                                                                replyingToCommentId = null
                                                            }
                                                        },
                                                        modifier = Modifier.size(48.dp)
                                                    ) {
                                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send reply")
                                                    }
                                                }
                                            }

                                            if (replies.isNotEmpty()) {
                                                Spacer(Modifier.height(6.dp))
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    replies.forEach { reply ->
                                                        val isReplyAuthor = uid == reply.authorId
                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = palette.cardSurface,
                                                            border = BorderStroke(1.dp, palette.dashedBorder),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Column(modifier = Modifier.padding(8.dp)) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                                ) {
                                                                    Text(
                                                                        text = reply.authorName,
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        color = palette.textPrimary
                                                                    )
                                                                    Text(
                                                                        text = DateUtils.formatRelativeOrDate(reply.createdAt),
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = palette.textMuted
                                                                    )
                                                                }
                                                                Spacer(Modifier.height(2.dp))
                                                                Text(
                                                                    text = buildString {
                                                                        if (!reply.replyToAuthor.isNullOrBlank()) {
                                                                            append("@${reply.replyToAuthor} ")
                                                                        }
                                                                        append(reply.text)
                                                                    },
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = palette.textSecondary
                                                                )
                                                                if (isReplyAuthor || session.isSuperAdmin) {
                                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                                        TextButton(onClick = { viewModel.deleteComment(post, reply) }) {
                                                                            Text("Delete", color = palette.errorFg)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingPost?.let { target ->
        AlertDialog(
            onDismissRequest = { editingPost = null },
            title = { Text("Edit Post") },
            text = {
                OutlinedTextField(
                    value = editPostText,
                    onValueChange = { editPostText = it },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                NeoPrimaryButton(
                    text = "Save",
                    onClick = {
                        viewModel.updatePost(target.id, editPostText) {
                            editingPost = null
                        }
                    }
                )
            },
            dismissButton = {
                NeoSecondaryButton(
                    text = "Cancel",
                    onClick = { editingPost = null }
                )
            }
        )
    }

    confirmDeletePost?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDeletePost = null },
            title = { Text("Delete Post?") },
            text = { Text("Are you sure you want to permanently delete this post?") },
            confirmButton = {
                NeoPrimaryButton(
                    text = "Delete",
                    containerColor = palette.errorFg,
                    contentColor = Color.White,
                    onClick = {
                        viewModel.deletePost(target.id)
                        confirmDeletePost = null
                    }
                )
            },
            dismissButton = {
                NeoSecondaryButton(
                    text = "Cancel",
                    onClick = { confirmDeletePost = null }
                )
            }
        )
    }

    ReportDialog(
        target = reportTarget,
        onDismiss = { reportTarget = null },
        onSubmit = { category, details ->
            val current = reportTarget ?: return@ReportDialog
            viewModel.submitReport(
                targetType = current.type,
                targetId = current.id,
                targetAuthorId = current.authorId,
                targetExcerpt = current.excerpt,
                reporterId = uid,
                reporterEmail = session.user?.email ?: "anonymous",
                reason = "$category: ${details.trim()}"
            ) {
                reportTarget = null
            }
        }
    )

    fullScreenPhotoUrl?.let { imgUrl ->
        Dialog(
            onDismissRequest = { fullScreenPhotoUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .padding(16.dp)
            ) {
                CycleImage(
                    urlOrBase64 = imgUrl,
                    contentDescription = "Full photo view",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { fullScreenPhotoUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close photo", tint = Color.White)
                }
            }
        }
    }
}
