package mx.uasnack.app.ui.tiendas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.uasnack.app.domain.modelo.Tienda
import mx.uasnack.app.ui.componentes.BotonPrincipal
import mx.uasnack.app.ui.tema.EstiloDatos

// Pantalla 4 · Tiendas (#48). Mismo comportamiento que la PWA (#47):
// dos bloques ("Tu facultad" / "Otras facultades"); cerrada = atenuada y no se puede tocar.
// "Repetir último pedido" llega cuando exista GET /pedidos.
//
// statusBarsPadding (y no safeDrawingPadding): abajo ya está la barra inferior, que deja su propio
// espacio para la barra del sistema; si la pantalla también lo dejara, saldría doble.
@Composable
fun TiendasPantalla(
    alAbrirTienda: (Int) -> Unit,
    viewModel: TiendasViewModel = viewModel(factory = TiendasViewModel.Fabrica)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        when {
            // Los tres estados: cargando, error (sin nada que mostrar) y vacío
            estado.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            estado.tiendas.isEmpty() && estado.error != null -> MensajeCentrado(
                titulo = "No se pudo cargar",
                mensaje = estado.error,
                alReintentar = viewModel::refrescar
            )

            estado.tiendas.isEmpty() -> MensajeCentrado(
                titulo = "No hay tiendas",
                mensaje = "Todavía no hay tiendas disponibles."
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Text("Tiendas", style = MaterialTheme.typography.headlineMedium) }
                // Si falló el refresco pero hay caché, se avisa sin quitar la lista
                estado.error?.let { error ->
                    item {
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                bloque("Tu facultad", estado.tiendas.filter { it.esMiFacultad }, alAbrirTienda)
                bloque("Otras facultades", estado.tiendas.filterNot { it.esMiFacultad }, alAbrirTienda)
            }
        }
    }
}

// Un título y sus tarjetas. Si no hay tiendas, no agrega nada.
private fun LazyListScope.bloque(titulo: String, tiendas: List<Tienda>, alAbrirTienda: (Int) -> Unit) {
    if (tiendas.isEmpty()) return
    item {
        Text(
            titulo,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
    items(tiendas, key = { it.id }) { tienda -> TarjetaTienda(tienda, alAbrirTienda) }
}

@Composable
private fun TarjetaTienda(tienda: Tienda, alAbrirTienda: (Int) -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            // Cerrada: 45 % de opacidad y sin clickable (no se puede tocar)
            .alpha(if (tienda.abierta) 1f else 0.45f)
            .then(if (tienda.abierta) Modifier.clickable { alAbrirTienda(tienda.id) } else Modifier)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(tienda.nombre, style = MaterialTheme.typography.titleMedium)
            tienda.ubicacion?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = if (tienda.abierta) "Abierta" else "Cerrada",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Horas en monoespaciada (Anexo B)
            Text(
                text = "${tienda.horarioApertura.orEmpty()}–${tienda.horarioCierre.orEmpty()}",
                style = EstiloDatos.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Mensaje centrado para error y vacío. Con alReintentar aparece el botón "Reintentar".
@Composable
private fun MensajeCentrado(titulo: String, mensaje: String?, alReintentar: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Text(
            titulo,
            style = MaterialTheme.typography.titleMedium,
            color = if (alReintentar != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        mensaje?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        alReintentar?.let { BotonPrincipal(texto = "Reintentar", alPulsar = it) }
    }
}
