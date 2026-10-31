package mx.uasnack.app.ui.acceso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.uasnack.app.data.red.aErrorApi
import mx.uasnack.app.data.repositorio.RecuperacionPasswordRepository
import mx.uasnack.app.uasnackApp

data class EstadoOlvidePasswordUi(
    val correo: String = "",
    val cargando: Boolean = false,
    val errorCorreo: String? = null,
    val errorGeneral: String? = null
)

sealed interface EventoOlvidePassword {
    data class CodigoSolicitado(val correo: String) : EventoOlvidePassword
}

class OlvidePasswordViewModel(
    private val repository: RecuperacionPasswordRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoOlvidePasswordUi())
    val estado: StateFlow<EstadoOlvidePasswordUi> = _estado.asStateFlow()

    private val _eventos = Channel<EventoOlvidePassword>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    fun cambiarCorreo(valor: String) {
        _estado.update {
            it.copy(
                correo = valor,
                errorCorreo = null,
                errorGeneral = null
            )
        }
    }

    fun solicitarCodigo() {
        val actual = _estado.value

        if (actual.cargando) return

        val correo = actual.correo.trim()

        if (correo.isBlank()) return

        _estado.update {
            it.copy(
                cargando = true,
                errorCorreo = null,
                errorGeneral = null
            )
        }

        viewModelScope.launch {
            repository.solicitarCodigo(correo)
                .onSuccess {
                    _estado.update {
                        it.copy(cargando = false)
                    }

                    _eventos.send(
                        EventoOlvidePassword.CodigoSolicitado(
                            correo.lowercase()
                        )
                    )
                }
                .onFailure { excepcion ->

                    val error = excepcion.aErrorApi()

                    val errorCorreo =
                        error.primerErrorPorCampo["correo_institucional"]

                    _estado.update {
                        it.copy(
                            cargando = false,
                            errorCorreo = errorCorreo,
                            errorGeneral = if (errorCorreo == null) {
                                error.mensaje
                            } else {
                                null
                            }
                        )
                    }
                }
        }
    }

    companion object {
        val Fabrica = viewModelFactory {
            initializer {
                OlvidePasswordViewModel(
                    uasnackApp().recuperacionPasswordRepository
                )
            }
        }
    }
}