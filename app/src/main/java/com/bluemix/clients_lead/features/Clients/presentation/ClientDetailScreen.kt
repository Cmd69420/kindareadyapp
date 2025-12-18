package com.bluemix.clients_lead.features.Clients.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bluemix.clients_lead.domain.model.Client
import com.bluemix.clients_lead.features.Clients.vm.ClientDetailViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import ui.AppTheme
import ui.components.Scaffold
import ui.components.Text
import ui.components.topbar.TopBar
import ui.components.topbar.TopBarDefaults

/**
 * Client detail screen following proper ViewModel patterns.
 * ViewModel is injected without parameters, clientId is passed via loadClient().
 */

@Composable
fun ClientDetailScreen(
    clientId: String,
    onNavigateBack: () -> Unit,
    viewModel: ClientDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(clientId) {
        viewModel.loadClient(clientId)
    }

    Scaffold(
        modifier = Modifier.background(AppTheme.colors.background),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopBar(
                colors = TopBarDefaults.topBarColors(
                    containerColor = AppTheme.colors.background,
                    scrolledContainerColor = AppTheme.colors.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {   // ✅ Add back button
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = AppTheme.colors.text
                        )
                    }
                    Text(
                        text = "Client Details",
                        style = AppTheme.typography.h2,
                        color = AppTheme.colors.text
                    )

                    var isRefreshing by remember { mutableStateOf(false) }
                    val rotation by animateFloatAsState(
                        targetValue = if (isRefreshing) 360f else 0f,
                        animationSpec = tween(600),
                        finishedListener = { isRefreshing = false },
                        label = "refreshRotation"
                    )

                    IconButton(
                        onClick = {
                            isRefreshing = true
                            viewModel.refresh()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = AppTheme.colors.text,
                            modifier = Modifier.graphicsLayer { rotationZ = rotation }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Crossfade(
            targetState = when {
                uiState.isLoading -> "loading"
                uiState.error != null -> "error"
                uiState.client != null -> "content"
                else -> "empty"
            },
            animationSpec = tween(300),
            label = "contentCrossfade"
        ) { state ->
            when (state) {
                "loading" -> LoadingContent(paddingValues)
                "error" -> ErrorContent(
                    paddingValues = paddingValues,
                    error = uiState.error ?: "Unknown error",
                    onRetry = { viewModel.refresh() }
                )
                "content" -> uiState.client?.let { client ->
                    AnimatedClientDetailContent(
                        paddingValues = paddingValues,
                        client = client,
                        onCall = { phone ->
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        },
                        onEmail = { email ->
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                            context.startActivity(intent)
                        },
                        onNavigate = { lat, lng ->
                            val uri = Uri.parse(
                                "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"
                            )
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            context.startActivity(intent)
                        }

                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = AppTheme.colors.primary)
            Text(
                text = "Loading client details...",
                style = AppTheme.typography.body1,
                color = AppTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun ErrorContent(
    paddingValues: PaddingValues,
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = AppTheme.colors.error
            )
            Text(
                text = error,
                style = AppTheme.typography.body1,
                color = AppTheme.colors.error
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun AnimatedClientDetailContent(
    paddingValues: PaddingValues,
    client: Client,
    onCall: (String) -> Unit,
    onEmail: (String) -> Unit,
    onNavigate: (Double, Double) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Animated Header Card
        AnimatedHeaderCard(client = client)

        // Contact Information Section
        AnimatedSection(
            title = "Contact Information",
            index = 1
        ) {
            client.phone?.let { phone ->
                AnimatedDetailRow(
                    icon = Icons.Default.Phone,
                    label = "Phone",
                    value = phone,
                    actionIcon = Icons.Default.Call,
                    onActionClick = { onCall(phone) },
                    index = 0
                )
            }

            client.email?.let { email ->
                AnimatedDetailRow(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = email,
                    actionIcon = Icons.Default.Send,
                    onActionClick = { onEmail(email) },
                    index = 1
                )
            }
        }

        // Address Information Section
        AnimatedSection(
            title = "Address",
            index = 2
        ) {
            client.address?.let { address ->
                AnimatedDetailRow(
                    icon = Icons.Default.LocationOn,
                    label = "Address",
                    value = address,
                    actionIcon = if (client.hasLocation && client.latitude != null && client.longitude != null) {
                        Icons.Default.Directions
                    } else null,
                    onActionClick = if (client.hasLocation && client.latitude != null && client.longitude != null) {
                        { onNavigate(client.latitude, client.longitude) }
                    } else null,
                    index = 0
                )
            }

            if (client.hasLocation && client.latitude != null && client.longitude != null) {
                AnimatedDetailRow(
                    icon = Icons.Default.MyLocation,
                    label = "Coordinates",
                    value = "${String.format("%.6f", client.latitude)}, ${String.format("%.6f", client.longitude)}",
                    index = 1
                )
            }
        }

        // Notes Section
        client.notes?.let { notes ->
            AnimatedSection(
                title = "Notes",
                index = 3
            ) {
                AnimatedNotesCard(notes = notes)
            }
        }
    }
}

@Composable
private fun AnimatedHeaderCard(client: Client) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "headerScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "headerAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.primary.copy(alpha = 0.1f))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar with pulse animation
            val infiniteTransition = rememberInfiniteTransition(label = "avatarPulse")
            val avatarScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "avatarScale"
            )

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(avatarScale)
                    .clip(CircleShape) // ✅ Makes it perfectly round
                    .background(AppTheme.colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = AppTheme.colors.onPrimary
                )
            }

            Text(
                text = client.name,
                style = AppTheme.typography.h1,
                color = AppTheme.colors.text
            )

            // Animated Status Badge
            AnimatedStatusBadge(status = client.status)
        }
    }
}

@Composable
private fun AnimatedStatusBadge(status: String) {
    val backgroundColor by animateColorAsState(
        targetValue = when (status) {
            "active" -> AppTheme.colors.success
            "inactive" -> AppTheme.colors.disabled
            "completed" -> AppTheme.colors.tertiary
            else -> AppTheme.colors.surface
        },
        animationSpec = tween(300),
        label = "badgeBackground"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = status.uppercase(),
            style = AppTheme.typography.label2,
            color = when (status) {
                "active" -> AppTheme.colors.onSuccess
                "inactive" -> AppTheme.colors.onDisabled
                "completed" -> AppTheme.colors.onTertiary
                else -> AppTheme.colors.text
            }
        )
    }
}

@Composable
private fun AnimatedSection(
    title: String,
    index: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay((index * 100).toLong())
        isVisible = true
    }

    val offsetY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 20.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "sectionOffsetY"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "sectionAlpha"
    )

    Column(
        modifier = Modifier
            .offset(y = offsetY)
            .graphicsLayer { this.alpha = alpha },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = AppTheme.typography.h3,
            color = AppTheme.colors.text
        )

        content()
    }
}

@Composable
private fun AnimatedDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    index: Int
) {
    var isVisible by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay((index * 80).toLong())
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = when {
            !isVisible -> 0.9f
            isPressed -> 0.98f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "rowScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "rowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.colors.surface)
            .then(
                if (onActionClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            }
                        )
                    }
                } else Modifier
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = AppTheme.colors.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = AppTheme.typography.label2,
                    color = AppTheme.colors.textSecondary
                )
                Text(
                    text = value,
                    style = AppTheme.typography.body1,
                    color = AppTheme.colors.text
                )
            }

            if (actionIcon != null && onActionClick != null) {
                var isActionPressed by remember { mutableStateOf(false) }
                val actionScale by animateFloatAsState(
                    targetValue = if (isActionPressed) 0.9f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "actionScale"
                )

                IconButton(
                    onClick = onActionClick,
                    modifier = Modifier.scale(actionScale)
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = "Action",
                        tint = AppTheme.colors.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedNotesCard(notes: String) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "notesScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "notesAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.colors.surface)
            .padding(16.dp)
    ) {
        Text(
            text = notes,
            style = AppTheme.typography.body1,
            color = AppTheme.colors.text
        )
    }
}