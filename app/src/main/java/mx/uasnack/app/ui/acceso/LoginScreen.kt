package mx.uasnack.app.ui.acceso

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit = { _, _ -> },
    onRegisterClick: () -> Unit = {}
) {

    var correo by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var correoError by remember {
        mutableStateOf(false)
    }

    var passwordError by remember {
        mutableStateOf(false)
    }

    val fondo = Color(0xFFF7FBF9)
    val verde = Color(0xFF176C59)
    val texto = Color(0xFF071B22)
    val textoSecundario = Color(0xFF28434B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(fondo)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(
                start = 32.dp,
                end = 32.dp,
                top = 45.dp,
                bottom = 40.dp
            )
    ) {

        Text(
            text = "Iniciar sesión",
            color = texto,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "Entra con tu correo institucional",
            color = textoSecundario,
            fontSize = 19.sp
        )

        Spacer(
            modifier = Modifier.height(42.dp)
        )

        Text(
            text = "Correo institucional",
            color = Color(0xFF163B45),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(9.dp)
        )

        OutlinedTextField(
            value = correo,
            onValueChange = {
                correo = it
                correoError = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
            singleLine = true,
            isError = correoError,
            supportingText = {
                if (correoError) {
                    Text("Ingresa tu correo institucional")
                }
            },
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(
            modifier = Modifier.height(26.dp)
        )

        Text(
            text = "Contraseña",
            color = Color(0xFF163B45),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(9.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
            singleLine = true,
            isError = passwordError,
            supportingText = {
                if (passwordError) {
                    Text("Ingresa tu contraseña")
                }
            },
            shape = RoundedCornerShape(14.dp),
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {

                IconButton(
                    onClick = {
                        passwordVisible = !passwordVisible
                    }
                ) {

                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (passwordVisible) {
                            "Ocultar contraseña"
                        } else {
                            "Mostrar contraseña"
                        },
                        tint = Color(0xFF465653)
                    )
                }
            }
        )

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        Button(
            onClick = {

                correoError = correo.isBlank()
                passwordError = password.isBlank()

                if (!correoError && !passwordError) {
                    onLoginClick(correo, password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = verde
            )
        ) {

            Text(
                text = "Entrar",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(47.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "No tienes cuenta? ",
                color = Color.Black,
                fontSize = 18.sp
            )

            Text(
                text = "Crea una",
                color = verde,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .clickable {
                        onRegisterClick()
                    }
            )
        }
    }
}