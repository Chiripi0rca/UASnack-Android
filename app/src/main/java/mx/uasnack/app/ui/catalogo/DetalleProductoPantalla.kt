package mx.uasnack.app.ui.catalogo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.uasnack.app.ui.componentes.BotonPrincipal
import mx.uasnack.app.ui.componentes.ChipOpcion
import mx.uasnack.app.ui.componentes.formatearPrecio
import mx.uasnack.app.ui.tema.EstiloDatos

// Pantalla 6 · Detalle de producto (#51). Mismo flujo y textos que la PWA (#50).
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetalleProductoPantalla(
    alVolver: () -> Unit,
    viewModel: DetalleProductoViewModel = viewModel(factory = DetalleProductoViewModel.Fabrica)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // Al agregar con éxito se regresa al catálogo (una sola vez, por eso es evento)
    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                EventoDetalle.Agregado -> alVolver()
            }
        }
    }

    val producto = estado.producto
    if (producto == null) {
        Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
            if (estado.cargando) {
                CircularProgressIndicator()
            } else {
                Text(estado.error ?: "No encontramos lo que buscas.", color = MaterialTheme.colorScheme.error)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "← Volver",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(onClick = alVolver)
                .padding(vertical = 14.dp)
        )
        Text(producto.nombre, style = MaterialTheme.typography.headlineMedium)
        producto.descripcion?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text(formatearPrecio(producto.precio), style = EstiloDatos)
        // Rango de preparación tal como lo manda el servidor: nunca un tiempo fijo
        Text(
            text = "Preparación: ${producto.tiempoPrepMin}–${producto.tiempoPrepMax} min",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (producto.extras.isNotEmpty()) {
            Text("Extras", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                producto.extras.forEach { extra ->
                    ChipOpcion(
                        texto = extra.nombre,
                        detalle = extra.precio?.let { "+${formatearPrecio(it)}" },
                        activo = extra.id in estado.extrasSeleccionados,
                        alPulsar = { viewModel.alternarExtra(extra.id) }
                    )
                }
            }
        }

        if (producto.quitables.isNotEmpty()) {
            Text("Quitar", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                producto.quitables.forEach { quitable ->
                    ChipOpcion(
                        texto = "Sin ${quitable.nombre.lowercase()}",
                        activo = quitable.id in estado.quitablesSeleccionados,
                        alPulsar = { viewModel.alternarQuitable(quitable.id) }
                    )
                }
            }
        }

        // Cantidad: entre 1 y las unidades disponibles (botones de 48 dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedIconButton(
                onClick = { viewModel.cambiarCantidad(estado.cantidad - 1) },
                enabled = estado.cantidad > 1,
                modifier = Modifier.semantics { contentDescription = "Quitar una" }
            ) { Text("−") }
            Text(
                text = "${estado.cantidad}",
                style = EstiloDatos,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            OutlinedIconButton(
                onClick = { viewModel.cambiarCantidad(estado.cantidad + 1) },
                enabled = estado.cantidad < estado.disponibles,
                modifier = Modifier.semantics { contentDescription = "Agregar una" }
            ) { Text("+") }
        }

        // Mismos textos que la PWA
        if (!estado.puedeAgregar) {
            Text(
                text = if (producto.agotado) "Este producto está agotado." else "Ya tienes en el carrito todas las unidades disponibles.",
                color = MaterialTheme.colorScheme.error
            )
        }
        estado.aviso?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        BotonPrincipal(
            texto = "Agregar al carrito · ${formatearPrecio(estado.importe)}",
            alPulsar = viewModel::agregar,
            habilitado = estado.puedeAgregar,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }

    // OtraTienda: preguntar antes de vaciar el carrito
    if (estado.preguntarVaciar) {
        AlertDialog(
            onDismissRequest = viewModel::cancelarVaciar,
            title = { Text("Carrito de otra tienda") },
            text = { Text("Tu carrito tiene productos de otra tienda. ¿Lo vacías para agregar este?") },
            confirmButton = { TextButton(onClick = viewModel::confirmarVaciar) { Text("Vaciar y agregar") } },
            dismissButton = { TextButton(onClick = viewModel::cancelarVaciar) { Text("Cancelar") } }
        )
    }
}
