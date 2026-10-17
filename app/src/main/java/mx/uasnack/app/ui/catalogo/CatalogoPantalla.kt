package mx.uasnack.app.ui.catalogo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.uasnack.app.domain.modelo.Producto
import mx.uasnack.app.ui.componentes.BotonPrincipal
import mx.uasnack.app.ui.componentes.ChipOpcion
import mx.uasnack.app.ui.componentes.formatearPrecio
import mx.uasnack.app.ui.tema.EstiloDatos

// Pantalla 5 · Catálogo (#48). Agotado al 45 %, con etiqueta, NO abre y NUNCA se oculta.
// alIrACarrito es null hasta que exista la pantalla de Carrito (#170); mientras, no sale el botón.
// Todavía no hay Coil en el proyecto: no se dibuja la foto (foto_url). Cuando se agregue, va un
// AsyncImage en TarjetaProducto.
@Composable
fun CatalogoPantalla(
    alAbrirProducto: (Int) -> Unit,
    alVolver: () -> Unit,
    alIrACarrito: (() -> Unit)? = null,
    viewModel: CatalogoViewModel = viewModel(factory = CatalogoViewModel.Fabrica)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            text = "← Tiendas",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(onClick = alVolver)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        )
        Text(
            text = estado.nombreTienda.ifEmpty { "Catálogo" },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Chips de categoría (solo si hay más de una)
        if (estado.categorias.size > 1) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChipOpcion("Todo", activo = estado.categoriaActiva == null, alPulsar = { viewModel.elegirCategoria(null) })
                estado.categorias.forEach { id ->
                    ChipOpcion(
                        texto = nombreCategoria(id),
                        activo = estado.categoriaActiva == id,
                        alPulsar = { viewModel.elegirCategoria(id) }
                    )
                }
            }
        }

        Box(Modifier.weight(1f)) {
            when {
                estado.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                estado.productos.isEmpty() && estado.error != null -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("No se pudo cargar", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text(estado.error.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    BotonPrincipal(texto = "Reintentar", alPulsar = viewModel::refrescar)
                }

                estado.productos.isEmpty() -> Text(
                    text = "Esta tienda todavía no tiene productos aquí.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Si falló el refresco pero hay caché, se avisa sin quitar la lista
                    estado.error?.let { error ->
                        item {
                            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    items(estado.productos, key = { it.id }) { producto ->
                        TarjetaProducto(producto, alAbrirProducto)
                    }
                }
            }
        }

        // "Ver carrito" solo si ya hay algo y la pantalla de Carrito existe
        if (alIrACarrito != null && estado.articulosEnCarrito > 0) {
            BotonPrincipal(
                texto = "Ver carrito · ${estado.articulosEnCarrito}",
                alPulsar = alIrACarrito,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun TarjetaProducto(producto: Producto, alAbrirProducto: (Int) -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            // Agotado: 45 % y sin clickable (se ve, pero no abre el detalle)
            .alpha(if (producto.agotado) 0.45f else 1f)
            .then(if (producto.agotado) Modifier else Modifier.clickable { alAbrirProducto(producto.id) })
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(producto.nombre, style = MaterialTheme.typography.titleMedium)
                producto.descripcion?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Importes en monoespaciada (Anexo B)
                Text(formatearPrecio(producto.precio), style = EstiloDatos)
            }
            if (producto.agotado) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = "Agotado",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
