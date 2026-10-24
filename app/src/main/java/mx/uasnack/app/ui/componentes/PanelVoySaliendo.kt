package mx.uasnack.app.ui.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun PanelVoySaliendo(
    tiempoEstimado: String,
    saturacionAlta: Boolean,
    onNotificarSalida: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Panel: Voy saliendo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tiempo estimado de llegada: $tiempoEstimado",
                style = MaterialTheme.typography.bodyMedium
            )

            // Advertencia de saturación condicional
            if (saturacionAlta) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Advertencia: Alta saturación en la zona o servicio en este momento.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNotificarSalida,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Notificar Salida")
            }
        }
    }
}