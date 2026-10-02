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
import mx.uasnack.app.data.red.aErrorApi
import mx.uasnack.app.data.repositorio.SesionRepository
import mx.uasnack.app.uasnackApp
import mx.uasnack.app.ui.navegacion.Rutas

const val DIGITOS_CODIGO = 6

data class EstadoVerificacionUi(
    val correo: String = "",
    val digitos: List<String> = List(DIGITOS_CODIGO) { "" },
    val verificando: Boolean = false,
    val reenviando: Boolean = false,
    val error: String? = null,
    // Cuenta regresiva para poder pedir otro código
    val segundosParaReenvio: Int = 0
) {
    val puedeReenviar: Boolean get() = segundosParaReenvio == 0 && !reenviando
}

sealed interface EventoVerificacion {
    data object CorreoVerificado : EventoVerificacion
}

class VerificacionViewModel(
    private val estadoGuardado: SavedStateHandle,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    // El correo llega en la ruta (verificacion/{correo}); Navigation lo deja en el SavedStateHandle
    private val _estado = MutableStateFlow(
        EstadoVerificacionUi(correo = estadoGuardado.get<String>(Rutas.ARG_CORREO).orEmpty())
    )
    val estado: StateFlow<EstadoVerificacionUi> = _estado.asStateFlow()

    private val _eventos = Channel<EventoVerificacion>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    private var cuentaRegresiva: Job? = null

    init {
        // Se guarda la hora en que se puede reenviar (no los segundos), como la PWA:
        // así la cuenta sigue bien después de girar la pantalla o de que Android cierre la app.
        val reenvioDisponibleEn = estadoGuardado.get<Long>(CLAVE_REENVIO_EN)
        if (reenvioDisponibleEn != null) {
            iniciarCuentaRegresiva(reenvioDisponibleEn)
        } else {
            // Primera vez en la pantalla: los segundos vienen en la ruta. Desde Registro son los que
            // mandó el servidor; desde Login son 0 y se puede pedir un código nuevo de inmediato.
            val segundos = estadoGuardado.get<Int>(Rutas.ARG_REENVIAR_EN) ?: 0
            if (segundos > 0) esperarReenvio(segundos)
        }
    }

    // Escribe uno o varios dígitos a partir de la casilla "indice" (sirve para teclear y para pegar).
    // Devuelve la casilla que debe quedar enfocada, o null si no cambió nada.
    fun escribirDesde(indice: Int, texto: String): Int? {
        val actual = _estado.value
        if (actual.verificando) return null

        val numeros = texto.filter(Char::isDigit).take(DIGITOS_CODIGO - indice)
        if (numeros.isEmpty()) return null

        val nuevos = actual.digitos.toMutableList()
        numeros.forEachIndexed { i, digito -> nuevos[indice + i] = digito.toString() }
        // Al escribir de nuevo se quita el error anterior
        _estado.update { it.copy(digitos = nuevos, error = null) }

        // Con las seis casillas llenas se verifica solo, igual que la PWA
        if (nuevos.all { it.isNotEmpty() }) verificar(nuevos.joinToString(""))

        return minOf(indice + numeros.length, DIGITOS_CODIGO - 1)
    }

    fun borrar(indice: Int) {
        if (_estado.value.verificando) return
        _estado.update { estado ->
            estado.copy(digitos = estado.digitos.toMutableList().also { it[indice] = "" })
        }
    }

    // POST /auth/verificar. Si el código es correcto, el repositorio guarda la sesión y se entra a la app.
    private fun verificar(codigo: String) {
        _estado.update { it.copy(verificando = true, error = null) }

        viewModelScope.launch {
            sesionRepository.verificar(_estado.value.correo, codigo)
                .onSuccess {
                    _estado.update { it.copy(verificando = false) }
                    _eventos.send(EventoVerificacion.CorreoVerificado)
                }
                .onFailure {
                    // Código incorrecto, vencido, demasiados intentos o sin conexión:
                    // se muestra el mensaje del servidor y se vacían las casillas para escribir otra vez
                    val error = it.aErrorApi()
                    _estado.update { estado ->
                        estado.copy(verificando = false, error = error.mensaje, digitos = List(DIGITOS_CODIGO) { "" })
                    }
                }
        }
    }

    // POST /auth/reenviar-codigo
    fun reenviarCodigo() {
        if (!_estado.value.puedeReenviar) return

        _estado.update { it.copy(reenviando = true, error = null) }

        viewModelScope.launch {
            sesionRepository.reenviarCodigo(_estado.value.correo)
                .onSuccess { segundos ->
                    // El código anterior ya no sirve: se vacían las casillas y empieza otra espera
                    _estado.update { it.copy(reenviando = false, digitos = List(DIGITOS_CODIGO) { "" }) }
                    esperarReenvio(segundos)
                }
                .onFailure {
                    val error = it.aErrorApi()
                    _estado.update { estado -> estado.copy(reenviando = false, error = error.mensaje) }
                    // Si se pidió antes de tiempo (429), el servidor dice cuánto falta en Retry-After
                    error.reintentarEn?.let { segundos -> esperarReenvio(segundos) }
                }
        }
    }

    private fun esperarReenvio(segundos: Int) {
        val fin = System.currentTimeMillis() + segundos * 1000L
        estadoGuardado[CLAVE_REENVIO_EN] = fin
        iniciarCuentaRegresiva(fin)
    }

    // Baja los segundos cada segundo hasta llegar a cero. Vive en viewModelScope,
    // así que no se reinicia al girar la pantalla y se cancela sola al salir de ella.
    private fun iniciarCuentaRegresiva(fin: Long) {
        cuentaRegresiva?.cancel()
        cuentaRegresiva = viewModelScope.launch {
            while (true) {
                val restantes = ((fin - System.currentTimeMillis() + 999) / 1000).toInt().coerceAtLeast(0)
                _estado.update { it.copy(segundosParaReenvio = restantes) }
                if (restantes == 0) break
                delay(1000)
            }
        }
    }

    companion object {
        private const val CLAVE_REENVIO_EN = "reenvio_disponible_en"

        // Cómo se crea el ViewModel: con su SavedStateHandle (trae el correo de la ruta)
        // y el repositorio que arma UasnackApp
        val Fabrica = viewModelFactory {
            initializer { VerificacionViewModel(createSavedStateHandle(), uasnackApp().sesionRepository) }
        }
    }
}
