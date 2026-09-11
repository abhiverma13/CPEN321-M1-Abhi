package com.example.cpen321application

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.cpen321application.ui.theme.CPEN321ApplicationTheme
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPEN321ApplicationTheme {
                M1Application(this, BuildConfig.API_BASE_URL, BuildConfig.GOOGLE_CLIENT_ID)
            }
        }
    }
}

private enum class Screen { HOME, LOGIN_AND_SERVER, LIVE_UPDATES, TIMER }

@Composable
private fun M1Application(activity: Activity, apiBaseUrl: String, googleClientId: String) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    val title = when (screen) {
        Screen.HOME -> "CPEN 321 M1"
        Screen.LOGIN_AND_SERVER -> "Login + Server"
        Screen.LIVE_UPDATES -> "Live Updates"
        Screen.TIMER -> "Timer Surprise"
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(20.dp)) {
            Text(title)
            Spacer(modifier = Modifier.padding(6.dp))
            if (screen != Screen.HOME) {
                Button(onClick = { screen = Screen.HOME }) { Text("Back") }
                Spacer(modifier = Modifier.padding(6.dp))
            }
            when (screen) {
                Screen.HOME -> HomeScreen { screen = it }
                Screen.LOGIN_AND_SERVER -> LoginAndServerScreen(activity, apiBaseUrl, googleClientId)
                Screen.LIVE_UPDATES -> LiveUpdatesScreen(apiBaseUrl)
                Screen.TIMER -> TimerScreen()
            }
        }
    }
}

@Composable
private fun HomeScreen(onNavigate: (Screen) -> Unit) {
    var healthStatus by remember { mutableStateOf("Checking local backend...") }
    LaunchedEffect(Unit) { healthStatus = fetchHealthStatus(BuildConfig.API_BASE_URL) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(healthStatus)
        Button(onClick = { onNavigate(Screen.LOGIN_AND_SERVER) }, modifier = Modifier.fillMaxWidth()) {
            Text("Login + Server")
        }
        Button(onClick = { onNavigate(Screen.LIVE_UPDATES) }, modifier = Modifier.fillMaxWidth()) {
            Text("Live Updates")
        }
        Button(onClick = { onNavigate(Screen.TIMER) }, modifier = Modifier.fillMaxWidth()) {
            Text("Timer")
        }
    }
}

@Composable
private fun LoginAndServerScreen(activity: Activity, apiBaseUrl: String, googleClientId: String) {
    var signedInUser by remember { mutableStateOf<GoogleUser?>(null) }
    var serverInfo by remember { mutableStateOf<ServerInfo?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    val credentialManager = remember { CredentialManager.create(activity) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(signedInUser) {
        val user = signedInUser ?: return@LaunchedEffect
        status = "Loading server information..."
        runCatching { fetchServerInfo(apiBaseUrl, user.idToken) }
            .onSuccess { serverInfo = it; status = null }
            .onFailure { status = "Could not load server information: ${it.message ?: "Unknown error"}" }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (signedInUser == null) {
            Button(
                enabled = googleClientId.isNotBlank(),
                onClick = {
                    scope.launch {
                        status = "Opening Google sign-in..."
                        signInWithGoogle(activity, credentialManager, googleClientId)
                            .onSuccess { signedInUser = it }
                            .onFailure { status = "Google sign-in failed: ${it.message ?: "Unknown error"}" }
                    }
                },
            ) { Text("Sign in with Google") }
            if (googleClientId.isBlank()) Text("GOOGLE_CLIENT_ID is missing from frontend/local.properties.")
        } else {
            Text("Google user: ${signedInUser?.displayName}")
            serverInfo?.let { info ->
                Text("Server IP address: ${info.serverIp}")
                Text("Client IP address: ${info.clientIp}")
                Text("Server local time: ${info.serverTime}")
                Text("Client local time: ${clientTime()}")
                Text("Your name: ${info.ownerName}")
            }
        }
        status?.let { Text(it) }
    }
}

@Composable
private fun LiveUpdatesScreen(apiBaseUrl: String) {
    val context = LocalContext.current
    val pixels = remember { mutableStateListOf<Color>().apply { repeat(256) { add(Color.White) } } }
    var status by remember { mutableStateOf("Connecting to pixel stream...") }
    val webSocketUrl = remember(apiBaseUrl) { websocketUrl(apiBaseUrl) }

    DisposableEffect(webSocketUrl) {
        val client = OkHttpClient()
        val socket = client.newWebSocket(Request.Builder().url(webSocketUrl).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                (context as? Activity)?.runOnUiThread { status = "Receiving live pixel updates" }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val update = JSONObject(text)
                    val x = update.getInt("x")
                    val y = update.getInt("y")
                    require(x in 0..15 && y in 0..15)
                    Triple(x, y, Color(AndroidColor.parseColor(update.getString("color"))))
                }.onSuccess { (x, y, color) ->
                    (context as? Activity)?.runOnUiThread { pixels[y * 16 + x] = color }
                }
            }

            override fun onFailure(webSocket: WebSocket, throwable: Throwable, response: Response?) {
                (context as? Activity)?.runOnUiThread {
                    status = "Pixel stream unavailable: ${throwable.message ?: "Connection failed"}"
                }
            }
        })
        onDispose {
            socket.close(1000, "Leaving live updates")
            client.dispatcher.executorService.shutdown()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(status)
        Canvas(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).border(1.dp, Color.DarkGray),
        ) {
            val cellWidth = size.width / 16f
            val cellHeight = size.height / 16f
            pixels.forEachIndexed { index, color ->
                drawRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset((index % 16) * cellWidth, (index / 16) * cellHeight),
                    size = androidx.compose.ui.geometry.Size(cellWidth, cellHeight),
                )
            }
        }
    }
}

@Composable
private fun TimerScreen() {
    var minutes by remember { mutableStateOf("0") }
    var seconds by remember { mutableStateOf("10") }
    var secondsRemaining by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var weatherRequestNumber by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf<String?>(null) }
    var city by remember { mutableStateOf("Vancouver") }
    var weather by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(running, secondsRemaining) {
        if (!running) return@LaunchedEffect
        if (secondsRemaining > 0) {
            delay(1_000)
            secondsRemaining -= 1
        } else {
            running = false
            weatherRequestNumber += 1
        }
    }

    LaunchedEffect(weatherRequestNumber) {
        if (weatherRequestNumber == 0) return@LaunchedEffect
        status = "Timer complete — revealing weather..."
        runCatching { fetchWeather(city) }
            .onSuccess { weather = it; status = null }
            .onFailure { status = "Could not retrieve weather: ${it.message ?: "Unknown error"}" }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit) }, label = { Text("Minutes") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !running)
        OutlinedTextField(seconds, { seconds = it.filter(Char::isDigit) }, label = { Text("Seconds (0–59)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !running)
        OutlinedTextField(city, { city = it }, label = { Text("Weather city") }, enabled = !running)
        if (running) {
            Text("Time remaining: ${secondsRemaining / 60}:${(secondsRemaining % 60).toString().padStart(2, '0')}")
            Button(onClick = { running = false }) { Text("Cancel timer") }
        } else {
            Button(onClick = {
                val minuteValue = minutes.toIntOrNull()
                val secondValue = seconds.toIntOrNull()
                val totalSeconds = (minuteValue ?: -1) * 60 + (secondValue ?: -1)
                if (totalSeconds <= 0 || secondValue !in 0..59 || city.isBlank()) {
                    status = "Enter a city and a timer greater than zero with seconds from 0 to 59."
                } else {
                    secondsRemaining = totalSeconds
                    weather = null
                    status = null
                    running = true
                }
            }) { Text("Start timer") }
        }
        status?.let { Text(it) }
        weather?.let { Text("Surprise: $it") }
    }
}

private data class GoogleUser(val displayName: String, val idToken: String)
private data class ServerInfo(val serverIp: String, val clientIp: String, val serverTime: String, val ownerName: String)

private suspend fun signInWithGoogle(activity: Activity, credentialManager: CredentialManager, serverClientId: String): Result<GoogleUser> = runCatching {
    val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    val credential = credentialManager.getCredential(activity, request).credential
    check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        "Google did not return an ID token."
    }
    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
    GoogleUser(
        displayName = listOfNotNull(googleCredential.givenName, googleCredential.familyName).joinToString(" ")
            .ifBlank { googleCredential.displayName ?: "Google user" },
        idToken = googleCredential.idToken,
    )
}.recoverCatching { error ->
    if (error is GetCredentialException) throw IllegalStateException(error.message ?: "Credential Manager could not complete sign-in.")
    throw error
}

private suspend fun fetchServerInfo(apiBaseUrl: String, idToken: String): ServerInfo = withContext(Dispatchers.IO) {
    val ip = authorizedJson("${apiBaseUrl.trimEnd('/')}/api/server/ip", idToken)
    val time = authorizedJson("${apiBaseUrl.trimEnd('/')}/api/server/time", idToken)
    val owner = authorizedJson("${apiBaseUrl.trimEnd('/')}/api/server/owner", idToken)
    ServerInfo(ip.getString("serverIp"), ip.getString("clientIp"), time.getString("serverTime"),
        "${owner.getString("firstName")} ${owner.getString("lastName")}")
}

private fun authorizedJson(url: String, idToken: String): JSONObject {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.setRequestProperty("Authorization", "Bearer $idToken")
    connection.connectTimeout = 5_000
    connection.readTimeout = 5_000
    return try {
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { reader -> reader.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}: $body")
        JSONObject(body)
    } finally {
        connection.disconnect()
    }
}

private suspend fun fetchHealthStatus(apiBaseUrl: String): String = withContext(Dispatchers.IO) {
    val healthUrl = "${apiBaseUrl.trimEnd('/')}/health"
    try {
        val connection = URL(healthUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) "Backend healthy" else "Backend error: HTTP ${connection.responseCode}"
        } finally {
            connection.disconnect()
        }
    } catch (error: Exception) {
        "Backend unreachable: ${error.message ?: error.javaClass.simpleName}"
    }
}

private suspend fun fetchWeather(city: String): String = withContext(Dispatchers.IO) {
    val encodedCity = java.net.URLEncoder.encode(city, Charsets.UTF_8.name())
    val geocoding = JSONObject(URL("https://geocoding-api.open-meteo.com/v1/search?name=$encodedCity&count=1").readText())
    val result = geocoding.optJSONArray("results")?.optJSONObject(0) ?: throw IOException("No weather location found for $city")
    val forecastUrl = "https://api.open-meteo.com/v1/forecast?latitude=${result.getDouble("latitude")}&longitude=${result.getDouble("longitude")}&current=temperature_2m,weather_code"
    val current = JSONObject(URL(forecastUrl).readText()).getJSONObject("current")
    "Current weather in ${result.getString("name")}: ${weatherDescription(current.getInt("weather_code"))}, ${current.getDouble("temperature_2m")}°C"
}

private fun websocketUrl(apiBaseUrl: String): String = when {
    apiBaseUrl.startsWith("https://") -> "wss://${apiBaseUrl.removePrefix("https://").trimEnd('/')}/ws/pixels"
    apiBaseUrl.startsWith("http://") -> "ws://${apiBaseUrl.removePrefix("http://").trimEnd('/')}/ws/pixels"
    else -> throw IllegalArgumentException("API_BASE_URL must use http or https")
}

private fun clientTime(): String = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss 'GMT'xxx"))

private fun weatherDescription(code: Int): String = when (code) {
    0 -> "clear sky"
    1, 2, 3 -> "partly cloudy"
    45, 48 -> "foggy"
    51, 53, 55, 56, 57 -> "drizzle"
    61, 63, 65, 66, 67, 80, 81, 82 -> "rain"
    71, 73, 75, 77, 85, 86 -> "snow"
    95, 96, 99 -> "thunderstorm"
    else -> "weather code $code"
}
