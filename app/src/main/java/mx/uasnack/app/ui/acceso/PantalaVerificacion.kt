package mx.uasnack.app.ui.acceso

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun PantallaVerificacion(
    onVerificar: (String) -> Unit = {}
) {
    var codigo by remember { mutableStateOf(List(6) { "" }) }
    var tiempoRestante by remember { mutableStateOf(60) }
    var sePuedeReenviar by remember { mutableStateOf(false) }

    // Contador regresivo para reenviar código
    LaunchedEffect(key1 = tiempoRestante, key2 = sePuedeReenviar) {
        if (!sePuedeReenviar && tiempoRestante > 0) {
            delay(1000L)
            tiempoRestante--
        } else if (tiempoRestante == 0) {
            sePuedeReenviar = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Código de verificación",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ingresa el código de 6 dígitos enviado a tu correo.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 6 Casillas de código
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 6) {
                OutlinedTextField(
                    value = codigo[i],
                    onValueChange = { valor ->
                        if (valor.length <= 1) {
                            codigo = codigo.toMutableList().also { it[i] = valor }
                        }
                    },
                    modifier = Modifier
                        .width(48.dp)
                        .height(56.dp),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botón para verificar
        Button(
            onClick = { onVerificar(codigo.joinToString("")) },
            modifier = Modifier.fillMaxWidth(),
            enabled = codigo.all { it.isNotEmpty() }
        ) {
            Text("Verificar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contador y Botón de Reenviar
        if (sePuedeReenviar) {
            TextButton(
                onClick = {
                    tiempoRestante = 60
                    sePuedeReenviar = false
                }
            ) {
                Text("Reenviar código")
            }
        } else {
            Text(
                text = "Reenviar código en ${tiempoRestante}s",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}