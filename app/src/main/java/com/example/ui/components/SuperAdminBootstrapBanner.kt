package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.data.repository.SuperAdminConfig
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LocalCyclePalette
import kotlinx.coroutines.launch

@Composable
fun SuperAdminBootstrapBanner(
    email: String?,
    emailVerified: Boolean,
    profile: UserProfile?,
    firestoreSuperAdminSynced: Boolean,
    onSyncClick: suspend () -> Pair<Boolean, String>,
    modifier: Modifier = Modifier
) {
    if (!SuperAdminConfig.isSuperAdminEmail(email)) return
    if (emailVerified && firestoreSuperAdminSynced) return

    val palette = LocalCyclePalette.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    NeoCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        containerColor = palette.alertBg,
        borderColor = palette.alertBorder,
        shadowColor = palette.alertBorder,
        shadowOffset = 4.dp,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = GoldAccent,
                border = BorderStroke(2.dp, CharcoalInk),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = "Super Admin sync",
                    tint = CharcoalInk,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Super Admin DB Clearance Sync Needed",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (!emailVerified) {
                        "Your email ($email) must be verified before Super Admin authority can activate."
                    } else {
                        "Current DB role is \"${profile?.role ?: "PUBLIC"}\". Tap below to sync SUPER_ADMIN clearance for $email."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                statusMsg?.let { (ok, text) ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (ok) palette.accentGreen else palette.errorFg
                    )
                }
                if (emailVerified) {
                    Spacer(Modifier.height(10.dp))
                    NeoPrimaryButton(
                        text = "Activate Super Admin",
                        loading = loading,
                        onClick = {
                            scope.launch {
                                loading = true
                                statusMsg = onSyncClick()
                                loading = false
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Key,
                                contentDescription = null,
                                tint = CharcoalInk,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("activate_super_admin_button")
                    )
                }
            }
        }
    }
}
