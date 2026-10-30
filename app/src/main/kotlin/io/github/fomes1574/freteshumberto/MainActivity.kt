package io.github.fomes1574.freteshumberto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.fomes1574.freteshumberto.ui.FretesApp
import io.github.fomes1574.freteshumberto.ui.FretesViewModel
import io.github.fomes1574.freteshumberto.ui.theme.TemaMeusFretes

class MainActivity : ComponentActivity() {
    // Fábrica explícita: usa a Application desta Activity (a fábrica padrão do AndroidViewModel
    // guarda a primeira Application numa variável estática).
    private val vm: FretesViewModel by viewModels {
        viewModelFactory { initializer { FretesViewModel(application as FretesApplication) } }
    }

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
