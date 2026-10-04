package com.example.malayanreserves
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.malayanreserves.ui.theme.MalayanReservesTheme
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lint.kotlin.metadata.Visibility
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.TextButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import okhttp3.FormBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import okhttp3.OkHttpClient
import okhttp3.Request



// To hold Values for Registration
data class RegistrationData(
    var email: String = "",
    var password: String = "",
    var userType: String = "",
    var carType: String = "",
    var carColor: String = "",
    var plate: String = ""
)


//Just a list to access the different windows easier
enum class Screen {
    Login,
    RegisterStep1,
    RegisterStep2,
    Parking,
    UserDetails
}

//Main Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MalayanReservesTheme {
                AppEntryPoint()
            }
        }
    }
}



//Control that allows the app to start with the start screen
@Composable
fun AppEntryPoint() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(Screen.Login) }
    val registrationData = remember { mutableStateOf(RegistrationData()) }
    val loggedInEmail = remember { mutableStateOf("") }
    val reservedSlots = remember { mutableStateListOf<String>() } // Reserve slot tracker
    var showLoginDialog by remember { mutableStateOf(false) }

    LaunchedEffect(loggedInEmail.value) {
        if (loggedInEmail.value.isNotEmpty()) {
            val updatedReserved = fetchUsedParkingSlots()
            reservedSlots.clear()
            reservedSlots.addAll(updatedReserved)
        }
    }

    when (currentScreen) {
        Screen.Login -> {
            loginscreen(
                onLogin = { showLoginDialog = true },
                onRegister = { currentScreen = Screen.RegisterStep1 }
            )

            if (showLoginDialog) {
                LoginDialog(
                    onDismiss = { showLoginDialog = false },
                    onLoginConfirmed = { email, password ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val result = loginUser(email, password)
                            withContext(Dispatchers.Main) {
                                if (result.contains("success", ignoreCase = true)) {
                                    loggedInEmail.value = email
                                    currentScreen = Screen.Parking
                                } else {
                                    Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                                }
                                showLoginDialog = false
                            }
                        }
                    }
                )
            }
        }

        Screen.RegisterStep1 -> RegistrationScreenStep1(
            registrationData = registrationData.value,
            onNext = { currentScreen = Screen.RegisterStep2 }
        )

        Screen.RegisterStep2 -> RegistrationScreenStep2(
            registrationData = registrationData.value,
            onFinish = { currentScreen = Screen.Login }
        )

        Screen.Parking -> ParkingLayout(
            userEmail = loggedInEmail.value,
            reservedSlots = reservedSlots,
            onReserved = { slot -> reservedSlots.add(slot) },
            onNavigateToUser = { currentScreen = Screen.UserDetails },
            onNavigateToHome = { currentScreen = Screen.Parking }
        )

        Screen.UserDetails -> UserDetailsScreen(
            email = loggedInEmail.value,
            onBack = { currentScreen = Screen.Parking }
        )
    }
}




//The Main Log in Screen
@Composable
fun loginscreen(onLogin: () -> Unit, onRegister: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp))
        {
            // Image and login/register buttons...
            Image(
                painter = painterResource(id = R.drawable.malayan),
                contentDescription = "App logo",
                modifier = Modifier
                    .width(300.dp)
                    .height(300.dp),
                contentScale = ContentScale.Crop
            )

            Button(onClick = onLogin, modifier = Modifier
                .width(300.dp)
                .height(60.dp) ,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00008B))
            )
            {

                Text("Login", fontSize = 18.sp)
            }

            Button(onClick = onRegister, modifier = Modifier
                .width(300.dp)
                .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00008B))
            )
            {
                Text("Register", fontSize = 18.sp)
            }
        }
    }
}


//Input email password
@Composable
fun RegistrationScreenStep1(
    registrationData: RegistrationData,
    onNext: () -> Unit
) {
    var email by remember { mutableStateOf(registrationData.email) }
    var userType by remember { mutableStateOf(registrationData.userType) }
    var password by remember { mutableStateOf(registrationData.password) }
    var confirmPassword by remember { mutableStateOf("") }

    val userTypes = listOf("Student", "Faculty", "Employee")
    var expanded by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val trimmedEmail = email.trim()
    val trimmedPassword = password.trim()
    val trimmedConfirmPassword = confirmPassword.trim()
    val isPasswordMatch = trimmedPassword == trimmedConfirmPassword

    val isValid = trimmedEmail.endsWith("@live.mcl.edu.ph") &&
            isPasswordMatch && trimmedPassword.isNotBlank()

    val passwordStrength = when {
        trimmedPassword.length >= 10 && trimmedPassword.any { it.isDigit() } && trimmedPassword.any { it.isUpperCase() } -> "Strong"
        trimmedPassword.length >= 6 -> "Medium"
        else -> "Weak"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Register - Step 1", fontSize = 22.sp)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = userType,
                onValueChange = {},
                readOnly = true,
                label = { Text("User Type") },
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                userTypes.forEach { type ->
                    DropdownMenuItem(
                        onClick = {
                            userType = type
                            expanded = false
                        },
                        text = { Text(type) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = icon, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (password.isNotBlank()) {
            Text(
                text = "Strength: $passwordStrength",
                color = when (passwordStrength) {
                    "Strong" -> Color(0xFF2E7D32)
                    "Medium" -> Color(0xFFF9A825)
                    else -> Color.Red
                },
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(imageVector = icon, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (confirmPassword.isNotBlank() && !isPasswordMatch) {
            Text("Passwords do not match", color = Color.Red, modifier = Modifier.align(Alignment.Start))
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                // Save to shared state
                registrationData.email = email.trim()
                registrationData.password = password.trim()
                registrationData.userType = userType
                onNext()
            },
            enabled = isValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
        }
    }
}





//The part 2 of the Registration, asks the user for their car details
@Composable
fun RegistrationScreenStep2(
    registrationData: RegistrationData,
    onFinish: () -> Unit
) {
    var carType by remember { mutableStateOf("") }
    var carColor by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val isValid = carColor.isNotBlank() && licensePlate.isNotBlank()
    val carTypes = listOf("Sedan", "SUV", "Compact", "Coupe", "Van")

    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Register - Step 2", fontSize = 22.sp)

        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = carType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Car Type") },
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                carTypes.forEach { type ->
                    DropdownMenuItem(
                        onClick = {
                            carType = type
                            expanded = false
                        },
                        text = { Text(type) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = carColor,
            onValueChange = { carColor = it },
            label = { Text("Car Color") }
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = licensePlate,
            onValueChange = { licensePlate = it },
            label = { Text("License Plate") }
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                registrationData.carType = carType
                registrationData.carColor = carColor
                registrationData.plate = licensePlate

                // Call PHP backend
                CoroutineScope(Dispatchers.IO).launch {
                    val result = registerUser(
                        email = registrationData.email,
                        password = registrationData.password,
                        userType = registrationData.userType,
                        carType = registrationData.carType,
                        carColor = registrationData.carColor,
                        plate = registrationData.plate
                    )
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                        if (result.contains("inserted")) {
                            onFinish()
                        }
                    }
                }
            },
            enabled = isValid,
        ) {
            Text("Finish")
        }
    }
}



// main Parking Layout
@Composable
fun ParkingLayout(
    userEmail: String,
    reservedSlots: List<String>,
    onReserved: (String) -> Unit,
    onNavigateToUser: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 3f)
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .background(Color(0xFF666460)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF666460)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RoadDivider()
                ParkingRowWithCurbs(userEmail, reservedSlots, onReserved, "P01", "P02", "P03")
                ParkingRowWithCurbs(userEmail, reservedSlots, onReserved, "P04", "P05", "P06")
                RoadDivider()
                ParkingRowWithCurbs(userEmail, reservedSlots, onReserved, "P07", "P08", "P09")
                ParkingRowWithCurbs(userEmail, reservedSlots, onReserved, "P10", "P11", "P12")
                RoadDivider()
            }

            BottomAppBar(
                containerColor = Color.DarkGray
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = onNavigateToHome) {
                        Text("Home")
                    }
                    Button(onClick = onNavigateToUser) {
                        Text("User")
                    }
                }
            }
        }
    }
}



//Combination of the curbs and the Parking spots
@Composable
fun ParkingRowWithCurbs(
    userEmail: String,
    reservedSlots: List<String>,
    onReserved: (String) -> Unit,
    vararg labels: String
) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CurbBox()
        labels.forEach { slotLabel ->
            TheButton(
                txt = slotLabel,
                userEmail = userEmail,
                isReserved = reservedSlots.contains(slotLabel),
                onReserved = onReserved
            )
        }
        CurbBox()
    }
}



//The Detail Screen

@Composable
fun UserDetailsScreen(email: String, onBack: () -> Unit) {
    var userDetails by remember { mutableStateOf<UserDetails?>(null) }

    LaunchedEffect(email) {
        userDetails = withContext(Dispatchers.IO) {
            fetchUserDetails(email)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (userDetails != null) {
                Text(
                    text = "User Email: $email",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "User Type: ${userDetails!!.userType}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Car Type: ${userDetails!!.carType}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Car Color: ${userDetails!!.carColor}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "License Plate: ${userDetails!!.licensePlate}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            } else {
                Text(
                    text = "Loading or user not found...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Red
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Back", fontWeight = FontWeight.Bold)
            }
        }
    }
}

//For user Log in
@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    onLoginConfirmed: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Login") },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton (onClick = { onLoginConfirmed(email, password) }) {
                Text("Login")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


//The Buttons or The Parking Spots
@Composable
fun TheButton(
    txt: String,
    userEmail: String,
    isReserved: Boolean,
    onReserved: (String) -> Unit
) {
    val mContext = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var reservedDetails by remember { mutableStateOf<UserDetails?>(null) }

    Button(
        onClick = {
            if (isReserved) {
                CoroutineScope(Dispatchers.IO).launch {
                    val details = fetchSlotDetails(txt)
                    withContext(Dispatchers.Main) {
                        reservedDetails = details
                        showDetailsDialog = true
                    }
                }
            } else {
                showDialog = true
            }
        },
        enabled = !isReserved || true, // allow click even if reserved to show popup
        shape = RoundedCornerShape(5.dp),
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.White,
            containerColor = if (isReserved) Color.Gray else Color.Transparent
        ),
        modifier = Modifier
            .border(width = 3.dp, color = Color.Yellow)
            .shadow(8.dp, RoundedCornerShape(3.dp))
            .height(150.dp)
            .width(80.dp)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = AnnotatedString(txt),
            fontSize = 14.sp
        )
    }

    if (showDialog) {
        ReservationDialog(
            slotName = txt,
            userEmail = userEmail,
            onDismiss = { showDialog = false },
            onReserve = {
                onReserved(txt)
                showDialog = false
            }
        )
    }

    if (showDetailsDialog && reservedDetails != null) {
        SlotDetailsDialog(details = reservedDetails!!, onDismiss = { showDetailsDialog = false })
    }
}


//The Road
@Composable
fun RoadDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(83.dp)
            .shadow(5.dp)
            .padding(10.dp)
            .background(Color(0xFF33312B)),

        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically

        ) {
            repeat(6) {
                Spacer(
                    modifier = Modifier
                        .width(30.dp)
                        .height(6.dp)
                        .background(Color.White, RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

//The Curbs on the both sides of parking spots
@Composable
fun CurbBox() {
    Box(
        modifier = Modifier
            .height(150.dp)
            .width(25.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Blue)
            .border(
                width = 3.dp,
                color = Color.White
            )
    )
}


//Function to Insert user into the database
suspend fun registerUser(
    email: String,
    password: String,
    userType: String,
    carType: String,
    carColor: String,
    plate: String
): String = withContext(Dispatchers.IO) {
    val baseUrl = "http://192.168.1.13/IT123P/REST/add_record.php"
    val params = "email=${URLEncoder.encode(email, "UTF-8")}" +
            "&password=${URLEncoder.encode(password, "UTF-8")}" +
            "&user_type=${URLEncoder.encode(userType, "UTF-8")}" +
            "&car_type=${URLEncoder.encode(carType, "UTF-8")}" +
            "&car_color=${URLEncoder.encode(carColor, "UTF-8")}" +
            "&plate=${URLEncoder.encode(plate, "UTF-8")}"

    val url = URL("$baseUrl?$params")
    val conn = url.openConnection() as HttpURLConnection

    return@withContext try {
        conn.requestMethod = "GET"
        conn.connect()

        val responseCode = conn.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            "Error: $responseCode"
        }
    } catch (e: Exception) {
        "Exception: ${e.message}"
    } finally {
        conn.disconnect()
    }
}

//Used for Login in checks if email and password are in database
suspend fun loginUser(email: String, password: String): String = withContext(Dispatchers.IO) {
    val baseUrl = "http://192.168.1.13/IT123P/REST/login.php"
    val params = "email=${URLEncoder.encode(email, "UTF-8")}" +
            "&password=${URLEncoder.encode(password, "UTF-8")}"

    val url = URL("$baseUrl?$params")
    val conn = url.openConnection() as HttpURLConnection

    return@withContext try {
        conn.requestMethod = "GET"
        conn.connect()

        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            "Error: ${conn.responseCode}"
        }
    } catch (e: Exception) {
        "Exception: ${e.message}"
    } finally {
        conn.disconnect()
    }
}


//This part of the code is for getting the user details
data class UserDetails(
    val userType: String = "",
    val carType: String = "",
    val carColor: String = "",
    val licensePlate: String = ""
)


suspend fun fetchUserDetails(email: String): UserDetails? {
    return try {
        val url = "http://192.168.1.13/IT123P/REST/user_details.php?email=${URLEncoder.encode(email, "UTF-8")}"
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        val response = connection.inputStream.bufferedReader().use { it.readText() }

        val json = JSONObject(response)
        if (json.has("error")) null
        else UserDetails(
            userType = json.getString("usertype"),
            carType = json.getString("cartype"),
            carColor = json.getString("carcolor"),
            licensePlate = json.getString("plate")
        )
    } catch (e: Exception) {
        Log.e("UserDetailsFetch", "Exception: ${e.message}")
        null
    }
}

//For making reservation
@Composable
fun ReservationDialog(
    slotName: String,
    userEmail: String,
    onDismiss: () -> Unit,
    onReserve: () -> Unit
) {
    val context = LocalContext.current
    var pickedDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var pickedTime by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reserve $slotName") },
        text = {
            Column {
                OutlinedTextField(
                    value = pickedDate,
                    onValueChange = { pickedDate = it },
                    label = { Text("Date (yyyy-MM-dd)") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pickedTime,
                    onValueChange = { pickedTime = it },
                    label = { Text("Time (HH:mm)") },
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val now = LocalDateTime.now()
                        CoroutineScope(Dispatchers.IO).launch {
                            val result = reserveSlot(
                                email = userEmail,
                                slotName = slotName,
                                resDate = now.toLocalDate().toString(),
                                time = now.toLocalTime().withSecond(0).withNano(0).toString(),
                                mode = "now"
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                                onReserve()
                            }
                        }
                    }) { Text("Reserve Now") }

                    Button(onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            val result = reserveSlot(
                                email = userEmail,
                                slotName = slotName,
                                resDate = pickedDate,
                                time = pickedTime,
                                mode = "later"
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                                onReserve()
                            }
                        }
                    }) { Text("Reserve") }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


suspend fun reserveSlot(
    email: String,
    slotName: String,
    resDate: String,
    time: String,
    mode: String // "now" or "later"
): String = withContext(Dispatchers.IO) {
    val client = OkHttpClient()

    val formBody = FormBody.Builder()
        .add("email", email)
        .add("slotname", slotName)
        .add("resdate", resDate)
        .add("time", time)
        .add("mode", mode)
        .build()

    val request = Request.Builder()
        .url("http://192.168.1.13/IT123P/REST/reserve_spot.php") // replace with your actual URL
        .post(formBody)
        .build()

    try {
        val response = client.newCall(request).execute()
        response.body?.string() ?: "error: empty response"
    } catch (e: Exception) {
        e.printStackTrace()
        "error: ${e.message}"
    }
}


suspend fun fetchUsedParkingSlots(): List<String> = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("http://192.168.1.13/IT123P/REST/get_used_parking.php")
        .build()

    try {
        val response = client.newCall(request).execute()
        val body = response.body?.string()
        if (!body.isNullOrEmpty()) {
            val jsonArray = JSONArray(body)
            val slots = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                slots.add(jsonArray.getString(i))
            }
            slots
        } else emptyList()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

//gets the detail of the user parked in the parking spot
suspend fun fetchSlotDetails(slotName: String): UserDetails? {
    return try {
        val url = "http://192.168.1.13/IT123P/REST/get_slot_details.php?slotname=${URLEncoder.encode(slotName, "UTF-8")}"
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        val response = connection.inputStream.bufferedReader().use { it.readText() }

        val json = JSONObject(response)
        if (json.has("error")) null
        else UserDetails(
            userType = json.getString("usertype"),
            carType = json.getString("cartype"),
            carColor = json.getString("carcolor"),
            licensePlate = json.getString("plate")
        )
    } catch (e: Exception) {
        Log.e("FetchSlotDetails", "Exception: ${e.message}")
        null
    }
}

@Composable
fun SlotDetailsDialog(
    details: UserDetails,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reserved Slot Details") },
        text = {
            Column {
                Text("Car Type: ${details.carType}")
                Text("Car Color: ${details.carColor}")
                Text("License Plate: ${details.licensePlate}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}