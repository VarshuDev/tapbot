package com.tapbot.feature.botdetail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapbot.core.model.BotCredentialSpec
import com.tapbot.feature.botdetail.ui.BotHubTheme

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
    saveSuccess: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusLg))
            .background(BotHubTheme.PrimarySurface)
            .border(1.dp, BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusLg))
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = BotHubTheme.PrimaryBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Required Credentials",
                color = BotHubTheme.PrimaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Credentials are encrypted on-device with Android Keystore (AES-256-GCM) and NEVER sent to our servers.",
            color = BotHubTheme.SecondaryText,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
        )

        val hasConfiguredCredentials = values.isNotEmpty() && values.values.any { it.isNotBlank() }

        if (!isConfiguring && hasConfiguredCredentials) {
            // Read-only / Configured State View
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
                    .background(BotHubTheme.SuccessSurface)
                    .border(1.dp, BotHubTheme.SuccessBorder, RoundedCornerShape(BotHubTheme.RadiusMd))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = BotHubTheme.Success,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Credentials Configured & Locked",
                        color = BotHubTheme.Success,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${values.size} credential(s) safely stored in hardware Keystore vault.",
                        color = BotHubTheme.SecondaryText,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDeleteClick,
                    shape = RoundedCornerShape(BotHubTheme.RadiusSm),
                    border = BorderStroke(1.dp, BotHubTheme.ErrorBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = BotHubTheme.ErrorSurface,
                        contentColor = BotHubTheme.Error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = { onConfigureToggle(true) },
                    shape = RoundedCornerShape(BotHubTheme.RadiusSm),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BotHubTheme.PrimaryBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reconfigure", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            // Editable Configuration Fields
            specs.forEach { spec ->
                CredentialInputField(
                    spec = spec,
                    value = values[spec.key].orEmpty(),
                    errorMessage = errors[spec.key],
                    onValueChanged = { onValueChanged(spec.key, it) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!generalError.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BotHubTheme.RadiusSm))
                        .background(BotHubTheme.ErrorSurface)
                        .border(1.dp, BotHubTheme.ErrorBorder, RoundedCornerShape(BotHubTheme.RadiusSm))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = BotHubTheme.Error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = generalError,
                        color = BotHubTheme.Error,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (saveSuccess) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BotHubTheme.RadiusSm))
                        .background(BotHubTheme.SuccessSurface)
                        .border(1.dp, BotHubTheme.SuccessBorder, RoundedCornerShape(BotHubTheme.RadiusSm))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BotHubTheme.Success,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Credentials encrypted & saved successfully!",
                        color = BotHubTheme.Success,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = onSaveClick,
                enabled = !isSaving,
                shape = RoundedCornerShape(BotHubTheme.RadiusMd),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BotHubTheme.PrimaryBlue,
                    contentColor = Color.White
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Encrypting & Saving...", fontSize = 14.sp)
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save & Continue", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = spec.label + if (spec.isRequired) " *" else " (Optional)",
            color = BotHubTheme.PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        if (!spec.description.isNullOrBlank()) {
            Text(
                text = spec.description,
                color = BotHubTheme.SecondaryText,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChanged,
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage != null,
            placeholder = {
                Text(
                    text = if (spec.isSecret) "Enter token (e.g. 123456:ABC-DEF)" else "Enter value",
                    color = BotHubTheme.MutedText,
                    fontSize = 13.sp
                )
            },
            visualTransformation = if (spec.isSecret && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            trailingIcon = if (spec.isSecret) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide token" else "Show token",
                            tint = BotHubTheme.SecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else null,
            shape = RoundedCornerShape(BotHubTheme.RadiusMd),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = BotHubTheme.ElevatedSurface,
                unfocusedContainerColor = BotHubTheme.ElevatedSurface,
                focusedBorderColor = BotHubTheme.PrimaryBlue,
                unfocusedBorderColor = BotHubTheme.Border,
                focusedTextColor = BotHubTheme.PrimaryText,
                unfocusedTextColor = BotHubTheme.PrimaryText,
                errorBorderColor = BotHubTheme.Error
            ),
            singleLine = true
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = BotHubTheme.Error,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}
