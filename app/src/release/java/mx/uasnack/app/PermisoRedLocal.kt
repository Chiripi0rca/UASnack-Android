package mx.uasnack.app

import androidx.compose.runtime.Composable

// Versión de release: muestra la app directo. El APK final se conecta al servidor por internet y
// no necesita el permiso de red local (ver src/debug/java/mx/uasnack/app/PermisoRedLocal.kt).
@Composable
fun ConPermisoRedLocal(contenido: @Composable () -> Unit) = contenido()
