package mx.uasnack.app.ui.acceso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.uasnack.app.domain.modelo.Facultad
import mx.uasnack.app.ui.componentes.BotonPrincipal
import mx.uasnack.app.ui.componentes.BotonVerPassword
import mx.uasnack.app.ui.componentes.CampoTexto
import mx.uasnack.app.ui.tema.TemaUASnack

// Pantalla 2 · Registro. Conecta el ViewModel con la navegación; lo visual está en RegistroContenido.
@Composable
fun RegistroPantalla(
    alCrearCuenta: (correo: String) -> Unit,
    alIrALogin: () -> Unit,
    viewModel: RegistroViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                is EventoRegistro.CuentaCreada -> alCrearCuenta(evento.correo)
            }
        }
    }

    RegistroContenido(
        estado = estado,
        acciones = AccionesRegistro(
            alCambiarNombre = viewModel::cambiarNombre,
            alCambiarApellido = viewModel::cambiarApellido,
            alCambiarCorreo = viewModel::cambiarCorreo,
            alElegirFacultad = viewModel::elegirFacultad,
            alReintentarFacultades = viewModel::cargarFacultades,
            alCambiarPassword = viewModel::cambiarPassword,
            alAlternarVerPassword = viewModel::alternarVerPassword,
            alCambiarTelefono = viewModel::cambiarTelefono,
            alCambiarAceptaTerminos = viewModel::cambiarAceptaTerminos,
            alCrearCuenta = viewModel::crearCuenta,
            alIrALogin = alIrALogin
        )
    )
}

// Agrupa los eventos de la pantalla para no pasar once parámetros sueltos
private data class AccionesRegistro(
    val alCambiarNombre: (String) -> Unit = {},
    val alCambiarApellido: (String) -> Unit = {},
    val alCambiarCorreo: (String) -> Unit = {},
    val alElegirFacultad: (Int) -> Unit = {},
    val alReintentarFacultades: () -> Unit = {},
    val alCambiarPassword: (String) -> Unit = {},
    val alAlternarVerPassword: () -> Unit = {},
    val alCambiarTelefono: (String) -> Unit = {},
    val alCambiarAceptaTerminos: (Boolean) -> Unit = {},
    val alCrearCuenta: () -> Unit = {},
    val alIrALogin: () -> Unit = {}
)

@Composable
private fun RegistroContenido(estado: EstadoRegistroUi, acciones: AccionesRegistro) {
    // Opciones de teclado que se repiten: texto normal y "siguiente" para pasar al otro campo
    val tecladoNombre = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Deja libre la barra de estado, la de navegación y el teclado
            .safeDrawingPadding()
            // Con el teclado abierto los campos no caben: la pantalla debe poder desplazarse
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = "Crea tu cuenta", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        CampoTexto(
            valor = estado.nombre,
            alCambiar = acciones.alCambiarNombre,
            etiqueta = "Nombre",
            error = estado.errores["nombre"],
            opcionesTeclado = tecladoNombre
        )

        CampoTexto(
            valor = estado.apellido,
            alCambiar = acciones.alCambiarApellido,
            etiqueta = "Apellido",
            error = estado.errores["apellido"],
            opcionesTeclado = tecladoNombre
        )

        CampoTexto(
            valor = estado.correo,
            alCambiar = acciones.alCambiarCorreo,
            etiqueta = "Correo institucional",
            error = estado.errores["correo_institucional"],
            opcionesTeclado = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next
            )
        )

        SelectorFacultad(
            estado = estado,
            alElegir = acciones.alElegirFacultad,
            alReintentar = acciones.alReintentarFacultades
        )

        CampoTexto(
            valor = estado.password,
            alCambiar = acciones.alCambiarPassword,
            etiqueta = "Contraseña",
            error = estado.errores["password"],
            opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            transformacion = if (estado.verPassword) VisualTransformation.None else PasswordVisualTransformation(),
            iconoFinal = { BotonVerPassword(visible = estado.verPassword, alPulsar = acciones.alAlternarVerPassword) }
        )

        CampoTexto(
            valor = estado.telefono,
            alCambiar = acciones.alCambiarTelefono,
            etiqueta = "Teléfono",
            error = estado.errores["telefono"],
            opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done)
        )

        // Toda la fila marca la casilla (no solo el cuadrito) y mide al menos 48 dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(
                    value = estado.aceptaTerminos,
                    onValueChange = acciones.alCambiarAceptaTerminos,
                    role = Role.Checkbox
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // onCheckedChange = null porque el clic ya lo maneja la fila completa
            Checkbox(checked = estado.aceptaTerminos, onCheckedChange = null)
            Text(
                // TODO: convertir "aviso de privacidad" y "reglas de faltas" en enlaces cuando existan sus rutas
                text = "He leído y acepto el aviso de privacidad y las reglas de faltas.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        if (estado.errorGeneral != null) {
            Text(
                text = estado.errorGeneral,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(16.dp))

        BotonPrincipal(
            texto = "Crear cuenta",
            alPulsar = acciones.alCrearCuenta,
            habilitado = estado.botonHabilitado,
            cargando = estado.cargando,
            textoCargando = "Creando cuenta…"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "¿Ya tienes cuenta?", style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = acciones.alIrALogin) {
                Text("Inicia sesión")
            }
        }
    }
}

// Campo de facultad: un menú desplegable con la lista que llega del servidor.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorFacultad(
    estado: EstadoRegistroUi,
    alElegir: (Int) -> Unit,
    alReintentar: () -> Unit
) {
    // Si el menú está abierto es solo de la interfaz; rememberSaveable lo conserva al girar
    var abierto by rememberSaveable { mutableStateOf(false) }
    val facultades = (estado.facultades as? EstadoFacultades.Listas)?.facultades.orEmpty()

    // Mismo texto que la PWA según el estado de la lista
    val marcador = when (estado.facultades) {
        EstadoFacultades.Cargando -> "Cargando facultades…"
        EstadoFacultades.Error -> "No se pudieron cargar"
        is EstadoFacultades.Listas -> "Elige tu facultad"
    }

    ExposedDropdownMenuBox(
        expanded = abierto,
        // Solo se abre cuando ya hay lista
        onExpandedChange = { abierto = it && facultades.isNotEmpty() }
    ) {
        CampoTexto(
            valor = estado.nombreFacultad,
            alCambiar = {},
            etiqueta = "Facultad",
            marcador = marcador,
            error = estado.errores["id_facultad"],
            soloLectura = true,
            habilitado = estado.facultades is EstadoFacultades.Listas,
            iconoFinal = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            // El campo es el ancla del menú; PrimaryNotEditable porque no se escribe en él
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            facultades.forEach { facultad ->
                DropdownMenuItem(
                    text = { Text(facultad.nombre) },
                    onClick = {
                        alElegir(facultad.id)
                        abierto = false
                    }
                )
            }
        }
    }

    if (estado.facultades is EstadoFacultades.Error) {
        TextButton(onClick = alReintentar) { Text("Reintentar") }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun RegistroContenidoPreview() {
    TemaUASnack {
        RegistroContenido(
            estado = EstadoRegistroUi(
                nombre = "Ana",
                correo = "ana@uas.edu.mx",
                facultades = EstadoFacultades.Listas(listOf(Facultad(1, "Facultad de Informática Culiacán"))),
                idFacultad = 1,
                errores = mapOf("correo_institucional" to "Este correo ya está registrado.")
            ),
            acciones = AccionesRegistro()
        )
    }
}
