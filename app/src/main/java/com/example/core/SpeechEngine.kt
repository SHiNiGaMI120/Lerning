package com.example.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "SpeechEngine"

/**
 * Manages real Android Text-to-Speech (with dialect, slow/normal rate, and character pitch)
 * and Android SpeechRecognizer for Pronunciation Lab & Voice AI Tutor.
 */
class SpeechEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _speechError = MutableStateFlow<String?>(null)
    val speechError: StateFlow<String?> = _speechError.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
            if (!ttsReady) {
                Log.w(TAG, "TextToSpeech initialization returned status=$status")
            }
        }
    }

    fun speak(
        text: String,
        dialectCode: String,
        slow: Boolean = false,
        pitch: Float = 1.0f
    ) {
        val engine = tts ?: return
        if (!ttsReady || text.isBlank()) return
        val locale = localeForDialect(dialectCode)
        val langResult = engine.setLanguage(locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            val fallback = if (dialectCode.startsWith("es")) Locale("es", "ES") else Locale.US
            engine.setLanguage(fallback)
        }
        engine.setSpeechRate(if (slow) 0.68f else 0.98f)
        engine.setPitch(pitch.coerceIn(0.75f, 1.3f))
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lingo_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun startListening(
        dialectCode: String,
        onResult: (String) -> Unit
    ) {
        _speechError.value = null
        _partialTranscript.value = ""

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _speechError.value = "التعرف الصوتي غير متوفر على هذا الجهاز حاليًا. يمكنك كتابة نطقك أو استخدام المحاكي الصوتي."
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _speechError.value = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التقاط صوت واضح، حاول التحدث بوضوح أكبر."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "انتهى وقت الاستماع دون صوت."
                        SpeechRecognizer.ERROR_AUDIO -> "تعذر الوصول للميكروفون."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "يرجى منح صلاحية الميكروفون أولًا."
                        else -> "تعذر التعرف على الصوت (رمز $error). حاول مرة أخرى."
                    }
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull().orEmpty().trim()
                    _partialTranscript.value = best
                    if (best.isNotBlank()) {
                        onResult(best)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        _partialTranscript.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, dialectCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, dialectCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            _isListening.value = true
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
            _isListening.value = false
            _speechError.value = "تعذر تشغيل الميكروفون: ${e.localizedMessage ?: "خطأ غير معروف"}"
        }
    }

    fun stopListening() {
        _isListening.value = false
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
    }

    fun release() {
        stopSpeaking()
        tts?.shutdown()
        tts = null
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
    }

    private fun localeForDialect(dialectCode: String): Locale = when (dialectCode) {
        "en-GB" -> Locale.UK
        "es-ES" -> Locale("es", "ES")
        "es-MX" -> Locale("es", "MX")
        else -> Locale.US
    }
}
