package io.github.fomes1574.freteshumberto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.fomes1574.freteshumberto.ui.FretesApp
import io.github.fomes1574.freteshumberto.ui.FretesViewModel
import io.github.fomes1574.freteshumberto.ui.theme.TemaMeusFretes

class MainActivity : ComponentActivity() {
    private val vm: FretesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val letra by vm.letra.collectAsStateWithLifecycle()
            TemaMeusFretes(escalaLetra = letra.escala) {
                FretesApp(vm)
            }
        }
    }
}
