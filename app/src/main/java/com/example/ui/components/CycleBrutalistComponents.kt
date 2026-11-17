package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.AdminPeachBg
import com.example.ui.theme.AmberBronze
import com.example.ui.theme.BrandShadowDark
import com.example.ui.theme.BrandShadowLight
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCyclePalette
import com.example.ui.theme.PaperCream
import com.example.ui.theme.PublicGrayBg
import com.example.ui.theme.SpaceGroteskFontFamily

@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = LocalCyclePalette.current.cardSurface,
    borderColor: Color = LocalCyclePalette.current.border,
    shadowColor: Color = LocalCyclePalette.current.border,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 5.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .padding(bottom = shadowOffset)
            .drawBehind {
                val r = cornerRadius.toPx()
                drawRoundRect(
                    color = shadowColor,
                    topLeft = Offset(0f, shadowOffset.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(r, r)
                )
            }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            shape = shape,
            color = containerColor,
            border = BorderStroke(borderWidth, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

@Composable
fun CycleBrandBadge(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val palette = LocalCyclePalette.current
    val shadowOffset = if (compact) 4.dp else 5.dp
    val shadowColor = if (palette.isDark) BrandShadowDark else BrandShadowLight
    val pillShape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .padding(bottom = shadowOffset)
            .drawBehind {
                val radiusPx = size.height / 2f
                drawRoundRect(
                    color = shadowColor,
                    topLeft = Offset(0f, shadowOffset.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
            }
    ) {
        Surface(
            modifier = Modifier
                .clip(pillShape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            shape = pillShape,
            color = CharcoalInk
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (compact) 14.dp else 20.dp,
                    vertical = if (compact) 7.dp else 10.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Terminal,
                    contentDescription = "Cycle.exe logo",
                    tint = GoldAccent,
                    modifier = Modifier.size(if (compact) 16.dp else 20.dp)
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = PaperCream)) {
                            append("CYCLE")
                        }
                        withStyle(SpanStyle(color = GoldAccent)) {
                            append(".exe")
                        }
                    },
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 15.sp else 20.sp,
                    letterSpacing = (-0.4).sp,
                    maxLines = 1
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = GoldAccent
                ) {
                    Text(
                        text = "2.0",
                        color = CharcoalInk,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 10.sp else 12.sp,
                        modifier = Modifier.padding(
                            horizontal = if (compact) 7.dp else 9.dp,
                            vertical = if (compact) 2.dp else 3.dp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun RoleBadge(role: String, modifier: Modifier = Modifier) {
    val palette = LocalCyclePalette.current
    val (bg, fg, border) = when (role.uppercase()) {
        "SUPER_ADMIN" -> Triple(CharcoalInk, GoldAccent, GoldAccent)
        "ADMIN" -> Triple(AdminPeachBg, CharcoalInk, CharcoalInk)
        "MEMBER" -> Triple(palette.accentGreenBg, palette.accentGreen, palette.accentGreen)
        else -> Triple(PublicGrayBg, CharcoalInk, palette.border)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = role.uppercase(),
                color = fg,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private const val WEB_BASE_URL = "https://ananta-ace.github.io/Cycle.exe/"

internal fun resolveCycleImageUrl(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    if (value.startsWith("http://", ignoreCase = true) ||
        value.startsWith("https://", ignoreCase = true) ||
        value.startsWith("data:image", ignoreCase = true) ||
        value.startsWith("content://", ignoreCase = true) ||
        value.startsWith("file://", ignoreCase = true)
    ) {
        return value
    }
    val cleaned = value.removePrefix("./").removePrefix("/")
    return "$WEB_BASE_URL$cleaned"
}

@Composable
fun CycleImage(
    urlOrBase64: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: (@Composable BoxScope.() -> Unit)? = null
) {
    val raw = urlOrBase64?.trim().orEmpty()
    if (raw.isBlank()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            placeholder?.invoke(this)
        }
        return
    }

    val value = remember(raw) { resolveCycleImageUrl(raw) }

    if (value.startsWith("data:image", ignoreCase = true)) {
        val bitmap: ImageBitmap? = remember(value) {
            runCatching {
                val base64Part = value.substringAfter("base64,", "")
                if (base64Part.isBlank()) null
                else {
                    val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            }.getOrNull()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        } else {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                placeholder?.invoke(this)
            }
        }
    } else if (value.startsWith("content://") || value.startsWith("file://")) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse(value))
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(value)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

@Composable
fun CycleAvatar(
    photoUrl: String?,
    name: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
    borderColor: Color = LocalCyclePalette.current.border
) {
    val palette = LocalCyclePalette.current
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "C"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(palette.chipBg)
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            CycleImage(
                urlOrBase64 = photoUrl,
                contentDescription = "$name avatar",
                modifier = Modifier.fillMaxSize(),
                placeholder = {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                }
            )
        } else {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        }
    }
}

@Composable
fun NeoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color = GoldAccent,
    contentColor: Color = CharcoalInk,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val palette = LocalCyclePalette.current
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = palette.chipBg,
            disabledContentColor = palette.textMuted
        ),
        border = BorderStroke(2.dp, if (enabled) palette.border else palette.dashedBorder),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = contentColor
            )
            Spacer(Modifier.width(8.dp))
        } else if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NeoSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = LocalCyclePalette.current.textPrimary,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val palette = LocalCyclePalette.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = palette.cardSurface,
            contentColor = contentColor
        ),
        border = BorderStroke(2.dp, if (enabled) palette.border else palette.dashedBorder),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HeroSectionHeader(
    badgeText: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalCyclePalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = GoldAccent.copy(alpha = 0.25f),
            border = BorderStroke(1.5.dp, GoldAccent)
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelMedium,
                color = if (palette.isDark) GoldAccent else AmberBronze,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = palette.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun SyncLoadingCard(
    title: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalCyclePalette.current
    NeoCard(modifier = modifier, shadowOffset = 4.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(color = GoldAccent)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary
            )
            Text(
                text = "Fetching live updates from Cycle.exe cloud...",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SyncErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCyclePalette.current
    NeoCard(
        modifier = modifier,
        containerColor = palette.errorBg,
        borderColor = palette.errorFg,
        shadowColor = palette.errorFg,
        shadowOffset = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.CloudOff, contentDescription = null, tint = palette.errorFg)
                Text(
                    text = "Unable to sync live data",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.errorFg
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textPrimary
            )
            NeoPrimaryButton(
                text = "Retry Server Sync",
                onClick = onRetry,
                leadingIcon = {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = CharcoalInk)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
