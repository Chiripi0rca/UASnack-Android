package mx.uasnack.app.domain.modelo

data class Perfil(
    val id: Int,
    val nombre: String,
    val apellido: String,
    val correoInstitucional: String,
    val facultad: String,
    val reputacion: Int,
    val faltas: Int
)