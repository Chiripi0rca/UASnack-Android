package mx.uasnack.app.data.repositorio

import mx.uasnack.app.data.red.ApiService
import mx.uasnack.app.data.red.dto.OlvidePasswordRequest
import mx.uasnack.app.data.red.dto.RestablecerPasswordRequest

class RecuperacionPasswordRepository(
    private val api: ApiService
) {

    // Paso 1: solicitar el código por correo.
    suspend fun solicitarCodigo(correo: String): Result<Unit> {
        return try {
            api.olvidePassword(
                OlvidePasswordRequest(
                    correoInstitucional = correo
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Paso 2: validar el código y establecer la contraseña nueva.
    suspend fun restablecerPassword(
        correo: String,
        codigo: String,
        password: String,
        confirmacion: String
    ): Result<Unit> {
        return try {
            api.restablecerPassword(
                RestablecerPasswordRequest(
                    correoInstitucional = correo,
                    codigo = codigo,
                    password = password,
                    passwordConfirmation = confirmacion
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}