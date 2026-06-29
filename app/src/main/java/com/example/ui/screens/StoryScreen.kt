package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.InteractivePageCurlContainer
import com.example.ui.viewmodel.StoryViewModel

@Composable
fun StoryScreen(
    viewModel: StoryViewModel,
    onNavigateBackToIntro: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        InteractivePageCurlContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onPageCurled = {
                viewModel.navigateForward()
            },
            currentContent = {
                StoryPageContent(
                    state = state,
                    viewModel = viewModel,
                    onNavigateBackToIntro = onNavigateBackToIntro
                )
            },
            nextContent = {
                // Peek preview of next page background while turning
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFFDF5),
                                    Color(0xFFF2EAD3)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "Next Page",
                            tint = Color.LightGray,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Flipping the page...",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun StoryPageContent(
    state: com.example.ui.viewmodel.StoryUiState,
    viewModel: StoryViewModel,
    onNavigateBackToIntro: () -> Unit
) {
    val gradientColor = when (state.currentLightingEffect.uppercase()) {
        "EXPLOSION_RED" -> Color(0xFFFFECEC)
        "CALM_BLUE" -> Color(0xFFEDF8FF)
        "SPOOKY_PURPLE" -> Color(0xFFFAF0FF)
        "FOREST_GREEN" -> Color(0xFFF1FFF3)
        "SUNSHINE_YELLOW" -> Color(0xFFFFFFEC)
        else -> Color(0xFFFFFDF8)
    }

    val activeLightColor = when (state.currentLightingEffect.uppercase()) {
        "EXPLOSION_RED" -> Color.Red
        "CALM_BLUE" -> Color(0xFF03A9F4)
        "SPOOKY_PURPLE" -> Color(0xFF9C27B0)
        "FOREST_GREEN" -> Color(0xFF4CAF50)
        "SUNSHINE_YELLOW" -> Color(0xFFFFEB3B)
        else -> Color.LightGray
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        gradientColor,
                        Color(0xFFFFFBF0)
                    )
                )
            )
            .padding(16.dp)
    ) {
        // 1. Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    viewModel.resetStorySession()
                    onNavigateBackToIntro()
                }
            ) {
                Icon(Icons.Default.Home, contentDescription = "Home", tint = MaterialTheme.colorScheme.primary)
            }

            // Central Page Indicator
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            ) {
                Text(
                    "Page ${state.currentPageNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Sync Indicator showing light / audio state
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Syncing effect",
                    tint = activeLightColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { viewModel.replayPage() }) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Read Aloud",
                        tint = if (state.isSpeaking) MaterialTheme.colorScheme.secondary else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Story Sheet (Scrollable layout styling to represent a physically bound book page)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(2.dp, Color(0xFFE5D5C5), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                if (state.isGenerating) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Natively generating the next story page...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Story Text (Using premium display font look, serif style)
                        Text(
                            text = state.currentStoryText,
                            fontSize = 18.sp,
                            lineHeight = 28.sp,
                            fontStyle = FontStyle.Normal,
                            fontFamily = FontFamily.Serif,
                            color = Color(0xFF2C2C2C),
                            modifier = Modifier.weight(1f)
                        )

                        // 3. Children Branching Choices Cards
                        if (state.currentChoices.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text(
                                    "What should ${state.protagonistName} do?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )

                                state.currentChoices.forEach { choice ->
                                    Card(
                                        onClick = { viewModel.selectChoice(choice) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(14.dp)
                                                .fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = choice,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Go",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Storybook completed or loading ending text
                            Text(
                                text = "The End. You have lived an extraordinary adventure!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Manual Navigation / Back & Replay Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.navigateBack() },
                enabled = state.currentPageNumber > 1 && !state.isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Previous")
            }

            Button(
                onClick = { viewModel.navigateForward() },
                enabled = state.pages.maxOfOrNull { it.pageNumber } ?: 1 > state.currentPageNumber && !state.isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Text("Next")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
            }
        }
    }
}
