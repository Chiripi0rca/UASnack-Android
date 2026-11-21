
package mx.uasnack.app.ui.pagos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.paymentsheet.PaymentSheet
import mx.uasnack.app.data.pago.StripeConfiguracion
import mx.uasnack.app.domain.modelo.MetodoPago
import mx.uasnack.app.ui.componentes.BotonPrincipal
import mx.uasnack.app.ui.componentes.StripePaymentSheet

@Composable
fun MetodosPagoPantalla(
    viewModel: MetodosPagoViewModel = viewModel(
        factory = MetodosPagoViewModel.Fabrica
    )
) {

    val estado by viewModel.estado.collectAsState()

    val context = LocalContext.current

    var metodoPendienteEliminar by remember {
        mutableStateOf<MetodoPago?>(null)
    }

    // Reutiliza PaymentSheet de la tarea #190.
    val paymentSheet = StripePaymentSheet { resultado ->
        viewModel.procesarResultado(resultado)
    }

    // Recibe el SetupIntent y abre Stripe.
    LaunchedEffect(viewModel) {

        viewModel.eventos.collect { evento ->

            when (evento) {

                is EventoMetodosPago.AbrirSetupIntent -> {

                    // Las tarjetas pertenecen a la plataforma.
                    // No se utiliza la cuenta conectada de una tienda.
                    val configurado =
                        StripeConfiguracion.inicializar(
                            context = context
                        )

                    if (!configurado) {

                        viewModel.errorConfiguracionStripe()

                    } else {

                        paymentSheet.presentWithSetupIntent(
                            evento.clientSecret,
                            PaymentSheet.Configuration.Builder(
                                "UASnack"
                            ).build()
                        )
                    }
                }
            }
        }
    }

    // Diálogo para confirmar la eliminación.
    metodoPendienteEliminar?.let { metodo ->

        AlertDialog(
            onDismissRequest = {
                metodoPendienteEliminar = null
            },
            title = {
                Text("Eliminar tarjeta")
            },
            text = {
                Text(
                    "¿Deseas eliminar tu tarjeta ${metodo.marca} terminada en ${metodo.ultimos4}?"
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {
                        viewModel.eliminarMetodo(metodo.id)
                        metodoPendienteEliminar = null
                    },
                    enabled = estado.eliminandoId == null
                ) {
                    Text(
                        text = "Eliminar",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        metodoPendienteEliminar = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Métodos de pago",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Administra las tarjetas que utilizas para pagar tus pedidos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {

            when {

                // Estado de carga inicial.
                estado.cargando && estado.metodos.isEmpty() -> {

                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Error sin tarjetas disponibles.
                estado.metodos.isEmpty() && estado.error != null -> {

                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = "No se pudieron cargar tus tarjetas",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )

                        Text(
                            text = estado.error.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = viewModel::cargarMetodos
                        ) {
                            Text("Reintentar")
                        }
                    }
                }

                // Estado vacío.
                estado.metodos.isEmpty() -> {

                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Outlined.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "No tienes tarjetas guardadas",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Agrega una tarjeta para utilizarla en tus próximos pedidos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Estado con tarjetas.
                else -> {

                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        if (estado.cargando) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }

                        estado.error?.let { mensaje ->

                            item {
                                Text(
                                    text = mensaje,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                TextButton(
                                    onClick = viewModel::cargarMetodos
                                ) {
                                    Text("Reintentar")
                                }
                            }
                        }

                        items(
                            items = estado.metodos,
                            key = { it.id }
                        ) { metodo ->

                            TarjetaMetodoPago(
                                metodo = metodo,
                                eliminando =
                                    estado.eliminandoId == metodo.id,
                                habilitado =
                                    estado.eliminandoId == null &&
                                            !estado.agregando,
                                alEliminar = {
                                    metodoPendienteEliminar = metodo
                                }
                            )
                        }
                    }
                }
            }
        }

        BotonPrincipal(
            texto = "Agregar tarjeta",
            alPulsar = viewModel::agregarMetodo,
            habilitado = !estado.agregando &&
                    estado.eliminandoId == null &&
                    !estado.cargando,
            cargando = estado.agregando,
            textoCargando = "Preparando formulario..."
        )
    }
}

@Composable
private fun TarjetaMetodoPago(
    metodo: MetodoPago,
    eliminando: Boolean,
    habilitado: Boolean,
    alEliminar: () -> Unit
) {

    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Outlined.CreditCard,
                contentDescription = "Tarjeta bancaria",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                Text(
                    text = metodo.marca.uppercase(),
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "•••• ${metodo.ultimos4}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Vence ${metodo.vence}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (eliminando) {

                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )

            } else {

                TextButton(
                    onClick = alEliminar,
                    enabled = habilitado
                ) {
                    Text(
                        text = "Eliminar",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
