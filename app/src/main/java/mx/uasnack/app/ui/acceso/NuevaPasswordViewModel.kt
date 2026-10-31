package mx.uasnack.app.ui.acceso

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.uasnack.app.data.red.aErrorApi
import mx.uasnack.app.data.repositorio.RecuperacionPasswordRepository
import mx.uasnack.app.ui.navegacion.Rutas
import mx.uasnack.app.uasnackApp

data class EstadoNuevaPasswordUi(
    val correo: String = "",
    val codigo: String = "",
    val password: String = "",
    val confirmacion: String = "",

    val cargando: Boolean = false,
    val restablecida: Boolean = false,

    val errorCodigo: String? = null,
    val errorPassword: String? = null,
    val errorGeneral: String? = null,

    val reenviando: Boolean = false,
    val codigoReenviado: Boolean = false,
    val errorReenvio: String? = null
)

class NuevaPasswordViewModel(
    estadoGuardado: SavedStateHandle,
    private val repository: RecuperacionPasswordRepository
) : ViewModel() {

    // El correo viene de la ruta de navegación.
    private val correo = estadoGuardado
        .get<String>(Rutas.ARG_CORREO)
        .orEmpty()

    private val _estado = MutableStateFlow(
        EstadoNuevaPasswordUi(correo = correo)
    )

    val estado: StateFlow<EstadoNuevaPasswordUi> = _estado.asStateFlow()

    // Campo de código: únicamente acepta números y hasta seis dígitos.
    fun cambiarCodigo(valor: String) {
        val codigo = valor.filter { it.isDigit() }.take(6)

        _estado.update {
            it.copy(
                codigo = codigo,
                errorCodigo = null,
                errorGeneral = null
            )
        }
    }

    fun cambiarPassword(valor: String) {
        _estado.update {
            it.copy(
                password = valor,
                errorPassword = null,
                errorGeneral = null
            )
        }
    }

    fun cambiarConfirmacion(valor: String) {
        _estado.update {
            it.copy(
                confirmacion = valor,
                errorGeneral = null
            )
        }
    }

    // Enviar el código y registrar la nueva contraseña.
    fun restablecerPassword() {

        val actual = _estado.value

        if (actual.cargando || actual.restablecida) return

        if (actual.codigo.length != 6 || actual.password.isBlank()) return

        _estado.update {
            it.copy(
                cargando = true,
                errorCodigo = null,
                errorPassword = null,
                errorGeneral = null
            )
        }

        viewModelScope.launch {

            repository.restablecerPassword(
                correo = correo,
                codigo = actual.codigo,
                password = actual.password,
                confirmacion = actual.confirmacion
            )
                .onSuccess {

                    _estado.update {
                        it.copy(
                            cargando = false,
                            restablecida = true
                        )
                    }
                }
                .onFailure { excepcion ->

                    val error = excepcion.aErrorApi()
                    val campos = error.primerErrorPorCampo

                    val errorCodigo = campos["codigo"]

                    val errorPassword =
                        campos["password"]
                            ?: campos["password_confirmation"]

                    _estado.update {
                        it.copy(
                            cargando = false,
                            errorCodigo = errorCodigo,
                            errorPassword = errorPassword,
                            errorGeneral = if (
                                errorCodigo == null &&
                                errorPassword == null
                            ) {
                                error.mensaje
                            } else {
                                null
                            }
                        )
                    }
                }
        }
    }

    // Solicitar otro código si el anterior no llegó.
    fun reenviarCodigo() {

        val actual = _estado.value

        if (actual.reenviando || correo.isBlank()) return

        _estado.update {
            it.copy(
                reenviando = true,
                errorReenvio = null,
                codigoReenviado = false
            )
        }

        viewModelScope.launch {

            repository.solicitarCodigo(correo)
                .onSuccess {
                    _estado.update {
                        it.copy(
                            reenviando = false,
                            codigoReenviado = true
                        )
                    }
                }
                .onFailure { excepcion ->

                    val error = excepcion.aErrorApi()

                    _estado.update {
                        it.copy(
                            reenviando = false,
                            errorReenvio = error.mensaje
                        )
                    }
                }
        }
    }

    companion object {

        val Fabrica = viewModelFactory {

            initializer {

                val app = uasnackApp()

                NuevaPasswordViewModel(
                    createSavedStateHandle(),
                    app.recuperacionPasswordRepository
                )
            }
        }
    }
}