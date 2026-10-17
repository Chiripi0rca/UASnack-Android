package mx.uasnack.app.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Marca de lugar para los destinos de la barra inferior que aún no tienen pantalla (#229).
// Sin una ruta registrada, tocar Pedidos, Pagos o Perfil cerraría la app.
// Se borra cuando ya nadie la use.
@Composable
fun PantallaPendiente(titulo: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = titulo, style = MaterialTheme.typography.titleLarge)
        Text(text = "Pantalla pendiente.", style = MaterialTheme.typography.bodyMedium)
    }
}
