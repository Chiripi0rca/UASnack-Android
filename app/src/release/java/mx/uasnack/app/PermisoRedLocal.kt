package mx.uasnack.app

import android.app.Activity

// Versión de release: no hace nada. El APK final se conecta al servidor por internet y no
// necesita el permiso de red local (ver src/debug/java/mx/uasnack/app/PermisoRedLocal.kt).
@Suppress("UNUSED_PARAMETER")
fun pedirPermisoRedLocal(actividad: Activity) = Unit
