package com.example.malayanreserves.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.malayanreserves.R
import com.example.malayanreserves.data.Registration
import com.example.malayanreserves.data.Rules
import com.example.malayanreserves.data.UserType
import com.example.malayanreserves.data.Vehicle
import com.example.malayanreserves.ui.theme.Amber
import com.example.malayanreserves.ui.theme.Green
import com.example.malayanreserves.ui.theme.Muted
import com.example.malayanreserves.ui.theme.Navy
import com.example.malayanreserves.ui.theme.Red
import com.example.malayanreserves.ui.theme.Yellow

private enum class AuthPage { WELCOME, LOGIN, REGISTER }

@Composable
fun AuthFlow(
    isDemo: Boolean,
    busy: Boolean,
    onLogin: (String, String) -> Unit,
    onDemo: () -> Unit,
    onRegister: (Registration) -> Unit
) {
    var page by rememberSaveable { mutableStateOf(AuthPage.WELCOME) }
    when (page) {
        AuthPage.WELCOME -> Welcome(isDemo, busy, { page = AuthPage.LOGIN }, { page = AuthPage.REGISTER }, onDemo)
        AuthPage.LOGIN -> Login(busy, onBack = { page = AuthPage.WELCOME }, onLogin = onLogin, onRegister = { page = AuthPage.REGISTER })
        AuthPage.REGISTER -> Register(busy, onBack = { page = AuthPage.WELCOME }, onRegister = onRegister, onLogin = { page = AuthPage.LOGIN })
    }
}

@Composable
private fun Welcome(isDemo: Boolean, busy: Boolean, onLogin: () -> Unit, onRegister: () -> Unit, onDemo: () -> Unit) {
    StatusBarIcons(light = true)
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy, Color(0xFF14337A))))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Spacer(Modifier.weight(1f))
        Image(
            painterResource(R.drawable.malayan), contentDescription = "Malayan Reserves logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(132.dp).clip(RoundedCornerShape(28.dp)).align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(28.dp))
        Text(
            "Malayan Reserves", color = Color.White, style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Reserve a campus parking slot before you arrive, and skip the circling.",
            color = Color.White.copy(alpha = 0.78f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            Pill("12 slots", Color.White.copy(alpha = 0.12f), Color.White)
            Pill("Live map", Color.White.copy(alpha = 0.12f), Color.White)
            Pill("Book 7 days ahead", Color.White.copy(alpha = 0.12f), Color.White)
        }
        Spacer(Modifier.weight(1.2f))
        Button(
            onClick = onLogin, modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy)
        ) { Text("Sign in", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onRegister, modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
        ) { Text("Create an account") }
        if (isDemo) {
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onDemo, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Explore with the demo account", color = Yellow)
            }
            Text(
                "Demo mode: everything is stored on this phone, with simulated drivers.",
                color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AuthScaffold(title: String, subtitle: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(onBack = onBack)
    StatusBarIcons(light = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
        }
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Navy)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Muted)
        Spacer(Modifier.height(24.dp))
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String = "Password", imeAction: ImeAction = ImeAction.Done) {
    var show by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        trailingIcon = {
            IconButton(onClick = { show = !show }) {
                Icon(if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (show) "Hide password" else "Show password")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, busy: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled && !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
        else Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Login(busy: Boolean, onBack: () -> Unit, onLogin: (String, String) -> Unit, onRegister: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    AuthScaffold("Welcome back", "Sign in with your school email.", onBack) {
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("School email") },
            placeholder = { Text("name${Rules.EMAIL_DOMAIN}") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        PasswordField(password, { password = it })
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Sign in", email.isNotBlank() && password.isNotBlank(), busy) { onLogin(email, password) }
        TextButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) { Text("New here? Create an account") }
    }
}

@Composable
private fun Register(busy: Boolean, onBack: () -> Unit, onRegister: (Registration) -> Unit, onLogin: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(UserType.STUDENT) }
    var vType by rememberSaveable { mutableStateOf<String?>(null) }
    var vColor by rememberSaveable { mutableStateOf<String?>(null) }
    var plate by rememberSaveable { mutableStateOf("") }
    var tried by rememberSaveable { mutableStateOf(false) }

    val emailError = when {
        email.isBlank() -> if (tried) "Enter your school email." else null
        !Rules.validEmail(email) -> "Use your school email ending in ${Rules.EMAIL_DOMAIN}."
        else -> null
    }
    val passwordError = when {
        password.isNotEmpty() && password.length < 8 -> "At least 8 characters."
        confirm.isNotEmpty() && confirm != password -> "Passwords don't match."
        else -> null
    }
    val step1Ok = name.trim().length >= 2 && Rules.validEmail(email) && password.length >= 8 && confirm == password
    val plateError = if (plate.isNotBlank() && !Rules.validPlate(plate)) "Use the format ABC 1234." else null
    val step2Ok = vType != null && vColor != null && Rules.validPlate(plate)

    if (step == 1) {
        AuthScaffold("Create your account", "Step 1 of 2 · About you", onBack) {
            LinearProgressIndicator(progress = { 0.5f }, modifier = Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.outline, drawStopIndicator = {})
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(60) }, label = { Text("Full name") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text("School email") },
                placeholder = { Text("name${Rules.EMAIL_DOMAIN}") }, singleLine = true, isError = emailError != null,
                supportingText = emailError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            PasswordField(password, { password = it }, imeAction = ImeAction.Next)
            StrengthMeter(password)
            Spacer(Modifier.height(8.dp))
            PasswordField(confirm, { confirm = it }, label = "Confirm password")
            passwordError?.let { Text(it, color = Red, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
            Spacer(Modifier.height(16.dp))
            SectionLabel("I am a")
            ChoiceChips(UserType.entries, type, { it.label }, { type = it })
            Text(
                if (type.isStaff) "Faculty and employees can also use the priority slots P01–P03."
                else "Students can use slots P04–P12.",
                color = Muted, fontSize = 13.sp
            )
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Continue", true, false) {
                tried = true
                if (step1Ok) step = 2
            }
            if (tried && !step1Ok) Text("Fill in every field to continue.", color = Red, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            TextButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Already registered? Sign in") }
        }
    } else {
        AuthScaffold("Your vehicle", "Step 2 of 2 · So guards can match your car to your slot", onBack = { step = 1 }) {
            LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.outline, drawStopIndicator = {})
            Spacer(Modifier.height(20.dp))
            VehicleForm(vType, { vType = it }, vColor, { vColor = it }, plate, { plate = it }, plateError)
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Create account", step2Ok, busy) {
                onRegister(Registration(name.trim(), email, password, type, Vehicle(vType!!, vColor!!, Rules.normalizePlate(plate))))
            }
        }
    }
}

@Composable
private fun StrengthMeter(password: String) {
    if (password.isEmpty()) return
    val s = Rules.passwordStrength(password)
    val (label, color) = when (s) {
        0, 1 -> "Weak" to Red
        2 -> "Good" to Amber
        else -> "Strong" to Green
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        repeat(3) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i < s.coerceAtLeast(1)) color else MaterialTheme.colorScheme.outline)
            )
            Spacer(Modifier.size(4.dp))
        }
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
    }
    if (s < 3) Text("Use 10+ characters with a number and a capital letter.", color = Muted, fontSize = 12.sp)
}
