package com.bluemix.clients_lead.features.Clients.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp as Dp
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Upload
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import com.bluemix.clients_lead.features.Clients.vm.ClientDetailViewModel
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bluemix.clients_lead.domain.model.Client
import com.bluemix.clients_lead.features.Clients.vm.ClientFilter
import com.bluemix.clients_lead.features.Clients.vm.ClientsViewModel
import org.koin.androidx.compose.koinViewModel
import ui.AppTheme
import ui.components.progressindicators.CircularProgressIndicator
import ui.components.textfield.TextField
import ui.components.topbar.TopBar
import ui.components.topbar.TopBarDefaults
import ui.foundation.ripple




@Composable
fun ClientsScreen(
    viewModel: ClientsViewModel = koinViewModel(),
    onNavigateToDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSearchBar by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // File picker launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.uploadExcelFile(context, it)
        }
    }

    // Auto-clear error/success message after 3 seconds
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
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
                ) {
                    Text(
                        text = "Clients",
                        style = AppTheme.typography.h2,
                        color = AppTheme.colors.text
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Search button
                        IconButton(
                            onClick = { showSearchBar = !showSearchBar }
                        ) {
                            Icon(
                                imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (showSearchBar) "Close search" else "Search",
                                tint = AppTheme.colors.text
                            )
                        }

                        // Upload button with loading state
                        IconButton(
                            onClick = {
                                launcher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            },
                            enabled = !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = AppTheme.colors.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = "Upload Excel",
                                    tint = AppTheme.colors.text
                                )
                            }
                        }

                        // Refresh button
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
                            },
                            enabled = !uiState.isLoading
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
        },
        snackbarHost = {
            // Show error/success message at bottom
            AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (uiState.error?.contains("success", ignoreCase = true) == true)
                                AppTheme.colors.success.copy(alpha = 0.9f)
                            else
                                AppTheme.colors.error.copy(alpha = 0.9f)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.error ?: "",
                            style = AppTheme.typography.body2,
                            color = AppTheme.colors.onPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearError() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = AppTheme.colors.onPrimary
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppTheme.colors.background)
        ) {
            // Animated Search Field
            AnimatedVisibility(
                visible = showSearchBar,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.searchClients(it) },
                    placeholder = { Text("Search clients...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = uiState.searchQuery.isNotEmpty(),
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            IconButton(onClick = { viewModel.searchClients("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = AppTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                )
            }

            // Filter Chips with scroll animation
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = listOf(
                        ClientFilter.ALL to "All",
                        ClientFilter.ACTIVE to "Active",
                        ClientFilter.INACTIVE to "Inactive",
                        ClientFilter.COMPLETED to "Completed"
                    )
                ) { (filter, label) ->
                    AnimatedFilterChip(
                        label = label,
                        selected = uiState.selectedFilter == filter,
                        onClick = { viewModel.setFilter(filter) }
                    )
                }
            }

            // Content with crossfade
            Crossfade(
                targetState = when {
                    uiState.isLoading -> "loading"
                    uiState.error != null && !uiState.error!!.contains("success", ignoreCase = true) -> "error"
                    uiState.filteredClients.isEmpty() -> "empty"
                    else -> "content"
                },
                animationSpec = tween(300),
                label = "contentCrossfade"
            ) { state ->
                when (state) {
                    "loading" -> LoadingContent()
                    "error" -> ErrorContent(
                        error = uiState.error ?: "Unknown error",
                        onRetry = { viewModel.refresh() }
                    )

                    "empty" -> EmptyContent(
                        searchQuery = uiState.searchQuery,
                        filter = uiState.selectedFilter
                    )

                    "content" -> ClientsList(
                        clients = uiState.filteredClients,
                        onClientClick = onNavigateToDetail
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chipScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) AppTheme.colors.primary else AppTheme.colors.surface,
        animationSpec = tween(200),
        label = "chipBackgroundColor"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(
                onClick = onClick,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = AppTheme.typography.label1,
            color = if (selected) AppTheme.colors.onPrimary else AppTheme.colors.text
        )
    }
}

@Composable
private fun ClientsList(
    clients: List<Client>,
    onClientClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = clients,
            key = { _, client -> client.id }
        ) { index, client ->
            AnimatedClientCard(
                client = client,
                index = index,
                onClick = { onClientClick(client.id) }
            )
        }
    }
}

@Composable
private fun AnimatedClientCard(
    client: Client,
    index: Int,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300, delayMillis = index * 50),
        label = "cardAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(alpha)
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.colors.surface)
            // ✅ Combined press and click in one pointerInput
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (released) {
                            onClick() // ✅ Call navigation here
                        }
                    }
                )
            }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon with pulse animation
            AnimatedStatusIcon(status = client.status)

            Spacer(modifier = Modifier.width(12.dp))

            // Client Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.name,
                    style = AppTheme.typography.h4,
                    color = AppTheme.colors.text
                )

                client.email?.let {
                    Text(
                        text = it,
                        style = AppTheme.typography.body2,
                        color = AppTheme.colors.textSecondary
                    )
                }

                client.phone?.let {
                    Text(
                        text = it,
                        style = AppTheme.typography.body3,
                        color = AppTheme.colors.textSecondary
                    )
                }

                // Location badge with animation
                AnimatedLocationBadge(hasLocation = client.hasLocation)
            }

            // Animated arrow
            val arrowRotation by animateFloatAsState(
                targetValue = if (isPressed) -45f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "arrowRotation"
            )

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "View details",
                tint = AppTheme.colors.textSecondary,
                modifier = Modifier.graphicsLayer { rotationZ = arrowRotation }
            )
        }
    }
}

@Composable
private fun AnimatedStatusIcon(status: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == "active") 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusIconScale"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(if (status == "active") scale else 1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when (status) {
                    "active" -> AppTheme.colors.success.copy(alpha = 0.1f)
                    "inactive" -> AppTheme.colors.disabled
                    "completed" -> AppTheme.colors.tertiary.copy(alpha = 0.1f)
                    else -> AppTheme.colors.surface
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when (status) {
                "active" -> Icons.Default.Person
                "inactive" -> Icons.Default.PersonOff
                "completed" -> Icons.Default.CheckCircle
                else -> Icons.Default.Person
            },
            contentDescription = status,
            tint = when (status) {
                "active" -> AppTheme.colors.success
                "inactive" -> AppTheme.colors.textDisabled
                "completed" -> AppTheme.colors.tertiary
                else -> AppTheme.colors.text
            }
        )
    }
}

@Composable
private fun AnimatedLocationBadge(hasLocation: Boolean) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tint by animateColorAsState(
            targetValue = if (hasLocation) AppTheme.colors.success else AppTheme.colors.textDisabled,
            animationSpec = tween(200),
            label = "locationIconTint"
        )

        Icon(
            imageVector = if (hasLocation) Icons.Default.LocationOn else Icons.Default.LocationOff,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tint
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (hasLocation) "Has location" else "No location",
            style = AppTheme.typography.label3,
            color = AppTheme.colors.textSecondary
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = AppTheme.colors.primary)
            Text(
                text = "Loading clients...",
                style = AppTheme.typography.body1,
                color = AppTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun ErrorContent(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
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
                modifier = Modifier.size(48.dp),
                tint = AppTheme.colors.error
            )
            Text(
                text = error,
                style = AppTheme.typography.body1,
                color = AppTheme.colors.error
            )
            Button(onClick = onRetry) {
                Text("Retry", style = AppTheme.typography.button)
            }
        }
    }
}

@Composable
private fun EmptyContent(searchQuery: String, filter: ClientFilter) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PersonOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = AppTheme.colors.textDisabled
            )
            Text(
                text = if (searchQuery.isNotEmpty()) {
                    "No clients found matching\n\"$searchQuery\""
                } else {
                    "No ${filter.name.lowercase()} clients"
                },
                style = AppTheme.typography.body1,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
