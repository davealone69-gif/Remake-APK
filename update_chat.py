import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

new_chat_ui = """
@Composable
fun AIChatContent(viewModel: AIChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    var isSpeaking by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    var useThinking by remember { mutableStateOf(false) }
    
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    DisposableEffect(context) {
        val textToSpeech = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale.US
                val voices = tts?.voices
                val femaleVoice = voices?.firstOrNull { 
                    it.name.contains("female", ignoreCase = true) || 
                    it.name.contains("en-us-x-sfg", ignoreCase = true)
                }
                if (femaleVoice != null) {
                    tts?.voice = femaleVoice
                }
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gemini Assistant", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Thinking", style = MaterialTheme.typography.bodySmall, color = if (useThinking) MaterialTheme.colorScheme.primary else Color.Gray)
                Switch(checked = useThinking, onCheckedChange = { useThinking = it }, modifier = Modifier.scale(0.8f))
            }
        }
        Spacer(Modifier.height(8.dp))
        
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!msg.isUser) {
                        Icon(
                            Icons.Filled.SmartToy, 
                            contentDescription = "AI",
                            modifier = Modifier.size(24.dp).padding(top = 4.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (msg.isUser) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(12.dp)
                            .weight(1f, fill = false)
                    ) {
                        if (msg.isThinking) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Thinking...", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            Text(
                                msg.text,
                                color = if (msg.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    if (!msg.isUser && !msg.isThinking && msg.text.isNotBlank()) {
                        IconButton(
                            onClick = {
                                if (isSpeaking) {
                                    tts?.stop()
                                    isSpeaking = false
                                } else {
                                    tts?.speak(msg.text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
                                    isSpeaking = true
                                }
                            },
                            modifier = Modifier.size(32.dp).padding(start = 4.dp, top = 4.dp)
                        ) {
                            Icon(
                                if (isSpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                                contentDescription = "Read Aloud",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask something...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    viewModel.sendMessage(inputText, useThinking)
                    inputText = ""
                },
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
"""

pattern = re.compile(r"@Composable\nfun AIChatContent\(\).*?^}$", re.DOTALL | re.MULTILINE)
content = pattern.sub(new_chat_ui.strip(), content)

# Need to add some imports if they don't exist
imports_to_add = [
    "import androidx.compose.ui.draw.scale",
    "import androidx.compose.foundation.lazy.LazyColumn",
    "import androidx.compose.foundation.lazy.items",
    "import androidx.lifecycle.viewmodel.compose.viewModel"
]

for imp in imports_to_add:
    if imp not in content:
        content = content.replace("package com.example\n", f"package com.example\n\n{imp}")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
