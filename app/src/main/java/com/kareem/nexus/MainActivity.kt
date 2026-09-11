package com.kareem.nexus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.kareem.nexus.ui.CaptureViewModel
import com.kareem.nexus.ui.NexusApp
import com.kareem.nexus.ui.theme.NexusTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val captureViewModel: CaptureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) captureViewModel.ingestShare(intent)
        setContent { NexusTheme { NexusApp(captureViewModel) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureViewModel.ingestShare(intent)
    }

    override fun onResume() {
        super.onResume()
        captureViewModel.refreshAccessState()
    }
}

