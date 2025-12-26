package com.bluemix.clients_lead.features.Clients.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.data.repository.OCRRepository
import com.bluemix.clients_lead.features.Clients.vm.ClientsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import timber.log.Timber

@Composable
fun CreateClientScreen(
    viewModel: ClientsViewModel = koinViewModel(),
    ocrRepository: OCRRepository = koinInject(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

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

    var showScanner by remember { mutableStateOf(false) }
    var isProcessingOCR by remember { mutableStateOf(false) }
    var ocrMessage by remember { mutableStateOf<String?>(null) }
    var showOCRConfirmation by remember { mutableStateOf(false) }
    var extractedInfo by remember { mutableStateOf<com.bluemix.clients_lead.data.repository.ExtractedClientInfo?>(null) }

    // Handle success navigation
    LaunchedEffect(uiState.createSuccess) {
        if (uiState.createSuccess) {
            kotlinx.coroutines.delay(1500)
            viewModel.resetCreateState()
            onNavigateBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        IconButton(onClick = onNavigateBack) {
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

                    // Enhanced OCR Scan Button
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF8B5CF6).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Quick Fill with AI",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Text(
                                text = "Scan a business card to automatically fill in details",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                lineHeight = 18.sp
                            )

                            Button(
                                onClick = { showScanner = true },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF8B5CF6),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isProcessingOCR
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isProcessingOCR) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "Processing...",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = "📸 Scan Business Card",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // OCR Status Message
                    AnimatedVisibility(
                        visible = ocrMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    ocrMessage?.startsWith("✅") == true ->
                                        Color(0xFF4CAF50).copy(alpha = 0.15f)
                                    ocrMessage?.startsWith("❌") == true ->
                                        Color(0xFFFF5252).copy(alpha = 0.15f)
                                    else -> Color(0xFF5E92F3).copy(alpha = 0.15f)
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ocrMessage ?: "",
                                    fontSize = 14.sp,
                                    color = when {
                                        ocrMessage?.startsWith("✅") == true -> Color(0xFF4CAF50)
                                        ocrMessage?.startsWith("❌") == true -> Color(0xFFFF5252)
                                        else -> Color(0xFF5E92F3)
                                    },
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    // Basic Information Section
                    SectionHeader("Basic Information")

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

                    // Location Section
                    SectionHeader("Location Details")

                    CustomTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = "Address",
                        placeholder = "Enter full address",
                        leadingIcon = "🏠",
                        minLines = 3,
                        maxLines = 5
                    )

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

                    // Notes Section
                    SectionHeader("Additional Notes (Optional)")

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
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFF5252).copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(text = "❌", fontSize = 20.sp)
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
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(text = "✅", fontSize = 20.sp)
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
                                Text(text = "➕", fontSize = 18.sp)
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

        // Scanner Dialog
        if (showScanner) {
            ImageCaptureDialog(
                onImageCaptured = { bitmap ->
                    scope.launch {
                        isProcessingOCR = true
                        ocrMessage = "🔍 Extracting text from image..."

                        when (val result = ocrRepository.extractTextFromImage(bitmap)) {
                            is AppResult.Success -> {
                                val clientInfo = ocrRepository.parseClientInfo(result.data)
                                extractedInfo = clientInfo

                                Timber.d("📋 Extracted info: $clientInfo")
                                Timber.d("🎯 Confidence: ${clientInfo.confidence}")

                                if (clientInfo.confidence < 0.3f) {
                                    ocrMessage = "⚠️ Low quality scan. Please review and correct the information."
                                } else {
                                    ocrMessage = "✅ Information extracted successfully!"
                                }

                                // Smart auto-fill: only fill empty fields
                                clientInfo.name?.let { if (name.isBlank()) name = it }
                                clientInfo.phone?.let { if (phone.isBlank()) phone = it }
                                clientInfo.email?.let { if (email.isBlank()) email = it }
                                clientInfo.address?.let { if (address.isBlank()) address = it }
                                clientInfo.pincode?.let { if (pincode.isBlank()) pincode = it }

                                // Show confidence-based message
                                if (clientInfo.confidence >= 0.7f) {
                                    showOCRConfirmation = true
                                }

                                kotlinx.coroutines.delay(3000)
                                ocrMessage = null
                            }
                            is AppResult.Error -> {
                                Timber.e("OCR Error: ${result.error.message}")
                                ocrMessage = "❌ ${result.error.message}"
                                kotlinx.coroutines.delay(3000)
                                ocrMessage = null
                            }
                        }

                        isProcessingOCR = false
                        showScanner = false
                    }
                },
                onDismiss = {
                    showScanner = false
                    isProcessingOCR = false
                }
            )
        }

        // OCR Confirmation Dialog
        if (showOCRConfirmation && extractedInfo != null) {
            OCRConfirmationDialog(
                extractedInfo = extractedInfo!!,
                onDismiss = { showOCRConfirmation = false }
            )
        }
    }
}

@Composable
private fun OCRConfirmationDialog(
    extractedInfo: com.bluemix.clients_lead.data.repository.ExtractedClientInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Information Extracted",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "The following information was found:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )

                Divider(color = Color.White.copy(alpha = 0.1f))

                extractedInfo.name?.let {
                    InfoRow("Name", it)
                }
                extractedInfo.phone?.let {
                    InfoRow("Phone", it)
                }
                extractedInfo.email?.let {
                    InfoRow("Email", it)
                }
                extractedInfo.address?.let {
                    InfoRow("Address", it)
                }
                extractedInfo.pincode?.let {
                    InfoRow("Pincode", it)
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                Text(
                    text = "Confidence: ${(extractedInfo.confidence * 100).toInt()}%",
                    color = Color(0xFF5E92F3),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5E92F3)
                )
            ) {
                Text("Got it!")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            modifier = Modifier.weight(0.3f)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.7f),
            textAlign = TextAlign.End
        )
    }
}

/* ---------------- REUSABLE COMPONENTS ---------------- */

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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            textStyle = LocalTextStyle.current.copy(
                color = Color.White,  // This ensures text is white while typing
                fontSize = 14.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                // Text colors - all set to white
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                disabledTextColor = Color.White.copy(alpha = 0.5f),
                errorTextColor = Color.White,

                // Border colors
                focusedBorderColor = Color(0xFF5E92F3),
                unfocusedBorderColor = Color(0xFF404040),
                errorBorderColor = Color(0xFFFF5252),
                disabledBorderColor = Color(0xFF404040).copy(alpha = 0.5f),

                // Container/background colors
                focusedContainerColor = Color(0xFF1A1A1A),
                unfocusedContainerColor = Color(0xFF1A1A1A),
                errorContainerColor = Color(0xFF1A1A1A),
                disabledContainerColor = Color(0xFF1A1A1A),

                // Cursor color
                cursorColor = Color(0xFF5E92F3),
                errorCursorColor = Color(0xFFFF5252),

                // Leading icon colors
                focusedLeadingIconColor = Color.White.copy(alpha = 0.7f),
                unfocusedLeadingIconColor = Color.White.copy(alpha = 0.5f),
                errorLeadingIconColor = Color.White.copy(alpha = 0.7f),

                // Placeholder color (already defined above but this ensures consistency)
                focusedPlaceholderColor = Color(0xFF808080),
                unfocusedPlaceholderColor = Color(0xFF808080),
                errorPlaceholderColor = Color(0xFF808080),
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