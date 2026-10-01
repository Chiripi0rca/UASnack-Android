package mx.uasnack.app.ui.acceso

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RegisterScreen(
    onLoginClick: () -> Unit = {}
) {

    // ----------------------------
    // VALORES DE LOS CAMPOS
    // ----------------------------

    var nombre by remember {
        mutableStateOf("")
    }

    var apellido by remember {
        mutableStateOf("")
    }

    var correo by remember {
        mutableStateOf("")
    }

    var facultad by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var telefono by remember {
        mutableStateOf("")
    }

    var acepta by remember {
        mutableStateOf(false)
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    // ----------------------------
    // ESTADOS DE ERROR
    // ----------------------------

    var nombreError by remember {
        mutableStateOf(false)
    }

    var apellidoError by remember {
        mutableStateOf(false)
    }

    var correoError by remember {
        mutableStateOf(false)
    }

    var facultadError by remember {
        mutableStateOf(false)
    }

    var passwordError by remember {
        mutableStateOf(false)
    }

    var telefonoError by remember {
        mutableStateOf(false)
    }

    var aceptaError by remember {
        mutableStateOf(false)
    }

    // ----------------------------
    // COLORES
    // ----------------------------

    val fondo = Color(0xFFF7FBF9)
    val texto = Color(0xFF071B22)
    val verde = Color(0xFF176C59)

    // ----------------------------
    // VALIDACIONES
    // ----------------------------

    fun validarNombre(): Boolean {

        val correcto =
            nombre.trim().length >= 2 &&
                    nombre.trim().all { it.isLetter() || it.isWhitespace() }

        nombreError = !correcto

        return correcto
    }

    fun validarApellido(): Boolean {

        val correcto =
            apellido.trim().length >= 2 &&
                    apellido.trim().all { it.isLetter() || it.isWhitespace() }

        apellidoError = !correcto

        return correcto
    }

    fun validarCorreo(): Boolean {

        val correcto =
            correo.isNotBlank() &&
                    Patterns.EMAIL_ADDRESS.matcher(correo).matches()

        correoError = !correcto

        return correcto
    }

    fun validarFacultad(): Boolean {

        val correcto = facultad.isNotBlank()

        facultadError = !correcto

        return correcto
    }

    fun validarPassword(): Boolean {

        val correcto = password.length >= 8

        passwordError = !correcto

        return correcto
    }

    fun validarTelefono(): Boolean {

        val correcto =
            telefono.length == 10 &&
                    telefono.all { it.isDigit() }

        telefonoError = !correcto

        return correcto
    }

    fun validarAceptacion(): Boolean {

        aceptaError = !acepta

        return acepta
    }

    // ----------------------------
    // PANTALLA
    // ----------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(fondo)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = 32.dp,
                bottom = 32.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
        ) {

            // ============================
            // TÍTULO
            // ============================

            Text(
                text = "Crea tu cuenta",
                color = texto,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            // ============================
            // NOMBRE
            // ============================

            Text(
                text = "Nombre",
                color = texto,
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = {
                    nombre = it

                    if (nombreError) {
                        validarNombre()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nombreError,
                supportingText = {
                    if (nombreError) {
                        Text("Ingresa un nombre válido.")
                    }
                },
                shape = RoundedCornerShape(9.dp)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // ============================
            // APELLIDO
            // ============================

            Text(
                text = "Apellido",
                color = texto,
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            OutlinedTextField(
                value = apellido,
                onValueChange = {
                    apellido = it

                    if (apellidoError) {
                        validarApellido()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = apellidoError,
                supportingText = {
                    if (apellidoError) {
                        Text("Ingresa un apellido válido.")
                    }
                },
                shape = RoundedCornerShape(9.dp)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // ============================
            // CORREO
            // ============================

            Text(
                text = "Correo institucional",
                color = texto,
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            OutlinedTextField(
                value = correo,
                onValueChange = {
                    correo = it

                    if (correoError) {
                        validarCorreo()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                isError = correoError,
                supportingText = {
                    if (correoError) {
                        Text("Ingresa un correo válido.")
                    }
                },
                shape = RoundedCornerShape(9.dp)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // ============================
            // FACULTAD
            // ============================

            Text(
                text = "Facultad",
                color = texto,
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            OutlinedTextField(
                value = facultad,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Aquí posteriormente abriremos
                        // el selector cargado desde el backend.
                    },
                readOnly = true,
                placeholder = {
                    Text("Elige tu facultad")
                },
                isError = facultadError,
                supportingText = {
                    if (facultadError) {
                        Text("Selecciona una facultad.")
                    }
                },
                shape = RoundedCornerShape(9.dp)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // ============================
            // CONTRASEÑA
            // ============================

            Text(
                text = "Contraseña",
                color = texto,
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it

                    if (passwordError) {
                        validarPassword()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation =
                    if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                trailingIcon = {

                    IconButton(
                        onClick = {
                            passwordVisible = !passwordVisible
                        }
                    ) {

                        Icon(
                            imageVector =
                                if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                            contentDescription = null
                        )
                    }
                },
                isError = passwordError,
                supportingText = {
                    if (passwordError) {
                        Text("La contraseña debe tener al menos 8 caracteres.")
                    }
                },
                shape = RoundedCornerShape(9.dp)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // ============================
            // TELÉFONO
            // ============================

            Text(
                text = "Teléfono",
                color = texto,
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            OutlinedTextField(
                value = telefono,
                onValueChange = {

                    // Solo permitir números
                    if (it.all { caracter -> caracter.isDigit() } &&
                        it.length <= 10
                    ) {
                        telefono = it
                    }

                    if (telefonoError) {
                        validarTelefono()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                isError = telefonoError,
                supportingText = {
                    if (telefonoError) {
                        Text("El teléfono debe tener 10 dígitos.")
                    }
                },
                shape = RoundedCornerShape(9.dp)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // ============================
            // CHECKBOX
            // ============================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Checkbox(
                    checked = acepta,
                    onCheckedChange = {
                        acepta = it
                        aceptaError = !it
                    }
                )

                Text(
                    text = "He leído y acepto el aviso de privacidad y las reglas de faltas.",
                    color = texto,
                    fontSize = 13.sp
                )
            }

            if (aceptaError) {

                Text(
                    text = "Debes aceptar el aviso de privacidad y las reglas de faltas.",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // ============================
            // CREAR CUENTA
            // ============================

            Button(
                onClick = {

                    val nombreCorrecto = validarNombre()
                    val apellidoCorrecto = validarApellido()
                    val correoCorrecto = validarCorreo()
                    val facultadCorrecta = validarFacultad()
                    val passwordCorrecta = validarPassword()
                    val telefonoCorrecto = validarTelefono()
                    val aceptaCorrecto = validarAceptacion()

                    if (
                        nombreCorrecto &&
                        apellidoCorrecto &&
                        correoCorrecto &&
                        facultadCorrecta &&
                        passwordCorrecta &&
                        telefonoCorrecto &&
                        aceptaCorrecto
                    ) {

                        // Aquí posteriormente conectaremos
                        // el registro con el backend.
                    }

                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = verde
                )
            ) {

                Text(
                    text = "Crear cuenta",
                    fontSize = 14.sp
                )
            }

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            // ============================
            // VOLVER AL LOGIN
            // ============================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "¿Ya tienes cuenta? ",
                    color = Color.Black,
                    fontSize = 14.sp
                )

                Text(
                    text = "Inicia sesión",
                    color = verde,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        onLoginClick()
                    }
                )
            }
        }
    }
}