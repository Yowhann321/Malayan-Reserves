package com.example.malayanreserves.ui

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.core.view.WindowCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.malayanreserves.data.Rules
import com.example.malayanreserves.data.Vehicle
import com.example.malayanreserves.ui.theme.Line
import com.example.malayanreserves.ui.theme.Muted
import com.example.malayanreserves.ui.theme.Navy
import com.example.malayanreserves.ui.theme.carColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ---------- formatting ----------

object Fmt {
    private val time = SimpleDateFormat("h:mm a", Locale.US)
    private val day = SimpleDateFormat("EEE, MMM d", Locale.US)
    private val shortDay = SimpleDateFormat("EEE d", Locale.US)

    fun time(t: Long): String = time.format(Date(t))
    fun range(a: Long, b: Long): String = "${time(a)} – ${time(b)}"

    fun day(t: Long, now: Long): String {
        val diff = ((Rules.startOfDay(t) - Rules.startOfDay(now)) / (24 * 60 * Rules.MINUTE)).toInt()
        return when (diff) {
            0 -> "Today"
            1 -> "Tomorrow"
            -1 -> "Yesterday"
            else -> day.format(Date(t))
        }
    }

    fun chipDay(t: Long, now: Long): String = when (val d = day(t, now)) {
        "Today", "Tomorrow" -> d
        else -> shortDay.format(Date(t))
    }

    fun whenLabel(start: Long, end: Long, now: Long) = "${day(start, now)}, ${range(start, end)}"

    /** "1h 05m", "12m" or "45s". */
    fun span(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        return when {
            h > 0 -> "${h}h ${m.toString().padStart(2, '0')}m"
            m > 0 -> "${m}m"
            else -> "${s}s"
        }
    }

    /** "1:05:09" or "14:32". */
    fun clock(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}"
        else "$m:${sec.toString().padStart(2, '0')}"
    }

    fun hours(min: Int): String = if (min % 60 == 0) "${min / 60}h" else "${min / 60}h ${min % 60}m"

    fun dayStarts(now: Long): List<Long> {
        val first = Rules.startOfDay(now)
        return (0 until Rules.DAYS_AHEAD).map { i ->
            Calendar.getInstance().apply { timeInMillis = first; add(Calendar.DAY_OF_MONTH, i) }.timeInMillis
        }
    }
}

/** Shows text in capitals without changing what the keyboard typed (rewriting the value confuses some keyboards). */
object UppercaseTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString) = TransformedText(AnnotatedString(text.text.uppercase()), OffsetMapping.Identity)
}

/** Light status bar icons on dark screens, dark icons on light ones. */
@Composable
fun StatusBarIcons(light: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !light
    }
}

// ---------- drawing ----------

/** A top-down car, pointing up, in the owner's car color. */
fun DrawScope.drawCar(color: Color, topLeft: Offset, size: Size, pointingDown: Boolean = false) {
    val w = size.width
    val h = size.height
    rotate(if (pointingDown) 180f else 0f, pivot = Offset(topLeft.x + w / 2, topLeft.y + h / 2)) {
        val x = topLeft.x
        val y = topLeft.y
        val dark = Color(0xFF1B1F27)
        // shadow
        drawRoundRect(Color.Black.copy(alpha = 0.25f), Offset(x + w * 0.06f, y + h * 0.04f), Size(w * 0.92f, h * 0.98f), CornerRadius(w * 0.3f))
        // wheels
        listOf(0.16f, 0.72f).forEach { fy ->
            drawRoundRect(dark, Offset(x - w * 0.02f, y + h * fy), Size(w * 0.14f, h * 0.14f), CornerRadius(w * 0.05f))
            drawRoundRect(dark, Offset(x + w * 0.88f, y + h * fy), Size(w * 0.14f, h * 0.14f), CornerRadius(w * 0.05f))
        }
        // body
        drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(w * 0.3f))
        // windshield, roof, rear window
        val glass = Color(0xFF233142)
        drawRoundRect(glass, Offset(x + w * 0.14f, y + h * 0.22f), Size(w * 0.72f, h * 0.16f), CornerRadius(w * 0.14f))
        drawRoundRect(color.copy(alpha = 1f).let { lerpDark(it) }, Offset(x + w * 0.16f, y + h * 0.40f), Size(w * 0.68f, h * 0.28f), CornerRadius(w * 0.12f))
        drawRoundRect(glass, Offset(x + w * 0.18f, y + h * 0.70f), Size(w * 0.64f, h * 0.12f), CornerRadius(w * 0.12f))
        // headlights
        drawRoundRect(Color(0xFFFFF3B0), Offset(x + w * 0.12f, y + h * 0.02f), Size(w * 0.2f, h * 0.05f), CornerRadius(w * 0.05f))
        drawRoundRect(Color(0xFFFFF3B0), Offset(x + w * 0.68f, y + h * 0.02f), Size(w * 0.2f, h * 0.05f), CornerRadius(w * 0.05f))
        // tail lights
        drawRoundRect(Color(0xFFE0453A), Offset(x + w * 0.1f, y + h * 0.93f), Size(w * 0.2f, h * 0.045f), CornerRadius(w * 0.05f))
        drawRoundRect(Color(0xFFE0453A), Offset(x + w * 0.7f, y + h * 0.93f), Size(w * 0.2f, h * 0.045f), CornerRadius(w * 0.05f))
    }
}

private fun lerpDark(c: Color) = Color(c.red * 0.85f, c.green * 0.85f, c.blue * 0.85f, 1f)

@Composable
fun CarIcon(vehicle: Vehicle, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 34.dp, height = 58.dp)) {
        drawCar(carColor(vehicle.color), Offset(size.width * 0.08f, 0f), Size(size.width * 0.84f, size.height))
    }
}

// ---------- small building blocks ----------

@Composable
fun Pill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Text(
        text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line),
        content = { Column(Modifier.padding(16.dp)) { content() } }
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<T>, selected: T?, label: (T) -> String, onSelect: (T) -> Unit, enabled: (T) -> Boolean = { true }) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        options.forEach { o ->
            FilterChip(
                selected = o == selected,
                onClick = { onSelect(o) },
                enabled = enabled(o),
                label = { Text(label(o)) },
                leadingIcon = if (o == selected) ({ Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }) else null
            )
        }
    }
}

/** Vehicle type, color and plate. Used when registering and when editing the vehicle. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleForm(
    type: String?, onType: (String) -> Unit,
    color: String?, onColor: (String) -> Unit,
    plate: String, onPlate: (String) -> Unit,
    plateError: String?
) {
    SectionLabel("Vehicle type")
    ChoiceChips(Rules.VEHICLE_TYPES, type, { it }, onType)
    Spacer(Modifier.height(16.dp))
    SectionLabel("Color" + (color?.let { " · $it" } ?: ""))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Rules.VEHICLE_COLORS.forEach { c ->
            val selected = c == color
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(carColor(c))
                    .border(if (selected) 3.dp else 1.dp, if (selected) Navy else Line, CircleShape)
                    .clickable { onColor(c) },
                contentAlignment = Alignment.Center
            ) {
                if (selected) Icon(Icons.Filled.Check, "Selected", tint = if (c in listOf("White", "Silver", "Yellow")) Navy else Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = plate,
        onValueChange = { onPlate(it.take(9)) },
        visualTransformation = UppercaseTransformation,
        label = { Text("Plate number") },
        placeholder = { Text("ABC 1234") },
        singleLine = true,
        isError = plateError != null,
        supportingText = { Text(plateError ?: "Other drivers only see the last two digits.") },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        modifier = Modifier.fillMaxWidth()
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        if (type != null && color != null) {
            CarIcon(Vehicle(type, color, plate))
            Spacer(Modifier.size(12.dp))
            Column {
                Text("$color $type", fontWeight = FontWeight.SemiBold)
                Text(Rules.normalizePlate(plate).ifBlank { "No plate yet" }, color = Muted, fontSize = 13.sp)
            }
        }
    }
}
