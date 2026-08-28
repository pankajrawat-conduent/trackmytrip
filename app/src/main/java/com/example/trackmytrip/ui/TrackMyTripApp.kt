@file:OptIn(ExperimentalLayoutApi::class)

package com.example.trackmytrip.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackmytrip.data.TripStore
import com.example.trackmytrip.model.DistanceSource
import com.example.trackmytrip.model.DistanceUnit
import com.example.trackmytrip.model.ExpenseCategory
import com.example.trackmytrip.model.JourneyUpdate
import com.example.trackmytrip.model.LocationSource
import com.example.trackmytrip.model.TransportMode
import com.example.trackmytrip.model.Trip
import com.example.trackmytrip.model.TripStatus
import com.example.trackmytrip.model.TripType
import com.example.trackmytrip.ui.theme.TrackMyTripTheme
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackMyTripApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tripStore = remember(context) { TripStore(context) }
    val trips = remember { mutableStateListOf<Trip>() }
    var hasLoaded by remember { mutableStateOf(false) }
    var selectedTripId by remember { mutableStateOf<Long?>(null) }
    var showTripEditor by remember { mutableStateOf(false) }
    var editingTrip by remember { mutableStateOf<Trip?>(null) }
    var showUpdateEditor by remember { mutableStateOf(false) }
    var editingUpdate by remember { mutableStateOf<JourneyUpdate?>(null) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(true) }
    var showSignIn by remember { mutableStateOf(false) }
    var selectedDestination by remember { mutableStateOf("Home") }
    var nextId by remember { mutableLongStateOf(1000L) }

    LaunchedEffect(Unit) {
        val storedTrips = tripStore.loadTrips()
        trips.clear()
        trips.addAll(storedTrips.ifEmpty { sampleTrips() })
        nextId = ((trips.maxOfOrNull { trip ->
            maxOf(trip.id, trip.updates.maxOfOrNull { it.id } ?: 0L)
        } ?: 0L) + 1L).coerceAtLeast(1000L)
        hasLoaded = true
    }

    LaunchedEffect(trips.toList(), hasLoaded) {
        if (hasLoaded) tripStore.saveTrips(trips)
    }

    val selectedTrip = selectedTripId?.let { id -> trips.firstOrNull { it.id == id } }

    if (showOnboarding) {
        OnboardingScreen(
            onContinueAsGuest = { showOnboarding = false },
            onSignIn = {
                showOnboarding = false
                showSignIn = true
            }
        )
        return
    }

    if (showSignIn) {
        AuthScreen(
            onContinueAsGuest = { showSignIn = false },
            onSignedIn = { showSignIn = false }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "TrackMyTrip", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Offline journey memory MVP",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = {
                if (selectedTrip == null) {
                    editingTrip = null
                    showTripEditor = true
                } else {
                    editingUpdate = null
                    showUpdateEditor = true
                }
            }) {
                Text(
                    text = if (selectedTrip == null) "New trip" else "Add update",
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        },
        bottomBar = {
            TrackBottomNavigation(
                selectedDestination = if (selectedTrip == null) selectedDestination else "Trips",
                onDestinationSelected = { destination ->
                    selectedDestination = destination
                    if (destination != "Trips") selectedTripId = null
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (selectedTrip == null) {
                when (selectedDestination) {
                    "Insights" -> InsightsScreen(trips = trips)
                    "Settings" -> SettingsScreen(onPrivacy = { showPrivacyDialog = true })
                    "Timeline" -> TimelineScreen(
                        trips = trips,
                        onTripSelected = {
                            selectedDestination = "Trips"
                            selectedTripId = it.id
                        }
                    )
                    "Expenses" -> ExpensesScreen(trips = trips)
                    "Map" -> MapScreen(trips = trips)
                    "Share" -> SharePreviewScreen(
                        trips = trips,
                        onShare = { shareText(context, trips.joinToString("\n\n") { it.toShareSummary() }) }
                    )
                    else -> TripDashboard(
                        trips = trips,
                        onTripClick = {
                            selectedDestination = "Trips"
                            selectedTripId = it.id
                        },
                        onCreateTrip = {
                            editingTrip = null
                            showTripEditor = true
                        },
                        onEditTrip = {
                            editingTrip = it
                            showTripEditor = true
                        },
                        onDeleteTrip = { trip -> trips.removeAll { it.id == trip.id } },
                        onArchiveToggle = { trip ->
                            trips.replaceTrip(
                                trip.copy(
                                    status = if (trip.status == TripStatus.Archived) {
                                        TripStatus.Active
                                    } else {
                                        TripStatus.Archived
                                    }
                                )
                            )
                        },
                        onPrivacy = { showPrivacyDialog = true },
                        onShareAll = {
                            shareText(context, trips.joinToString("\n\n") { it.toShareSummary() })
                        }
                    )
                }
            } else {
                TripDetailScreen(
                    trip = selectedTrip,
                    onBack = { selectedTripId = null },
                    onEditTrip = {
                        editingTrip = selectedTrip
                        showTripEditor = true
                    },
                    onDeleteTrip = {
                        trips.removeAll { it.id == selectedTrip.id }
                        selectedTripId = null
                    },
                    onArchiveToggle = {
                        trips.replaceTrip(
                            selectedTrip.copy(
                                status = if (selectedTrip.status == TripStatus.Archived) {
                                    TripStatus.Active
                                } else {
                                    TripStatus.Archived
                                }
                            )
                        )
                    },
                    onCompleteTrip = {
                        trips.replaceTrip(selectedTrip.copy(status = TripStatus.Completed))
                    },
                    onAddUpdate = {
                        editingUpdate = null
                        showUpdateEditor = true
                    },
                    onEditUpdate = {
                        editingUpdate = it
                        showUpdateEditor = true
                    },
                    onDeleteUpdate = { update ->
                        trips.replaceTrip(selectedTrip.copy(updates = selectedTrip.updates.filterNot { it.id == update.id }))
                    },
                    onShareTrip = { showSummaryDialog = true }
                )
            }
        }
    }

    if (showTripEditor) {
        TripEditorDialog(
            trip = editingTrip,
            onDismiss = { showTripEditor = false },
            onSave = { draft ->
                val savedTrip = draft.copy(
                    id = draft.id.takeIf { it != 0L } ?: nextId++,
                    updates = editingTrip?.updates ?: draft.updates
                )
                if (editingTrip == null) {
                    trips.add(0, savedTrip)
                    selectedTripId = savedTrip.id
                } else {
                    trips.replaceTrip(savedTrip)
                }
                showTripEditor = false
            }
        )
    }

    if (showUpdateEditor && selectedTrip != null) {
        UpdateEditorDialog(
            update = editingUpdate,
            onDismiss = { showUpdateEditor = false },
            onSave = { draft ->
                val savedUpdate = draft.copy(id = draft.id.takeIf { it != 0L } ?: nextId++)
                val updates = if (editingUpdate == null) {
                    listOf(savedUpdate) + selectedTrip.updates
                } else {
                    selectedTrip.updates.map { if (it.id == savedUpdate.id) savedUpdate else it }
                }
                trips.replaceTrip(selectedTrip.copy(updates = updates))
                showUpdateEditor = false
            }
        )
    }

    if (showPrivacyDialog) {
        ConfirmDialog(
            title = "Delete all local data?",
            message = "This clears every trip, journey update, expense, and attachment reference stored on this device.",
            confirmText = "Delete all",
            onDismiss = { showPrivacyDialog = false },
            onConfirm = {
                tripStore.clear()
                trips.clear()
                selectedTripId = null
                showPrivacyDialog = false
            }
        )
    }

    if (showSummaryDialog && selectedTrip != null) {
        SummaryDialog(
            summary = selectedTrip.toShareSummary(),
            onDismiss = { showSummaryDialog = false },
            onShare = { shareText(context, selectedTrip.toShareSummary()) }
        )
    }
}

@Composable
private fun TrackBottomNavigation(
    selectedDestination: String,
    onDestinationSelected: (String) -> Unit
) {
    val destinations = listOf(
        "Home" to "⌂",
        "Trips" to "⌖",
        "Insights" to "◈",
        "Settings" to "⚙"
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        destinations.forEach { (label, symbol) ->
            NavigationBarItem(
                selected = selectedDestination == label,
                onClick = { onDestinationSelected(label) },
                icon = {
                    if (label == "Trips") {
                        Box {
                            Text(symbol)
                            Badge(modifier = Modifier.align(Alignment.TopEnd))
                        }
                    } else {
                        Text(symbol)
                    }
                },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun TripDashboard(
    trips: List<Trip>,
    onTripClick: (Trip) -> Unit,
    onCreateTrip: () -> Unit,
    onEditTrip: (Trip) -> Unit,
    onDeleteTrip: (Trip) -> Unit,
    onArchiveToggle: (Trip) -> Unit,
    onPrivacy: () -> Unit,
    onShareAll: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<TripStatus?>(null) }
    val visibleTrips = trips.filter { trip ->
        val matchesQuery = query.isBlank() ||
            trip.title.contains(query, ignoreCase = true) ||
            trip.destination.contains(query, ignoreCase = true)
        val matchesStatus = statusFilter == null || trip.status == statusFilter
        matchesQuery && matchesStatus
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HeroCard(onCreateTrip = onCreateTrip) }
        item { DashboardQuickActions() }
        item { StatsRow(trips = trips) }
        item { PrivacyAndShareRow(onPrivacy = onPrivacy, onShareAll = onShareAll, enabled = trips.isNotEmpty()) }
        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = query,
                onValueChange = { query = it },
                label = { Text("Search trips or destinations") },
                singleLine = true
            )
        }
        item {
            StatusFilter(
                selected = statusFilter,
                onSelected = { statusFilter = it }
            )
        }
        item {
            Text(
                text = "My Trips",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        if (visibleTrips.isEmpty()) {
            item {
                EmptyState(
                    title = "No trips found",
                    message = "Create a trip or clear your filters to continue."
                )
            }
        } else {
            items(visibleTrips, key = { it.id }) { trip ->
                TripCard(
                    trip = trip,
                    onClick = { onTripClick(trip) },
                    onEdit = { onEditTrip(trip) },
                    onDelete = { onDeleteTrip(trip) },
                    onArchiveToggle = { onArchiveToggle(trip) }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun OnboardingScreen(
    onContinueAsGuest: () -> Unit,
    onSignIn: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Text(
                    modifier = Modifier.align(Alignment.TopStart),
                    text = "📍 + journal",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    modifier = Modifier.align(Alignment.BottomStart),
                    text = "Capture journeys,\nnot just locations.",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        item {
            OnboardingValueCard(
                title = "Remember the moments that mattered",
                message = "Notes, photos, places, transport, odometer evidence, and expenses stay organized in one travel story."
            )
        }
        item {
            OnboardingValueCard(
                title = "Your location stays in your control",
                message = "Only when you tap. Never in the background. Edit or remove anytime."
            )
        }
        item {
            OnboardingValueCard(
                title = "Made for the road less connected",
                message = "Log offline. Keep your battery for the journey. Guest data stays on this device."
            )
        }
        item {
            Button(modifier = Modifier.fillMaxWidth(), onClick = onContinueAsGuest) {
                Text("Continue as guest")
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onSignIn) {
                Text("Sign in")
            }
        }
    }
}

@Composable
private fun OnboardingValueCard(title: String, message: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(modifier = Modifier.padding(top = 6.dp), text = message)
        }
    }
}

@Composable
private fun AuthScreen(onContinueAsGuest: () -> Unit, onSignedIn: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome back", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = "Accounts are optional for this MVP. You can continue locally anytime.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true
        )
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            enabled = email.isNotBlank() && password.isNotBlank(),
            onClick = onSignedIn
        ) {
            Text("Sign in")
        }
        TextButton(modifier = Modifier.fillMaxWidth(), onClick = onContinueAsGuest) {
            Text("Continue as guest")
        }
    }
}

@Composable
private fun HeroCard(onCreateTrip: () -> Unit) {
    Card(shape = RoundedCornerShape(28.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                            Color(0xFF2D5E50)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Capture journeys, not just locations.",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = "Log offline. Save meaningful checkpoints. Share the story when you are ready.",
                    color = MaterialTheme.colorScheme.onPrimary
                )
                FlowRow(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                        Text(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            text = "Only when you tap",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(50)) {
                        Text(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            text = "Offline-first",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Button(
                    modifier = Modifier.padding(top = 20.dp),
                    onClick = onCreateTrip
                ) {
                    Text("Create new trip")
                }
            }
        }
    }
}

@Composable
private fun DashboardQuickActions() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Figma screen set", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = "Dedicated MVP surfaces are available for Trips, Timeline, Insights, Settings, plus expense, map, and share sections inside trip flows."
            )
        }
    }
}

@Composable
private fun StatsRow(trips: List<Trip>) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            modifier = Modifier.weight(1f),
            value = trips.size.toString(),
            label = "Trips"
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = trips.flatMap { it.updates }.distinctBy { it.placeName }.size.toString(),
            label = "Places"
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = "₹${trips.sumOf { it.totalExpense }.toInt()}",
            label = "Expenses"
        )
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PrivacyAndShareRow(onPrivacy: () -> Unit, onShareAll: () -> Unit, enabled: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(modifier = Modifier.weight(1f), onClick = onPrivacy) {
            Text("Privacy controls")
        }
        Button(modifier = Modifier.weight(1f), enabled = enabled, onClick = onShareAll) {
            Text("Share summaries")
        }
    }
}

@Composable
private fun StatusFilter(selected: TripStatus?, onSelected: (TripStatus?) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelected(null) },
            label = { Text("All") }
        )
        TripStatus.entries.forEach { status ->
            FilterChip(
                selected = selected == status,
                onClick = { onSelected(status) },
                label = { Text(status.displayName) }
            )
        }
    }
}

@Composable
private fun TripCard(
    trip: Trip,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onArchiveToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column {
            TripPhotoHeader(trip = trip)
            Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = trip.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = trip.destination, style = MaterialTheme.typography.bodyMedium)
                }
                StatusPill(text = trip.status.displayName)
            }
            FlowRow(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(onClick = {}, label = { Text(trip.type.displayName) })
                AssistChip(onClick = {}, label = { Text("${trip.updates.size} updates") })
                AssistChip(onClick = {}, label = { Text("₹${trip.totalExpense.toInt()}") })
                AssistChip(onClick = {}, label = { Text(trip.displayDistance) })
                trip.coverPhotoRef?.let { AssistChip(onClick = {}, label = { Text("Cover: $it") }) }
            }
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onArchiveToggle) {
                    Text(if (trip.status == TripStatus.Archived) "Restore" else "Archive")
                }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
            }
        }
    }
}

@Composable
private fun TripPhotoHeader(trip: Trip) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            )
            .padding(16.dp)
    ) {
        Text(
            modifier = Modifier.align(Alignment.TopStart),
            text = trip.coverPhotoRef ?: "Trip cover",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            modifier = Modifier.align(Alignment.BottomStart),
            text = trip.destination,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TripDetailScreen(
    trip: Trip,
    onBack: () -> Unit,
    onEditTrip: () -> Unit,
    onDeleteTrip: () -> Unit,
    onArchiveToggle: () -> Unit,
    onCompleteTrip: () -> Unit,
    onAddUpdate: () -> Unit,
    onEditUpdate: (JourneyUpdate) -> Unit,
    onDeleteUpdate: (JourneyUpdate) -> Unit,
    onShareTrip: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("Back to trips") }
            TripHeader(
                trip = trip,
                onEditTrip = onEditTrip,
                onDeleteTrip = onDeleteTrip,
                onArchiveToggle = onArchiveToggle,
                onCompleteTrip = onCompleteTrip,
                onAddUpdate = onAddUpdate,
                onShareTrip = onShareTrip
            )
        }
        item { CheckpointMapCard(trip = trip) }
        item { ExpenseSummaryCard(trip = trip) }
        item {
            Text(
                text = "Timeline",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        if (trip.updates.isEmpty()) {
            item { EmptyTimeline(onAddUpdate = onAddUpdate) }
        } else {
            items(trip.updates, key = { it.id }) { update ->
                TimelineItem(
                    update = update,
                    onEdit = { onEditUpdate(update) },
                    onDelete = { onDeleteUpdate(update) }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun TripHeader(
    trip: Trip,
    onEditTrip: () -> Unit,
    onDeleteTrip: () -> Unit,
    onArchiveToggle: () -> Unit,
    onCompleteTrip: () -> Unit,
    onAddUpdate: () -> Unit,
    onShareTrip: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = trip.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(text = "${trip.startDate} • ${trip.destination}")
                }
                StatusPill(text = trip.status.displayName)
            }
            Text(modifier = Modifier.padding(top = 12.dp), text = trip.description.ifBlank { "No description added." })
            FlowRow(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(onClick = {}, label = { Text(trip.type.displayName) })
                AssistChip(onClick = {}, label = { Text("${trip.updates.size} memories") })
                AssistChip(onClick = {}, label = { Text("₹${trip.totalExpense.toInt()} expenses") })
                trip.vehicle?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                AssistChip(onClick = {}, label = { Text(trip.displayDistance) })
                trip.coverPhotoRef?.let { AssistChip(onClick = {}, label = { Text("Cover: $it") }) }
                trip.startOdometerPhotoRef?.let { AssistChip(onClick = {}, label = { Text("Start odo photo") }) }
                trip.endOdometerPhotoRef?.let { AssistChip(onClick = {}, label = { Text("End odo photo") }) }
            }
            FlowRow(
                modifier = Modifier.padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onAddUpdate) { Text("Add update") }
                OutlinedButton(onClick = onEditTrip) { Text("Edit trip") }
                OutlinedButton(onClick = onShareTrip) { Text("Share") }
                OutlinedButton(
                    onClick = onCompleteTrip,
                    enabled = trip.status != TripStatus.Completed
                ) { Text("Complete") }
                OutlinedButton(onClick = onArchiveToggle) {
                    Text(if (trip.status == TripStatus.Archived) "Restore" else "Archive")
                }
                TextButton(onClick = onDeleteTrip) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun CheckpointMapCard(trip: Trip) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = "Checkpoint map", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "MVP map view lists saved checkpoints only. It does not imply a continuous GPS route."
            )
            if (trip.updates.isEmpty()) {
                Text(modifier = Modifier.padding(top = 12.dp), text = "No checkpoints yet.")
            } else {
                trip.updates.forEachIndexed { index, update ->
                    Text(
                        modifier = Modifier.padding(top = 10.dp),
                        text = "${index + 1}. ${update.placeName} (${update.locationSource.displayName}) ${update.coordinateText.orEmpty()}"
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseSummaryCard(trip: Trip) {
    val expenses = trip.updates.filter { it.expenseAmount != null }
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = "Expense summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = "Total: ₹${trip.totalExpense.toInt()}")
            if (expenses.isEmpty()) {
                Text(modifier = Modifier.padding(top = 8.dp), text = "No expenses logged yet.")
            } else {
                val byCategory = expenses.groupBy { it.expenseCategory ?: ExpenseCategory.Other }
                byCategory.forEach { (category, items) ->
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = "${category.displayName}: ₹${items.sumOf { it.expenseAmount ?: 0.0 }.toInt()}"
                    )
                }
                val receipts = expenses.mapNotNull { it.receiptRef }.filter { it.isNotBlank() }
                if (receipts.isNotEmpty()) {
                    Text(modifier = Modifier.padding(top = 8.dp), text = "Receipts: ${receipts.joinToString()}")
                }
            }
        }
    }
}

@Composable
private fun InsightsScreen(trips: List<Trip>) {
    val updates = trips.flatMap { it.updates }
    val transportModes = updates.groupBy { it.transportMode }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Insights",
                subtitle = "A creator-friendly snapshot from saved journeys."
            )
        }
        item { StatsRow(trips = trips) }
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Travel patterns", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = "Days, places, transport modes, and cost totals are calculated from local records only."
                    )
                    FlowRow(
                        modifier = Modifier.padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(onClick = {}, label = { Text("${updates.size} memories") })
                        AssistChip(onClick = {}, label = { Text("${updates.distinctBy { it.placeName }.size} places") })
                        AssistChip(onClick = {}, label = { Text("₹${trips.sumOf { it.totalExpense }.toInt()} total") })
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Transport mix", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (transportModes.isEmpty()) {
                        Text(modifier = Modifier.padding(top = 8.dp), text = "No journey updates yet.")
                    } else {
                        transportModes.forEach { (mode, items) ->
                            Text(
                                modifier = Modifier.padding(top = 8.dp),
                                text = "${mode.displayName}: ${items.size} updates"
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun SettingsScreen(onPrivacy: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Settings",
                subtitle = "Transparent controls for an offline-first travel journal."
            )
        }
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Privacy and location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Location is only represented when you explicitly select a source while editing a checkpoint.")
                    Text("The app does not run a background location service or continuous route trace.")
                    Button(onClick = onPrivacy) {
                        Text("Delete all local data")
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("About this MVP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("TrackMyTrip is currently a local-only Compose MVP with polished Figma-aligned surfaces.")
                    Text("Photo, receipt, GPS, and map fields are MVP capture surfaces ready for native integrations.")
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun TimelineScreen(
    trips: List<Trip>,
    onTripSelected: (Trip) -> Unit
) {
    val updates = trips.flatMap { trip ->
        trip.updates.map { update -> trip to update }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Timeline",
                subtitle = "A chronological travel journal assembled from saved checkpoints."
            )
        }
        if (updates.isEmpty()) {
            item {
                EmptyState(
                    title = "No memories yet",
                    message = "Add journey updates inside a trip to build your timeline."
                )
            }
        } else {
            items(updates, key = { it.second.id }) { (trip, update) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTripSelected(trip) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(update.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${trip.title} • ${update.timestamp} • ${update.placeName}")
                        Text(
                            modifier = Modifier.padding(top = 8.dp),
                            text = update.note.ifBlank { "No note added." }
                        )
                        FlowRow(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(onClick = {}, label = { Text(update.locationSource.displayName) })
                            AssistChip(onClick = {}, label = { Text(update.transportMode.displayName) })
                            update.photoRef?.let { AssistChip(onClick = {}, label = { Text("Photo") }) }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun ExpensesScreen(trips: List<Trip>) {
    val expenseRows = trips.flatMap { trip ->
        trip.updates.mapNotNull { update ->
            update.expenseAmount?.let { amount -> Triple(trip, update, amount) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Expenses",
                subtitle = "Costs are supporting context for the journey, grouped from saved updates."
            )
        }
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Total logged", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("₹${expenseRows.sumOf { it.third }.toInt()}", style = MaterialTheme.typography.headlineLarge)
                }
            }
        }
        if (expenseRows.isEmpty()) {
            item {
                EmptyState(
                    title = "No expenses yet",
                    message = "Add an amount while creating or editing a journey update."
                )
            }
        } else {
            items(expenseRows, key = { it.second.id }) { (trip, update, amount) ->
                Card(shape = RoundedCornerShape(20.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(update.expenseCategory?.displayName ?: ExpenseCategory.Other.displayName, fontWeight = FontWeight.Bold)
                            Text("${trip.title} • ${update.placeName}")
                            update.receiptRef?.let { Text("Receipt: $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        Text("₹${amount.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun MapScreen(trips: List<Trip>) {
    val checkpoints = trips.flatMap { trip ->
        trip.updates.map { update -> trip to update }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Checkpoint map",
                subtitle = "Saved places only. TrackMyTrip never draws a continuous background route."
            )
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Text(
                    modifier = Modifier.align(Alignment.TopStart),
                    text = "Map preview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "●     ●\n   ●       ●\nSaved checkpoints",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    modifier = Modifier.align(Alignment.BottomStart),
                    text = "No passive GPS tracking",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (checkpoints.isEmpty()) {
            item {
                EmptyState(
                    title = "No checkpoints",
                    message = "Add journey updates with manual/current/map selected locations."
                )
            }
        } else {
            items(checkpoints, key = { it.second.id }) { (trip, update) ->
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(update.placeName, fontWeight = FontWeight.Bold)
                        Text("${trip.title} • ${update.locationSource.displayName}")
                        update.coordinateText?.let {
                            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun SharePreviewScreen(
    trips: List<Trip>,
    onShare: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Share preview",
                subtitle = "Creator-friendly summaries keep a consistent TrackMyTrip identity."
            )
        }
        if (trips.isEmpty()) {
            item {
                EmptyState(
                    title = "Nothing to share",
                    message = "Create a trip and add memories before exporting."
                )
            }
        } else {
            items(trips, key = { it.id }) { trip ->
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(trip.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("${trip.destination} • ${trip.type.displayName}")
                        Text(
                            modifier = Modifier.padding(top = 12.dp),
                            text = "${trip.updates.size} memories • ₹${trip.totalExpense.toInt()} expenses"
                        )
                        Text(
                            modifier = Modifier.padding(top = 12.dp),
                            text = trip.updates.firstOrNull()?.note ?: trip.description
                        )
                    }
                }
            }
            item {
                Button(modifier = Modifier.fillMaxWidth(), onClick = onShare) {
                    Text("Share all summaries")
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyTimeline(onAddUpdate: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "No journey updates yet", fontWeight = FontWeight.Bold)
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "Add a checkpoint with a note, place, location source, transport mode, photos, and optional expense."
            )
            Button(modifier = Modifier.padding(top = 16.dp), onClick = onAddUpdate) {
                Text("Add first update")
            }
        }
    }
}

@Composable
private fun TimelineItem(update: JourneyUpdate, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = update.title, fontWeight = FontWeight.Bold)
                Text(text = "${update.timestamp} • ${update.placeName}")
                Text(modifier = Modifier.padding(top = 8.dp), text = update.note.ifBlank { "No note added." })
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(onClick = {}, label = { Text(update.transportMode.displayName) })
                    AssistChip(onClick = {}, label = { Text(update.locationSource.displayName) })
                    update.coordinateText?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                    update.photoRef?.let { AssistChip(onClick = {}, label = { Text("Photo: $it") }) }
                    update.expenseAmount?.let {
                        AssistChip(
                            onClick = {},
                            label = { Text("${update.expenseCurrency} ${it.toInt()} ${update.expenseCategory?.displayName.orEmpty()}") }
                        )
                    }
                    update.expenseDescription?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                    update.receiptRef?.let { AssistChip(onClick = {}, label = { Text("Receipt: $it") }) }
                }
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onEdit) { Text("Edit") }
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun TripEditorDialog(
    trip: Trip?,
    onDismiss: () -> Unit,
    onSave: (Trip) -> Unit
) {
    var mediaTarget by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf(trip?.title.orEmpty()) }
    var destination by remember { mutableStateOf(trip?.destination.orEmpty()) }
    var startDate by remember { mutableStateOf(trip?.startDate ?: "Today") }
    var endDate by remember { mutableStateOf(trip?.endDate.orEmpty()) }
    var description by remember { mutableStateOf(trip?.description.orEmpty()) }
    var coverPhotoRef by remember { mutableStateOf(trip?.coverPhotoRef.orEmpty()) }
    var vehicle by remember { mutableStateOf(trip?.vehicle.orEmpty()) }
    var startOdometer by remember { mutableStateOf(trip?.startOdometerKm?.toString().orEmpty()) }
    var endOdometer by remember { mutableStateOf(trip?.endOdometerKm?.toString().orEmpty()) }
    var manualDistance by remember { mutableStateOf(trip?.manualDistance?.toString().orEmpty()) }
    var startOdometerPhotoRef by remember { mutableStateOf(trip?.startOdometerPhotoRef.orEmpty()) }
    var endOdometerPhotoRef by remember { mutableStateOf(trip?.endOdometerPhotoRef.orEmpty()) }
    var type by remember { mutableStateOf(trip?.type ?: TripType.RoadTrip) }
    var status by remember { mutableStateOf(trip?.status ?: TripStatus.Active) }
    var odometerUnit by remember { mutableStateOf(trip?.odometerUnit ?: DistanceUnit.Km) }
    var distanceSource by remember { mutableStateOf(trip?.distanceSource ?: DistanceSource.Odometer) }
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val value = uri?.toString() ?: return@rememberLauncherForActivityResult
        when (mediaTarget) {
            "cover" -> coverPhotoRef = value
            "startOdometer" -> startOdometerPhotoRef = value
            "endOdometer" -> endOdometerPhotoRef = value
        }
        mediaTarget = null
    }
    val endBeforeStart = startOdometer.toIntOrNull() != null &&
        endOdometer.toIntOrNull() != null &&
        endOdometer.toInt() < startOdometer.toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (trip == null) "Create trip" else "Edit trip") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Trip title") })
                OutlinedTextField(value = destination, onValueChange = { destination = it }, label = { Text("Destination") })
                ChipSelector(values = TripType.entries, selected = type, label = { it.displayName }, onSelected = { type = it })
                ChipSelector(values = TripStatus.entries, selected = status, label = { it.displayName }, onSelected = { status = it })
                OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start date") })
                OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("End date optional") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 2)
                MediaPickerRow(
                    label = "Cover photo",
                    value = coverPhotoRef,
                    onPick = {
                        mediaTarget = "cover"
                        mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onClear = { coverPhotoRef = "" }
                )
                OutlinedTextField(value = vehicle, onValueChange = { vehicle = it }, label = { Text("Vehicle optional") })
                ChipSelector(values = DistanceUnit.entries, selected = odometerUnit, label = { it.displayName }, onSelected = { odometerUnit = it })
                ChipSelector(values = DistanceSource.entries, selected = distanceSource, label = { it.displayName }, onSelected = { distanceSource = it })
                OutlinedTextField(
                    value = startOdometer,
                    onValueChange = { startOdometer = it.filter(Char::isDigit) },
                    label = { Text("Start odometer km optional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = endOdometer,
                    onValueChange = { endOdometer = it.filter(Char::isDigit) },
                    label = { Text("End odometer km optional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (endBeforeStart) {
                    Text("End odometer must be greater than or equal to start.", color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(
                    value = manualDistance,
                    onValueChange = { manualDistance = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Manual distance optional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                MediaPickerRow(
                    label = "Start odometer evidence",
                    value = startOdometerPhotoRef,
                    onPick = {
                        mediaTarget = "startOdometer"
                        mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onClear = { startOdometerPhotoRef = "" }
                )
                MediaPickerRow(
                    label = "End odometer evidence",
                    value = endOdometerPhotoRef,
                    onPick = {
                        mediaTarget = "endOdometer"
                        mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onClear = { endOdometerPhotoRef = "" }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && destination.isNotBlank() && !endBeforeStart,
                onClick = {
                    onSave(
                        Trip(
                            id = trip?.id ?: 0L,
                            title = title,
                            type = type,
                            destination = destination,
                            startDate = startDate,
                            endDate = endDate.takeIf { it.isNotBlank() },
                            description = description,
                            status = status,
                            coverPhotoRef = coverPhotoRef.takeIf { it.isNotBlank() },
                            vehicle = vehicle.takeIf { it.isNotBlank() },
                            startOdometerKm = startOdometer.toIntOrNull(),
                            endOdometerKm = endOdometer.toIntOrNull(),
                            odometerUnit = odometerUnit,
                            manualDistance = manualDistance.toDoubleOrNull(),
                            distanceSource = distanceSource,
                            startOdometerPhotoRef = startOdometerPhotoRef.takeIf { it.isNotBlank() },
                            endOdometerPhotoRef = endOdometerPhotoRef.takeIf { it.isNotBlank() },
                            updates = trip?.updates ?: emptyList()
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun UpdateEditorDialog(
    update: JourneyUpdate?,
    onDismiss: () -> Unit,
    onSave: (JourneyUpdate) -> Unit
) {
    val context = LocalContext.current
    var mediaTarget by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf(update?.title.orEmpty()) }
    var timestamp by remember { mutableStateOf(update?.timestamp ?: "Now") }
    var place by remember { mutableStateOf(update?.placeName.orEmpty()) }
    var coordinateText by remember { mutableStateOf(update?.coordinateText.orEmpty()) }
    var locationAccuracyText by remember { mutableStateOf(update?.locationAccuracyMeters?.toString().orEmpty()) }
    var note by remember { mutableStateOf(update?.note.orEmpty()) }
    var photoRef by remember { mutableStateOf(update?.photoRef.orEmpty()) }
    var amount by remember { mutableStateOf(update?.expenseAmount?.toString().orEmpty()) }
    var expenseCurrency by remember { mutableStateOf(update?.expenseCurrency ?: "INR") }
    var expenseDate by remember { mutableStateOf(update?.expenseDate ?: "Today") }
    var expenseDescription by remember { mutableStateOf(update?.expenseDescription.orEmpty()) }
    var receiptRef by remember { mutableStateOf(update?.receiptRef.orEmpty()) }
    var locationSource by remember { mutableStateOf(update?.locationSource ?: LocationSource.Manual) }
    var transportMode by remember { mutableStateOf(update?.transportMode ?: TransportMode.Car) }
    var category by remember { mutableStateOf(update?.expenseCategory ?: ExpenseCategory.Other) }
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val value = uri?.toString() ?: return@rememberLauncherForActivityResult
        when (mediaTarget) {
            "photo" -> photoRef = value
            "receipt" -> receiptRef = value
        }
        mediaTarget = null
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            captureOneTimeLocation(
                context = context,
                onLocation = { location ->
                    locationSource = LocationSource.CurrentLocation
                    coordinateText = "%.5f, %.5f".format(location.latitude, location.longitude)
                    locationAccuracyText = location.accuracy.takeIf { it > 0f }?.toString().orEmpty()
                },
                onUnavailable = {
                    locationSource = LocationSource.CurrentLocation
                    coordinateText = "Location unavailable; add manually"
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (update == null) "Add journey update" else "Edit journey update") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Update title") })
                OutlinedTextField(value = timestamp, onValueChange = { timestamp = it }, label = { Text("Time") })
                OutlinedTextField(value = place, onValueChange = { place = it }, label = { Text("Place / checkpoint") })
                ChipSelector(
                    values = LocationSource.entries,
                    selected = locationSource,
                    label = { it.displayName },
                    onSelected = { selected ->
                        locationSource = selected
                        if (selected == LocationSource.CurrentLocation) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                captureOneTimeLocation(
                                    context = context,
                                    onLocation = { location ->
                                        coordinateText = "%.5f, %.5f".format(location.latitude, location.longitude)
                                        locationAccuracyText = location.accuracy.takeIf { it > 0f }?.toString().orEmpty()
                                    },
                                    onUnavailable = { coordinateText = "Location unavailable; add manually" }
                                )
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }
                    }
                )
                OutlinedTextField(
                    value = coordinateText,
                    onValueChange = { coordinateText = it },
                    label = { Text("Coordinates or map note optional") }
                )
                OutlinedTextField(
                    value = locationAccuracyText,
                    onValueChange = { locationAccuracyText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Accuracy meters optional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                ChipSelector(values = TransportMode.entries, selected = transportMode, label = { it.displayName }, onSelected = { transportMode = it })
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, minLines = 2)
                MediaPickerRow(
                    label = "Journey photo",
                    value = photoRef,
                    onPick = {
                        mediaTarget = "photo"
                        mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onClear = { photoRef = "" }
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Expense amount optional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(value = expenseCurrency, onValueChange = { expenseCurrency = it.uppercase().take(3) }, label = { Text("Currency") })
                OutlinedTextField(value = expenseDate, onValueChange = { expenseDate = it }, label = { Text("Expense date") })
                OutlinedTextField(value = expenseDescription, onValueChange = { expenseDescription = it }, label = { Text("Expense description optional") })
                ChipSelector(values = ExpenseCategory.entries, selected = category, label = { it.displayName }, onSelected = { category = it })
                MediaPickerRow(
                    label = "Receipt image",
                    value = receiptRef,
                    onPick = {
                        mediaTarget = "receipt"
                        mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onClear = { receiptRef = "" }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && place.isNotBlank(),
                onClick = {
                    onSave(
                        JourneyUpdate(
                            id = update?.id ?: 0L,
                            title = title,
                            timestamp = timestamp,
                            placeName = place,
                            locationSource = locationSource,
                            coordinateText = coordinateText.takeIf { it.isNotBlank() },
                            note = note,
                            photoRef = photoRef.takeIf { it.isNotBlank() },
                            transportMode = transportMode,
                            expenseAmount = amount.toDoubleOrNull(),
                            expenseCategory = category,
                            receiptRef = receiptRef.takeIf { it.isNotBlank() },
                            locationAccuracyMeters = locationAccuracyText.toFloatOrNull(),
                            expenseCurrency = expenseCurrency.ifBlank { "INR" },
                            expenseDate = expenseDate.ifBlank { timestamp },
                            expenseDescription = expenseDescription.takeIf { it.isNotBlank() }
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SummaryDialog(summary: String, onDismiss: () -> Unit, onShare: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trip summary export") },
        text = {
            Text(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                text = summary
            )
        },
        confirmButton = {
            Button(onClick = onShare) { Text("Share") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun EmptyState(title: String, message: String) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontWeight = FontWeight.Bold)
            Text(modifier = Modifier.padding(top = 8.dp), text = message)
        }
    }
}

@Composable
private fun <T> ChipSelector(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEach { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelected(value) },
                label = { Text(label(value)) }
            )
        }
    }
}

@Composable
private fun MediaPickerRow(
    label: String,
    value: String,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = value.ifBlank { "No media selected" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onPick) { Text("Pick image") }
                TextButton(enabled = value.isNotBlank(), onClick = onClear) { Text("Clear") }
            }
        }
    }
}

private fun MutableList<Trip>.replaceTrip(trip: Trip) {
    val index = indexOfFirst { it.id == trip.id }
    if (index >= 0) this[index] = trip
}

private fun Trip.toShareSummary(): String {
    val checkpoints = updates.joinToString("\n") { "- ${it.timestamp}: ${it.title} at ${it.placeName}" }
    return """
        $title
        ${type.displayName} • ${status.displayName}
        Destination: $destination
        Dates: $startDate${endDate?.let { " to $it" }.orEmpty()}
        Distance: $displayDistance
        Expenses: ₹${totalExpense.toInt()}
        
        Highlights:
        ${if (checkpoints.isBlank()) "No journey updates yet." else checkpoints}
    """.trimIndent()
}

private fun shareText(context: android.content.Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(intent, "Share trip summary"))
}

@SuppressLint("MissingPermission")
private fun captureOneTimeLocation(
    context: android.content.Context,
    onLocation: (Location) -> Unit,
    onUnavailable: () -> Unit
) {
    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { locationManager.isProviderEnabled(it) }

    val lastKnown = providers
        .mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
        .maxByOrNull { it.time }

    if (lastKnown != null) {
        onLocation(lastKnown)
        return
    }

    val provider = providers.firstOrNull()
    if (provider == null) {
        onUnavailable()
        return
    }

    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            locationManager.removeUpdates(this)
            onLocation(location)
        }

        @Deprecated("Deprecated in Android framework")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

        override fun onProviderEnabled(provider: String) = Unit

        override fun onProviderDisabled(provider: String) = Unit
    }
    locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
}

private fun sampleTrips(): List<Trip> {
    return listOf(
        Trip(
            id = 1L,
            title = "Western Ghats Road Trip",
            type = TripType.RoadTrip,
            destination = "Bengaluru to Coorg",
            startDate = "28 Aug 2026",
            endDate = null,
            description = "A monsoon drive with coffee estates, viewpoints, and quiet detours.",
            status = TripStatus.Active,
            coverPhotoRef = "coffee-estate-cover.jpg",
            vehicle = "Car",
            startOdometerKm = 42810,
            endOdometerKm = null,
            updates = listOf(
                JourneyUpdate(
                    id = 11L,
                    title = "Sunrise checkpoint",
                    timestamp = "06:40",
                    placeName = "Ramanagara",
                    locationSource = LocationSource.Manual,
                    coordinateText = null,
                    note = "Stopped for tea and captured the first photo set of the trip.",
                    photoRef = "sunrise-stop.jpg",
                    transportMode = TransportMode.Car,
                    expenseAmount = 180.0,
                    expenseCategory = ExpenseCategory.Food,
                    receiptRef = null
                ),
                JourneyUpdate(
                    id = 12L,
                    title = "Fuel stop",
                    timestamp = "08:15",
                    placeName = "Maddur",
                    locationSource = LocationSource.CurrentLocation,
                    coordinateText = "On-demand snapshot",
                    note = "Logged fuel and reset the next driving segment.",
                    photoRef = null,
                    transportMode = TransportMode.Car,
                    expenseAmount = 2400.0,
                    expenseCategory = ExpenseCategory.Fuel,
                    receiptRef = "fuel-receipt.jpg"
                )
            )
        ),
        Trip(
            id = 2L,
            title = "Jaipur Weekend",
            type = TripType.Vacation,
            destination = "Jaipur",
            startDate = "12 Sep 2026",
            endDate = "14 Sep 2026",
            description = "A short city break for forts, markets, food, and photo walks.",
            status = TripStatus.Upcoming,
            coverPhotoRef = null,
            vehicle = null,
            startOdometerKm = null,
            endOdometerKm = null,
            updates = emptyList()
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun TrackMyTripAppPreview() {
    TrackMyTripTheme {
        TrackMyTripApp()
    }
}
