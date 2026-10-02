package mx.uasnack.app.ui.componentes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mx.uasnack.app.ui.tema.TextoDeshabilitado

// Botón de la acción principal de cada pantalla (B.4: 56 dp de alto, forma de píldora, texto 14/600).
// Mientras "cargando" es true el botón muestra textoCargando y no se puede volver a pulsar:
// así reacciona en cuanto se toca y se evita el doble envío.
@Composable
fun BotonPrincipal(
    texto: String,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    textoCargando: String = texto
) {
    Button(
        onClick = alPulsar,
        enabled = habilitado && !cargando,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        // B.4: deshabilitado con fondo surface container y texto #9AA5A0, nunca solo opacidad
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContentColor = TextoDeshabilitado
        )
    ) {
        Text(text = if (cargando) textoCargando else texto, style = MaterialTheme.typography.labelLarge)
    }
}
