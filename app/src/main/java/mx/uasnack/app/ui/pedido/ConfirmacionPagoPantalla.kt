
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.paymentsheet.PaymentSheet
import mx.uasnack.app.data.pago.StripeConfiguracion
import mx.uasnack.app.domain.modelo.LineaCarrito
import mx.uasnack.app.ui.componentes.BotonPrincipal
import mx.uasnack.app.ui.componentes.StripePaymentSheet
import mx.uasnack.app.ui.componentes.formatearPrecio
import mx.uasnack.app.ui.tema.EstiloDatos

// Pantalla 9 · Confirmación y pago (#195).
@Composable
fun ConfirmacionPagoPantalla(
    alVolver: () -> Unit,
    alPedidoConfirmado: (String, String?) -> Unit,
    viewModel: CobroViewModel = viewModel(
        factory = CobroViewModel.Fabrica
    )
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Componente reutilizable preparado en la tarea #190.
    val paymentSheet = StripePaymentSheet { resultado ->
        viewModel.procesarResultado(resultado)
    }

    // Los eventos se ejecutan una sola vez y no forman parte del estado visual.
    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {

                is EventoCobro.AbrirPaymentSheet -> {

                    val configurado = StripeConfiguracion.inicializar(
                        context = context,
                        stripeAccountId = evento.pedido.stripeAccount
                    )

                    if (!configurado) {
                        viewModel.errorConfiguracionStripe()
                    } else {
                        paymentSheet.presentWithPaymentIntent(
                            evento.pedido.clientSecret,
                            PaymentSheet.Configuration.Builder(
                                "UASnack"
                            ).build()
                        )
                    }
                }

                is EventoCobro.IrAPedidoConfirmado -> {
                    alPedidoConfirmado(
                        evento.codigoPedido,
                        evento.hora
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        when {
            estado.cargandoCarrito && estado.pedidoCreado == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            estado.carrito.vacio && estado.pedidoCreado == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Tu carrito está vacío",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Agrega productos antes de continuar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(onClick = alVolver) {
                        Text("Volver")
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {

                    if (estado.pedidoCreado == null) {
                        TextButton(
                            onClick = alVolver,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("← Volver a retiro")
                        }
                    }

                    Text(
                        text = "Confirmación y pago",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        item {
                            ResumenRetiro(
                                modoRetiro = estado.modoRetiro,
                                franja = estado.franja
                            )
                        }

                        if (estado.carrito.lineas.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Productos de tu pedido",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            items(
                                items = estado.carrito.lineas,
                                key = { it.clave }
                            ) { linea ->
                                TarjetaConfirmacionLinea(linea)
                            }
                        }

                        estado.pedidoCreado?.let { pedido ->
                            item {
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Folio del pedido",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Text(
                                            text = pedido.codigoPedido,
                                            style = EstiloDatos
                                        )

                                        Text(
                                            text = "Total confirmado por el servidor",
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        Text(
                                            text = pedido.total
                                                .toBigDecimalOrNull()
                                                ?.let { formatearPrecio(it) }
                                                ?: pedido.total,
                                            style = EstiloDatos,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } ?: item {
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "El importe definitivo será calculado y validado por el servidor antes de abrir el formulario de pago.",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    estado.error?.let { mensaje ->
                        Text(
                            text = mensaje,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            )
                        )
                    }

                    BotonPrincipal(
                        texto = if (estado.pedidoCreado == null) {
                            "Confirmar y pagar"
                        } else {
                            "Volver a intentar pago"
                        },
                        alPulsar = viewModel::confirmarCobro,
                        modifier = Modifier.padding(16.dp),
                        habilitado = !estado.procesando &&
                                !estado.pagoCompletado &&
                                (
                                        estado.pedidoCreado != null ||
                                                !estado.carrito.vacio
                                        ),
                        cargando = estado.procesando,
                        textoCargando = "Creando pedido..."
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumenRetiro(
    modoRetiro: String,
    franja: String?
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Método de retiro",
                style = MaterialTheme.typography.titleMedium
            )

            when (modoRetiro) {
                "franja" -> {
                    Text(
                        text = "Apartar franja",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    franja?.let {
                        Text(
                            text = it,
                            style = EstiloDatos,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                "voy_saliendo" -> {
                    Text(
                        text = "Voy saliendo",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaConfirmacionLinea(
    linea: LineaCarrito
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = linea.nombre,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Cantidad: ${linea.cantidad}",
                style = MaterialTheme.typography.bodyMedium
            )

            val opciones = linea.extras.map { it.nombre } +
                    linea.quitables.map {
                        "Sin ${it.nombre.lowercase()}"
                    }

            if (opciones.isNotEmpty()) {
                Text(
                    text = opciones.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
