package mx.uasnack.app.ui.componentes

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Chip de opción, igual que ChipOpcion de la web (#167): categorías en el Catálogo y
// extras/quitables en Detalle de producto.
// Activo = fondo primaryContainer + palomita (no solo un borde). 48 dp de alto mínimo.
// detalle: texto extra, por ejemplo "+$5.00" (solo para mostrar).
@Composable
fun ChipOpcion(
    texto: String,
    activo: Boolean,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    habilitado: Boolean = true
) {
    FilterChip(
        selected = activo,
        onClick = alPulsar,
        enabled = habilitado,
        modifier = modifier.heightIn(min = 48.dp),
        label = { Text(if (detalle != null) "$texto  $detalle" else texto) },
        leadingIcon = if (activo) {
            { Icon(Icons.Filled.Check, contentDescription = null) }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
