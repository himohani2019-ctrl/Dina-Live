package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.ui.screens.DinaLiveMainApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DinaLiveViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: DinaLiveViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DinaLiveMainApp(viewModel = viewModel)
            }
        }
    }
}
