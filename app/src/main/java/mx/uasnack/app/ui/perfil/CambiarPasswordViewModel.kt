package mx.uasnack.app.ui.perfil

import androidx.lifecycle.ViewModel
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
import mx.uasnack.app.uasnackApp

data class EstadoCambiarPasswordUi(
    val actual: String = "",
    val nueva: String = "",
    val confirmacion: String = "",

    val cargando: Boolean = false,
    val exitoso: Boolean = false,

    val errorActual: String? = null,
    val errorNueva: String? = null,
    val errorGeneral: String? = null
)

class CambiarPasswordViewModel(
    private val repository: RecuperacionPasswordRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoCambiarPasswordUi())

    val estado: StateFlow<EstadoCambiarPasswordUi> =
        _estado.asStateFlow()

    fun cambiarActual(valor: String) {
        _estado.update {
            it.copy(
                actual = valor,
                errorActual = null,
                errorGeneral = null
            )
        }
    }

    fun cambiarNueva(valor: String) {
        _estado.update {
            it.copy(
                nueva = valor,
                errorNueva = null,
                errorGeneral = null
            )
        }
    }

    fun cambiarConfirmacion(valor: String) {
        _estado.update {
            it.copy(
                confirmacion = valor,
                errorNueva = null,
                errorGeneral = null
            )
        }
    }

    fun cambiarPassword() {

        val actual = _estado.value

        if (actual.cargando || actual.exitoso) return

        if (actual.actual.isBlank() || actual.nueva.isBlank()) return

        _estado.update {
            it.copy(
                cargando = true,
                errorActual = null,
                errorNueva = null,
                errorGeneral = null
            )
        }

        viewModelScope.launch {

            repository.cambiarPassword(
                actual = actual.actual,
                nueva = actual.nueva,
                confirmacion = actual.confirmacion
            )
                .onSuccess {

                    _estado.update {
                        it.copy(
                            cargando = false,
                            exitoso = true
                        )
                    }
                }
                .onFailure { excepcion ->

                    val error = excepcion.aErrorApi()
                    val campos = error.primerErrorPorCampo

                    val errorActual =
                        if (error.codigo == "PASSWORD_ACTUAL_INCORRECTA") {
                            error.mensaje
                        } else {
                            campos["password_actual"]
                        }

                    val errorNueva = campos["password"]

                    val errorGeneral =
                        if (errorActual == null && errorNueva == null) {
                            error.mensaje
                        } else {
                            null
                        }

                    _estado.update {
                        it.copy(
                            cargando = false,
                            errorActual = errorActual,
                            errorNueva = errorNueva,
                            errorGeneral = errorGeneral
                        )
                    }
                }
        }
    }

    companion object {

        val Fabrica = viewModelFactory {

            initializer {

                val app = uasnackApp()

                CambiarPasswordViewModel(
                    app.recuperacionPasswordRepository
                )
            }
        }
    }
}