package mx.uasnack.app.ui.pedido

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.uasnack.app.data.repositorio.CarritoRepository
import mx.uasnack.app.domain.modelo.Carrito
import mx.uasnack.app.uasnackApp

data class EstadoCarritoUi(
    val carrito: Carrito = Carrito(),
    // true solo mientras DataStore lee el carrito por primera vez
    val cargando: Boolean = true
)

// Cosas que pasan una sola vez: no van en el estado para que no se repitan al girar la pantalla
sealed interface EventoCarrito {
    // Se quitó la última línea: regresar al catálogo de esa tienda
    data class RegresarACatalogo(val idTienda: Int) : EventoCarrito
}

// Pantalla 7 · Carrito (#170). Solo local (DataStore), sin red. totalLocal es solo para mostrar.
class CarritoViewModel(private val carritoRepository: CarritoRepository) : ViewModel() {

    val estado: StateFlow<EstadoCarritoUi> = carritoRepository.carrito()
        .map { EstadoCarritoUi(carrito = it, cargando = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoCarritoUi())

    private val _eventos = Channel<EventoCarrito>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    // 0 o menos quita la línea (lo resuelve CarritoRepository)
    fun cambiarCantidad(clave: String, cantidad: Int) = alterar { carritoRepository.cambiarCantidad(clave, cantidad) }

    fun quitar(clave: String) = alterar { carritoRepository.quitar(clave) }

    // Hace el cambio y, si el carrito quedó vacío, avisa para regresar al catálogo de esa tienda
    private fun alterar(cambio: suspend () -> Unit) {
        viewModelScope.launch {
            val idTienda = carritoRepository.carrito().first().idTienda
            cambio()
            if (idTienda != null && carritoRepository.carrito().first().vacio) {
                _eventos.send(EventoCarrito.RegresarACatalogo(idTienda))
            }
        }
    }

    companion object {
        val Fabrica = viewModelFactory {
            initializer { CarritoViewModel(uasnackApp().carritoRepository) }
        }
    }
}
