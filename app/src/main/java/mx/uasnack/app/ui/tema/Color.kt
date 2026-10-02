package mx.uasnack.app.ui.tema

import androidx.compose.ui.graphics.Color

// Colores del Anexo B (B.1) del documento general.
// Son los mismos valores que la web tiene en shared/diseno/tokens.css: si cambia uno, cambian los dos.
// Ninguna pantalla debe usar Color(0xFF...) directo; se piden a MaterialTheme.colorScheme.

// Los 15 roles de B.1
val Primario = Color(0xFF3F7B39)                // Acción principal, estado LISTO
val SobrePrimario = Color(0xFFFFFFFF)           // Texto sobre Primario
val ContenedorPrimario = Color(0xFFDAFBD5)      // Selección, chips activos, ENTREGADO
val SobreContenedorPrimario = Color(0xFF0F2A0C)
val ContenedorSecundario = Color(0xFFB9B1FC)    // Estado ACEPTADO
val Terciario = Color(0xFF816553)               // Avisos, "voy saliendo", "ya llegué"
val ContenedorTerciario = Color(0xFFFCC6A2)     // Fondo de aviso, EN_PREPARACION
val ColorError = Color(0xFFB3261E)              // Destructivo, validación
val ContenedorError = Color(0xFFF9DEDC)         // RECHAZADO, NO_RECOGIDO
val Superficie = Color(0xFFF8FAF7)              // Fondo de pantalla
val ContenedorSuperficie = Color(0xFFECF0EB)    // Tarjetas secundarias, botón deshabilitado
val SobreSuperficie = Color(0xFF1B2328)         // Texto principal
val SobreSuperficieVariante = Color(0xFF766A63) // Texto secundario
val Contorno = Color(0xFF7D7169)                // Bordes de campo y botón contorneado
val ContornoVariante = Color(0xFFC9D3C7)        // Bordes de tarjeta, separadores

// Roles que Material 3 usa y B.1 no lista. Si no se definen, Material pone su morado por defecto.
// Salen de los textos de la tabla de estados (B.3).
val SobreContenedorSecundario = Color(0xFF1B1840) // Texto de ACEPTADO
val SobreTerciario = Color(0xFFFFFFFF)
val SobreContenedorTerciario = Color(0xFF3A2210)  // Texto de EN_PREPARACION
val SobreError = Color(0xFFFFFFFF)
val SobreContenedorError = Color(0xFF410E0B)      // Texto de RECHAZADO y NO_RECOGIDO

// B.4: el botón deshabilitado lleva este texto sobre ContenedorSuperficie, nunca solo opacidad.
val TextoDeshabilitado = Color(0xFF959E94)
