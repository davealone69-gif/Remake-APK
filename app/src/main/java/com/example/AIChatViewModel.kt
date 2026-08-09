package com.example

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.ResponseBody

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isThinking: Boolean = false,
    val imageBitmap: Bitmap? = null
)

class AIChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Hello! I am your AI Workshop Diagnostic Assistant. You can ask me about service manuals, DTC fault codes (e.g., P0300, P0A80, C1256), wiring diagrams, or attach a photo of a fault for analysis.",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun diagnoseDtcCode(code: String, vehicleContext: String = "General Vehicle") {
        val prompt = "Diagnose DTC Fault Code: $code for vehicle: $vehicleContext. Provide:\n1. Fault Description & Severity\n2. Primary Root Causes\n3. Step-by-Step Diagnostic & Repair Steps\n4. Recommended Replacement Parts"
        sendMessage(prompt)
    }

    fun sendMessage(text: String, attachedImage: Bitmap? = null, useThinking: Boolean = false) {
        if (text.isBlank() && attachedImage == null) return

        val userMsg = ChatMessage(
            text = if (text.isBlank()) "[Attached Photo for Fault Analysis]" else text,
            isUser = true,
            imageBitmap = attachedImage
        )
        _messages.value = _messages.value + userMsg

        val aiMsgId = java.util.UUID.randomUUID().toString()
        val initialAiMsg = ChatMessage(id = aiMsgId, text = "", isUser = false, isThinking = true)
        _messages.value = _messages.value + initialAiMsg

        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY.ifBlank {
                    System.getenv("GEMINI_API_KEY") ?: ""
                }

                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    _messages.value = _messages.value.map {
                        if (it.id == aiMsgId) it.copy(
                            text = "Please set your GEMINI_API_KEY in the Secrets panel to enable live Gemini AI responses.",
                            isThinking = false
                        ) else it
                    }
                    _isLoading.value = false
                    return@launch
                }

                val systemInstruction = Content(
                    parts = listOf(Part(text = "You are an expert master automotive & equipment mechanic AI. Provide precise diagnostic steps, DTC fault code analysis, wiring diagram checks, and repair instructions in concise formatted text.")),
                    role = "system"
                )

                val history = _messages.value.dropLast(1).mapNotNull {
                    if (it.text.isNotBlank() && !it.isThinking) {
                        val parts = mutableListOf<Part>()
                        if (it.imageBitmap != null) {
                            parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = it.imageBitmap.toBase64())))
                        }
                        if (it.text != "[Attached Photo for Fault Analysis]") {
                            parts.add(Part(text = it.text))
                        }
                        Content(parts = parts, role = if (it.isUser) "user" else "model")
                    } else null
                }.toMutableList()

                val currentParts = mutableListOf<Part>()
                if (attachedImage != null) {
                    currentParts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = attachedImage.toBase64())))
                }
                if (text.isNotBlank()) {
                    currentParts.add(Part(text = text))
                }
                history.add(Content(parts = currentParts, role = "user"))

                val config = if (useThinking) {
                    GenerationConfig(thinkingConfig = ThinkingConfig(thinkingLevel = "HIGH"))
                } else null

                val request = GenerateContentRequest(
                    contents = history,
                    generationConfig = config,
                    systemInstruction = systemInstruction
                )

                var responseStream: ResponseBody? = null
                try {
                    if (useThinking) {
                        responseStream = RetrofitClient.service.generateProContentStream(apiKey, request)
                    } else {
                        responseStream = RetrofitClient.service.generateContentStream(apiKey, request)
                    }
                } catch (e: Exception) {
                    // Fallback to gemini-3.5-flash if pro or thinking failed (e.g. rate limit 429)
                    val fallbackRequest = GenerateContentRequest(
                        contents = history,
                        systemInstruction = systemInstruction
                    )
                    responseStream = RetrofitClient.service.generateContentStream(apiKey, fallbackRequest)
                }

                var fullResponse = ""

                responseStream.byteStream().bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        if (line!!.startsWith("data: ")) {
                            val data = line!!.removePrefix("data: ").trim()
                            if (data == "[DONE]") continue

                            try {
                                val chunk = moshi.adapter(GenerateContentResponse::class.java).fromJson(data)
                                val candidateParts = chunk?.candidates?.firstOrNull()?.content?.parts
                                if (candidateParts != null) {
                                    for (p in candidateParts) {
                                        if (p.text != null) {
                                            fullResponse += p.text
                                        }
                                    }
                                    _messages.value = _messages.value.map {
                                        if (it.id == aiMsgId) it.copy(text = fullResponse, isThinking = false) else it
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                if (fullResponse.isBlank()) {
                    _messages.value = _messages.value.map {
                        if (it.id == aiMsgId) it.copy(text = "DTC Diagnostic Analysis complete. Please check the steps above.", isThinking = false) else it
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _messages.value = _messages.value.map {
                    if (it.id == aiMsgId) it.copy(
                        text = "AI Diagnostic Notice: ${e.localizedMessage ?: "Service connection error. Please try again."}",
                        isThinking = false
                    ) else it
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}
