package com.example.core

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "AudioClipRecorder"

/**
 * Records the learner's own voice into app-private cacheDir (.m4a) with zero storage permissions
 * and plays it back for side-by-side pronunciation comparison against the native TTS model.
 */
class AudioClipRecorder(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var timerJob: Job? = null
    private var currentClipFile: File? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _hasRecording = MutableStateFlow(false)
    val hasRecording: StateFlow<Boolean> = _hasRecording.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun startRecording(clipKey: String = "default") {
        _error.value = null
        stopPlayback()
        stopRecording()

        val safeKey = clipKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(40)
        val outFile = File(context.cacheDir, "lingo_rec_$safeKey.m4a")
        currentClipFile = outFile

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            _isRecording.value = true
            _hasRecording.value = false
            _recordingSeconds.value = 0

            timerJob?.cancel()
            timerJob = scope.launch {
                while (_isRecording.value) {
                    delay(1000L)
                    _recordingSeconds.value += 1
                    if (_recordingSeconds.value >= 30) {
                        stopRecording()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            cleanupRecorder()
            _error.value = "تعذر بدء التسجيل الصوتي: ${e.localizedMessage ?: "تحقق من صلاحية الميكروفون"}"
        }
    }

    fun stopRecording() {
        timerJob?.cancel()
        timerJob = null
        val recorder = mediaRecorder ?: return
        try {
            recorder.stop()
            val file = currentClipFile
            _hasRecording.value = file != null && file.exists() && file.length() > 0L
        } catch (e: Exception) {
            Log.w(TAG, "MediaRecorder stop threw (likely too short): ${e.message}")
            _hasRecording.value = false
        } finally {
            cleanupRecorder()
        }
    }

    fun playRecording(onCompleted: () -> Unit = {}) {
        _error.value = null
        if (_isRecording.value) stopRecording()
        stopPlayback()

        val file = currentClipFile
        if (file == null || !file.exists() || file.length() == 0L) {
            _error.value = "لا يوجد تسجيل صوتي بعد. اضغط على زر التسجيل أولًا."
            return
        }

        try {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    _isPlaying.value = false
                    stopPlayback()
                    onCompleted()
                }
                setOnErrorListener { _, _, _ ->
                    _isPlaying.value = false
                    stopPlayback()
                    _error.value = "تعذر تشغيل التسجيل الصوتي."
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
            _isPlaying.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play recording", e)
            stopPlayback()
            _error.value = "تعذر تشغيل صوتك المسجّل: ${e.localizedMessage ?: "خطأ غير معروف"}"
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        _isPlaying.value = false
    }

    fun resetClip() {
        stopRecording()
        stopPlayback()
        _hasRecording.value = false
        _recordingSeconds.value = 0
        _error.value = null
    }

    fun release() {
        stopRecording()
        stopPlayback()
    }

    private fun cleanupRecorder() {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null
        _isRecording.value = false
    }
}
