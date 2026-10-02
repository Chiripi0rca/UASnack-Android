package mx.uasnack.app.ui.acceso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.uasnack.app.ui.componentes.formatearTiempo
import mx.uasnack.app.ui.tema.EstiloDatos
import mx.uasnack.app.ui.tema.FuenteDatos
import mx.uasnack.app.ui.tema.TemaUASnack

// Pantalla 3 · Verificación. Conecta el ViewModel con la navegación; lo visual está en VerificacionContenido.
@Composable
fun VerificacionPantalla(
    alVerificar: () -> Unit,
    viewModel: VerificacionViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                EventoVerificacion.CorreoVerificado -> alVerificar()
            }
        }
    }

    VerificacionContenido(
        estado = estado,
        alEscribirDesde = viewModel::escribirDesde,
        alBorrar = viewModel::borrar,
        alReenviar = viewModel::reenviarCodigo
    )
}

@Composable
private fun VerificacionContenido(
    estado: EstadoVerificacionUi,
    alEscribirDesde: (indice: Int, texto: String) -> Int?,
    alBorrar: (indice: Int) -> Unit,
    alReenviar: () -> Unit
) {
    // Un FocusRequester por casilla para mover el cursor entre ellas
    val enfoques = remember { List(DIGITOS_CODIGO) { FocusRequester() } }

    // Al abrir la pantalla (o al recrearla tras girar) el cursor va a la primera casilla vacía,
    // nunca a una que ya tiene dígito: así el teclado no escribe encima de lo que ya estaba
    LaunchedEffect(Unit) {
        val primeraVacia = estado.digitos.indexOfFirst { it.isEmpty() }
        enfoques[if (primeraVacia == -1) DIGITOS_CODIGO - 1 else primeraVacia].requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Verifica tu correo", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = buildAnnotatedString {
                append("Enviamos un código de $DIGITOS_CODIGO dígitos a ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(estado.correo) }
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Las seis casillas se reparten el ancho con weight, así caben en pantallas de 360 dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            estado.digitos.forEachIndexed { i, digito ->
                OutlinedTextField(
                    // TextFieldValue con el cursor siempre al final del dígito: así "borrar" lo quita
                    // aunque la casilla haya recibido el foco tocándola por la izquierda
                    value = TextFieldValue(text = digito, selection = TextRange(digito.length)),
                    onValueChange = { nuevoValor ->
                        val valor = nuevoValor.text
                        when {
                            valor.isEmpty() -> alBorrar(i)
                            else -> {
                                // Si la casilla ya tenía un dígito, se queda solo con el nuevo;
                                // si llegan varios (pegar), se reparten desde esta casilla
                                val texto = if (valor.length == 2 && digito.isNotEmpty()) valor.replaceFirst(digito, "") else valor
                                alEscribirDesde(i, texto)?.let { siguiente -> enfoques[siguiente].requestFocus() }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .focusRequester(enfoques[i])
                        // Borrar en una casilla vacía regresa a la anterior
                        .onPreviewKeyEvent { evento ->
                            if (evento.type == KeyEventType.KeyDown && evento.key == Key.Backspace && digito.isEmpty() && i > 0) {
                                enfoques[i - 1].requestFocus()
                                true
                            } else {
                                false
                            }
                        }
                        .semantics { contentDescription = "Dígito ${i + 1} de $DIGITOS_CODIGO" },
                    // Monoespaciada (B.2) y más grande que el texto normal: se lee dígito por dígito
                    textStyle = EstiloDatos.copy(fontSize = 22.sp, textAlign = TextAlign.Center),
                    isError = estado.error != null,
                    readOnly = estado.verificando,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = MaterialTheme.shapes.small
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (estado.verificando) {
            Text(text = "Verificando…", style = MaterialTheme.typography.bodyMedium)
        }

        if (estado.error != null) {
            Text(
                text = estado.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(8.dp))

        // Mismos tres textos que la PWA: con cuenta regresiva, enviando y disponible
        TextButton(onClick = alReenviar, enabled = estado.puedeReenviar) {
            when {
                estado.segundosParaReenvio > 0 -> Text(
                    buildAnnotatedString {
                        append("Reenviar código en ")
                        withStyle(SpanStyle(fontFamily = FuenteDatos)) {
                            append(formatearTiempo(estado.segundosParaReenvio))
                        }
                    }
                )
                estado.reenviando -> Text("Enviando…")
                else -> Text("Reenviar código")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VerificacionContenidoPreview() {
    TemaUASnack {
        VerificacionContenido(
            estado = EstadoVerificacionUi(
                correo = "ana@uas.edu.mx",
                digitos = listOf("4", "8", "1", "", "", ""),
                segundosParaReenvio = 43
            ),
            alEscribirDesde = { _, _ -> null },
            alBorrar = {},
            alReenviar = {}
        )
    }
}
