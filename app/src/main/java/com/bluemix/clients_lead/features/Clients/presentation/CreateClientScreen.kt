package com.bluemix.clients_lead.features.Clients.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluemix.clients_lead.features.Clients.vm.ClientsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CreateClientScreen(
    viewModel: ClientsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var pincodeError by remember { mutableStateOf<String?>(null) }

    // Handle success navigation
    LaunchedEffect(uiState.createSuccess) {
        if (uiState.createSuccess) {
            kotlinx.coroutines.delay(1500)
            viewModel.resetCreateState()
            onNavigateBack()
        }
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(top = 80.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF000000))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel),
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = "Create New Client",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.size(40.dp))
                    }

                    // Section: Basic Information
                    SectionHeader(title = "Basic Information")

                    // Name Field (Required)
                    CustomTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = if (it.isBlank()) "Name is required" else null
                        },
                        label = "Client Name",
                        placeholder = "Enter full name",
                        isRequired = true,
                        isError = nameError != null,
                        errorMessage = nameError,
                        leadingIcon = "👤"
                    )

                    // Phone Field
                    CustomTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            phoneError = if (it.isNotBlank() && !it.matches(Regex("^[0-9]{10,15}$"))) {
                                "Enter valid phone (10-15 digits)"
                            } else null
                        },
                        label = "Phone Number",
                        placeholder = "Enter phone number",
                        isError = phoneError != null,
                        errorMessage = phoneError,
                        leadingIcon = "📞",
                        keyboardType = KeyboardType.Phone
                    )

                    // Email Field
                    CustomTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = if (it.isNotBlank() &&
                                !android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches()) {
                                "Enter valid email address"
                            } else null
                        },
                        label = "Email Address",
                        placeholder = "Enter email",
                        isError = emailError != null,
                        errorMessage = emailError,
                        leadingIcon = "📧",
                        keyboardType = KeyboardType.Email
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Section: Location Details
                    SectionHeader(title = "Location Details")

                    // Address Field
                    CustomTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = "Address",
                        placeholder = "Enter full address",
                        leadingIcon = "🏠",
                        minLines = 3,
                        maxLines = 5
                    )

                    // Pincode Field
                    CustomTextField(
                        value = pincode,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                pincode = it
                                pincodeError = if (it.isNotBlank() && it.length != 6) {
                                    "Pincode must be 6 digits"
                                } else null
                            }
                        },
                        label = "Pincode",
                        placeholder = "Enter 6-digit pincode",
                        isError = pincodeError != null,
                        errorMessage = pincodeError,
                        leadingIcon = "📍",
                        keyboardType = KeyboardType.Number
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Section: Additional Notes
                    SectionHeader(title = "Additional Notes (Optional)")

                    CustomTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Notes",
                        placeholder = "Add any additional information",
                        leadingIcon = "📝",
                        minLines = 4,
                        maxLines = 8
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Error Message
                    AnimatedVisibility(
                        visible = uiState.createError != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "❌",
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = uiState.createError ?: "",
                                    fontSize = 14.sp,
                                    color = Color(0xFFFF5252),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    // Success Message
                    AnimatedVisibility(
                        visible = uiState.createSuccess,
                        enter = fadeIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "✅",
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = "Client created successfully!",
                                    fontSize = 14.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Create Button
                    Button(
                        onClick = {
                            // Validate all fields
                            var hasErrors = false

                            if (name.isBlank()) {
                                nameError = "Name is required"
                                hasErrors = true
                            }

                            if (email.isNotBlank() &&
                                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                emailError = "Enter valid email address"
                                hasErrors = true
                            }

                            if (phone.isNotBlank() && !phone.matches(Regex("^[0-9]{10,15}$"))) {
                                phoneError = "Enter valid phone (10-15 digits)"
                                hasErrors = true
                            }

                            if (pincode.isNotBlank() && pincode.length != 6) {
                                pincodeError = "Pincode must be 6 digits"
                                hasErrors = true
                            }

                            if (!hasErrors) {
                                viewModel.createClientAction(
                                    name = name.trim(),
                                    phone = phone.trim().ifBlank { null },
                                    email = email.trim().ifBlank { null },
                                    address = address.trim().ifBlank { null },
                                    pincode = pincode.trim().ifBlank { null },
                                    notes = notes.trim().ifBlank { null }
                                )
                            }
                        },
                        enabled = !uiState.isCreating && name.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5E92F3),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF2A2A2A),
                            disabledContentColor = Color(0xFF808080)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "➕",
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "Create Client",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF5E92F3),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    leadingIcon: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    maxLines: Int = 1
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Label
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFB0B0B0)
            )
            if (isRequired) {
                Text(
                    text = "*",
                    fontSize = 14.sp,
                    color = Color(0xFFFF5252)
                )
            }
        }

        // Text Field
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color(0xFF808080),
                    fontSize = 14.sp
                )
            },
            leadingIcon = leadingIcon?.let {
                {
                    Text(
                        text = it,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            },
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            minLines = minLines,
            maxLines = maxLines,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF5E92F3),
                unfocusedBorderColor = Color(0xFF404040),
                errorBorderColor = Color(0xFFFF5252),
                focusedContainerColor = Color(0xFF1A1A1A),
                unfocusedContainerColor = Color(0xFF1A1A1A),
                errorContainerColor = Color(0xFF1A1A1A),
                cursorColor = Color(0xFF5E92F3)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Error Message
        AnimatedVisibility(
            visible = isError && errorMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = errorMessage ?: "",
                fontSize = 12.sp,
                color = Color(0xFFFF5252),
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}