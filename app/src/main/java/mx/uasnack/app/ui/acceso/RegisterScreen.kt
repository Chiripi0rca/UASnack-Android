package mx.uasnack.app.ui.acceso

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onLoginClick: () -> Unit = {}) {

    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var facultad by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var acepta by remember { mutableStateOf(false) }

    var nombreTocado by remember { mutableStateOf(false) }
    var apellidoTocado by remember { mutableStateOf(false) }
    var correoTocado by remember { mutableStateOf(false) }
    var facultadTocado by remember { mutableStateOf(false) }
    var passwordTocado by remember { mutableStateOf(false) }
    var telefonoTocado by remember { mutableStateOf(false) }
    var aceptaTocado by remember { mutableStateOf(false) }

    var facultadAbierta by remember { mutableStateOf(false) }

    // Posteriormente vendrán del backend
    val facultades = emptyList<String>()

    val fondo = Color(0xFFF7FBF9)
    val verde = Color(0xFF176C59)
    val texto = Color(0xFF071B22)
    val textoSecundario = Color(0xFF28434B)

    Column(
        Modifier
            .fillMaxSize()
            .background(fondo)
            .padding(24.dp)
    ) {

        Text(
            "Crea tu cuenta",
            color = texto,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                nombreTocado = true
            },
            label = { Text("Nombre", color = textoSecundario) },
            isError = nombreTocado && nombre.isBlank(),
            supportingText = {
                if (nombreTocado && nombre.isBlank())
                    Text("Ingresa tu nombre")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = apellido,
            onValueChange = {
                apellido = it
                apellidoTocado = true
            },
            label = { Text("Apellido", color = textoSecundario) },
            isError = apellidoTocado && apellido.isBlank(),
            supportingText = {
                if (apellidoTocado && apellido.isBlank())
                    Text("Ingresa tu apellido")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = correo,
            onValueChange = {
                correo = it
                correoTocado = true
            },
            label = { Text("Correo institucional", color = textoSecundario) },
            isError = correoTocado &&
                    !Patterns.EMAIL_ADDRESS.matcher(correo).matches(),
            supportingText = {
                if (correoTocado &&
                    !Patterns.EMAIL_ADDRESS.matcher(correo).matches()
                )
                    Text("Correo no válido")
            },
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = facultadAbierta,
            onExpandedChange = {
                facultadAbierta = !facultadAbierta
                facultadTocado = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            OutlinedTextField(
                value = facultad,
                onValueChange = {},
                readOnly = true,
                label = { Text("Facultad", color = textoSecundario) },
                placeholder = { Text("Selecciona tu facultad") },
                isError = facultadTocado && facultad.isBlank(),
                supportingText = {
                    if (facultadTocado && facultad.isBlank())
                        Text("Selecciona una facultad")
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = facultadAbierta
                    )
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = facultadAbierta,
                onDismissRequest = {
                    facultadAbierta = false
                }
            ) {
                facultades.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(opcion, color = texto) },
                        onClick = {
                            facultad = opcion
                            facultadAbierta = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordTocado = true
            },
            label = { Text("Contraseña", color = textoSecundario) },
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordTocado && password.length < 8,
            supportingText = {
                if (passwordTocado && password.length < 8)
                    Text("Mínimo 8 caracteres")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = telefono,
            onValueChange = {
                if (it.all(Char::isDigit) && it.length <= 10)
                    telefono = it

                telefonoTocado = true
            },
            label = { Text("Teléfono", color = textoSecundario) },
            isError = telefonoTocado && telefono.length != 10,
            supportingText = {
                if (telefonoTocado && telefono.length != 10)
                    Text("Debe tener 10 dígitos")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row {
            Checkbox(
                checked = acepta,
                onCheckedChange = {
                    acepta = it
                    aceptaTocado = true
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = verde
                )
            )

            Text(
                "Acepto el aviso de privacidad y las reglas",
                color = texto
            )
        }

        if (aceptaTocado && !acepta) {
            Text(
                "Debes marcar esta casilla para continuar",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Button(
            onClick = {
                aceptaTocado = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = verde
            )
        ) {
            Text("Crear cuenta", color = Color.White)
        }

        TextButton(onClick = onLoginClick) {
            Text("¿Ya tienes cuenta? ", color = texto)
            Text("Inicia sesión", color = verde)
        }
    }
}
