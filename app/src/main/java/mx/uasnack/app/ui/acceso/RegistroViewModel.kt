package mx.uasnack.app.ui.acceso

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import mx.uasnack.app.domain.modelo.Facultad

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
    data class CuentaCreada(val correo: String) : EventoRegistro
}

class RegistroViewModel(private val estadoGuardado: SavedStateHandle) : ViewModel() {

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

    fun cargarFacultades() {
        // TODO (#160): pedir GET /facultades y pasar a Listas(...) o a Error si falla.
        // Mientras no exista la capa de red se usa la facultad del seeder del backend.
        _estado.update {
            it.copy(facultades = EstadoFacultades.Listas(listOf(Facultad(1, "Facultad de Informática Culiacán"))))
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

        // TODO (#160 y #161): llamar a POST /auth/registro con nombre, apellido,
        // correo_institucional, id_facultad, password, telefono y acepto_terminos = true,
        // con cargando = true mientras responde, y según la respuesta:
        //  - 201         -> _eventos.send(EventoRegistro.CuentaCreada(correo))
        //  - 422         -> errores = primer mensaje de cada campo de "campos"
        //  - otro error  -> errorGeneral = mensaje del servidor
        // Mientras no exista la capa de red, se pasa directo a Verificación para probar la navegación.
        _eventos.trySend(EventoRegistro.CuentaCreada(actual.correo.trim().lowercase()))
    }

    private companion object {
        const val TELEFONO_DIGITOS = 10
        const val CLAVE_NOMBRE = "nombre"
        const val CLAVE_APELLIDO = "apellido"
        const val CLAVE_CORREO = "correo"
        const val CLAVE_FACULTAD = "id_facultad"
        const val CLAVE_TELEFONO = "telefono"
        const val CLAVE_TERMINOS = "acepta_terminos"
    }
}
