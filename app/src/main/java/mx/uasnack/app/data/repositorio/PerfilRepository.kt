package mx.uasnack.app.data.repositorio

import mx.uasnack.app.data.red.ApiService
import mx.uasnack.app.domain.modelo.Perfil

class PerfilRepository(
    private val api: ApiService
) {
    suspend fun obtenerPerfil(): Result<Perfil> {
        return try {
            val respuesta = api.obtenerPerfil()
            val usuario = respuesta.usuario

            Result.success(
                Perfil(
                    id = usuario.id,
                    nombre = usuario.nombre,
                    apellido = usuario.apellido,
                    correoInstitucional = usuario.correoInstitucional,
                    facultad = usuario.facultad?.nombre.orEmpty(),
                    reputacion = usuario.reputacion,
                    faltas = usuario.faltas
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}