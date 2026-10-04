package com.example.malayanreserves.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.malayanreserves.UiState
import com.example.malayanreserves.data.Rules
import com.example.malayanreserves.data.Vehicle
import com.example.malayanreserves.ui.theme.Blue
import com.example.malayanreserves.ui.theme.BlueSoft
import com.example.malayanreserves.ui.theme.Muted
import com.example.malayanreserves.ui.theme.Navy
import com.example.malayanreserves.ui.theme.Red
import com.example.malayanreserves.ui.theme.Yellow

private val TEAM = listOf("Johann Lijauco", "Mikhail David", "Zei Ramirez", "Austine Ang", "Angelo Ebrada")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(state: UiState, isDemo: Boolean, onSaveVehicle: (Vehicle, () -> Unit) -> Unit, onLogout: () -> Unit) {
    val user = state.user ?: return
    var editing by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall, color = Navy)
        Spacer(Modifier.height(12.dp))
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Navy), contentAlignment = Alignment.Center) {
                    Text(
                        user.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() },
                        color = Yellow, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(user.name, style = MaterialTheme.typography.titleMedium, color = Navy)
                    Text(user.email, color = Muted, fontSize = 13.sp)
                }
                Pill(user.type.label, BlueSoft, Blue)
            }
            if (user.type.isStaff) {
                Text("You can use the faculty and staff slots P01–P03.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("Vehicle")
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CarIcon(user.vehicle)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("${user.vehicle.color} ${user.vehicle.type}", fontWeight = FontWeight.SemiBold)
                    Text(user.vehicle.plate, color = Muted, fontSize = 14.sp)
                }
                TextButton(onClick = { editing = true }) {
                    Icon(Icons.Filled.Edit, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("About")
        Card {
            Text(if (isDemo) "Demo mode" else "Connected to the parking server", fontWeight = FontWeight.SemiBold)
            Text(
                if (isDemo) "Accounts and reservations are saved on this phone. Other drivers on the map are simulated."
                else "Reservations are shared with every driver through the campus parking server.",
                color = Muted, fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            Text("Made by", fontWeight = FontWeight.SemiBold)
            Text(TEAM.joinToString(" · "), color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Text("A student project concept. Not an official Mapúa Malayan Colleges Laguna service.", color = Muted, fontSize = 12.sp)
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Red)
            Spacer(Modifier.width(8.dp))
            Text("Sign out", color = Red)
        }
        Spacer(Modifier.height(12.dp))
    }

    if (editing) {
        var type by rememberSaveable { mutableStateOf<String?>(user.vehicle.type) }
        var color by rememberSaveable { mutableStateOf<String?>(user.vehicle.color) }
        var plate by rememberSaveable { mutableStateOf(user.vehicle.plate) }
        val plateError = if (plate.isNotBlank() && !Rules.validPlate(plate)) "Use the format ABC 1234." else null
        ModalBottomSheet(
            onDismissRequest = { editing = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
                Text("Edit vehicle", style = MaterialTheme.typography.titleLarge, color = Navy)
                Spacer(Modifier.height(16.dp))
                VehicleForm(type, { type = it }, color, { color = it }, plate, { plate = it }, plateError)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onSaveVehicle(Vehicle(type!!, color!!, plate)) { editing = false } },
                    enabled = type != null && color != null && Rules.validPlate(plate) && !state.busy,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Save vehicle", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
