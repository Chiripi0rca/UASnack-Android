package mx.uasnack.app.ui.pedido

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.uasnack.app.data.red.ApiService
import mx.uasnack.app.data.red.dto.CrearPedidoRequest
import mx.uasnack.app.data.red.dto.PedidoCreadoDto
import java.util.UUID

data class CobroUiState(
    val procesando: Boolean = false,
    val pedidoCreado: PedidoCreadoDto? = null,
    val error: String? = null
)

class CobroViewModel(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CobroUiState())
    val uiState: StateFlow<CobroUiState> = _uiState.asStateFlow()

    // Llave única vinculada al intento de pedido/cobro actual
    private var llaveIdempotenciaActual: String? = null

    fun confirmarCobro(request: CrearPedidoRequest) {
        // 1. Evita doble toque / ejecuciones simultáneas
        if (_uiState.value.procesando) return

        // 2. Conserva la misma llave si es un reintento del mismo pedido
        val idempotencyKey = llaveIdempotenciaActual ?: UUID.randomUUID().toString().also {
            llaveIdempotenciaActual = it
        }

        _uiState.update { it.copy(procesando = true, error = null) }

        viewModelScope.launch {
            try {
                val respuesta = apiService.crearPedido(
                    body = request,
                    idempotencyKey = idempotencyKey
                )
                // Éxito: limpiamos la llave para futuros pedidos nuevos
                llaveIdempotenciaActual = null
                _uiState.update {
                    it.copy(
                        procesando = false,
                        pedidoCreado = respuesta.datos // Si el campo en ObjetoDto tiene otro nombre (ej. data), ajústalo aquí
                    )
                }
            } catch (e: Exception) {
                // Falla de red/timeout: conservamos llaveIdempotenciaActual para el reintento
                _uiState.update {
                    it.copy(
                        procesando = false,
                        error = e.message ?: "Error de conexión al procesar el cobro"
                    )
                }
            }
        }
    }

    fun reiniciarIntento() {
        llaveIdempotenciaActual = null
        _uiState.value = CobroUiState()
    }
}