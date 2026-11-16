package com.auramate

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

// TODO: point this at YOUR backend. Never put an Anthropic API key inside the app.
// Expected: POST {"message": "..."} -> {"reply": "..."}
const val BACKEND_URL = "https://your-server.example.com/chat"

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { if (it == TextToSpeech.SUCCESS) tts?.language = Locale("hi", "IN") }
        val commands = CommandHandler(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) { Chat(commands) { tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "am") } }
            }
        }
    }

    override fun onDestroy() { tts?.shutdown(); super.onDestroy() }
}

@Composable
fun Chat(commands: CommandHandler, speak: (String) -> Unit) {
    val msgs = remember { mutableStateListOf("Aura" to "Namaste! Alarm, WhatsApp, apps ya search, kuch bhi boliye.") }
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun send(text: String) {
        if (text.isBlank()) return
        msgs.add("Aap" to text); input = ""
        scope.launch {
            val reply = commands.handle(text) ?: askAi(text)
            msgs.add("Aura" to reply); speak(reply)
        }
    }

    val contactsPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) { contactsPerm.launch(Manifest.permission.READ_CONTACTS) }

    val mic = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK)
            r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(::send)
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(12.dp)) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(msgs) { (who, text) -> Text("$who: $text") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Kuch bhi boliye…") })
            Button(onClick = {
                mic.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN"))
            }) { Text("Mic") }
            Button(onClick = { send(input) }) { Text("Bhejein") }
        }
    }
}

suspend fun askAi(message: String): String = withContext(Dispatchers.IO) {
    try {
        val c = URL(BACKEND_URL).openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        c.outputStream.use { it.write(JSONObject().put("message", message).toString().toByteArray()) }
        JSONObject(c.inputStream.bufferedReader().readText()).getString("reply")
    } catch (e: Exception) { "AI se judne mein dikkat aayi. Backend URL check kijiye." }
}
