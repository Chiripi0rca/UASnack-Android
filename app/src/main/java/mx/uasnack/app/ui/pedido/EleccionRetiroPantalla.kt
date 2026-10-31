package mx.uasnack.app.ui.pedido

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.uasnack.app.domain.modelo.Franja
import mx.uasnack.app.ui.componentes.BotonPrincipal
import mx.uasnack.app.ui.componentes.ChipOpcion
import mx.uasnack.app.ui.componentes.PanelVoySaliendo

// Pantalla 8 · Elección de retiro (#55). Mismo flujo que la PWA (#54).
// Arriba se elige el modo; abajo, la rejilla de franjas o el panel "Voy saliendo" (#60).
// Nunca un tiempo fijo: las franjas y el rango de espera los manda el servidor.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EleccionRetiroPantalla(
    alVolver: () -> Unit,
    viewModel: EleccionRetiroViewModel = viewModel(factory = EleccionRetiroViewModel.Fabrica)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        if (estado.cargando) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
            return@Box
        }

        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = alVolver) { Text("← Carrito") }
            Text("¿Cómo recoges?", style = MaterialTheme.typography.headlineMedium)

            val modos = listOf(ModoRetiro.FRANJA to "Apartar franja", ModoRetiro.VOY_SALIENDO to "Voy saliendo")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modos.forEachIndexed { indice, (modo, texto) ->
                    SegmentedButton(
                        selected = estado.modo == modo,
                        onClick = { viewModel.elegirModo(modo) },
                        shape = SegmentedButtonDefaults.itemShape(index = indice, count = modos.size)
                    ) { Text(texto) }
                }
            }

            estado.error?.let { mensaje ->
                Text(mensaje, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = viewModel::refrescar) { Text("Reintentar") }
            }

            // Sin franjas no hay nada que avisar: RejillaFranjas ya dice que no hay horas
            if (estado.modo == ModoRetiro.FRANJA && !estado.cabeHoy && estado.franjas.isNotEmpty()) {
                AvisoPedidoGrande(
                    minutos = estado.minutos,
                    minutosMaximos = estado.minutosMaximosHoy,
                    alEditarCarrito = alVolver
                )
            }

            Box(Modifier.weight(1f)) {
                when (estado.modo) {
                    ModoRetiro.FRANJA -> RejillaFranjas(
                        franjas = estado.franjas,
                        elegida = estado.franjaElegida,
                        alElegir = viewModel::elegirFranja
                    )
                    ModoRetiro.VOY_SALIENDO -> estado.espera?.let { PanelVoySaliendo(estimado = it) }
                }
            }

            // "Continuar" lleva a Confirmación y pago (pantalla 9), que aún no existe: deshabilitado,
            // igual que hacía el Carrito antes de esta pantalla.
            BotonPrincipal(texto = "Continuar", alPulsar = {}, habilitado = false)
        }
    }
}

// El pedido no cabe en ninguna hora de hoy: se dice cuánto necesita, cuánto cabe y qué hacer.
// Las franjas se siguen mostrando abajo, deshabilitadas. La PWA (#54) debe usar los mismos textos.
@Composable
private fun AvisoPedidoGrande(minutos: Int, minutosMaximos: Int?, alEditarCarrito: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // TalkBack lo lee al aparecer
            .semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val color = MaterialTheme.colorScheme.onErrorContainer
            if (minutosMaximos == 0) {
                // Ya no queda cocina hoy: quitar productos no ayuda
                Text(
                    "Por hoy ya no hay horas para recoger",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    "Esta tienda ya no tiene cocina libre hoy. Prueba con “Voy saliendo”.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = color
                )
                return@Column
            }

            Text(
                "Tu pedido es muy grande para hoy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            val cuanto = minutosMaximos?.let { " y hoy solo caben hasta $it min" } ?: ""
            Text(
                "Necesita $minutos min de cocina$cuanto. Quita algunos productos o divídelo en dos pedidos.",
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
            TextButton(onClick = alEditarCarrito) { Text("Editar carrito") }
        }
    }
}

@Composable
private fun RejillaFranjas(franjas: List<Franja>, elegida: Franja?, alElegir: (Franja) -> Unit) {
    if (franjas.isEmpty()) {
        Text(
            "Por hoy ya no hay franjas en esta tienda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(franjas, key = { it.fecha + it.inicio }) { franja ->
            // Las llenas se ven deshabilitadas, nunca se ocultan. Con descuento: "−10%".
            ChipOpcion(
                texto = "${franja.inicio}–${franja.fin}",
                detalle = franja.descuento?.let { "−$it%" },
                activo = elegida?.inicio == franja.inicio,
                habilitado = franja.disponible,
                alPulsar = { alElegir(franja) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
