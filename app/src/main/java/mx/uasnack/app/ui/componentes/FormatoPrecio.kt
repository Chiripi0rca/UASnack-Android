package mx.uasnack.app.ui.componentes

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

// "$25.00", igual que formatearPrecio() de la PWA. Solo para mostrar: el cobro lo calcula el servidor.
private val FORMATO_MXN: NumberFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"))

fun formatearPrecio(valor: BigDecimal): String = FORMATO_MXN.format(valor)
