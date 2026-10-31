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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.uasnack.app.ui.componentes.BotonPrincipal
import mx.uasnack.app.ui.componentes.CampoTexto

@Composable
fun NuevaPasswordPantalla(
    alVolverARecuperacion: () -> Unit,
    alIrALogin: () -> Unit,
    viewModel: NuevaPasswordViewModel = viewModel(
        factory = NuevaPasswordViewModel.Fabrica
    )
) {

    val estado = viewModel.estado.collectAsStateWithLifecycle().value

    // Si no existe un correo en la ruta, se regresa a la primera pantalla.
    LaunchedEffect(estado.correo) {
        if (estado.correo.isBlank()) {
            alVolverARecuperacion()
        }
    }

    if (estado.correo.isBlank()) return

    // Pantalla de confirmación después de cambiar la contraseña.
    if (estado.restablecida) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {

            Text(
                text = "Listo",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tu contraseña cambió. Por seguridad cerramos tu sesión en todos tus dispositivos.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            BotonPrincipal(
                texto = "Iniciar sesión",
                alPulsar = alIrALogin
            )
        }

        return
    }

    // Formulario de nueva contraseña.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {

        Text(
            text = "Crea tu contraseña nueva",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = buildAnnotatedString {
                append("Si ")

                withStyle(
                    SpanStyle(fontWeight = FontWeight.SemiBold)
                ) {
                    append(estado.correo)
                }

                append(
                    " tiene cuenta, le mandamos un código de 6 dígitos. Vence en 10 minutos."
                )
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Código de recuperación.
        CampoTexto(
            valor = estado.codigo,
            alCambiar = viewModel::cambiarCodigo,
            etiqueta = "Código",
            error = estado.errorCodigo,
            opcionesTeclado = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Nueva contraseña.
        CampoTexto(
            valor = estado.password,
            alCambiar = viewModel::cambiarPassword,
            etiqueta = "Contraseña nueva",
            error = estado.errorPassword,
            opcionesTeclado = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            transformacion = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Confirmación de contraseña.
        CampoTexto(
            valor = estado.confirmacion,
            alCambiar = viewModel::cambiarConfirmacion,
            etiqueta = "Confirma la contraseña",
            opcionesTeclado = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            accionesTeclado = KeyboardActions(
                onDone = {
                    viewModel.restablecerPassword()
                }
            ),
            transformacion = PasswordVisualTransformation()
        )

        // Errores generales del servidor.
        if (estado.errorGeneral != null) {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = estado.errorGeneral,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        BotonPrincipal(
            texto = "Cambiar contraseña",
            textoCargando = "Cambiando...",
            alPulsar = viewModel::restablecerPassword,
            habilitado = estado.codigo.length == 6 &&
                    estado.password.isNotEmpty(),
            cargando = estado.cargando
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Reenvío del código.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            TextButton(
                onClick = viewModel::reenviarCodigo,
                enabled = !estado.reenviando
            ) {

                Text(
                    text = when {
                        estado.reenviando -> "Enviando..."
                        estado.codigoReenviado -> "Te mandamos otro código"
                        else -> "No me llegó el código"
                    }
                )
            }
        }

        if (estado.errorReenvio != null) {

            Text(
                text = estado.errorReenvio,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}