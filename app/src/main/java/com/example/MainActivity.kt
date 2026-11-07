package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CreateRoutineScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditRoutineScreen
import com.example.ui.screens.RunModeScreen
import com.example.ui.screens.RoutineViewScreen
import com.example.ui.screens.TaskCatalogScreen
import com.example.ui.screens.TaskListScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.RoutineViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    private val viewModel: RoutineViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val currentScreen by viewModel.currentScreen.collectAsState()

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                            Screen.RoutineView -> RoutineViewScreen(viewModel = viewModel)
                            Screen.RunMode -> RunModeScreen(viewModel = viewModel)
                            Screen.EditRoutine -> EditRoutineScreen(viewModel = viewModel)
                            Screen.TaskList -> TaskListScreen(viewModel = viewModel)
                            Screen.CreateRoutine -> CreateRoutineScreen(viewModel = viewModel)
                            Screen.Auth -> AuthScreen(viewModel = viewModel)
                            Screen.TaskCatalog -> TaskCatalogScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
