package com.oliver.videoeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import com.oliver.videoeditor.ui.screens.EditorScreen
import com.oliver.videoeditor.ui.screens.HomeScreen
import com.oliver.videoeditor.viewmodel.EditorViewModel

class MainActivity : ComponentActivity() {
    private val vm: EditorViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val orange = Color(0xFFFF6B1A)
            val scheme = if (vm.dark) darkColorScheme(primary = orange, background = Color(0xFF0A0A0A),
                surface = Color(0xFF161616), surfaceVariant = Color(0xFF242424), onPrimary = Color.White)
            else lightColorScheme(primary = orange, onPrimary = Color.White)
            MaterialTheme(colorScheme = scheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (vm.project == null) HomeScreen(vm) else {
                        BackHandler { vm.close() }
                        EditorScreen(vm)
                    }
                }
            }
        }
    }
}
