package mx.uasnack.app.ui.catalogo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.uasnack.app.data.red.aErrorApi
import mx.uasnack.app.data.repositorio.CarritoRepository
import mx.uasnack.app.data.repositorio.CatalogoRepository
import mx.uasnack.app.domain.modelo.Producto
import mx.uasnack.app.ui.navegacion.Rutas
import mx.uasnack.app.uasnackApp

data class EstadoCatalogoUi(
    val nombreTienda: String = "",
    // true solo mientras no hay nada que mostrar (ni caché ni respuesta del servidor)
    val cargando: Boolean = true,
    // Ya filtrados por la categoría activa. Los agotados se incluyen: se atenúan, nunca se ocultan.
    val productos: List<Producto> = emptyList(),
    // Categorías que tiene esta tienda, para los chips
    val categorias: List<Int> = emptyList(),
    // null = "Todo"
    val categoriaActiva: Int? = null,
    val error: String? = null,
    // Para el botón "Ver carrito" (lo usa la pantalla de Carrito, #170)
    val articulosEnCarrito: Int = 0
)

// Catálogo de una tienda (#48). Offline-first, igual que Tiendas.
class CatalogoViewModel(
    estadoGuardado: SavedStateHandle,
    private val catalogoRepository: CatalogoRepository,
    carritoRepository: CarritoRepository
) : ViewModel() {

    // El id llega en la ruta "tiendas/{idTienda}" y sobrevive a la muerte del proceso
    private val idTienda: Int = checkNotNull(estadoGuardado[Rutas.ARG_ID_TIENDA])

    private val categoriaActiva = MutableStateFlow<Int?>(null)
    private val cargando = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    // Un solo StateFlow para la pantalla, armado con todo lo que cambia: productos de Room,
    // categoría elegida, nombre de la tienda, carrito y el estado del refresco.
    // El filtro por categoría se hace aquí (no en el servidor) para que también funcione sin red.
    val estado: StateFlow<EstadoCatalogoUi> = combine(
        catalogoRepository.catalogo(idTienda),
        categoriaActiva,
        catalogoRepository.tiendas().map { lista -> lista.firstOrNull { it.id == idTienda }?.nombre.orEmpty() },
        carritoRepository.carrito().map { it.totalArticulos },
        combine(cargando, error) { estaCargando, mensaje -> estaCargando to mensaje }
    ) { todos, categoria, nombre, articulos, (estaCargando, mensajeError) ->
        EstadoCatalogoUi(
            nombreTienda = nombre,
            cargando = estaCargando && todos.isEmpty(),
            productos = todos.filter { categoria == null || it.idCategoria == categoria },
            categorias = todos.mapNotNull { it.idCategoria }.distinct().sorted(),
            categoriaActiva = categoria,
            error = mensajeError,
            articulosEnCarrito = articulos
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoCatalogoUi())

    init {
        refrescar()
    }

    fun elegirCategoria(idCategoria: Int?) {
        categoriaActiva.value = idCategoria
    }

    // GET /tiendas/{id}/productos. Si falla, se queda lo de la caché y se muestra el mensaje.
    fun refrescar() {
        viewModelScope.launch {
            error.value = null
            catalogoRepository.refrescarCatalogo(idTienda)
                .onFailure { error.value = it.aErrorApi().mensaje }
            cargando.value = false
        }
    }

    companion object {
        val Fabrica = viewModelFactory {
            initializer {
                val app = uasnackApp()
                CatalogoViewModel(createSavedStateHandle(), app.catalogoRepository, app.carritoRepository)
            }
        }
    }
}
