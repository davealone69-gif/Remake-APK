package com.example

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepairSearchScreen(
    onBack: (() -> Unit)? = null,
    viewModel: RepairSearchViewModel = viewModel()
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showVehicleDialog by remember { mutableStateOf(false) }
    var customVehicleInput by remember { mutableStateOf("") }

    val isSearching by viewModel.isSearching.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val currentGuide by viewModel.currentGuide.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()

    var isSpeaking by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    val presetQueries = listOf(
        "P0300 Misfire Diagnostic Procedure",
        "Brake Pad & Rotor Torque Specs",
        "Transmission Fluid Flush & Level",
        "HV Battery Fan Filter Cleaning",
        "A/C Compressor Relay & Wiring",
        "Timing Chain Tensioner Replacement",
        "P0171 Fuel System Too Lean",
        "ABS Wheel Speed Sensor Testing"
    )

    val vehiclePresets = listOf(
        "2020 Toyota Camry 2.5L",
        "2018 Ford F-150 3.5L EcoBoost",
        "2021 Honda Civic 1.5T",
        "2019 Chevy Silverado 5.3L V8",
        "2022 Tesla Model 3 Long Range",
        "2017 Nissan Altima 2.5L"
    )

    val categoryPresets = listOf(
        "All Repair Guides",
        "DTC Troubleshooting",
        "TSB & Recalls",
        "Wiring & Fuses",
        "Fluid & Torque Specs"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Grounded Automotive Search", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Google Search", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text("Live Technical Service Bulletins, Torque Specs & Repair Guides", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Target Vehicle Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Active Vehicle Target", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(selectedVehicle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedButton(onClick = { showVehicleDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Change")
                    }
                }
            }

            // Search Bar Input Field
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Look up repair guide, fault code, or component...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                viewModel.searchRepairGuide(searchQuery)
                            }
                        },
                        enabled = searchQuery.isNotBlank() && !isSearching,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Grounding Google Search Results...")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Search Grounded Repair Guide")
                        }
                    }
                }
            }

            // Category Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                categoryPresets.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.setCategory(category) },
                        label = { Text(category) },
                        leadingIcon = {
                            if (selectedCategory == category) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                }
            }

            // Quick Preset Suggestions
            Column {
                Text("Popular Automotive Searches", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    presetQueries.forEach { preset ->
                        AssistChip(
                            onClick = {
                                searchQuery = preset
                                viewModel.searchRepairGuide(preset)
                            },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }

            // Main Grounded Result View
            if (isSearching) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(16.dp))
                        Text("Querying Google Search Grounding for OEM Bulletins & Specs...", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Retrieving technical repair steps for $selectedVehicle", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            } else if (currentGuide != null) {
                val guide = currentGuide!!

                // Grounding Info Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.width(8.dp))
                                Text("Grounded Live Repair Guide", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        if (isSpeaking) {
                                            tts?.stop()
                                            isSpeaking = false
                                        } else {
                                            tts?.speak(guide.guideContent, TextToSpeech.QUEUE_FLUSH, null, "TTS_GUIDE")
                                            isSpeaking = true
                                        }
                                    }
                                ) {
                                    Icon(
                                        if (isSpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                                        contentDescription = "Read Aloud",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.saveToLibrary(guide)
                                        Toast.makeText(context, "Saved repair guide to Library!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        if (guide.isSavedToLibrary) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        contentDescription = "Save to Library",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        Text("Query: \"${guide.query}\"", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Vehicle: ${guide.vehicle} • ${guide.timestamp}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }

                // Guide Markdown Content Container
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            guide.guideContent,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                }

                // Google Grounding Web Citations & Queries Used Panel
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Google Search Grounding Sources & Search Queries", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        Text("Search Queries Processed by Grounding Engine:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        guide.searchQueries.forEach { q ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                Spacer(Modifier.width(6.dp))
                                Text(q, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Citations & External OEM Service Web Sources:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        guide.webCitations.forEach { citation ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(citation.url))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Opening source: ${citation.url}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(citation.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text(citation.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = "Open Link", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            // Search History Section
            if (searchHistory.isNotEmpty()) {
                Text("Recent Grounded Searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                searchHistory.forEach { hGuide ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.searchRepairGuide(hGuide.query) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(hGuide.query, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${hGuide.vehicle} • ${hGuide.timestamp}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    // Vehicle Change Dialog
    if (showVehicleDialog) {
        AlertDialog(
            onDismissRequest = { showVehicleDialog = false },
            title = { Text("Select or Enter Target Vehicle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select from common workshop vehicles:")
                    vehiclePresets.forEach { v ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setVehicle(v)
                                    showVehicleDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = selectedVehicle == v,
                                onClick = {
                                    viewModel.setVehicle(v)
                                    showVehicleDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(v, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Or enter custom vehicle model:", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = customVehicleInput,
                        onValueChange = { customVehicleInput = it },
                        placeholder = { Text("e.g. 2023 Subaru Outback 2.5L") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customVehicleInput.isNotBlank()) {
                            viewModel.setVehicle(customVehicleInput.trim())
                            customVehicleInput = ""
                        }
                        showVehicleDialog = false
                    }
                ) {
                    Text("Apply Vehicle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVehicleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
