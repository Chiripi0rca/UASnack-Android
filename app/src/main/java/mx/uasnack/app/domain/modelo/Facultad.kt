package mx.uasnack.app.domain.modelo

// Facultad tal como la usa la interfaz (el registro manda el id, no el nombre).
// Cuando exista la capa de red, FacultadDto.aDominio() devolverá este modelo.
data class Facultad(
    val id: Int,
    val nombre: String
)
