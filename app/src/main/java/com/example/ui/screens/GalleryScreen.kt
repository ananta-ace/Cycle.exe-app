package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GalleryPhoto
import com.example.ui.components.CycleImage
import com.example.ui.components.HeroSectionHeader
import com.example.ui.components.LightboxDialog
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoPrimaryButton
import com.example.ui.components.NeoSecondaryButton
import com.example.ui.components.SyncErrorCard
import com.example.ui.components.SyncLoadingCard
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.LocalCyclePalette
import com.example.ui.viewmodel.AppSessionState
import com.example.ui.viewmodel.GalleryViewModel
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    session: AppSessionState,
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalCyclePalette.current
    val photosState by viewModel.photosState.collectAsStateWithLifecycle()
    val photos = photosState.items
    val uploading by viewModel.uploading.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var activePhotoIndex by remember { mutableStateOf<Int?>(null) }
    var uploadSheetOpen by remember { mutableStateOf(false) }
    var photoTitle by remember { mutableStateOf("") }
    var photoDesc by remember { mutableStateOf("") }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var confirmDeletePhoto by remember { mutableStateOf<GalleryPhoto?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedUri = uri
            viewModel.clearError()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 165.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                HeroSectionHeader(
                    badgeText = "Official Community Gallery",
                    title = "Moments From The Road",
                    subtitle = "Curated cycling moments, rides, and scenic routes managed by the Super Admin."
                )
                if (session.isSuperAdmin) {
                    Spacer(Modifier.height(10.dp))
                    NeoPrimaryButton(
                        text = "Upload Photo to Gallery",
                        onClick = {
                            viewModel.clearError()
                            photoTitle = ""
                            photoDesc = ""
                            pickedUri = null
                            uploadSheetOpen = true
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = CharcoalInk)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gallery_upload_button")
                    )
                }
            }
        }

        if (photosState.isLoading && photos.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SyncLoadingCard(title = "Loading Gallery...")
            }
        } else if (photosState.error != null && photos.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SyncErrorCard(
                    message = photosState.error.orEmpty(),
                    onRetry = { viewModel.refreshGallery() }
                )
            }
        } else if (photos.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                NeoCard(shadowOffset = 5.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            tint = palette.textMuted,
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Gallery is Ready",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Authentic ride photos uploaded by the Super Admin will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        } else {
            itemsIndexed(photos, key = { _, item -> item.id }) { index, photo ->
                NeoCard(
                    shadowOffset = 5.dp,
                    contentPadding = PaddingValues(0.dp),
                    onClick = { activePhotoIndex = index }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.25f)
                            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                            .background(Color.Black)
                    ) {
                        CycleImage(
                            urlOrBase64 = photo.photoURL,
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.ZoomOutMap,
                                    contentDescription = "Expand photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (session.isSuperAdmin) {
                            IconButton(
                                onClick = { confirmDeletePhoto = photo },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(40.dp)
                            ) {
                                Surface(shape = CircleShape, color = palette.errorFg) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete photo",
                                        tint = Color.White,
                                        modifier = Modifier.padding(6.dp).size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = photo.title.ifBlank { "Ride Moment" },
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!photo.description.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = photo.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = DateUtils.formatShortDate(photo.uploadedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textMuted
                        )
                    }
                }
            }
        }
    }

    LightboxDialog(
        photos = photos,
        activeIndex = activePhotoIndex,
        onIndexChange = { activePhotoIndex = it },
        onDismiss = { activePhotoIndex = null }
    )

    if (uploadSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { if (!uploading) uploadSheetOpen = false },
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
                        text = "Add Gallery Photo",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary
                    )
                    IconButton(
                        enabled = !uploading,
                        onClick = { uploadSheetOpen = false },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
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
                            text = error.orEmpty(),
                            color = palette.errorFg,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = photoTitle,
                    onValueChange = { photoTitle = it },
                    label = { Text("Photo Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = photoDesc,
                    onValueChange = { photoDesc = it },
                    label = { Text("Optional Description") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                NeoSecondaryButton(
                    text = if (pickedUri == null) "Choose Photo from Device" else "Change Selected Photo",
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                pickedUri?.let { uri ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                    ) {
                        CycleImage(
                            urlOrBase64 = uri.toString(),
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                uploadProgress?.let { pct ->
                    LinearProgressIndicator(
                        progress = { pct / 100f },
                        color = palette.accentGreen,
                        trackColor = palette.chipBg,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    NeoSecondaryButton(
                        text = "Cancel",
                        enabled = !uploading,
                        onClick = { uploadSheetOpen = false }
                    )
                    NeoPrimaryButton(
                        text = "Publish Photo",
                        loading = uploading,
                        enabled = !uploading && pickedUri != null && photoTitle.isNotBlank(),
                        onClick = {
                            val uri = pickedUri ?: return@NeoPrimaryButton
                            viewModel.uploadPhoto(
                                uri = uri,
                                title = photoTitle,
                                description = photoDesc,
                                uploadedBy = session.user?.uid ?: "super-admin",
                                uploadedByName = session.profile?.displayName ?: "Super Admin"
                            ) {
                                uploadSheetOpen = false
                            }
                        }
                    )
                }
            }
        }
    }

    confirmDeletePhoto?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDeletePhoto = null },
            title = { Text("Delete Gallery Photo?") },
            text = { Text("Are you sure you want to delete \"${target.title}\"?") },
            confirmButton = {
                NeoPrimaryButton(
                    text = "Delete",
                    containerColor = palette.errorFg,
                    contentColor = Color.White,
                    onClick = {
                        viewModel.deletePhoto(target.id)
                        confirmDeletePhoto = null
                    }
                )
            },
            dismissButton = {
                NeoSecondaryButton(
                    text = "Cancel",
                    onClick = { confirmDeletePhoto = null }
                )
            }
        )
    }
}
