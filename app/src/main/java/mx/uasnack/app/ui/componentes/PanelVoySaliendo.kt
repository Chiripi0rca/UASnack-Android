package mx.uasnack.app.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.uasnack.app.domain.modelo.Estimado
import mx.uasnack.app.ui.tema.EstiloDatos

// Panel del modo "voy saliendo" en Elección de retiro (#60). Mismos textos que la PWA (#59).
// En este modo el pedido se paga y queda en espera: entra a cocina hasta que el alumno avisa que
// ya va saliendo (§7.3). Aquí solo se informa; ese aviso llega después, en la pantalla del pedido.
// Siempre un rango, nunca un tiempo fijo: lo calcula el servidor (§7.4).
@Composable
fun PanelVoySaliendo(estimado: Estimado, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Voy saliendo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Tu pedido tardaría unos", style = MaterialTheme.typography.bodyMedium)
            Text("${estimado.min}–${estimado.max} min", style = EstiloDatos)

            // A lo mucho UN aviso: si no hay cupo, ese es el que importa; si hay cupo pero la cocina
            // está saturada, se recomienda apartar una franja. liveRegion: TalkBack lo lee al aparecer.
            val aviso = when {
                !estimado.voySaliendoDisponible ->
                    "Ahorita la tienda no acepta más pedidos en “voy saliendo”. Aparta una franja."
                estimado.saturada -> {
                    val pedidos = if (estimado.pedidosEnCocina == 1) "1 pedido" else "${estimado.pedidosEnCocina} pedidos"
                    "Ahorita hay $pedidos en cocina. Te conviene apartar una franja."
                }
                else -> null
            }
            aviso?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }
    }
}
