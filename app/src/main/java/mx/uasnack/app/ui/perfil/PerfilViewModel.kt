package mx.uasnack.app.ui.perfil

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
import mx.uasnack.app.data.repositorio.PerfilRepository
import mx.uasnack.app.data.repositorio.SesionRepository
import mx.uasnack.app.domain.modelo.Perfil
import mx.uasnack.app.uasnackApp

data class EstadoPerfilUi(
    val cargando: Boolean = true,
    val perfil: Perfil? = null,
    val error: String? = null,
    val cerrandoSesion: Boolean = false
)

class PerfilViewModel(
    private val perfilRepository: PerfilRepository,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoPerfilUi())
    val estado: StateFlow<EstadoPerfilUi> = _estado.asStateFlow()

    init {
        refrescar()
    }

    fun refrescar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }

            perfilRepository.obtenerPerfil()
                .onSuccess { perfil ->
                    _estado.update {
                        it.copy(
                            cargando = false,
                            perfil = perfil,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _estado.update {
                        it.copy(
                            cargando = false,
                            error = error.aErrorApi().mensaje
                        )
                    }
                }
        }
    }

    fun cerrarSesion(alTerminar: () -> Unit) {
        if (_estado.value.cerrandoSesion) return

        viewModelScope.launch {
            _estado.update { it.copy(cerrandoSesion = true) }
            sesionRepository.cerrarSesion()
            alTerminar()
        }
    }

    companion object {
        val Fabrica = viewModelFactory {
            initializer {
                val app = uasnackApp()
                PerfilViewModel(
                    app.perfilRepository,
                    app.sesionRepository
                )
            }
        }
    }
}
