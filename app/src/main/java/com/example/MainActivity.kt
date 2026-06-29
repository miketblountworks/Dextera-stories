package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.IntroScreen
import com.example.ui.screens.StoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StoryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val storyViewModel: StoryViewModel = viewModel()

                NavHost(
                    navController = navController,
                    startDestination = "intro"
                ) {
                    composable("intro") {
                        IntroScreen(
                            viewModel = storyViewModel,
                            onNavigateToStory = { navController.navigate("story") },
                            onNavigateToCamera = { navController.navigate("camera") }
                        )
                    }
                    composable("story") {
                        StoryScreen(
                            viewModel = storyViewModel,
                            onNavigateBackToIntro = { navController.popBackStack() }
                        )
                    }
                    composable("camera") {
                        CameraScreen(
                            viewModel = storyViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
