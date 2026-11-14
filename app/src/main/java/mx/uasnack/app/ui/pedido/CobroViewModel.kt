
package mx.uasnack.app.ui.pedido

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.uasnack.app.data.red.aErrorApi
import mx.uasnack.app.data.red.dto.CrearPedidoRequest
import mx.uasnack.app.data.red.dto.LineaPedidoRequest
import mx.uasnack.app.data.repositorio.CarritoRepository
import mx.uasnack.app.data.repositorio.PedidoRepository
import mx.uasnack.app.domain.modelo.Carrito
import mx.uasnack.app.domain.modelo.PedidoCreado
import mx.uasnack.app.ui.navegacion.Rutas
import mx.uasnack.app.uasnackApp
import com.stripe.android.paymentsheet.PaymentSheetResult
import java.util.UUID
import kotlinx.coroutines.flow.receiveAsFlow

data class CobroUiState(
    val carrito: Carrito = Carrito(),
    val cargandoCarrito: Boolean = true,
    val procesando: Boolean = false,
    val pedidoCreado: PedidoCreado? = null,
    val modoRetiro: String = "",
    val franja: String? = null,
    val error: String? = null,
    val pagoCompletado: Boolean = false
)

// Eventos de una sola ejecución. No deben guardarse en el StateFlow.
sealed interface EventoCobro {
    data class AbrirPaymentSheet(
        val pedido: PedidoCreado
    ) : EventoCobro

    data class IrAPedidoConfirmado(
        val codigoPedido: String,
        val hora: String?
    ) : EventoCobro
}

class CobroViewModel(
    private val estadoGuardado: SavedStateHandle,
    private val carritoRepository: CarritoRepository,
    private val pedidoRepository: PedidoRepository
) : ViewModel() {

    private val idTienda: Int =
        checkNotNull(estadoGuardado[Rutas.ARG_ID_TIENDA])

    private val modoRetiro: String =
        checkNotNull(estadoGuardado[Rutas.ARG_MODO_RETIRO])

    private val franja: String? =
        estadoGuardado.get<String>(Rutas.ARG_FRANJA)
            ?.takeIf { it.isNotBlank() }

    // Si Android recrea el proceso, se recupera el pedido pendiente de pago.
    private val pedidoRestaurado = recuperarPedido()

    private val _uiState = MutableStateFlow(
        CobroUiState(
            cargandoCarrito = pedidoRestaurado == null,
            pedidoCreado = pedidoRestaurado,
            modoRetiro = modoRetiro,
            franja = franja
        )
    )

    val uiState: StateFlow<CobroUiState> = _uiState.asStateFlow()

    private val _eventos = Channel<EventoCobro>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    // La clave queda guardada mientras se esté intentando crear EL MISMO pedido.
    private var llaveIdempotenciaActual: String?
        get() = estadoGuardado[CLAVE_IDEMPOTENCIA]
        set(value) {
            estadoGuardado[CLAVE_IDEMPOTENCIA] = value
        }

    init {
        // El carrito es la fuente de datos local para construir la petición.
        viewModelScope.launch {
            carritoRepository.carrito().collect { carrito ->
                _uiState.update { actual ->
                    if (actual.pedidoCreado == null) {
                        actual.copy(
                            carrito = carrito,
                            cargandoCarrito = false
                        )
                    } else {
                        actual
                    }
                }
            }
        }
    }

    fun confirmarCobro() {
        val actual = _uiState.value

        if (actual.procesando || actual.pagoCompletado) return

        // Si ya existe el pedido, NO volver a llamar a POST /pedidos.
        // Solo se vuelve a presentar el mismo PaymentIntent.
        actual.pedidoCreado?.let { pedido ->
            abrirPaymentSheet(pedido)
            return
        }

        if (actual.cargandoCarrito || actual.carrito.vacio) {
            _uiState.update {
                it.copy(error = "Tu carrito está vacío.")
            }
            return
        }

        if (actual.carrito.idTienda != idTienda) {
            _uiState.update {
                it.copy(error = "El carrito ya no corresponde a esta tienda.")
            }
            return
        }

        if (modoRetiro == "franja" && franja == null) {
            _uiState.update {
                it.copy(error = "Selecciona una franja de retiro.")
            }
            return
        }

        if (modoRetiro != "franja" && modoRetiro != "voy_saliendo") {
            _uiState.update {
                it.copy(error = "El modo de retiro no es válido.")
            }
            return
        }

        val solicitud = CrearPedidoRequest(
            idTienda = idTienda,
            modoRetiro = modoRetiro,
            franja = if (modoRetiro == "franja") franja else null,
            lineas = actual.carrito.lineas.map { linea ->
                LineaPedidoRequest(
                    idProducto = linea.idProducto,
                    cantidad = linea.cantidad,
                    extras = linea.extras.map { it.id },
                    quitables = linea.quitables.map { it.id }
                )
            }
        )

        // Una clave por solicitud. Si falla la red se conserva para el reintento.
        val clave = llaveIdempotenciaActual
            ?: UUID.randomUUID().toString().also {
                llaveIdempotenciaActual = it
            }

        _uiState.update {
            it.copy(procesando = true, error = null)
        }

        viewModelScope.launch {
            val resultado = pedidoRepository.crear(
                pedido = solicitud,
                idempotencyKey = clave
            )

            if (resultado.isSuccess) {
                val pedido = resultado.getOrThrow()

                guardarPedido(pedido)

                _uiState.update {
                    it.copy(
                        procesando = false,
                        pedidoCreado = pedido,
                        error = null
                    )
                }

                // El pedido ya existe en el servidor. No se debe volver a crear
                // aunque el pago se cancele o falle.
                runCatching {
                    carritoRepository.vaciar()
                }

                _eventos.send(EventoCobro.AbrirPaymentSheet(pedido))
            } else {
                val mensaje = resultado.exceptionOrNull()
                    ?.aErrorApi()
                    ?.mensaje
                    ?: "No se pudo crear el pedido."

                _uiState.update {
                    it.copy(
                        procesando = false,
                        error = mensaje
                    )
                }
                // No borramos llaveIdempotenciaActual.
                // El siguiente intento utilizará la misma clave.
            }
        }
    }

    private fun abrirPaymentSheet(pedido: PedidoCreado) {
        _uiState.update { it.copy(error = null) }

        viewModelScope.launch {
            _eventos.send(EventoCobro.AbrirPaymentSheet(pedido))
        }
    }

    fun errorConfiguracionStripe() {
        _uiState.update {
            it.copy(
                error = "No se pudo iniciar el servicio de pago. Verifica la configuración de Stripe."
            )
        }
    }


    fun procesarResultado(resultado: PaymentSheetResult) {
        when (resultado) {

            is PaymentSheetResult.Completed -> {
                val pedido = _uiState.value.pedidoCreado ?: return

                _uiState.update {
                    it.copy(
                        pagoCompletado = true,
                        error = null
                    )
                }

                viewModelScope.launch {
                    _eventos.send(
                        EventoCobro.IrAPedidoConfirmado(
                            codigoPedido = pedido.codigoPedido,
                            hora = pedido.franjaInicio
                        )
                    )
                }
            }

            is PaymentSheetResult.Canceled -> {
                _uiState.update {
                    it.copy(
                        error = "Cancelaste el pago. Puedes intentarlo nuevamente."
                    )
                }
            }

            is PaymentSheetResult.Failed -> {
                _uiState.update {
                    it.copy(
                        error = resultado.error.localizedMessage
                            ?: "No se pudo completar el pago. Inténtalo nuevamente."
                    )
                }
            }
        }
    }


    private fun guardarPedido(pedido: PedidoCreado) {
        estadoGuardado[CODIGO_PEDIDO] = pedido.codigoPedido
        estadoGuardado[ESTADO_PEDIDO] = pedido.estado
        estadoGuardado[TOTAL_PEDIDO] = pedido.total
        estadoGuardado[INICIO_FRANJA] = pedido.franjaInicio
        estadoGuardado[FIN_FRANJA] = pedido.franjaFin
        estadoGuardado[CLIENT_SECRET] = pedido.clientSecret
        estadoGuardado[STRIPE_ACCOUNT] = pedido.stripeAccount
    }

    private fun recuperarPedido(): PedidoCreado? {
        val codigo = estadoGuardado.get<String>(CODIGO_PEDIDO) ?: return null
        val estado = estadoGuardado.get<String>(ESTADO_PEDIDO) ?: return null
        val total = estadoGuardado.get<String>(TOTAL_PEDIDO) ?: return null
        val clientSecret = estadoGuardado.get<String>(CLIENT_SECRET) ?: return null
        val stripeAccount = estadoGuardado.get<String>(STRIPE_ACCOUNT) ?: return null

        return PedidoCreado(
            codigoPedido = codigo,
            estado = estado,
            total = total,
            franjaInicio = estadoGuardado.get<String>(INICIO_FRANJA),
            franjaFin = estadoGuardado.get<String>(FIN_FRANJA),
            clientSecret = clientSecret,
            stripeAccount = stripeAccount
        )
    }

    companion object {
        private const val CLAVE_IDEMPOTENCIA = "cobro.claveIdempotencia"
        private const val CODIGO_PEDIDO = "cobro.codigoPedido"
        private const val ESTADO_PEDIDO = "cobro.estadoPedido"
        private const val TOTAL_PEDIDO = "cobro.totalPedido"
        private const val INICIO_FRANJA = "cobro.inicioFranja"
        private const val FIN_FRANJA = "cobro.finFranja"
        private const val CLIENT_SECRET = "cobro.clientSecret"
        private const val STRIPE_ACCOUNT = "cobro.stripeAccount"

        val Fabrica = viewModelFactory {
            initializer {
                val app = uasnackApp()

                CobroViewModel(
                    estadoGuardado = createSavedStateHandle(),
                    carritoRepository = app.carritoRepository,
                    pedidoRepository = app.pedidoRepository
                )
            }
        }
    }
}
