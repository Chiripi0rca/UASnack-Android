package mx.uasnack.app.ui.acceso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.uasnack.app.ui.componentes.BotonPrincipal
import mx.uasnack.app.ui.componentes.BotonVerPassword
import mx.uasnack.app.ui.componentes.CampoTexto
import mx.uasnack.app.ui.componentes.formatearTiempo
import mx.uasnack.app.ui.tema.FuenteDatos
import mx.uasnack.app.ui.tema.TemaUASnack

// Pantalla 1 · Acceso. Esta parte conecta el ViewModel con la navegación;
// lo que se dibuja está en LoginContenido, que no tiene estado propio.
@Composable
fun LoginPantalla(
    alIniciarSesion: () -> Unit,
    alIrARegistro: () -> Unit,
    alIrAVerificacion: (correo: String) -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Fabrica)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // Escucha los eventos de una sola vez y navega
    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                EventoLogin.SesionIniciada -> alIniciarSesion()
                is EventoLogin.CorreoSinVerificar -> alIrAVerificacion(evento.correo)
            }
        }
    }

    LoginContenido(
        estado = estado,
        alCambiarCorreo = viewModel::cambiarCorreo,
        alCambiarPassword = viewModel::cambiarPassword,
        alAlternarVerPassword = viewModel::alternarVerPassword,
        alEntrar = viewModel::entrar,
        alIrARegistro = alIrARegistro
    )
}

@Composable
private fun LoginContenido(
    estado: EstadoLoginUi,
    alCambiarCorreo: (String) -> Unit,
    alCambiarPassword: (String) -> Unit,
    alAlternarVerPassword: () -> Unit,
    alEntrar: () -> Unit,
    alIrARegistro: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Deja libre la barra de estado, la de navegación y el teclado
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Text(text = "Iniciar sesión", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Entra con tu correo institucional",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        CampoTexto(
            valor = estado.correo,
            alCambiar = alCambiarCorreo,
            etiqueta = "Correo institucional",
            error = estado.errorCorreo,
            // Teclado con @, sin autocorrección, y "siguiente" pasa a la contraseña
            opcionesTeclado = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next
            )
        )

        Spacer(Modifier.height(8.dp))

        CampoTexto(
            valor = estado.password,
            alCambiar = alCambiarPassword,
            etiqueta = "Contraseña",
            error = estado.errorPassword,
            opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            // "Listo" en el teclado hace lo mismo que el botón Entrar
            accionesTeclado = KeyboardActions(onDone = { alEntrar() }),
            transformacion = if (estado.verPassword) VisualTransformation.None else PasswordVisualTransformation(),
            iconoFinal = { BotonVerPassword(visible = estado.verPassword, alPulsar = alAlternarVerPassword) }
        )

        // Error general del servidor; si hay bloqueo (429) se agrega la cuenta regresiva
        if (estado.errorGeneral != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = buildAnnotatedString {
                    append(estado.errorGeneral)
                    if (estado.bloqueado) {
                        append(" Intenta de nuevo en ")
                        // La hora va en monoespaciada (B.2) para que no "baile" al cambiar cada segundo
                        withStyle(SpanStyle(fontFamily = FuenteDatos)) {
                            append(formatearTiempo(estado.segundosBloqueo))
                        }
                        append(".")
                    }
                },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(24.dp))

        BotonPrincipal(
            texto = "Entrar",
            alPulsar = alEntrar,
            habilitado = !estado.bloqueado,
            cargando = estado.cargando,
            textoCargando = "Entrando..."
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "¿No tienes cuenta?", style = MaterialTheme.typography.bodyLarge)
            // TextButton en vez de un Text con clickable: así cumple el área táctil de 48 dp
            TextButton(onClick = alIrARegistro) {
                Text("Crea una")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginContenidoPreview() {
    TemaUASnack {
        LoginContenido(
            estado = EstadoLoginUi(
                correo = "alumno@uas.edu.mx",
                errorGeneral = "Demasiados intentos.",
                segundosBloqueo = 43
            ),
            alCambiarCorreo = {},
            alCambiarPassword = {},
            alAlternarVerPassword = {},
            alEntrar = {},
            alIrARegistro = {}
        )
    }
}
