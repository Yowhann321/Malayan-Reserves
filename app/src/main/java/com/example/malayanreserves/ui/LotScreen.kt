package com.example.malayanreserves.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.malayanreserves.UiState
import com.example.malayanreserves.data.Booking
import com.example.malayanreserves.data.ResStatus
import com.example.malayanreserves.data.Rules
import com.example.malayanreserves.data.SlotState
import com.example.malayanreserves.data.SlotView
import com.example.malayanreserves.data.User
import com.example.malayanreserves.ui.theme.Amber
import com.example.malayanreserves.ui.theme.AmberSoft
import com.example.malayanreserves.ui.theme.Asphalt
import com.example.malayanreserves.ui.theme.AsphaltDark
import com.example.malayanreserves.ui.theme.Blue
import com.example.malayanreserves.ui.theme.BlueSoft
import com.example.malayanreserves.ui.theme.Green
import com.example.malayanreserves.ui.theme.GreenSoft
import com.example.malayanreserves.ui.theme.Line
import com.example.malayanreserves.ui.theme.Muted
import com.example.malayanreserves.ui.theme.Navy
import com.example.malayanreserves.ui.theme.Red
import com.example.malayanreserves.ui.theme.Yellow
import com.example.malayanreserves.ui.theme.carColor

@Composable
fun LotScreen(state: UiState, onOpenMine: () -> Unit, onReserve: (String, Long, Int, () -> Unit) -> Unit) {
    val lot = state.lot
    val user = state.user ?: return
    var openSlot by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("Hi, ${user.name.substringBefore(' ')}", color = Muted)
        Text("Campus parking", style = MaterialTheme.typography.headlineSmall, color = Navy)
        Spacer(Modifier.height(12.dp))

        if (lot == null) {
            Text("Loading the lot…", color = Muted)
            return@Column
        }
        val occupied = lot.slots.count { it.state == SlotState.OCCUPIED || it.state == SlotState.MINE }
        val held = lot.slots.count { it.state == SlotState.HELD }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("${lot.free}", "free", Green, GreenSoft, Modifier.weight(1f))
            Stat("$occupied", "parked", Navy, BlueSoft, Modifier.weight(1f))
            Stat("$held", "held", Amber, AmberSoft, Modifier.weight(1f))
        }

        state.mine?.let { r ->
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Blue)
                    .clickable(onClick = onOpenMine)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.LocalParking, null, tint = Yellow)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Your slot ${r.slotId}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        when (r.status) {
                            ResStatus.ACTIVE -> "Parked until ${Fmt.time(r.end)}"
                            ResStatus.HELD -> "Held for you. Arrive by ${Fmt.time(r.start + Rules.ARRIVAL_WINDOW_MIN * Rules.MINUTE)}"
                            else -> Fmt.whenLabel(r.start, r.end, state.now)
                        },
                        color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open", tint = Color.White)
            }
        }

        Spacer(Modifier.height(14.dp))
        LotMap(lot.slots, user) { openSlot = it }
        Spacer(Modifier.height(10.dp))
        Legend()
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap a slot to park now or book up to ${Rules.DAYS_AHEAD} days ahead. The map refreshes every 10 seconds.",
            color = Muted, fontSize = 13.sp
        )
    }

    val slot = openSlot?.let { id -> lot?.slots?.firstOrNull { it.slot.id == id } }
    if (slot != null) {
        SlotSheet(slot, user, state, onDismiss = { openSlot = null }, onReserve = { start, dur ->
            onReserve(slot.slot.id, start, dur) { openSlot = null }
        })
    }
}

@Composable
private fun Stat(value: String, label: String, fg: Color, bg: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(bg).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(value, color = fg, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = fg, fontSize = 13.sp)
    }
}

// ---------- the drawn lot ----------

@Composable
private fun LotMap(slots: List<SlotView>, user: User, onTap: (String) -> Unit) {
    val rows = slots.groupBy { it.slot.row }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Asphalt)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("▼ ENTRANCE", color = Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.width(8.dp))
            Hazard(Modifier.weight(1f).height(8.dp))
            Spacer(Modifier.width(8.dp))
            Text("GUARD", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        SlotRow(rows[0].orEmpty(), facingDown = true, user, onTap)
        Aisle()
        SlotRow(rows[1].orEmpty(), facingDown = false, user, onTap)
        Island()
        SlotRow(rows[2].orEmpty(), facingDown = true, user, onTap)
        Aisle()
        Box(Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF59606B)), contentAlignment = Alignment.Center) {
            Text("COVERED PARKING", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(4.dp))
        SlotRow(rows[3].orEmpty(), facingDown = false, user, onTap)
    }
}

@Composable
private fun Hazard(modifier: Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(2.dp))) {
        drawRect(Yellow)
        var x = -size.height
        while (x < size.width) {
            drawLine(Color(0xFF1B1F27), Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = size.height * 0.45f)
            x += size.height * 1.4f
        }
    }
}

@Composable
private fun Aisle() {
    Canvas(Modifier.fillMaxWidth().height(34.dp)) {
        val y = size.height / 2
        drawLine(Color.White.copy(alpha = 0.55f), Offset(0f, y), Offset(size.width, y), strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 18f)))
        // one-way arrows
        listOf(0.2f, 0.8f).forEach { fx ->
            val x = size.width * fx
            drawLine(Color.White.copy(alpha = 0.35f), Offset(x - 14f, y - 8f), Offset(x, y), strokeWidth = 4f)
            drawLine(Color.White.copy(alpha = 0.35f), Offset(x - 14f, y + 8f), Offset(x, y), strokeWidth = 4f)
        }
    }
}

@Composable
private fun Island() {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(16.dp).clip(RoundedCornerShape(8.dp)).background(Yellow).padding(3.dp)) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(Color(0xFF4E8F4A)))
    }
}

@Composable
private fun SlotRow(slots: List<SlotView>, facingDown: Boolean, user: User, onTap: (String) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        slots.forEach { SlotCell(it, facingDown, user, onTap) }
    }
}

@Composable
private fun RowScope.SlotCell(sv: SlotView, facingDown: Boolean, user: User, onTap: (String) -> Unit) {
    val locked = sv.slot.staffOnly && !user.type.isStaff
    val (stateText, accent) = when (sv.state) {
        SlotState.AVAILABLE -> (if (locked) "STAFF" else "FREE") to (if (locked) Color.White.copy(alpha = 0.5f) else Color(0xFF4ADE80))
        SlotState.HELD -> "HELD" to Yellow
        SlotState.OCCUPIED -> "" to Color.Transparent
        SlotState.MINE -> "YOU" to Color(0xFF7FB0FF)
    }
    val describe = buildString {
        append("Slot ${sv.slot.id}, ")
        append(when (sv.state) {
            SlotState.AVAILABLE -> if (locked) "staff only" else "available"
            SlotState.HELD -> "held"
            SlotState.OCCUPIED -> "occupied"
            SlotState.MINE -> "your slot"
        })
    }
    Box(
        Modifier
            .weight(1f)
            .height(104.dp)
            .clickable { onTap(sv.slot.id) }
            .semantics { contentDescription = describe }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val line = Color.White.copy(alpha = 0.85f)
            val w = size.width
            val h = size.height
            // stall lines: both sides and the back
            drawLine(line, Offset(1.5f, 0f), Offset(1.5f, h), strokeWidth = 3f)
            drawLine(line, Offset(w - 1.5f, 0f), Offset(w - 1.5f, h), strokeWidth = 3f)
            val backY = if (facingDown) 1.5f else h - 1.5f
            drawLine(line, Offset(0f, backY), Offset(w, backY), strokeWidth = 3f)
            if (sv.slot.staffOnly) {
                drawRect(Yellow.copy(alpha = 0.12f), Offset(4f, 4f), Size(w - 8f, h - 8f))
            }
            when (sv.state) {
                SlotState.AVAILABLE -> if (!locked) drawRoundRect(accent.copy(alpha = 0.16f), Offset(8f, 8f), Size(w - 16f, h - 16f), CornerRadius(14f))
                SlotState.HELD -> drawRoundRect(Yellow, Offset(8f, 8f), Size(w - 16f, h - 16f), CornerRadius(14f),
                    style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f))))
                SlotState.MINE -> drawRoundRect(Color(0xFF7FB0FF), Offset(6f, 6f), Size(w - 12f, h - 12f), CornerRadius(14f), style = Stroke(width = 5f))
                SlotState.OCCUPIED -> {}
            }
            sv.vehicle?.let { v ->
                // Keep the painted slot number visible: the car sits on the aisle side of the stall.
                val label = 24.dp.toPx()
                val carH = (h - label - 10.dp.toPx()).coerceAtMost(w * 0.5f * 1.85f)
                val carW = carH / 1.85f
                val top = if (facingDown) label + 2.dp.toPx() else h - label - 2.dp.toPx() - carH
                drawCar(carColor(v.color), Offset((w - carW) / 2, top), Size(carW, carH), pointingDown = facingDown)
            }
        }
        // painted slot number at the back of the stall; your own slot gets a blue tag
        val mine = sv.state == SlotState.MINE
        Text(
            if (mine) "${sv.slot.id} · YOU" else sv.slot.id, color = Color.White.copy(alpha = 0.9f),
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = if (mine) 11.sp else 12.sp,
            modifier = Modifier.align(if (facingDown) Alignment.TopCenter else Alignment.BottomCenter).padding(vertical = 5.dp)
                .then(if (mine) Modifier.clip(RoundedCornerShape(4.dp)).background(Blue).padding(horizontal = 5.dp, vertical = 1.dp) else Modifier)
        )
        if (stateText.isNotEmpty() && sv.vehicle == null) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                if (locked) Icon(Icons.Filled.Lock, null, tint = accent, modifier = Modifier.size(14.dp))
                Text(stateText, color = accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(Green, "Free")
        LegendDot(Yellow, "Held")
        LegendDot(Muted, "Parked")
        LegendDot(Blue, "Yours")
        LegendDot(Color(0xFFE8C766), "Staff")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 12.sp, color = Muted)
    }
}

// ---------- booking sheet ----------

private enum class Mode { NOW, LATER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotSheet(sv: SlotView, user: User, state: UiState, onDismiss: () -> Unit, onReserve: (Long, Int) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val now = state.now
    val locked = sv.slot.staffOnly && !user.type.isStaff
    val hasLive = state.mine != null
    val freeNow = sv.state == SlotState.AVAILABLE
    val nowDurations = (Rules.MIN_HOURS..Rules.MAX_HOURS).map { it * 60 }
        .filter { d -> sv.bookings.none { Rules.overlaps(now, now + d * Rules.MINUTE, it.start, it.end) } }
    val canParkNow = freeNow && nowDurations.isNotEmpty()

    var mode by remember(sv.slot.id) { mutableStateOf(if (canParkNow) Mode.NOW else Mode.LATER) }
    var duration by remember(sv.slot.id) { mutableIntStateOf(120) }
    val days = remember(Rules.startOfDay(now)) { Fmt.dayStarts(now) }
    var day by remember(sv.slot.id) { mutableLongStateOf(days.first()) }
    val starts = Rules.availableStarts(day, duration, sv.bookings, now)
    var start by remember(sv.slot.id) { mutableLongStateOf(0L) }
    // Default to 8 AM (or the next opening after it) rather than the first slot of the day.
    LaunchedEffect(day, duration, starts.size) {
        if (start !in starts) start = starts.firstOrNull { it >= day + 8 * 60 * Rules.MINUTE } ?: starts.firstOrNull() ?: 0L
    }
    LaunchedEffect(mode, canParkNow) {
        if (mode == Mode.NOW && duration !in nowDurations) duration = nowDurations.lastOrNull { it <= 120 } ?: nowDurations.firstOrNull() ?: 60
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Color.White) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Navy), contentAlignment = Alignment.Center) {
                    Text(sv.slot.id, color = Yellow, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Slot ${sv.slot.id}", style = MaterialTheme.typography.titleLarge, color = Navy)
                    Text(sv.slot.note, color = Muted, fontSize = 13.sp)
                }
                StatePill(sv, now)
            }
            sv.vehicle?.let { v ->
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CarIcon(v, Modifier.size(width = 22.dp, height = 38.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("${v.color} ${v.type} · ${v.plate}", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    sv.until?.let { Text("until ${Fmt.time(it)}", color = Muted, fontSize = 13.sp) }
                }
            }

            Spacer(Modifier.height(18.dp))
            when {
                locked -> Notice(Icons.Filled.Lock, "Faculty and staff only",
                    "Slots P01–P03 are kept for faculty and employees. Students can book any slot from P04 to P12.")
                hasLive -> Notice(Icons.Filled.LocalParking, "You already have a reservation",
                    "One reservation at a time keeps the lot fair. Finish or cancel ${state.mine!!.slotId} first.")
                else -> {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = mode == Mode.NOW, onClick = { mode = Mode.NOW }, enabled = canParkNow,
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text("Park now") }
                        SegmentedButton(
                            selected = mode == Mode.LATER, onClick = { mode = Mode.LATER },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text("Book later") }
                    }
                    if (!canParkNow) {
                        Text(
                            if (!freeNow) "This slot is in use right now. You can still book it for later."
                            else "Someone has booked this slot within the hour. Book a later time instead.",
                            color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    if (mode == Mode.LATER) {
                        SectionLabel("Day")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(days) { d ->
                                FilterChip(selected = d == day, onClick = { day = d }, label = { Text(Fmt.chipDay(d, now)) })
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    SectionLabel("How long")
                    ChoiceChips(
                        options = (Rules.MIN_HOURS..Rules.MAX_HOURS).map { it * 60 },
                        selected = duration, label = { Fmt.hours(it) }, onSelect = { duration = it },
                        enabled = { mode == Mode.LATER || it in nowDurations }
                    )

                    if (mode == Mode.LATER) {
                        Spacer(Modifier.height(12.dp))
                        SectionLabel("Start time")
                        if (starts.isEmpty()) {
                            Text("No ${Fmt.hours(duration)} openings left on this day. Try a shorter time or another day.", color = Muted, fontSize = 13.sp)
                        } else {
                            val timesState = rememberLazyListState()
                            LaunchedEffect(start, day) {
                                val i = starts.indexOf(start)
                                if (i >= 0) timesState.animateScrollToItem((i - 1).coerceAtLeast(0))
                            }
                            LazyRow(state = timesState, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(starts) { t ->
                                    FilterChip(selected = t == start, onClick = { start = t }, label = { Text(Fmt.time(t)) })
                                }
                            }
                        }
                        Text("Advance bookings run from 6 AM to 10 PM.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }

                    Spacer(Modifier.height(16.dp))
                    val selStart = if (mode == Mode.NOW) now else start
                    val selEnd = selStart + duration * Rules.MINUTE
                    SectionLabel("${Fmt.day(if (mode == Mode.NOW) now else day, now)} on ${sv.slot.id}")
                    Timeline(
                        dayStart = if (mode == Mode.NOW) Rules.startOfDay(now) else day,
                        bookings = sv.bookings, now = now,
                        selection = if (mode == Mode.LATER && start == 0L) null else selStart to selEnd
                    )

                    Spacer(Modifier.height(18.dp))
                    val ready = mode == Mode.NOW && canParkNow || mode == Mode.LATER && start != 0L
                    Button(
                        onClick = { onReserve(if (mode == Mode.NOW) now else start, duration) },
                        enabled = ready && !state.busy,
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        Text(
                            if (!ready) "Choose a time"
                            else if (mode == Mode.NOW) "Hold ${sv.slot.id} until ${Fmt.time(selEnd)}"
                            else "Book ${sv.slot.id} · ${Fmt.range(selStart, selEnd)}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        if (mode == Mode.NOW) "The slot is held for ${Rules.ARRIVAL_WINDOW_MIN} minutes. Tap “I've parked” when you arrive."
                        else "Check in within ${Rules.ARRIVAL_WINDOW_MIN} minutes of your start time, or the slot is released.",
                        color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatePill(sv: SlotView, now: Long) {
    when (sv.state) {
        SlotState.AVAILABLE -> {
            val next = sv.nextStart
            if (next != null && next - now < 3 * 60 * Rules.MINUTE) Pill("Free until ${Fmt.time(next)}", GreenSoft, Green)
            else Pill("Free", GreenSoft, Green)
        }
        SlotState.HELD -> Pill("Held", AmberSoft, Amber)
        SlotState.OCCUPIED -> Pill("Occupied", MaterialTheme.colorScheme.surfaceVariant, Muted)
        SlotState.MINE -> Pill("Yours", BlueSoft, Blue)
    }
}

@Composable
private fun Notice(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AmberSoft).padding(14.dp)) {
        Icon(icon, null, tint = Amber)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = Navy)
            Text(body, fontSize = 13.sp, color = Muted)
        }
    }
}

/** A 24-hour bar: grey for bookings, blue for the chosen time, red for now. */
@Composable
private fun Timeline(dayStart: Long, bookings: List<Booking>, now: Long, selection: Pair<Long, Long>?) {
    val dayMs = 24 * 60 * Rules.MINUTE
    Column {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GreenSoft)
                .border(1.dp, Line, RoundedCornerShape(8.dp))
        ) {
            fun x(t: Long) = ((t - dayStart).toFloat() / dayMs).coerceIn(0f, 1f) * size.width
            for (h in 1 until 24) {
                val hx = size.width * h / 24f
                drawLine(Color.Black.copy(alpha = if (h % 6 == 0) 0.15f else 0.05f), Offset(hx, 0f), Offset(hx, size.height))
            }
            if (now > dayStart) drawRect(Color.Black.copy(alpha = 0.06f), Offset.Zero, Size(x(now), size.height))
            bookings.filter { it.end > dayStart && it.start < dayStart + dayMs }.forEach {
                drawRect(if (it.mine) Blue.copy(alpha = 0.5f) else Color(0xFF9AA1AD), Offset(x(it.start), 0f), Size(x(it.end) - x(it.start), size.height))
            }
            selection?.let { (a, b) ->
                drawRoundRect(Blue, Offset(x(a), 3f), Size((x(b) - x(a)).coerceAtLeast(4f), size.height - 6f), CornerRadius(6f))
            }
            if (now in dayStart until dayStart + dayMs) {
                drawLine(Red, Offset(x(now), 0f), Offset(x(now), size.height), strokeWidth = 4f)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("12 AM", "6 AM", "12 PM", "6 PM", "12 AM").forEach { Text(it, fontSize = 10.sp, color = Muted) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
            LegendDot(Color(0xFF9AA1AD), "Booked")
            LegendDot(Blue, "Your pick")
            LegendDot(Red, "Now")
        }
    }
}
