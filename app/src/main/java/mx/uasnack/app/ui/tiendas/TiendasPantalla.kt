package mx.uasnack.app.ui.tiendas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Pantalla principal temporal: solo sirve para tener a dónde llegar después de iniciar sesión.
@Composable
fun TiendasPantalla() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Tiendas", style = MaterialTheme.typography.titleLarge)
        Text(text = "Pantalla pendiente.", style = MaterialTheme.typography.bodyMedium)
    }
}
