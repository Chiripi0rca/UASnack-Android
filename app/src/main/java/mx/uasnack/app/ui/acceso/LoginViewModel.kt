package mx.uasnack.app.ui.acceso

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

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

class LoginViewModel(private val estadoGuardado: SavedStateHandle) : ViewModel() {

    // El correo se guarda en SavedStateHandle para recuperarlo si Android cierra la app.
    // La contraseña no se guarda a propósito.
    private val _estado = MutableStateFlow(EstadoLoginUi(correo = estadoGuardado[CLAVE_CORREO] ?: ""))
    val estado: StateFlow<EstadoLoginUi> = _estado.asStateFlow()

    private val _eventos = Channel<EventoLogin>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

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

        // TODO (#160 y #161): llamar a SesionRepository.login(correo, password) dentro de
        // viewModelScope.launch, con cargando = true mientras responde, y según la respuesta:
        //  - éxito                      -> _eventos.send(EventoLogin.SesionIniciada)
        //  - 401 CREDENCIALES_INVALIDAS -> errorGeneral = mensaje del servidor
        //  - 403 CORREO_NO_VERIFICADO   -> _eventos.send(EventoLogin.CorreoSinVerificar(correo))
        //  - 422 VALIDACION             -> errorCorreo / errorPassword con el primer mensaje de cada campo
        //  - 429 DEMASIADOS_INTENTOS    -> errorGeneral + cuenta regresiva en segundosBloqueo (Retry-After)
        //  - rol distinto de "usuario"  -> no guardar sesión, errorGeneral = "Usa el panel web desde tu computadora"
        //  - sin conexión               -> errorGeneral = mensaje de sin conexión
        // Mientras no exista la capa de red, se entra directo para poder probar la navegación.
        _eventos.trySend(EventoLogin.SesionIniciada)
    }

    private companion object {
        const val CLAVE_CORREO = "correo"
    }
}
