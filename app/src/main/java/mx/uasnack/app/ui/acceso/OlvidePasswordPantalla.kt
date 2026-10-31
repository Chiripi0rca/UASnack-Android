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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.uasnack.app.ui.componentes.BotonPrincipal
import mx.uasnack.app.ui.componentes.CampoTexto

@Composable
fun OlvidePasswordPantalla(
    alVolverLogin: () -> Unit,
    alIrANuevaPassword: (String) -> Unit,
    viewModel: OlvidePasswordViewModel = viewModel(
        factory = OlvidePasswordViewModel.Fabrica
    )
) {

    val estado = viewModel.estado.collectAsStateWithLifecycle().value

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                is EventoOlvidePassword.CodigoSolicitado -> {
                    alIrANuevaPassword(evento.correo)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {

        Text(
            text = "Recupera tu contraseña",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Escribe tu correo institucional y te mandaremos un código para crear una contraseña nueva.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        CampoTexto(
            valor = estado.correo,
            alCambiar = viewModel::cambiarCorreo,
            etiqueta = "Correo institucional",
            error = estado.errorCorreo,
            opcionesTeclado = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                autoCorrectEnabled = false
            )
        )

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
            texto = "Mandar código",
            textoCargando = "Mandando...",
            alPulsar = viewModel::solicitarCodigo,
            habilitado = estado.correo.isNotBlank(),
            cargando = estado.cargando
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = alVolverLogin
            ) {
                Text("Volver a iniciar sesión")
            }
        }
    }
}