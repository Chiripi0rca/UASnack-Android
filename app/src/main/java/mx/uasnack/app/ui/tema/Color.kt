package mx.uasnack.app.ui.tema

import androidx.compose.ui.graphics.Color

// Colores del Anexo B (B.1) del documento general.
// Son los mismos valores que la web tiene en shared/diseno/tokens.css: si cambia uno, cambian los dos.
// Ninguna pantalla debe usar Color(0xFF...) directo; se piden a MaterialTheme.colorScheme.

// Los 15 roles de B.1
val Primario = Color(0xFF16624F)                // Acción principal, estado LISTO
val SobrePrimario = Color(0xFFFFFFFF)           // Texto sobre Primario
val ContenedorPrimario = Color(0xFFA6F2D8)      // Selección, chips activos, ENTREGADO
val SobreContenedorPrimario = Color(0xFF00201A)
val ContenedorSecundario = Color(0xFFCFE9DF)    // Estado ACEPTADO
val Terciario = Color(0xFF7A5900)               // Avisos, "voy saliendo", "ya llegué"
val ContenedorTerciario = Color(0xFFFFDF9B)     // Fondo de aviso, EN_PREPARACION
val ColorError = Color(0xFFB3261E)              // Destructivo, validación
val ContenedorError = Color(0xFFF9DEDC)         // RECHAZADO, NO_RECOGIDO
val Superficie = Color(0xFFF6FBF8)              // Fondo de pantalla
val ContenedorSuperficie = Color(0xFFEAF1EC)    // Tarjetas secundarias, botón deshabilitado
val SobreSuperficie = Color(0xFF161D1A)         // Texto principal
val SobreSuperficieVariante = Color(0xFF3F4945) // Texto secundario
val Contorno = Color(0xFF6F7975)                // Bordes de campo y botón contorneado
val ContornoVariante = Color(0xFFBFC9C4)        // Bordes de tarjeta, separadores

// Roles que Material 3 usa y B.1 no lista. Si no se definen, Material pone su morado por defecto.
// Salen de los textos de la tabla de estados (B.3).
val SobreContenedorSecundario = Color(0xFF0C1F19) // Texto de ACEPTADO
val SobreTerciario = Color(0xFFFFFFFF)
val SobreContenedorTerciario = Color(0xFF261A00)  // Texto de EN_PREPARACION
val SobreError = Color(0xFFFFFFFF)
val SobreContenedorError = Color(0xFF410E0B)      // Texto de RECHAZADO y NO_RECOGIDO

// B.4: el botón deshabilitado lleva este texto sobre ContenedorSuperficie, nunca solo opacidad.
val TextoDeshabilitado = Color(0xFF9AA5A0)
