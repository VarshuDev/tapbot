package com.tapbot.feature.botdetail.components

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapbot.core.model.BotCredentialSpec
import com.tapbot.core.ui.motion.TapBotMotion
import com.tapbot.core.ui.theme.TapBotColors
import com.tapbot.core.ui.theme.TapBotTheme
import com.tapbot.core.ui.theme.premiumCardBorder

@Composable
fun CredentialInputSection(
    specs: List<BotCredentialSpec>,
    values: Map<String, String>,
    errors: Map<String, String> = emptyMap(),
    generalError: String? = null,
    isConfiguring: Boolean = false,
    onValueChanged: (String, String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit = {},
    onConfigureToggle: (Boolean) -> Unit = {},
    isSaving: Boolean = false,
    saveSuccess: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = TapBotColors.SurfacePrimary,
            titleContentColor = TapBotColors.TextPrimary,
            textContentColor = TapBotColors.TextSecondary,
            title = {
                Text(
                    text = "Erase stored credentials?",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This permanently deletes this bot's encrypted Android Keystore tokens and API keys on this device.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteClick()
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(TapBotColors.RadiusSm),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TapBotColors.Error,
                        contentColor = Color.White
                    )
                ) {
                    Text("Erase", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Cancel", color = TapBotColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(TapBotColors.SurfacePrimary)
            .premiumCardBorder()
            .animateContentSize(animationSpec = TapBotMotion.ContentSpring)
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = TapBotColors.BrandCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Keystore Credentials",
                color = TapBotColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Encrypted on-device with hardware-backed Android Keystore (AES-256-GCM). Never leaves your phone.",
            color = TapBotColors.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
        )

        val hasConfiguredCredentials = values.isNotEmpty() && values.values.any { it.isNotBlank() }

        if (!isConfiguring && hasConfiguredCredentials) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                    .background(TapBotColors.SuccessSurface)
                    .border(1.dp, TapBotColors.SuccessBorder, RoundedCornerShape(TapBotColors.RadiusMd))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = TapBotColors.SuccessText,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Credentials Encrypted & Locked",
                        color = TapBotColors.SuccessText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${values.count { it.value.isNotBlank() }} credential(s) stored in Android Keystore.",
                        color = TapBotColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(TapBotColors.RadiusSm),
                    border = BorderStroke(1.dp, TapBotColors.ErrorBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = TapBotColors.ErrorSurface,
                        contentColor = TapBotColors.ErrorText
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Erase", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { onConfigureToggle(true) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(TapBotColors.RadiusSm),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TapBotColors.PrimaryCobalt,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reconfigure", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            specs.forEach { spec ->
                CredentialInputField(
                    spec = spec,
                    value = values[spec.key].orEmpty(),
                    errorMessage = errors[spec.key],
                    onValueChanged = { onValueChanged(spec.key, it) }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (!generalError.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(TapBotColors.RadiusSm))
                        .background(TapBotColors.ErrorSurface)
                        .border(1.dp, TapBotColors.ErrorBorder, RoundedCornerShape(TapBotColors.RadiusSm))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = TapBotColors.ErrorText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = generalError,
                        color = TapBotColors.ErrorText,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (saveSuccess) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(TapBotColors.RadiusSm))
                        .background(TapBotColors.SuccessSurface)
                        .border(1.dp, TapBotColors.SuccessBorder, RoundedCornerShape(TapBotColors.RadiusSm))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = TapBotColors.SuccessText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Credentials encrypted & saved to Keystore.",
                        color = TapBotColors.SuccessText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = onSaveClick,
                enabled = !isSaving,
                shape = RoundedCornerShape(TapBotColors.RadiusMd),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TapBotColors.PrimaryCobalt,
                    contentColor = Color.White,
                    disabledContainerColor = TapBotColors.SurfaceInteractive,
                    disabledContentColor = TapBotColors.TextMuted
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Encrypting & Saving...", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save & Lock in Keystore", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CredentialInputField(
    spec: BotCredentialSpec,
    value: String,
    errorMessage: String?,
    onValueChanged: (String) -> Unit
) {
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }

    val helpLabel = remember(spec.helpUrl) {
        val url = spec.helpUrl.orEmpty()
        when {
            url.contains("BotFather", ignoreCase = true) -> "Open @BotFather"
            url.contains("my.telegram.org", ignoreCase = true) -> "Open my.telegram.org"
            else -> "Get Credential"
        }
    }

    val isTelegramToken = spec.key.equals("bot_token", ignoreCase = true) ||
        spec.key.equals("telegram_bot_token", ignoreCase = true)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = spec.label + if (spec.isRequired) " *" else " (Optional)",
                color = TapBotColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (!spec.helpUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button) {
                            openExternalHelpUrl(context, spec.helpUrl!!)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                            .background(TapBotColors.BrandCyanSurface)
                            .border(1.dp, TapBotColors.BrandCyanBorder, RoundedCornerShape(TapBotColors.RadiusPill))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = helpLabel,
                            color = TapBotColors.BrandCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = TapBotColors.BrandCyan,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        if (spec.description.isNotBlank()) {
            Text(
                text = spec.description,
                color = TapBotColors.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = if (isTelegramToken) 4.dp else 6.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (isTelegramToken) {
            Text(
                text = "Tip: Run /newbot in @BotFather, copy the API token, then tap Paste below.",
                color = TapBotColors.TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        val placeholderText = spec.placeholder.ifBlank {
            if (spec.isSecret) "Paste secret value..." else "Enter value..."
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChanged,
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage != null,
            placeholder = {
                Text(
                    text = placeholderText,
                    color = TapBotColors.TextMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            visualTransformation = if (spec.isSecret && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            readClipboardText(context)?.let { pasted ->
                                onValueChanged(pasted.trim())
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste ${spec.label}",
                            tint = TapBotColors.BrandCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (spec.isSecret) {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide ${spec.label}" else "Show ${spec.label}",
                                tint = TapBotColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            },
            shape = RoundedCornerShape(TapBotColors.RadiusMd),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = TapBotColors.SurfaceElevated,
                unfocusedContainerColor = TapBotColors.SurfaceElevated,
                focusedBorderColor = TapBotColors.BrandCyan,
                unfocusedBorderColor = TapBotColors.Border,
                focusedTextColor = TapBotColors.TextPrimary,
                unfocusedTextColor = TapBotColors.TextPrimary,
                errorBorderColor = TapBotColors.Error
            ),
            singleLine = true
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = TapBotColors.ErrorText,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

private fun readClipboardText(context: Context): String? {
    return try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            ?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}

private fun openExternalHelpUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 360)
@Composable
private fun CredentialInputEditingPreview() {
    TapBotTheme {
        Column(modifier = Modifier.background(TapBotColors.Canvas).padding(16.dp)) {
            CredentialInputSection(
                specs = listOf(
                    BotCredentialSpec.TELEGRAM_BOT_TOKEN,
                    BotCredentialSpec.TELEGRAM_API_ID
                ),
                values = mapOf("bot_token" to "123456789:DemoSecretValueForPreview"),
                isConfiguring = true,
                onValueChanged = { _, _ -> },
                onSaveClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 360)
@Composable
private fun CredentialInputLockedPreview() {
    TapBotTheme {
        Column(modifier = Modifier.background(TapBotColors.Canvas).padding(16.dp)) {
            CredentialInputSection(
                specs = listOf(BotCredentialSpec.TELEGRAM_BOT_TOKEN),
                values = mapOf("bot_token" to "stored"),
                isConfiguring = false,
                onValueChanged = { _, _ -> },
                onSaveClick = {}
            )
        }
    }
}
