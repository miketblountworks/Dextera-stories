package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroScreen(
    viewModel: StoryViewModel,
    onNavigateToStory: () -> Unit,
    onNavigateToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // State variables for adding character form
    var newCharName by remember { mutableStateOf("") }
    var newCharGender by remember { mutableStateOf("Female") }
    var newCharAlignment by remember { mutableStateOf("Good") }
    var newCharTraits by remember { mutableStateOf("Brave, Playful") }
    
    // Dropdown menu flags
    var genderExpanded by remember { mutableStateOf(false) }
    var alignmentExpanded by remember { mutableStateOf(false) }

    // Smart light configurations state
    var showLightSettings by remember { mutableStateOf(false) }
    var bridgeIp by remember { mutableStateOf(state.hueBridgeIp) }
    var hueUser by remember { mutableStateOf(state.hueUsername) }
    var lightId by remember { mutableStateOf(state.hueLightId) }

    // Launcher for requesting Camera Permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setCameraPermissionGranted(isGranted)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Storybook Maker", 
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header Title
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "Book Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(64.dp).width(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Craft Your Magic Story",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Your voice, choices, and lights come together natively!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 1. Protagonist Form
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Who is the Protagonist?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = state.protagonistName,
                            onValueChange = { viewModel.updateProtagonist(it, state.protagonistAge) },
                            label = { Text("Protagonist Name") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Person, "Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = state.protagonistAge.toString(),
                            onValueChange = { 
                                val age = it.toIntOrNull() ?: 6
                                viewModel.updateProtagonist(state.protagonistName, age)
                            },
                            label = { Text("Protagonist Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Face, "Age") }
                        )
                    }
                }
            }

            // 2. Camera Setup Section
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Add Your Portrait",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Take a photo to turn yourself into a cartoon character in the book!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (!state.hasCameraPermission) {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                } else {
                                    onNavigateToCamera()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.hasCameraPermission) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (state.hasCameraPermission) "Photo" else "Permit")
                        }
                    }
                }
            }

            // 3. Supporting Characters Title
            item {
                Text(
                    "Dynamic Supporting Characters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // 4. Added Supporting Characters List
            if (state.supportingCharacters.isEmpty()) {
                item {
                    Text(
                        "No supporting characters yet. Add some to make the story dynamic!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(state.supportingCharacters) { char ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(char.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    "${char.gender} • ${char.alignment} • ${char.personalityTraits}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.deleteSupportingCharacter(char) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }

            // 5. Add Character Form
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Add Supporting Character",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = newCharName,
                            onValueChange = { newCharName = it },
                            label = { Text("Character Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Gender drop down menu
                            ExposedDropdownMenuBox(
                                expanded = genderExpanded,
                                onExpandedChange = { genderExpanded = !genderExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    readOnly = true,
                                    value = newCharGender,
                                    onValueChange = {},
                                    label = { Text("Gender") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = genderExpanded,
                                    onDismissRequest = { genderExpanded = false }
                                ) {
                                    listOf("Male", "Female", "Other").forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item) },
                                            onClick = {
                                                newCharGender = item
                                                genderExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Alignment drop down menu
                            ExposedDropdownMenuBox(
                                expanded = alignmentExpanded,
                                onExpandedChange = { alignmentExpanded = !alignmentExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    readOnly = true,
                                    value = newCharAlignment,
                                    onValueChange = {},
                                    label = { Text("Alignment") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = alignmentExpanded) },
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = alignmentExpanded,
                                    onDismissRequest = { alignmentExpanded = false }
                                ) {
                                    listOf("Good", "Evil").forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item) },
                                            onClick = {
                                                newCharAlignment = item
                                                alignmentExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = newCharTraits,
                            onValueChange = { newCharTraits = it },
                            label = { Text("Traits (comma-separated)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (newCharName.isNotEmpty()) {
                                    viewModel.addSupportingCharacter(
                                        newCharName,
                                        newCharGender,
                                        newCharAlignment,
                                        newCharTraits
                                    )
                                    // Reset fields
                                    newCharName = ""
                                    newCharTraits = "Brave, Playful"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Character")
                        }
                    }
                }
            }

            // 6. Smart Light Configuration Settings (Collapsible)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Smart Lights",
                                    tint = if (state.isHueConfigured) Color(0xFFD7A15C) else Color.Gray
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Smart Lights Integration",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { showLightSettings = !showLightSettings }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }

                        AnimatedVisibility(visible = showLightSettings) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                OutlinedTextField(
                                    value = bridgeIp,
                                    onValueChange = { bridgeIp = it },
                                    label = { Text("Hue Bridge IP") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = hueUser,
                                    onValueChange = { hueUser = it },
                                    label = { Text("Hue App Key (Username)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = lightId,
                                    onValueChange = { lightId = it },
                                    label = { Text("Light ID") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        viewModel.configureHue(bridgeIp, hueUser, lightId)
                                        showLightSettings = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Save")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Configuration")
                                }
                            }
                        }
                    }
                }
            }

            // 7. Core Action Button: Create Storybook
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.startNewStory()
                        onNavigateToStory()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    enabled = state.protagonistName.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Book, contentDescription = "Book")
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Open Magic Storybook",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
