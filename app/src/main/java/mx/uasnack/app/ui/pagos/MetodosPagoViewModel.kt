
package mx.uasnack.app.ui.pagos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.uasnack.app.data.red.aErrorApi
import mx.uasnack.app.data.repositorio.PagoRepository
import mx.uasnack.app.domain.modelo.MetodoPago
import mx.uasnack.app.uasnackApp

data class EstadoMetodosPagoUi(
    val cargando: Boolean = true,
    val metodos: List<MetodoPago> = emptyList(),
    val agregando: Boolean = false,
    val eliminandoId: String? = null,
    val error: String? = null
)

// Eventos de una sola ejecución.
sealed interface EventoMetodosPago {
    data class AbrirSetupIntent(
        val clientSecret: String
    ) : EventoMetodosPago
}

class MetodosPagoViewModel(
    private val pagoRepository: PagoRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(
        EstadoMetodosPagoUi()
    )

    val estado: StateFlow<EstadoMetodosPagoUi> =
        _estado.asStateFlow()

    private val _eventos =
        Channel<EventoMetodosPago>(Channel.BUFFERED)

    val eventos = _eventos.receiveAsFlow()

    init {
        cargarMetodos()
    }

    // GET /pagos/metodos
    fun cargarMetodos() {
        viewModelScope.launch {

            _estado.update {
                it.copy(
                    cargando = true,
                    error = null
                )
            }

            pagoRepository.metodos()
                .onSuccess { metodos ->

                    _estado.update {
                        it.copy(
                            cargando = false,
                            metodos = metodos,
                            error = null
                        )
                    }
                }
                .onFailure { excepcion ->

                    _estado.update {
                        it.copy(
                            cargando = false,
                            error = excepcion.aErrorApi().mensaje
                        )
                    }
                }
        }
    }

    // POST /pagos/metodos.
    // Solicita el SetupIntent, no guarda la tarjeta directamente.
    fun agregarMetodo() {

        val actual = _estado.value

        if (
            actual.agregando ||
            actual.eliminandoId != null
        ) return

        _estado.update {
            it.copy(
                agregando = true,
                error = null
            )
        }

        viewModelScope.launch {

            pagoRepository.agregarMetodo()
                .onSuccess { clientSecret ->

                    if (clientSecret.isBlank()) {

                        _estado.update {
                            it.copy(
                                agregando = false,
                                error = "No se recibió la información necesaria para agregar la tarjeta."
                            )
                        }

                    } else {

                        _eventos.send(
                            EventoMetodosPago.AbrirSetupIntent(
                                clientSecret
                            )
                        )
                    }
                }
                .onFailure { excepcion ->

                    _estado.update {
                        it.copy(
                            agregando = false,
                            error = excepcion.aErrorApi().mensaje
                        )
                    }
                }
        }
    }

    // Resultado del formulario de Stripe.
    fun procesarResultado(
        resultado: PaymentSheetResult
    ) {

        when (resultado) {

            is PaymentSheetResult.Completed -> {

                _estado.update {
                    it.copy(
                        agregando = false,
                        error = null
                    )
                }

                // Volver a consultar al servidor para obtener
                // la tarjeta que acaba de guardarse.
                cargarMetodos()
            }

            is PaymentSheetResult.Canceled -> {

                _estado.update {
                    it.copy(
                        agregando = false,
                        error = null
                    )
                }
            }

            is PaymentSheetResult.Failed -> {

                _estado.update {
                    it.copy(
                        agregando = false,
                        error = resultado.error.localizedMessage
                            ?: "No se pudo guardar la tarjeta. Inténtalo nuevamente."
                    )
                }
            }
        }
    }

    fun errorConfiguracionStripe() {
        _estado.update {
            it.copy(
                agregando = false,
                error = "No se pudo iniciar Stripe. Verifica la clave publicable."
            )
        }
    }

    // DELETE /pagos/metodos/{id}
    fun eliminarMetodo(id: String) {

        val actual = _estado.value

        if (
            id.isBlank() ||
            actual.eliminandoId != null ||
            actual.agregando
        ) return

        _estado.update {
            it.copy(
                eliminandoId = id,
                error = null
            )
        }

        viewModelScope.launch {

            pagoRepository.eliminarMetodo(id)
                .onSuccess {

                    _estado.update { actual ->
                        actual.copy(
                            metodos = actual.metodos.filterNot {
                                it.id == id
                            },
                            eliminandoId = null,
                            error = null
                        )
                    }
                }
                .onFailure { excepcion ->

                    _estado.update {
                        it.copy(
                            eliminandoId = null,
                            error = excepcion.aErrorApi().mensaje
                        )
                    }
                }
        }
    }

    companion object {

        val Fabrica = viewModelFactory {

            initializer {

                val app = uasnackApp()

                MetodosPagoViewModel(
                    pagoRepository = app.pagoRepository
                )
            }
        }
    }
}
