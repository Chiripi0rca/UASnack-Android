package mx.uasnack.app.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import mx.uasnack.app.ui.componentes.BotonPrincipal
import mx.uasnack.app.ui.componentes.CampoTexto

@Composable
fun CambiarPasswordPantalla(
    alVolverPerfil: () -> Unit,
    viewModel: CambiarPasswordViewModel = viewModel(
        factory = CambiarPasswordViewModel.Fabrica
    )
) {

    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // Si el cambio fue exitoso, mostramos la confirmación.
    if (estado.exitoso) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp)
        ) {

            Text(
                text = "Listo",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tu contraseña cambió. Cerramos tu sesión en tus otros dispositivos; aquí sigues dentro.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            BotonPrincipal(
                texto = "Volver al perfil",
                alPulsar = alVolverPerfil
            )
        }

        return
    }

    // Formulario para cambiar la contraseña.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),

        verticalArrangement = Arrangement.Top
    ) {

        Text(
            text = "Cambiar contraseña",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Contraseña actual.
        CampoTexto(
            valor = estado.actual,
            alCambiar = viewModel::cambiarActual,
            etiqueta = "Contraseña actual",
            error = estado.errorActual,
            opcionesTeclado = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            transformacion = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Contraseña nueva.
        CampoTexto(
            valor = estado.nueva,
            alCambiar = viewModel::cambiarNueva,
            etiqueta = "Contraseña nueva",
            error = estado.errorNueva,
            opcionesTeclado = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            transformacion = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Confirmación de la contraseña nueva.
        CampoTexto(
            valor = estado.confirmacion,
            alCambiar = viewModel::cambiarConfirmacion,
            etiqueta = "Confirma la contraseña nueva",
            opcionesTeclado = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            transformacion = PasswordVisualTransformation()
        )

        estado.errorGeneral?.let { mensaje ->

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = mensaje,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        BotonPrincipal(
            texto = "Cambiar contraseña",
            textoCargando = "Cambiando...",
            alPulsar = viewModel::cambiarPassword,
            habilitado = estado.actual.isNotBlank() &&
                    estado.nueva.isNotBlank(),
            cargando = estado.cargando
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = alVolverPerfil,
            enabled = !estado.cargando
        ) {
            Text("Cancelar")
        }
    }
}