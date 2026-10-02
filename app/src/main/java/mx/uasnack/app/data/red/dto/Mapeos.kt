package mx.uasnack.app.data.red.dto

import mx.uasnack.app.domain.modelo.Facultad
import mx.uasnack.app.domain.modelo.Rol
import mx.uasnack.app.domain.modelo.Usuario

// Traducción DTO -> modelo de dominio (tarea 25). La interfaz solo conoce los modelos de dominio;
// si la API cambia un nombre, se ajusta aquí y no llega a las pantallas.

fun UsuarioDto.aDominio() = Usuario(
    id = id,
    nombre = nombre,
    apellido = apellido,
    correoInstitucional = correoInstitucional,
    idFacultad = idFacultad,
    rol = when (rol) {
        "usuario" -> Rol.USUARIO
        "encargado_tienda" -> Rol.ENCARGADO_TIENDA
        "administrador" -> Rol.ADMINISTRADOR
        else -> Rol.DESCONOCIDO
    },
    reputacion = reputacion,
    correoVerificado = correoVerificadoEn != null
)

fun FacultadDto.aDominio() = Facultad(id = id, nombre = nombre)
