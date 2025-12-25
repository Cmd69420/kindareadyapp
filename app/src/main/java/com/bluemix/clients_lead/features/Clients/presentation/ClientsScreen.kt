package com.bluemix.clients_lead.features.Clients.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bluemix.clients_lead.domain.model.Client
import com.bluemix.clients_lead.features.Clients.vm.*
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
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToCreateClient: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSearchBar by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadExcelFile(context, it) }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.background(Color(0xFF000000)),
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateClient,
                containerColor = Color(0xFF5E92F3),
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Client"
                )
            }
        },
        topBar = {
            TopBar(
                colors = TopBarDefaults.topBarColors(
                    containerColor = Color(0xFF000000),
                    scrolledContainerColor = Color(0xFF1A1A1A)
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
                        color = Color.White
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { showSearchBar = !showSearchBar }) {
                            Icon(
                                imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (showSearchBar) "Close search" else "Search",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { launcher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
                            enabled = !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF5E92F3),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = "Upload Excel",
                                    tint = Color.White
                                )
                            }
                        }

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
                                tint = Color.White,
                                modifier = Modifier.graphicsLayer { rotationZ = rotation }
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = {
            AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (uiState.error?.contains("success", ignoreCase = true) == true)
                                Color(0xFF4CAF50)
                            else Color(0xFFFF5252)
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
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFF000000))
            ) {
                // Search mode toggle and search field
                AnimatedVisibility(
                    visible = showSearchBar,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Search mode toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchModeButton(
                                label = "Local",
                                icon = Icons.Default.MyLocation,
                                selected = uiState.searchMode == SearchMode.LOCAL,
                                onClick = { viewModel.setSearchMode(SearchMode.LOCAL) },
                                modifier = Modifier.weight(1f)
                            )
                            SearchModeButton(
                                label = "Remote",
                                icon = Icons.Default.Public,
                                selected = uiState.searchMode == SearchMode.REMOTE,
                                onClick = { viewModel.setSearchMode(SearchMode.REMOTE) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Search text field
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.searchClients(it) },
                            placeholder = {
                                Text(
                                    text = when (uiState.searchMode) {
                                        SearchMode.LOCAL -> "Search in current area..."
                                        SearchMode.REMOTE -> "Search pincode, city, or name..."
                                    },
                                    color = Color(0xFF808080)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            leadingIcon = {
                                if (uiState.isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color(0xFF5E92F3),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFF808080)
                                    )
                                }
                            },
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
                                            tint = Color(0xFF808080)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                // Filter Chips (Local mode only)
                AnimatedVisibility(
                    visible = uiState.isTrackingEnabled && uiState.searchMode == SearchMode.LOCAL
                ) {
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
                }

                // ✅ Distance sort toggle (Local mode only)
                AnimatedVisibility(
                    visible = uiState.isTrackingEnabled &&
                            uiState.searchMode == SearchMode.LOCAL &&
                            uiState.filteredClients.isNotEmpty()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sort:",
                            style = AppTheme.typography.body2,
                            color = Color(0xFFB0B0B0)
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (uiState.sortByDistance) Color(0xFF5E92F3) else Color(0xFF2A2A2A))
                                .clickable { viewModel.toggleDistanceSort() }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = if (uiState.sortByDistance) Color.White else Color(0xFF808080),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (uiState.sortByDistance) "Nearest First" else "Default",
                                style = AppTheme.typography.label1,
                                color = if (uiState.sortByDistance) Color.White else Color(0xFF808080)
                            )
                        }
                    }
                }

                // Content
                Crossfade(
                    targetState = when {
                        uiState.isLoading -> "loading"
                        uiState.error != null && !uiState.error!!.contains("success", ignoreCase = true) -> "error"
                        uiState.searchMode == SearchMode.REMOTE && uiState.searchQuery.isNotBlank() -> "remote_results"
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
                        "remote_results" -> ClientsList(
                            clients = uiState.remoteResults,
                            onClientClick = onNavigateToDetail,
                            isRemote = true,
                            userLocation = uiState.userLocation,
                            showDistance = true
                        )
                        "empty" -> EmptyContent(
                            searchQuery = uiState.searchQuery,
                            filter = uiState.selectedFilter,
                            searchMode = uiState.searchMode
                        )
                        "content" -> ClientsList(
                            clients = uiState.filteredClients,
                            onClientClick = onNavigateToDetail,
                            isRemote = false,
                            userLocation = uiState.userLocation,
                            showDistance = uiState.sortByDistance
                        )
                    }
                }
            }

            if (!uiState.isTrackingEnabled) {
                TrackingRequiredOverlay(
                    modifier = Modifier.fillMaxSize(),
                    onEnableTracking = { viewModel.enableTracking() },
                    onRefreshStatus = { viewModel.refreshTrackingState() }
                )
            }
        }
    }
}

@Composable
private fun SearchModeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF5E92F3) else Color(0xFF2A2A2A),
        animationSpec = tween(200),
        label = "searchModeBackgroundColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color(0xFF808080),
        animationSpec = tween(200),
        label = "searchModeContentColor"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(
                onClick = onClick,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = AppTheme.typography.button,
                color = contentColor
            )
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
        targetValue = if (selected) Color.White else Color(0xFF2A2A2A),
        animationSpec = tween(200),
        label = "chipBackgroundColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) Color.Black else Color.White,
        animationSpec = tween(200),
        label = "chipTextColor"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(
                onClick = onClick,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = AppTheme.typography.label1,
            color = textColor
        )
    }
}

@Composable
private fun ClientsList(
    clients: List<Client>,
    onClientClick: (String) -> Unit,
    isRemote: Boolean = false,
    userLocation: Pair<Double, Double>?,
    showDistance: Boolean = false
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isRemote && clients.isNotEmpty()) {
            item {
                Text(
                    text = "Found ${clients.size} client${if (clients.size != 1) "s" else ""} • Sorted by distance",
                    style = AppTheme.typography.body2,
                    color = Color(0xFF5E92F3),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        itemsIndexed(
            items = clients,
            key = { _, client -> client.id }
        ) { index, client ->
            AnimatedClientCard(
                client = client,
                index = index,
                onClick = { onClientClick(client.id) },
                showDistance = showDistance,
                userLocation = userLocation
            )
        }
    }
}

@Composable
private fun AnimatedClientCard(
    client: Client,
    index: Int,
    onClick: () -> Unit,
    showDistance: Boolean = false,
    userLocation: Pair<Double, Double>? = null
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
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2A2A2A))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (released) {
                            onClick()
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
            AnimatedStatusIcon(status = client.status)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.name,
                    style = AppTheme.typography.h4,
                    color = Color.White
                )

                client.email?.let {
                    Text(
                        text = it,
                        style = AppTheme.typography.body2,
                        color = Color(0xFFB0B0B0)
                    )
                }

                client.phone?.let {
                    Text(
                        text = it,
                        style = AppTheme.typography.body3,
                        color = Color(0xFFB0B0B0)
                    )
                }

                // ✅ Distance indicator
                if (showDistance && userLocation != null) {
                    val distance = client.formatDistance(userLocation.first, userLocation.second)
                    distance?.let {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF5E92F3)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = it,
                                style = AppTheme.typography.label3,
                                color = Color(0xFF5E92F3)
                            )
                        }
                    }
                }

                AnimatedLocationBadge(hasLocation = client.hasLocation)
            }

            val arrowRotation by animateFloatAsState(
                targetValue = if (isPressed) -45f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "arrowRotation"
            )

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "View details",
                tint = Color(0xFF808080),
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
                    "active" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    "inactive" -> Color(0xFF404040)
                    "completed" -> Color(0xFF5E92F3).copy(alpha = 0.2f)
                    else -> Color(0xFF2A2A2A)
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
                "active" -> Color(0xFF4CAF50)
                "inactive" -> Color(0xFF808080)
                "completed" -> Color(0xFF5E92F3)
                else -> Color.White
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
            targetValue = if (hasLocation) Color(0xFF4CAF50) else Color(0xFF808080),
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
            color = Color(0xFFB0B0B0)
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
            CircularProgressIndicator(color = Color(0xFF5E92F3))
            Text(
                text = "Loading clients...",
                style = AppTheme.typography.body1,
                color = Color(0xFFB0B0B0)
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
                tint = Color(0xFFFF5252)
            )
            Text(
                text = error,
                style = AppTheme.typography.body1,
                color = Color(0xFFFF5252),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5E92F3),
                    contentColor = Color.White
                )
            ) {
                Text("Retry", style = AppTheme.typography.button)
            }
        }
    }
}

@Composable
private fun EmptyContent(
    searchQuery: String,
    filter: ClientFilter,
    searchMode: SearchMode
) {
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
                tint = Color(0xFF808080)
            )
            Text(
                text = when {
                    searchQuery.isNotEmpty() && searchMode == SearchMode.REMOTE ->
                        "No clients found matching\n\"$searchQuery\""
                    searchQuery.isNotEmpty() ->
                        "No clients found matching\n\"$searchQuery\""
                    else ->
                        "No ${filter.name.lowercase()} clients"
                },
                style = AppTheme.typography.body1,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TrackingRequiredOverlay(
    modifier: Modifier = Modifier,
    onEnableTracking: () -> Unit,
    onRefreshStatus: () -> Unit
) {
    Box(
        modifier = modifier
            .background(Color(0xFF000000).copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = Color(0xFF5E92F3)
            )

            Text(
                text = "Location tracking required",
                style = AppTheme.typography.h3,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "To protect client data and verify that you are in the correct area, background location tracking must remain active while viewing clients.",
                style = AppTheme.typography.body2,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onEnableTracking,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5E92F3),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Enable Location Tracking",
                    style = AppTheme.typography.button
                )
            }

            OutlinedButton(
                onClick = onRefreshStatus,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Refresh tracking status",
                    style = AppTheme.typography.button
                )
            }

            Text(
                text = "We only use your location to verify your working area and show nearby clients. Your data is transmitted securely and never shared with other users.",
                style = AppTheme.typography.body2,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}