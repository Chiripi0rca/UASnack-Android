package mx.uasnack.app.ui.acceso

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
import mx.uasnack.app.data.repositorio.SesionRepository
import mx.uasnack.app.domain.modelo.Facultad
import mx.uasnack.app.uasnackApp

// Estado de la lista de facultades, para mostrar "Cargando…", "No se pudieron cargar" o la lista
sealed interface EstadoFacultades {
    data object Cargando : EstadoFacultades
    data object Error : EstadoFacultades
    data class Listas(val facultades: List<Facultad>) : EstadoFacultades
}

data class EstadoRegistroUi(
    val nombre: String = "",
    val apellido: String = "",
    val correo: String = "",
    // Se guarda el id porque es lo que pide POST /auth/registro (id_facultad)
    val idFacultad: Int? = null,
    val facultades: EstadoFacultades = EstadoFacultades.Cargando,
    val password: String = "",
    val verPassword: Boolean = false,
    val telefono: String = "",
    val aceptaTerminos: Boolean = false,
    val cargando: Boolean = false,
    // Errores del servidor (422): la llave es el nombre del campo en la API ("correo_institucional"...)
    val errores: Map<String, String> = emptyMap(),
    val errorGeneral: String? = null
) {
    // El botón solo se habilita con la casilla marcada (§9 del documento de Android, igual que la PWA)
    val botonHabilitado: Boolean get() = aceptaTerminos && !cargando

    // Nombre de la facultad elegida, para mostrarlo en el campo
    val nombreFacultad: String
        get() = (facultades as? EstadoFacultades.Listas)?.facultades
            ?.firstOrNull { it.id == idFacultad }?.nombre ?: ""
}

sealed interface EventoRegistro {
    // reenviarEnSegundos: lo que dijo el servidor que hay que esperar para reenviar el código
    data class CuentaCreada(val correo: String, val reenviarEnSegundos: Int) : EventoRegistro
}

class RegistroViewModel(
    private val estadoGuardado: SavedStateHandle,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    // Se recuperan del SavedStateHandle los datos que no son sensibles (la contraseña no se guarda)
    private val _estado = MutableStateFlow(
        EstadoRegistroUi(
            nombre = estadoGuardado[CLAVE_NOMBRE] ?: "",
            apellido = estadoGuardado[CLAVE_APELLIDO] ?: "",
            correo = estadoGuardado[CLAVE_CORREO] ?: "",
            idFacultad = estadoGuardado[CLAVE_FACULTAD],
            telefono = estadoGuardado[CLAVE_TELEFONO] ?: "",
            aceptaTerminos = estadoGuardado[CLAVE_TERMINOS] ?: false
        )
    )
    val estado: StateFlow<EstadoRegistroUi> = _estado.asStateFlow()

    private val _eventos = Channel<EventoRegistro>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    init {
        cargarFacultades()
    }

    // GET /facultades. Mientras carga el campo dice "Cargando facultades…"; si falla aparece "Reintentar".
    fun cargarFacultades() {
        _estado.update { it.copy(facultades = EstadoFacultades.Cargando) }
        viewModelScope.launch {
            sesionRepository.facultades()
                .onSuccess { lista -> _estado.update { it.copy(facultades = EstadoFacultades.Listas(lista)) } }
                .onFailure { _estado.update { it.copy(facultades = EstadoFacultades.Error) } }
        }
    }

    fun cambiarNombre(valor: String) {
        estadoGuardado[CLAVE_NOMBRE] = valor
        _estado.update { it.copy(nombre = valor, errores = it.errores - "nombre") }
    }

    fun cambiarApellido(valor: String) {
        estadoGuardado[CLAVE_APELLIDO] = valor
        _estado.update { it.copy(apellido = valor, errores = it.errores - "apellido") }
    }

    fun cambiarCorreo(valor: String) {
        estadoGuardado[CLAVE_CORREO] = valor
        _estado.update { it.copy(correo = valor, errores = it.errores - "correo_institucional") }
    }

    fun elegirFacultad(id: Int) {
        estadoGuardado[CLAVE_FACULTAD] = id
        _estado.update { it.copy(idFacultad = id, errores = it.errores - "id_facultad") }
    }

    fun cambiarPassword(valor: String) {
        _estado.update { it.copy(password = valor, errores = it.errores - "password") }
    }

    fun alternarVerPassword() {
        _estado.update { it.copy(verPassword = !it.verPassword) }
    }

    fun cambiarTelefono(valor: String) {
        // Solo se aceptan dígitos y hasta 10, igual que soloNumero() en la PWA.
        // Esto no valida: si faltan dígitos, el error lo da el servidor.
        val limpio = valor.filter(Char::isDigit).take(TELEFONO_DIGITOS)
        estadoGuardado[CLAVE_TELEFONO] = limpio
        _estado.update { it.copy(telefono = limpio, errores = it.errores - "telefono") }
    }

    fun cambiarAceptaTerminos(valor: Boolean) {
        estadoGuardado[CLAVE_TERMINOS] = valor
        _estado.update { it.copy(aceptaTerminos = valor) }
    }

    fun crearCuenta() {
        val actual = _estado.value
        if (!actual.botonHabilitado) return

        val correo = actual.correo.trim().lowercase()
        // El botón cambia a "Creando cuenta…" de inmediato y se quitan los errores anteriores
        _estado.update { it.copy(cargando = true, errores = emptyMap(), errorGeneral = null) }

        viewModelScope.launch {
            sesionRepository.registro(
                nombre = actual.nombre.trim(),
                apellido = actual.apellido.trim(),
                correo = correo,
                idFacultad = actual.idFacultad,
                password = actual.password,
                telefono = actual.telefono
            )
                .onSuccess { reenviarEnSegundos ->
                    // 201: la cuenta se creó y el servidor ya mandó el código al correo
                    _estado.update { it.copy(cargando = false) }
                    _eventos.send(EventoRegistro.CuentaCreada(correo, reenviarEnSegundos))
                }
                .onFailure {
                    val error = it.aErrorApi()
                    // 422: el primer mensaje de cada campo va debajo de su input (correo ya registrado,
                    // dominio no institucional...). El general solo se muestra si no hay errores por campo,
                    // igual que la PWA.
                    val errores = error.primerErrorPorCampo
                    _estado.update { estado ->
                        estado.copy(
                            cargando = false,
                            errores = errores,
                            errorGeneral = if (errores.isEmpty()) error.mensaje else null
                        )
                    }
                }
        }
    }

    companion object {
        private const val TELEFONO_DIGITOS = 10
        private const val CLAVE_NOMBRE = "nombre"
        private const val CLAVE_APELLIDO = "apellido"
        private const val CLAVE_CORREO = "correo"
        private const val CLAVE_FACULTAD = "id_facultad"
        private const val CLAVE_TELEFONO = "telefono"
        private const val CLAVE_TERMINOS = "acepta_terminos"

        // Cómo se crea el ViewModel: con su SavedStateHandle y el repositorio que arma UasnackApp
        val Fabrica = viewModelFactory {
            initializer { RegistroViewModel(createSavedStateHandle(), uasnackApp().sesionRepository) }
        }
    }
}
