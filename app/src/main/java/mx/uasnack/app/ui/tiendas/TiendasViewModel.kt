package mx.uasnack.app.ui.tiendas

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
import mx.uasnack.app.data.repositorio.CatalogoRepository
import mx.uasnack.app.domain.modelo.Tienda
import mx.uasnack.app.uasnackApp

// Todo lo que necesita la pantalla de Tiendas (#48), en un solo objeto
data class EstadoTiendasUi(
    // true solo mientras no hay nada que mostrar (ni caché ni respuesta del servidor)
    val cargando: Boolean = true,
    val tiendas: List<Tienda> = emptyList(),
    // Mensaje del servidor si falló el refresco (por ejemplo, sin conexión)
    val error: String? = null
)

// Offline-first: muestra al instante lo que hay en Room y en paralelo pide lo nuevo al servidor
class TiendasViewModel(private val catalogoRepository: CatalogoRepository) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoTiendasUi())
    val estado: StateFlow<EstadoTiendasUi> = _estado.asStateFlow()

    init {
        // Cada vez que cambia la caché, la lista se actualiza sola
        viewModelScope.launch {
            catalogoRepository.tiendas().collect { lista ->
                _estado.update { it.copy(tiendas = lista, cargando = it.cargando && lista.isEmpty()) }
            }
        }
        refrescar()
    }

    // GET /tiendas. Si falla, se queda lo de la caché y se muestra el mensaje del servidor.
    fun refrescar() {
        viewModelScope.launch {
            _estado.update { it.copy(error = null) }
            catalogoRepository.refrescarTiendas()
                .onFailure { e -> _estado.update { it.copy(error = e.aErrorApi().mensaje) } }
            _estado.update { it.copy(cargando = false) }
        }
    }

    companion object {
        val Fabrica = viewModelFactory {
            initializer { TiendasViewModel(uasnackApp().catalogoRepository) }
        }
    }
}
