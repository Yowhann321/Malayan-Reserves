package com.example.malayanreserves.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.malayanreserves.UiState
import com.example.malayanreserves.data.ResStatus
import com.example.malayanreserves.data.Reservation
import com.example.malayanreserves.data.Rules
import com.example.malayanreserves.ui.theme.Blue
import com.example.malayanreserves.ui.theme.BlueSoft
import com.example.malayanreserves.ui.theme.Green
import com.example.malayanreserves.ui.theme.GreenSoft
import com.example.malayanreserves.ui.theme.Muted
import com.example.malayanreserves.ui.theme.Navy
import com.example.malayanreserves.ui.theme.Red
import com.example.malayanreserves.ui.theme.RedSoft
import com.example.malayanreserves.ui.theme.Yellow

@Composable
fun MineScreen(state: UiState, onFind: () -> Unit, onArrive: () -> Unit, onExtend: () -> Unit, onEnd: () -> Unit) {
    var confirmEnd by rememberSaveable { mutableStateOf(false) }
    val r = state.mine
    val now = state.now

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("My parking", style = MaterialTheme.typography.headlineSmall, color = Navy)
        Spacer(Modifier.height(12.dp))

        if (r == null) {
            Card {
                Icon(Icons.Filled.DirectionsCar, null, tint = Blue, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(8.dp))
                Text("No reservation yet", style = MaterialTheme.typography.titleMedium, color = Navy)
                Text("Pick a free slot on the map to park now, or book one for later in the week.", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onFind) { Text("Find a slot") }
            }
        } else {
            ReservationCard(r, now, state.busy, onArrive, onExtend, onEnd = { confirmEnd = true })
        }

        Spacer(Modifier.height(16.dp))
        Card {
            SectionLabel("How it works")
            Rule(Icons.Filled.Timer, "Arrive within ${Rules.ARRIVAL_WINDOW_MIN} minutes of your start time and tap “I've parked”, or the slot goes back to the pool.")
            Rule(Icons.Filled.Schedule, "Stay ${Rules.MIN_HOURS}–${Rules.MAX_HOURS} hours. You can extend once by an hour if nobody has booked after you.")
            Rule(Icons.Filled.EventAvailable, "One reservation at a time, up to ${Rules.DAYS_AHEAD} days ahead.")
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("History")
        if (state.history.isEmpty()) {
            Text("Your past reservations will show up here.", color = Muted, fontSize = 14.sp)
        } else {
            Card {
                state.history.forEachIndexed { i, h ->
                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    HistoryRow(h, now)
                }
            }
        }
    }

    if (confirmEnd && r != null) {
        val parked = r.status == ResStatus.ACTIVE
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text(if (parked) "Leave ${r.slotId}?" else "Cancel this reservation?") },
            text = {
                Text(
                    if (parked) "The slot will be marked free so the next driver can use it."
                    else "${r.slotId} on ${Fmt.whenLabel(r.start, r.end, now)} will be released."
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmEnd = false; onEnd() }) {
                    Text(if (parked) "Leave slot" else "Cancel reservation", color = Red)
                }
            },
            dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Keep it") } }
        )
    }
}

@Composable
private fun ReservationCard(r: Reservation, now: Long, busy: Boolean, onArrive: () -> Unit, onExtend: () -> Unit, onEnd: () -> Unit) {
    val arriveBy = r.start + Rules.ARRIVAL_WINDOW_MIN * Rules.MINUTE
    val (label, sub) = when (r.status) {
        ResStatus.UPCOMING -> "Upcoming" to "Starts in ${Fmt.span(r.start - now)}"
        ResStatus.HELD -> "Held for you" to "Arrive by ${Fmt.time(arriveBy)}"
        else -> "Parked" to "Until ${Fmt.time(r.end)}"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Navy, Color(0xFF1A3C8F))))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(label, Yellow, Navy)
            Spacer(Modifier.weight(1f))
            Text(sub, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(r.slotId, color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, lineHeight = 56.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.padding(bottom = 8.dp)) {
                Text(Fmt.day(r.start, now), color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(Fmt.range(r.start, r.end), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        when (r.status) {
            ResStatus.HELD -> {
                val left = arriveBy - now
                Countdown("Time to arrive", Fmt.clock(left), left.toFloat() / (Rules.ARRIVAL_WINDOW_MIN * Rules.MINUTE), Yellow)
            }
            ResStatus.ACTIVE -> {
                val left = r.end - now
                Countdown("Time left", Fmt.clock(left), left.toFloat() / (r.end - r.start).coerceAtLeast(1), Color(0xFF7FE3A8))
                if (left in 1..(15 * Rules.MINUTE)) {
                    Text("Your time is almost up. Extend or move your car.", color = Yellow, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
            else -> {
                Text(
                    "You can check in up to 15 minutes early.", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        val primary = ButtonDefaults.buttonColors(
            containerColor = Yellow, contentColor = Navy,
            disabledContainerColor = Color.White.copy(alpha = 0.12f), disabledContentColor = Color.White.copy(alpha = 0.6f)
        )
        val outline = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        val border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))
        when (r.status) {
            ResStatus.UPCOMING, ResStatus.HELD -> {
                val canArrive = r.status == ResStatus.HELD || r.start - now <= 15 * Rules.MINUTE
                Button(onClick = onArrive, enabled = canArrive && !busy, colors = primary, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text(if (canArrive) "I've parked" else "Check-in opens at ${Fmt.time(r.start - 15 * Rules.MINUTE)}", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onEnd, enabled = !busy, colors = outline, border = border, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel reservation")
                }
            }
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onExtend, enabled = !r.extended && !busy, colors = primary, modifier = Modifier.weight(1f).height(50.dp)) {
                        Text(if (r.extended) "Extended" else "Extend +1h", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onEnd, enabled = !busy, colors = outline, border = border, modifier = Modifier.weight(1f).height(50.dp)) {
                        Text("Leave slot")
                    }
                }
            }
        }
    }
}

@Composable
private fun Countdown(label: String, value: String, fraction: Float, color: Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
    Spacer(Modifier.height(8.dp))
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        color = color, trackColor = Color.White.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
        drawStopIndicator = {}
    )
}

@Composable
private fun Rule(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 5.dp)) {
        Icon(icon, null, tint = Blue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 14.sp)
    }
}

@Composable
private fun HistoryRow(r: Reservation, now: Long) {
    val (label, fg, bg) = when (r.status) {
        ResStatus.COMPLETED -> Triple("Completed", Green, GreenSoft)
        ResStatus.CANCELLED -> Triple("Cancelled", Muted, MaterialTheme.colorScheme.surfaceVariant)
        ResStatus.NO_SHOW -> Triple("No-show", Red, RedSoft)
        else -> Triple(r.status.name, Blue, BlueSoft)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(BlueSoft), contentAlignment = Alignment.Center) {
            Text(r.slotId, color = Navy, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(Fmt.day(r.start, now), fontWeight = FontWeight.SemiBold)
            Text(Fmt.range(r.start, r.endedAt?.takeIf { r.status == ResStatus.COMPLETED } ?: r.end), color = Muted, fontSize = 13.sp)
        }
        Pill(label, bg, fg)
    }
}

