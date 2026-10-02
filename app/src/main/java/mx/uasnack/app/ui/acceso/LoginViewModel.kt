package mx.uasnack.app.ui.acceso

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.uasnack.app.data.red.ErrorApi
import mx.uasnack.app.data.red.aErrorApi
import mx.uasnack.app.data.repositorio.SesionRepository
import mx.uasnack.app.domain.modelo.Rol
import mx.uasnack.app.uasnackApp

// Todo lo que la pantalla de Acceso necesita para dibujarse, en un solo objeto.
data class EstadoLoginUi(
    val correo: String = "",
    val password: String = "",
    val verPassword: Boolean = false,
    val cargando: Boolean = false,
    // Errores que manda el servidor: por campo (422) y general (401, sin conexión...)
    val errorCorreo: String? = null,
    val errorPassword: String? = null,
    val errorGeneral: String? = null,
    // Segundos que faltan para poder reintentar tras demasiados intentos (429, header Retry-After)
    val segundosBloqueo: Int = 0
) {
    val bloqueado: Boolean get() = segundosBloqueo > 0
}

// Cosas que pasan una sola vez (navegar). No van en el estado para que no se repitan al girar la pantalla.
sealed interface EventoLogin {
    data object SesionIniciada : EventoLogin
    data class CorreoSinVerificar(val correo: String) : EventoLogin
}

class LoginViewModel(
    private val estadoGuardado: SavedStateHandle,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    // El correo se guarda en SavedStateHandle para recuperarlo si Android cierra la app.
    // La contraseña no se guarda a propósito.
    private val _estado = MutableStateFlow(EstadoLoginUi(correo = estadoGuardado[CLAVE_CORREO] ?: ""))
    val estado: StateFlow<EstadoLoginUi> = _estado.asStateFlow()

    private val _eventos = Channel<EventoLogin>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    private var cuentaBloqueo: Job? = null

    fun cambiarCorreo(valor: String) {
        estadoGuardado[CLAVE_CORREO] = valor
        // Al escribir se borra el error de ese campo: ya no corresponde a lo que hay escrito
        _estado.update { it.copy(correo = valor, errorCorreo = null) }
    }

    fun cambiarPassword(valor: String) {
        _estado.update { it.copy(password = valor, errorPassword = null) }
    }

    fun alternarVerPassword() {
        _estado.update { it.copy(verPassword = !it.verPassword) }
    }

    fun entrar() {
        val actual = _estado.value
        // Evita el doble envío y respeta el bloqueo del 429
        if (actual.cargando || actual.bloqueado) return

        val correo = actual.correo.trim()
        // El botón cambia a "Entrando..." de inmediato, antes de que responda el servidor
        _estado.update { it.copy(cargando = true, errorCorreo = null, errorPassword = null, errorGeneral = null) }

        viewModelScope.launch {
            sesionRepository.login(correo, actual.password)
                .onSuccess { usuario ->
                    if (usuario.rol == Rol.USUARIO) {
                        _estado.update { it.copy(cargando = false) }
                        _eventos.send(EventoLogin.SesionIniciada)
                    } else {
                        // La app es solo para estudiantes: el encargado y el administrador usan el panel web.
                        // El login ya guardó el token, así que se cierra la sesión (también en el servidor).
                        sesionRepository.cerrarSesion()
                        _estado.update { it.copy(cargando = false, errorGeneral = MENSAJE_PANEL_WEB) }
                    }
                }
                .onFailure { mostrarError(it.aErrorApi(), correo) }
        }
    }

    // Decide qué hacer con cada error. Los textos son siempre los del servidor.
    private suspend fun mostrarError(error: ErrorApi, correo: String) {
        _estado.update { it.copy(cargando = false) }

        when (error.codigo) {
            // 403: la cuenta existe pero falta verificar el correo -> se manda a Verificación
            ErrorApi.CORREO_NO_VERIFICADO -> _eventos.send(EventoLogin.CorreoSinVerificar(correo))

            // 422: errores debajo de cada campo. Si no trae campos conocidos, se muestra el general.
            ErrorApi.VALIDACION -> {
                val porCampo = error.primerErrorPorCampo
                val errorCorreo = porCampo["correo_institucional"]
                val errorPassword = porCampo["password"]
                _estado.update {
                    it.copy(
                        errorCorreo = errorCorreo,
                        errorPassword = errorPassword,
                        errorGeneral = if (errorCorreo == null && errorPassword == null) error.mensaje else null
                    )
                }
            }

            // 429: mensaje + cuenta regresiva con los segundos del header Retry-After
            ErrorApi.DEMASIADOS_INTENTOS -> {
                _estado.update { it.copy(errorGeneral = error.mensaje) }
                iniciarBloqueo(error.reintentarEn ?: SEGUNDOS_BLOQUEO_SIN_HEADER)
            }

            // 401 (correo o contraseña incorrectos), sin conexión y cualquier otro
            else -> _estado.update { it.copy(errorGeneral = error.mensaje) }
        }
    }

    // Baja segundosBloqueo de uno en uno hasta cero; mientras tanto el botón Entrar queda deshabilitado
    private fun iniciarBloqueo(segundos: Int) {
        cuentaBloqueo?.cancel()
        cuentaBloqueo = viewModelScope.launch {
            for (restantes in segundos downTo 0) {
                _estado.update { it.copy(segundosBloqueo = restantes) }
                if (restantes > 0) delay(1000)
            }
            // Al terminar la espera se quita el mensaje del bloqueo
            _estado.update { it.copy(errorGeneral = null) }
        }
    }

    companion object {
        private const val CLAVE_CORREO = "correo"

        // Si el 429 no trajera Retry-After (no debería), se espera lo mismo que el throttle del login
        private const val SEGUNDOS_BLOQUEO_SIN_HEADER = 60

        const val MENSAJE_PANEL_WEB = "Esta app es para estudiantes. Usa el panel web desde tu computadora."

        // Cómo se crea el ViewModel: con su SavedStateHandle y el repositorio que arma UasnackApp
        val Fabrica = viewModelFactory {
            initializer { LoginViewModel(createSavedStateHandle(), uasnackApp().sesionRepository) }
        }
    }
}
