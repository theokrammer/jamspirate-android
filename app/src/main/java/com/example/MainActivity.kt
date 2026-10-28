package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.StudioScreen
import com.example.ui.StudioViewModel
import com.example.ui.theme.JamspirateTheme

class MainActivity : ComponentActivity() {
    private val studioViewModel: StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JamspirateTheme {
                StudioScreen(viewModel = studioViewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        studioViewModel.stopJam()
    }
}
