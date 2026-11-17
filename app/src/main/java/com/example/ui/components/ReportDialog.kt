package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.LocalCyclePalette

data class ReportTarget(
    val type: String,
    val id: String,
    val excerpt: String,
    val authorId: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDialog(
    target: ReportTarget?,
    onDismiss: () -> Unit,
    onSubmit: (category: String, details: String) -> Unit
) {
    if (target == null) return

    val palette = LocalCyclePalette.current
    val categories = remember {
        listOf(
            "Bullying / Harassment",
            "Hate Speech / Threats",
            "Spam / Advertisements",
            "Inappropriate Content",
            "Other Policy Violation"
        )
    }
    var selectedCategory by remember(target) { mutableStateOf(categories.first()) }
    var details by remember(target) { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            shadowOffset = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = palette.errorBg,
                    border = BorderStroke(2.dp, palette.errorFg),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Flag,
                        contentDescription = "Report content",
                        tint = palette.errorFg,
                        modifier = Modifier.padding(11.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Report Content to Moderators",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                Text(
                    text = "Moderators review reports of abuse, spam, or safety violations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.chipBg,
                border = BorderStroke(1.dp, palette.dashedBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "\"${target.excerpt.take(150)}\"",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Reason for report") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text("Additional context (optional)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                NeoSecondaryButton(
                    text = "Cancel",
                    onClick = onDismiss
                )
                NeoPrimaryButton(
                    text = "Submit Report",
                    containerColor = palette.errorFg,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    onClick = { onSubmit(selectedCategory, details) },
                    modifier = Modifier.testTag("submit_report_button")
                )
            }
        }
    }
}
