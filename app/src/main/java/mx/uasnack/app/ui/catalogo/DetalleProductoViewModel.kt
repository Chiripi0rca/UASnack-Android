package mx.uasnack.app.ui.catalogo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
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
import mx.uasnack.app.data.repositorio.CarritoRepository
import mx.uasnack.app.data.repositorio.CatalogoRepository
import mx.uasnack.app.domain.modelo.Producto
import mx.uasnack.app.domain.modelo.ResultadoAgregar
import mx.uasnack.app.ui.navegacion.Rutas
import mx.uasnack.app.uasnackApp
import java.math.BigDecimal

data class EstadoDetalleUi(
    val producto: Producto? = null,
    val cargando: Boolean = true,
    // Mensaje del servidor si falló el refresco del detalle
    val error: String? = null,
    // Ids de las opciones elegidas
    val extrasSeleccionados: Set<String> = emptySet(),
    val quitablesSeleccionados: Set<String> = emptySet(),
    val cantidad: Int = 1,
    // Unidades de este producto que ya están en el carrito (con y sin extras es el mismo stock)
    val yaEnCarrito: Int = 0,
    // Aviso de SinExistencias
    val aviso: String? = null,
    // true = mostrar el diálogo "tu carrito es de otra tienda, ¿vaciarlo?" (OtraTienda)
    val preguntarVaciar: Boolean = false
) {
    // Cuántas unidades más se pueden agregar
    val disponibles: Int get() = ((producto?.stockHoy ?: 0) - yaEnCarrito).coerceAtLeast(0)

    val puedeAgregar: Boolean get() = producto != null && !producto.agotado && disponibles > 0 && cantidad <= disponibles

    // Importe del botón: (precio + extras elegidos) × cantidad. SOLO para mostrar; cobra el servidor (#49).
    val importe: BigDecimal
        get() {
            val p = producto ?: return BigDecimal.ZERO
            val extras = p.extras.filter { it.id in extrasSeleccionados }
                .fold(BigDecimal.ZERO) { suma, extra -> suma + (extra.precio ?: BigDecimal.ZERO) }
            return (p.precio + extras) * cantidad.toBigDecimal()
        }
}

// Cosas que pasan una sola vez: no van en el estado para que no se repitan al girar la pantalla
sealed interface EventoDetalle {
    data object Agregado : EventoDetalle
}

// Pantalla 6 · Detalle de producto (#51). Mismo flujo que la PWA (#50).
class DetalleProductoViewModel(
    estadoGuardado: SavedStateHandle,
    private val catalogoRepository: CatalogoRepository,
    private val carritoRepository: CarritoRepository
) : ViewModel() {

    // El id llega en la ruta "productos/{idProducto}" y sobrevive a la muerte del proceso
    private val idProducto: Int = checkNotNull(estadoGuardado[Rutas.ARG_ID_PRODUCTO])

    private val _estado = MutableStateFlow(EstadoDetalleUi())
    val estado: StateFlow<EstadoDetalleUi> = _estado.asStateFlow()

    private val _eventos = Channel<EventoDetalle>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    init {
        // Lo de la caché al instante...
        viewModelScope.launch {
            catalogoRepository.producto(idProducto).collect { producto ->
                _estado.update { it.copy(producto = producto, cargando = it.cargando && producto == null) }
            }
        }
        // ...cuántas unidades ya hay en el carrito, para el tope de existencias...
        viewModelScope.launch {
            carritoRepository.carrito().collect { carrito ->
                val unidades = carrito.lineas.filter { it.idProducto == idProducto }.sumOf { it.cantidad }
                _estado.update { it.copy(yaEnCarrito = unidades) }
            }
        }
        // ...y en paralelo el detalle al día (extras y quitables) desde GET /productos/{id}
        viewModelScope.launch {
            catalogoRepository.refrescarProducto(idProducto)
                .onFailure { e -> _estado.update { it.copy(error = e.aErrorApi().mensaje) } }
            _estado.update { it.copy(cargando = false) }
        }
    }

    fun alternarExtra(id: String) = _estado.update {
        val elegidos = if (id in it.extrasSeleccionados) it.extrasSeleccionados - id else it.extrasSeleccionados + id
        it.copy(extrasSeleccionados = elegidos)
    }

    fun alternarQuitable(id: String) = _estado.update {
        val elegidos = if (id in it.quitablesSeleccionados) it.quitablesSeleccionados - id else it.quitablesSeleccionados + id
        it.copy(quitablesSeleccionados = elegidos)
    }

    // Entre 1 y las unidades disponibles
    fun cambiarCantidad(cantidad: Int) = _estado.update {
        it.copy(cantidad = cantidad.coerceIn(1, it.disponibles.coerceAtLeast(1)), aviso = null)
    }

    fun agregar() = intentarAgregar(vaciarAntes = false)

    // El alumno aceptó vaciar el carrito de la otra tienda
    fun confirmarVaciar() {
        _estado.update { it.copy(preguntarVaciar = false) }
        intentarAgregar(vaciarAntes = true)
    }

    fun cancelarVaciar() = _estado.update { it.copy(preguntarVaciar = false) }

    private fun intentarAgregar(vaciarAntes: Boolean) {
        val actual = _estado.value
        val producto = actual.producto ?: return
        val extras = producto.extras.filter { it.id in actual.extrasSeleccionados }
        val quitables = producto.quitables.filter { it.id in actual.quitablesSeleccionados }

        viewModelScope.launch {
            val resultado = if (vaciarAntes) {
                carritoRepository.vaciarYAgregar(producto, actual.cantidad, extras, quitables)
            } else {
                carritoRepository.agregar(producto, actual.cantidad, extras, quitables)
            }
            // Los tres resultados de agregar (#231)
            when (resultado) {
                ResultadoAgregar.Agregado -> _eventos.send(EventoDetalle.Agregado)
                is ResultadoAgregar.OtraTienda -> _estado.update { it.copy(preguntarVaciar = true) }
                is ResultadoAgregar.SinExistencias -> _estado.update {
                    it.copy(
                        aviso = if (resultado.disponibles == 0) {
                            "Ya tienes en el carrito todas las unidades disponibles."
                        } else {
                            "Solo puedes agregar ${resultado.disponibles} más."
                        }
                    )
                }
            }
        }
    }

    companion object {
        val Fabrica = viewModelFactory {
            initializer {
                val app = uasnackApp()
                DetalleProductoViewModel(createSavedStateHandle(), app.catalogoRepository, app.carritoRepository)
            }
        }
    }
}
