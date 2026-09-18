package com.example.myapplication_answering

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.myapplication_answering.ui.navigation.Destination
import com.example.myapplication_answering.ui.screens.MainScreen
import com.example.myapplication_answering.ui.theme.OCRAnswerWizTheme
import com.example.myapplication_answering.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OCRAnswerWizTheme {
                val backStack = rememberNavBackStack(Destination.Home)
                val viewModel: MainViewModel = viewModel()
                
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = entryProvider {
                        entry<Destination.Home> {
                            MainScreen(viewModel = viewModel)
                        }
                    }
                )
            }
        }
    }
}
