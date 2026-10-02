package mx.uasnack.app.ui.acceso

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

class VerificacionViewModel(private val estadoGuardado: SavedStateHandle) : ViewModel() {

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
            // TODO (#160): usar reenviar_en_segundos de la respuesta del registro en lugar de este valor
            esperarReenvio(SEGUNDOS_REENVIO_INICIAL)
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

    private fun verificar(codigo: String) {
        // TODO (#160 y #161): llamar a SesionRepository.verificar(correo, codigo) con
        // verificando = true mientras responde, y según la respuesta:
        //  - éxito -> guardar la sesión y _eventos.send(EventoVerificacion.CorreoVerificado)
        //  - error -> error = mensaje del servidor y vaciar las casillas (digitos = vacíos)
        // Mientras no exista la capa de red, se da por bueno para probar la navegación.
        _eventos.trySend(EventoVerificacion.CorreoVerificado)
    }

    fun reenviarCodigo() {
        if (!_estado.value.puedeReenviar) return

        // TODO (#160): llamar a POST /auth/reenviar-codigo con reenviando = true mientras responde;
        // en éxito vaciar las casillas y esperarReenvio(reenviar_en_segundos);
        // en error 429 esperarReenvio(Retry-After) y mostrar el mensaje del servidor.
        _estado.update { it.copy(digitos = List(DIGITOS_CODIGO) { "" }, error = null) }
        esperarReenvio(SEGUNDOS_REENVIO_INICIAL)
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

    private companion object {
        const val SEGUNDOS_REENVIO_INICIAL = 60
        const val CLAVE_REENVIO_EN = "reenvio_disponible_en"
    }
}
