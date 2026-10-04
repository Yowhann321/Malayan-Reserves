package com.example.malayanreserves.ui.theme

import androidx.compose.ui.graphics.Color

val Blue = Color(0xFF1449B8)
val BlueSoft = Color(0xFFE6EDFB)
val Navy = Color(0xFF0B1F4D)
val Yellow = Color(0xFFFFC72C)
val YellowSoft = Color(0xFFFFF4D1)
val Paper = Color(0xFFF4F6FA)
val Ink = Color(0xFF111827)
val Muted = Color(0xFF5B6475)
val Line = Color(0xFFDDE2EB)
val Asphalt = Color(0xFF3A3F48)
val AsphaltDark = Color(0xFF2C3038)
val Green = Color(0xFF1E9E5A)
val GreenSoft = Color(0xFFE3F5EA)
val Amber = Color(0xFFD98A00)
val AmberSoft = Color(0xFFFFF1D6)
val Red = Color(0xFFD03B3B)
val RedSoft = Color(0xFFFBE7E7)

/** Body colors used to draw each driver's car on the lot. */
fun carColor(name: String): Color = when (name) {
    "White" -> Color(0xFFF3F4F6)
    "Black" -> Color(0xFF1F2328)
    "Silver" -> Color(0xFFB8BEC8)
    "Gray" -> Color(0xFF7A808A)
    "Red" -> Color(0xFFC8352E)
    "Blue" -> Color(0xFF2D63D8)
    "Green" -> Color(0xFF2F8A55)
    "Yellow" -> Color(0xFFF2C230)
    "Orange" -> Color(0xFFE5772A)
    "Brown" -> Color(0xFF7B5236)
    else -> Color(0xFF9AA1AD)
}
