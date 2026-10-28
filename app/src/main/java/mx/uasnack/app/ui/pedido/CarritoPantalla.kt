package mx.uasnack.app.ui.pedido

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import mx.uasnack.app.domain.modelo.LineaCarrito
import mx.uasnack.app.ui.componentes.BotonPrincipal
import mx.uasnack.app.ui.componentes.formatearPrecio
import mx.uasnack.app.ui.tema.EstiloDatos

// Pantalla 7 · Carrito (#170). Mismo flujo que la PWA (#168).
@Composable
fun CarritoPantalla(
    alRegresarACatalogo: (Int) -> Unit,
    alIrATiendas: () -> Unit,
    alContinuar: (idTienda: Int) -> Unit,
    viewModel: CarritoViewModel = viewModel(factory = CarritoViewModel.Fabrica)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                is EventoCarrito.RegresarACatalogo -> alRegresarACatalogo(evento.idTienda)
            }
        }
    }

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        when {
            estado.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            // Vacío al entrar (por ejemplo, con el botón atrás después de pedir)
            estado.carrito.vacio -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
            ) {
                Text("Tu carrito está vacío", style = MaterialTheme.typography.titleMedium)
                Text("Agrega productos desde una tienda.", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = alIrATiendas) { Text("Ver tiendas") }
            }

            else -> Column(Modifier.fillMaxSize()) {
                Text(
                    text = "Carrito",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp)
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(estado.carrito.lineas, key = { it.clave }) { linea ->
                        TarjetaLinea(
                            linea = linea,
                            alCambiarCantidad = { viewModel.cambiarCantidad(linea.clave, it) },
                            alQuitar = { viewModel.quitar(linea.clave) }
                        )
                    }
                    item {
                        TextButton(onClick = { estado.carrito.idTienda?.let(alRegresarACatalogo) }) {
                            Text("Agregar más productos")
                        }
                    }
                }
                // "Continuar" lleva a Elección de retiro (pantalla 8, #55)
                BotonPrincipal(
                    texto = "Continuar · ${formatearPrecio(estado.carrito.totalLocal)}",
                    alPulsar = { estado.carrito.idTienda?.let(alContinuar) },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TarjetaLinea(linea: LineaCarrito, alCambiarCantidad: (Int) -> Unit, alQuitar: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(linea.nombre, style = MaterialTheme.typography.titleMedium)
            // "Queso extra · Sin cebolla", igual que la PWA
            val opciones = linea.extras.map { it.nombre } + linea.quitables.map { "Sin ${it.nombre.lowercase()}" }
            if (opciones.isNotEmpty()) {
                Text(
                    text = opciones.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(formatearPrecio(linea.subtotal), style = EstiloDatos)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedIconButton(
                    onClick = { alCambiarCantidad(linea.cantidad - 1) },
                    modifier = Modifier.semantics { contentDescription = "Quitar una de ${linea.nombre}" }
                ) { Text("−") }
                Text("${linea.cantidad}", style = EstiloDatos, modifier = Modifier.padding(horizontal = 16.dp))
                OutlinedIconButton(
                    onClick = { alCambiarCantidad(linea.cantidad + 1) },
                    modifier = Modifier.semantics { contentDescription = "Agregar una de ${linea.nombre}" }
                ) { Text("+") }
                Box(Modifier.weight(1f))
                TextButton(onClick = alQuitar) { Text("Quitar", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
