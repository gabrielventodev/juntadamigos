package com.juntadamigos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.juntadamigos.app.ui.MeetingScreen
import com.juntadamigos.app.ui.MeetingViewModel
import com.juntadamigos.app.ui.theme.JuntadamigosTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MeetingViewModel by viewModels {
        val graph = AppGraph(applicationContext)
        MeetingViewModel.factory(graph.autocomplete, graph.finder, graph.hasApiKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JuntadamigosTheme {
                MeetingScreen(viewModel)
            }
        }
    }
}
