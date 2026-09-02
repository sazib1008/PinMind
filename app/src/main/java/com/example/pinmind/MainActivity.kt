package com.example.pinmind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.pinmind.presentation.navigation.PinMindNavGraph
import com.example.pinmind.ui.theme.PinMindTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        org.osmdroid.config.Configuration.getInstance().userAgentValue = "PinMindApp/1.0 (${packageName})"
        enableEdgeToEdge()
        setContent {

            PinMindTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PinMindNavGraph()
                }
            }
        }
    }
}