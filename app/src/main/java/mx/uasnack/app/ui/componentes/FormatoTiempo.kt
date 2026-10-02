package mx.uasnack.app.ui.componentes

// Convierte segundos a "m:ss" (43 -> "0:43", 125 -> "2:05"), igual que formatearTiempo() de la PWA.
fun formatearTiempo(segundos: Int): String {
    val minutos = segundos / 60
    val resto = segundos % 60
    return "$minutos:${resto.toString().padStart(2, '0')}"
}
