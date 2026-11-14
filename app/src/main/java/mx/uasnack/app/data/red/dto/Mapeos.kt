
package mx.uasnack.app.data.red.dto

import mx.uasnack.app.domain.modelo.Facultad
import mx.uasnack.app.domain.modelo.MetodoPago
import mx.uasnack.app.domain.modelo.Pago
import mx.uasnack.app.domain.modelo.PedidoCreado
import mx.uasnack.app.domain.modelo.Rol
import mx.uasnack.app.domain.modelo.Usuario

// Usuario: DTO -> dominio.
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

// Facultad: DTO -> dominio.
fun FacultadDto.aDominio() = Facultad(
    id = id,
    nombre = nombre
)

// Pago: DTO -> dominio.
fun PagoDto.aDominio() = Pago(
    id = id,
    codigoPedido = codigoPedido,
    monto = monto,
    estado = estado,
    creadoEn = creadoEn
)

// Pedido creado: DTO -> dominio.
fun PedidoCreadoDto.aDominio() = PedidoCreado(
    codigoPedido = codigoPedido,
    estado = estado,
    total = total,
    franjaInicio = franjaInicio,
    franjaFin = franjaFin,
    clientSecret = pago.clientSecret,
    stripeAccount = pago.stripeAccount
)

// Método de pago: DTO -> dominio.
fun MetodoPagoDto.aDominio() = MetodoPago(
    id = id,
    marca = marca,
    ultimos4 = ultimos4,
    vence = vence
)
