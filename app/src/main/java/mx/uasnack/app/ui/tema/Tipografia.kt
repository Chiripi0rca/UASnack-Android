package mx.uasnack.app.ui.tema

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Fuentes del Anexo B (B.2): Figtree para toda la interfaz e IBM Plex Mono solo para folios,
// importes, PIN y horas.
// TODO: poner los .ttf en res/font/ (figtree_regular, figtree_medium, figtree_semibold,
// ibm_plex_mono_regular, ibm_plex_mono_medium) y cambiar estas dos líneas por
// FontFamily(Font(R.font.figtree_regular, FontWeight.Normal), ...). Mientras tanto se usa la del sistema.
val FuenteUi: FontFamily = FontFamily.Default
val FuenteDatos: FontFamily = FontFamily.Monospace

// Escala de B.2 traducida a los estilos de Material 3.
// Los estilos que no aparecen aquí conservan el valor por defecto de Material.
val Tipografia = Typography(
    // Headline: títulos de pantalla
    headlineMedium = TextStyle(fontFamily = FuenteUi, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    // Title grande
    titleLarge = TextStyle(fontFamily = FuenteUi, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    // Title
    titleMedium = TextStyle(fontFamily = FuenteUi, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    // Body grande: texto de campos y párrafos principales
    bodyLarge = TextStyle(fontFamily = FuenteUi, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    // Body
    bodyMedium = TextStyle(fontFamily = FuenteUi, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    // Texto de botones (B.4: label 14/600)
    labelLarge = TextStyle(fontFamily = FuenteUi, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    // Label: etiquetas pequeñas, con letterSpacing de .04em
    labelMedium = TextStyle(fontFamily = FuenteUi, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.04.em)
)

// Para datos que se leen carácter por carácter: folio, importe, PIN, horas y cuentas regresivas.
// Uso: Text(texto, style = EstiloDatos)
val EstiloDatos = TextStyle(fontFamily = FuenteDatos, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp)
