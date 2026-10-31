package mx.uasnack.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import mx.uasnack.app.ui.navegacion.NavegacionUASnack
import mx.uasnack.app.ui.tema.TemaUASnack

// Única Activity de la app: pone el tema y la navegación; las pantallas viven en ui/.
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // La app dibuja detrás de las barras del sistema. Se fuerzan íconos oscuros en las barras
        // porque la app siempre es clara: con el celular en modo oscuro, unos íconos blancos
        // quedarían invisibles sobre el fondo claro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        // El repositorio de sesión lo arma UasnackApp una sola vez para toda la app.
        // Se le pasa a la navegación para que decida si abrir en el login o en Tiendas.
        val sesionRepository = (application as UasnackApp).sesionRepository

        setContent {
            TemaUASnack {
                // Surface pinta el fondo (surface del Anexo B) y el color de texto por defecto
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Solo en debug y en Android 17+: espera el permiso para hablar con el backend local
                    ConPermisoRedLocal { NavegacionUASnack(sesionRepository) }
                }
            }
        }
    }
}
