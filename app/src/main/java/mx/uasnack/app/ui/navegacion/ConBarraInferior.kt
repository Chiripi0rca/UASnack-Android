package mx.uasnack.app.ui.navegacion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import mx.uasnack.app.ui.componentes.BarraInferior

// ESQUELETO (Sprint 02). Envuelve cada pantalla del grafo principal con la barra inferior (#229).
// En NavegacionUASnack, cada pantalla con sesión se escribe así:
//   composable(Rutas.TIENDAS) { ConBarraInferior(navController) { TiendasPantalla() } }
//
// contentWindowInsets = 0: las pantallas ya dejan su propio espacio para las barras del sistema
// (safeDrawingPadding); si el Scaffold también lo dejara, el espacio saldría doble.
@Composable
fun ConBarraInferior(
    navController: NavController,
    contenido: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = { BarraInferior(navController) },
        contentWindowInsets = WindowInsets(0)
    ) { espacio ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(espacio)
        ) {
            contenido()
        }
    }
}
