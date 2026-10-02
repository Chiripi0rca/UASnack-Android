package mx.uasnack.app.ui.componentes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation

// Campo de texto de toda la app (B.4: 56 dp de alto, radio de 8 dp, etiqueta flotante en primary).
// El mensaje de error lo manda el servidor; el campo solo lo muestra debajo, nunca inventa uno.
@Composable
fun CampoTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    marcador: String? = null,
    habilitado: Boolean = true,
    soloLectura: Boolean = false,
    opcionesTeclado: KeyboardOptions = KeyboardOptions.Default,
    accionesTeclado: KeyboardActions = KeyboardActions.Default,
    transformacion: VisualTransformation = VisualTransformation.None,
    iconoFinal: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        modifier = modifier.fillMaxWidth(),
        label = { Text(etiqueta) },
        placeholder = marcador?.let { { Text(it) } },
        isError = error != null,
        // Debajo del campo solo aparece algo cuando hay error
        supportingText = error?.let { { Text(it) } },
        enabled = habilitado,
        readOnly = soloLectura,
        singleLine = true,
        keyboardOptions = opcionesTeclado,
        keyboardActions = accionesTeclado,
        visualTransformation = transformacion,
        trailingIcon = iconoFinal,
        shape = MaterialTheme.shapes.small
    )
}

// Botón del ojo para mostrar u ocultar la contraseña. IconButton ya mide 48 dp (área táctil mínima).
@Composable
fun BotonVerPassword(visible: Boolean, alPulsar: () -> Unit) {
    IconButton(onClick = alPulsar) {
        Icon(
            imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña"
        )
    }
}
