package mx.uasnack.app.ui.pedido

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mx.uasnack.app.data.red.aErrorApi
import mx.uasnack.app.data.repositorio.CarritoRepository
import mx.uasnack.app.data.repositorio.CatalogoRepository
import mx.uasnack.app.data.repositorio.PedidoRepository
import mx.uasnack.app.domain.modelo.Estimado
import mx.uasnack.app.domain.modelo.Franja
import mx.uasnack.app.ui.navegacion.Rutas
import mx.uasnack.app.uasnackApp

// Cómo quiere recoger el alumno: apartando una franja o "voy saliendo" (§7.1 y §7.3)
enum class ModoRetiro { FRANJA, VOY_SALIENDO }

data class EstadoEleccionRetiroUi(
    val cargando: Boolean = true,
    val modo: ModoRetiro = ModoRetiro.FRANJA,
    // Minutos de cocina del carrito: lo que se le manda al servidor para saber dónde cabe
    val minutos: Int = 0,
    val franjas: List<Franja> = emptyList(),
    val franjaElegida: Franja? = null,
    val espera: Estimado? = null,
    // Mensaje del servidor si falló algo (por ejemplo, sin conexión)
    val error: String? = null
)

// Pantalla 8 · Elección de retiro (#55). Franjas (#173) y espera (#175) siempre de la red, sin caché.
class EleccionRetiroViewModel(
    estadoGuardado: SavedStateHandle,
    private val pedidoRepository: PedidoRepository,
    private val carritoRepository: CarritoRepository,
    private val catalogoRepository: CatalogoRepository
) : ViewModel() {

    private val idTienda: Int = checkNotNull(estadoGuardado[Rutas.ARG_ID_TIENDA])

    private val _estado = MutableStateFlow(EstadoEleccionRetiroUi())
    val estado: StateFlow<EstadoEleccionRetiroUi> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            _estado.update { it.copy(minutos = minutosDelCarrito()) }
            refrescar()
            // La cola de la cocina cambia minuto a minuto (§7.4): la espera se vuelve a pedir sola
            while (isActive) {
                delay(REFRESCO_ESPERA_MS)
                refrescarEspera()
            }
        }
    }

    fun elegirModo(modo: ModoRetiro) = _estado.update { it.copy(modo = modo) }

    // Una franja llena no se puede elegir (la pantalla ya la muestra deshabilitada)
    fun elegirFranja(franja: Franja) {
        if (franja.disponible) _estado.update { it.copy(franjaElegida = franja) }
    }

    // Pide franjas y espera al mismo tiempo
    fun refrescar() {
        viewModelScope.launch {
            _estado.update { it.copy(error = null) }
            val minutos = _estado.value.minutos
            val franjas = async { pedidoRepository.franjas(idTienda, minutos) }
            val espera = async { pedidoRepository.espera(idTienda, minutos) }

            franjas.await()
                .onSuccess { lista ->
                    _estado.update { estado ->
                        // Si la franja elegida ya no está o se llenó, se quita la selección
                        val sigue = lista.firstOrNull { it.inicio == estado.franjaElegida?.inicio && it.disponible }
                        estado.copy(franjas = lista, franjaElegida = sigue)
                    }
                }
                .onFailure { e -> _estado.update { it.copy(error = e.aErrorApi().mensaje) } }
            espera.await()
                .onSuccess { estimado -> _estado.update { it.copy(espera = estimado) } }
                .onFailure { e -> _estado.update { it.copy(error = e.aErrorApi().mensaje) } }

            _estado.update { it.copy(cargando = false) }
        }
    }

    private suspend fun refrescarEspera() {
        pedidoRepository.espera(idTienda, _estado.value.minutos)
            .onSuccess { estimado -> _estado.update { it.copy(espera = estimado) } }
    }

    // Σ tiempo de preparación × cantidad de cada línea. El tiempo sale de la caché del catálogo
    // (el producto ya se vio para poder agregarlo). Lo que no se cocina (#237) suma 0.
    private suspend fun minutosDelCarrito(): Int =
        carritoRepository.carrito().first().lineas.sumOf { linea ->
            val tiempo = catalogoRepository.producto(linea.idProducto).first()?.tiempoPrepMin ?: 0
            tiempo * linea.cantidad
        }

    companion object {
        private const val REFRESCO_ESPERA_MS = 30_000L

        val Fabrica = viewModelFactory {
            initializer {
                val app = uasnackApp()
                EleccionRetiroViewModel(
                    createSavedStateHandle(),
                    app.pedidoRepository,
                    app.carritoRepository,
                    app.catalogoRepository
                )
            }
        }
    }
}
