package mx.uasnack.app.ui.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEleccionRetiro(
    franjasDisponibles: List<String>,
    onFranjaSeleccionada: (String) -> Unit = {}
) {
    var opcionSeleccionada by remember { mutableStateOf(0) }
    val opcionesSegmentadas = listOf("En sucursal", "A domicilio")
    var franjaSeleccionada by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Elección de retiro",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Control Segmentado
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            opcionesSegmentadas.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = opcionSeleccionada == index,
                    onClick = { opcionSeleccionada = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = opcionesSegmentadas.size)
                ) {
                    Text(label)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Selecciona una franja horaria",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Rejilla de Franjas (Grid)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(franjasDisponibles) { franja ->
                OutlinedButton(
                    onClick = {
                        franjaSeleccionada = franja
                        onFranjaSeleccionada(franja)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (franjaSeleccionada == franja)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = franja)
                }
            }
        }
    }
}
