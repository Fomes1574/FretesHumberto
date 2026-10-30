package io.github.fomes1574.freteshumberto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.fomes1574.freteshumberto.ui.FretesApp
import io.github.fomes1574.freteshumberto.ui.theme.TemaMeusFretes

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemaMeusFretes {
                FretesApp()
            }
        }
    }
}
