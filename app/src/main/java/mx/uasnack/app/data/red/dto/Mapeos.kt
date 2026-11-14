package mx.uasnack.app.data.red.dto

import mx.uasnack.app.domain.modelo.Facultad
import mx.uasnack.app.domain.modelo.Pago
import mx.uasnack.app.domain.modelo.Rol
import mx.uasnack.app.domain.modelo.Usuario
import mx.uasnack.app.domain.modelo.MetodoPago
import mx.uasnack.app.domain.modelo.PedidoCreado

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
fun PagoDto.aDominio() = Pago(
    id = id,
    codigoPedido = codigoPedido,
    monto = monto,
    estado = estado,
    creadoEn = creadoEn
)

// Convierte la respuesta de crear pedido al modelo de dominio.
// Incluye los datos necesarios para PaymentSheet.
fun PedidoCreadoDto.aDominio() = PedidoCreado(
    codigoPedido = codigoPedido,
    estado = estado,
    total = total,
    franjaInicio = franjaInicio,
    franjaFin = franjaFin,
    clientSecret = pago.clientSecret,
    stripeAccount = pago.stripeAccount
)

// Convierte una tarjeta guardada al modelo de dominio.
fun MetodoPagoDto.aDominio() = MetodoPago(
    id = id,
    marca = marca,
    ultimos4 = ultimos4,
    vence = vence
)