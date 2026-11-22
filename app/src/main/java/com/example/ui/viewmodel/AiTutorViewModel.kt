package com.example.ui.viewmodel

import android.util.Log
import com.example.core.VoiceTurn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CefrLevel
import com.example.data.model.Dialect
import com.example.data.model.InteractiveStory
import com.example.data.model.PronunciationEvaluation
import com.example.data.model.SentenceLessonItem
import com.example.data.model.TargetLanguage
import com.example.data.model.TutorMessage
import com.example.data.model.TutorScenario
import com.example.data.model.TutorSessionScorecard
import com.example.data.repository.LingoAiRepository
import com.example.data.repository.LingoRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "AiTutorViewModel"

sealed interface AiOpState<out T> {
    data object Idle : AiOpState<Nothing>
    data object Loading : AiOpState<Nothing>
    data class Streaming(val partialText: String) : AiOpState<Nothing>
    data class Success<T>(val data: T) : AiOpState<T>
    data class Error(val message: String) : AiOpState<Nothing>
}

class AiTutorViewModel(
    private val repository: LingoRepository,
    private val aiRepository: LingoAiRepository,
    private val currentUserId: String
) : ViewModel() {

    val messagesState: StateFlow<DataState<List<TutorMessage>>> =
        repository.observeTutorMessages(currentUserId)
            .map<List<TutorMessage>, DataState<List<TutorMessage>>> { DataState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Failed observing tutor messages", root)
                emit(DataState.Error(root.localizedMessage ?: "تعذر تحميل سجل المحادثة"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading)

    private val _tutorStreamState = MutableStateFlow<AiOpState<String>>(AiOpState.Idle)
    val tutorStreamState: StateFlow<AiOpState<String>> = _tutorStreamState.asStateFlow()

    private val _pronunciationState = MutableStateFlow<AiOpState<PronunciationEvaluation>>(AiOpState.Idle)
    val pronunciationState: StateFlow<AiOpState<PronunciationEvaluation>> = _pronunciationState.asStateFlow()

    private val _generatedStoryState = MutableStateFlow<AiOpState<InteractiveStory>>(AiOpState.Idle)
    val generatedStoryState: StateFlow<AiOpState<InteractiveStory>> = _generatedStoryState.asStateFlow()

    private val _writingGradeState = MutableStateFlow<AiOpState<Pair<Int, String>>>(AiOpState.Idle)
    val writingGradeState: StateFlow<AiOpState<Pair<Int, String>>> = _writingGradeState.asStateFlow()

    private val _examGeneratorState = MutableStateFlow<AiOpState<com.example.data.model.GeneratedExamSet>>(AiOpState.Idle)
    val examGeneratorState: StateFlow<AiOpState<com.example.data.model.GeneratedExamSet>> = _examGeneratorState.asStateFlow()

    private val _examGradingState = MutableStateFlow<AiOpState<com.example.data.model.ExamAiGradingResult>>(AiOpState.Idle)
    val examGradingState: StateFlow<AiOpState<com.example.data.model.ExamAiGradingResult>> = _examGradingState.asStateFlow()

    private val _generatedUnitLessonState = MutableStateFlow<AiOpState<List<SentenceLessonItem>>>(AiOpState.Idle)
    val generatedUnitLessonState: StateFlow<AiOpState<List<SentenceLessonItem>>> = _generatedUnitLessonState.asStateFlow()

    private val _sessionScorecardState = MutableStateFlow<AiOpState<TutorSessionScorecard>>(AiOpState.Idle)
    val sessionScorecardState: StateFlow<AiOpState<TutorSessionScorecard>> = _sessionScorecardState.asStateFlow()

    private var tutorJob: Job? = null

    fun cancelTutorRequest() {
        tutorJob?.cancel()
        tutorJob = null
        _tutorStreamState.value = AiOpState.Idle
    }

    fun generateUnitLesson(
        language: TargetLanguage,
        dialect: Dialect,
        cefrLevel: CefrLevel,
        unitNumber: Int,
        unitTitleAr: String,
        subLevel: Int,
        onGenerated: (List<SentenceLessonItem>) -> Unit
    ) {
        viewModelScope.launch {
            _generatedUnitLessonState.value = AiOpState.Loading
            try {
                val items = aiRepository.generateUnitLessonSentences(
                    language = language,
                    dialect = dialect,
                    cefrLevel = cefrLevel,
                    unitNumber = unitNumber,
                    unitTitleAr = unitTitleAr,
                    subLevel = subLevel
                )
                _generatedUnitLessonState.value = AiOpState.Success(items)
                if (items.isNotEmpty()) {
                    onGenerated(items)
                }
            } catch (e: TimeoutCancellationException) {
                _generatedUnitLessonState.value = AiOpState.Error("انتهت مهلة توليد جمل الدرس. حاول مجددًا.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Unit lesson generation failed", e)
                _generatedUnitLessonState.value = AiOpState.Error(formatAiError(e, "تعذر توليد درس جديد بالذكاء الاصطناعي."))
            }
        }
    }

    private val _voiceSaveState = MutableStateFlow<AiOpState<Unit>>(AiOpState.Idle)
    val voiceSaveState = _voiceSaveState.asStateFlow()
    private val analyzedVoiceCalls = mutableSetOf<String>()

    fun resetVoiceResult() {
        _voiceSaveState.value = AiOpState.Idle
        clearSessionScorecard()
    }

    fun finishVoiceCall(callId: String, turns: List<VoiceTurn>, profile: com.example.data.model.UserProfile, scenario: TutorScenario) {
        if (turns.none { it.sender == "user" && it.text.isNotBlank() }) return
        if (_voiceSaveState.value is AiOpState.Loading) return
        _voiceSaveState.value = AiOpState.Loading
        val messages = turns.map { TutorMessage(sender = it.sender, text = it.text, language = profile.activeLanguage, scenarioId = scenario.id) }
        viewModelScope.launch {
            try {
                for (sender in listOf("user", "tutor")) {
                    val text = turns.filter { it.sender == sender }.joinToString("\n") { it.text }.take(8000)
                    if (text.isNotBlank()) repository.addTutorMessage(scenario.id, profile.activeLanguage, sender, text, stableId = "voice_${callId}_$sender").getOrThrow()
                }
                _voiceSaveState.value = AiOpState.Success(Unit)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _voiceSaveState.value = AiOpState.Error("تعذر حفظ النص في السجل. النص لسه ظاهر هنا وتقدر تعيد الحفظ.") }
            if (analyzedVoiceCalls.add(callId)) {
                generateSessionScorecard(profile.currentLanguageEnum(), Dialect.fromCode(profile.currentDialectCode(), profile.currentLanguageEnum()), profile.currentLevelEnum(), scenario, messages)
            }
        }
    }

    fun generateSessionScorecard(
        language: TargetLanguage,
        dialect: Dialect,
        cefrLevel: CefrLevel,
        scenario: TutorScenario,
        messages: List<TutorMessage>
    ) {
        val learnerTurns = messages.count { it.sender == "user" }
        if (learnerTurns == 0) {
            _sessionScorecardState.value = AiOpState.Error("أرسل رسالة واحدة على الأقل للمدرس الذكي قبل طلب تقرير الأداء.")
            return
        }
        viewModelScope.launch {
            _sessionScorecardState.value = AiOpState.Loading
            try {
                val scorecard = aiRepository.evaluateTutorSession(
                    language = language,
                    dialect = dialect,
                    cefrLevel = cefrLevel,
                    scenario = scenario,
                    messages = messages
                )
                _sessionScorecardState.value = AiOpState.Success(scorecard)
                repository.rewardXp(xpDelta = 20L)
            } catch (e: TimeoutCancellationException) {
                _sessionScorecardState.value = AiOpState.Error("انتهت مهلة إعداد تقرير الجلسة. حاول مرة أخرى.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Session scorecard failed", e)
                _sessionScorecardState.value = AiOpState.Error(formatAiError(e, "تعذر تحليل تقرير الجلسة."))
            }
        }
    }

    fun clearSessionScorecard() {
        _sessionScorecardState.value = AiOpState.Idle
    }

    fun startFreshTutorSession(messagesToClear: List<TutorMessage>) {
        viewModelScope.launch {
            _sessionScorecardState.value = AiOpState.Idle
            _tutorStreamState.value = AiOpState.Idle
            val ids = messagesToClear.map { it.id }.filter { it.isNotBlank() }
            if (ids.isNotEmpty()) {
                repository.clearTutorMessagesByIds(ids)
            }
        }
    }

    fun generateUnseenExam(
        language: TargetLanguage,
        dialect: Dialect,
        cefrLevel: CefrLevel,
        examType: String
    ) {
        viewModelScope.launch {
            _examGeneratorState.value = AiOpState.Loading
            try {
                val examSet = aiRepository.generateUnseenExamSet(language, dialect, cefrLevel, examType)
                _examGeneratorState.value = AiOpState.Success(examSet)
            } catch (e: TimeoutCancellationException) {
                _examGeneratorState.value = AiOpState.Error("انتهت مهلة توليد الاختبار. حاول مرة أخرى.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Unseen exam generation failed", e)
                _examGeneratorState.value = AiOpState.Error(formatAiError(e, "تعذر توليد أسئلة جديدة للاختبار."))
            }
        }
    }

    fun evaluateExamSpeakingAndWriting(
        language: TargetLanguage,
        cefrLevel: CefrLevel,
        examType: String,
        listeningScore: Int,
        readingScore: Int,
        speakingPrompt: String,
        speakingTranscript: String,
        writingPrompt: String,
        writingText: String,
        onGraded: (com.example.data.model.ExamAiGradingResult) -> Unit
    ) {
        if (speakingTranscript.trim().length < 5) {
            _examGradingState.value = AiOpState.Error("يرجى تسجيل إجابتك الصوتية في قسم التحدث أولًا.")
            return
        }
        if (writingText.trim().length < 10) {
            _examGradingState.value = AiOpState.Error("يرجى كتابة إجابة كاملة في قسم الكتابة لا تقل عن 10 أحرف.")
            return
        }
        viewModelScope.launch {
            _examGradingState.value = AiOpState.Loading
            try {
                val result = aiRepository.evaluateExamSpeakingAndWriting(
                    language = language,
                    cefrLevel = cefrLevel,
                    examType = examType,
                    listeningScore = listeningScore,
                    readingScore = readingScore,
                    speakingPrompt = speakingPrompt,
                    speakingTranscript = speakingTranscript,
                    writingPrompt = writingPrompt,
                    writingText = writingText
                )
                _examGradingState.value = AiOpState.Success(result)
                onGraded(result)
            } catch (e: TimeoutCancellationException) {
                _examGradingState.value = AiOpState.Error("انتهت مهلة تقييم الاختبار. حاول مرة أخرى.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Exam AI evaluation failed", e)
                _examGradingState.value = AiOpState.Error(formatAiError(e, "تعذر تقييم الاختبار."))
            }
        }
    }

    fun sendTutorMessage(
        scenario: TutorScenario,
        language: TargetLanguage,
        dialect: Dialect,
        cefrLevel: CefrLevel,
        existingMessages: List<TutorMessage>,
        userInput: String,
        deepThinking: Boolean,
        onSpokenReply: ((String) -> Unit)? = null
    ) {
        val trimmed = userInput.trim()
        if (trimmed.isEmpty()) return

        tutorJob?.cancel()
        tutorJob = viewModelScope.launch {
            _tutorStreamState.value = AiOpState.Loading
            // Save user message concurrently so Firestore latency never delays the AI call
            launch {
                runCatching {
                    repository.addTutorMessage(
                        scenarioId = scenario.id,
                        language = language.code,
                        sender = "user",
                        text = trimmed,
                        deepThinking = deepThinking
                    )
                }
            }

            try {
                var fullReply = ""
                aiRepository.streamTutorReply(
                    scenario = scenario,
                    language = language,
                    dialect = dialect,
                    cefrLevel = cefrLevel,
                    history = existingMessages,
                    userInput = trimmed,
                    deepThinking = deepThinking
                ).collect { accumulated ->
                    fullReply = accumulated
                    _tutorStreamState.value = AiOpState.Streaming(accumulated)
                }

                val parts = fullReply.split("---شرح وتصحيح بالعربية---", limit = 2)
                val targetPart = parts.firstOrNull().orEmpty().trim().ifBlank { fullReply }
                val arabicPart = parts.getOrNull(1).orEmpty().trim()

                repository.addTutorMessage(
                    scenarioId = scenario.id,
                    language = language.code,
                    sender = "tutor",
                    text = targetPart,
                    arabicExplanation = arabicPart,
                    deepThinking = deepThinking
                )
                repository.rewardXp(xpDelta = if (deepThinking) 25L else 15L)
                _tutorStreamState.value = AiOpState.Idle
                onSpokenReply?.invoke(targetPart)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Tutor stream timed out", e)
                _tutorStreamState.value = AiOpState.Error("استغرق الرد وقتًا أطول من المعتاد. اضغط إرسال مجددًا أو جرب الواي فاي.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Tutor stream failed", e)
                _tutorStreamState.value = AiOpState.Error(formatAiError(e, "تعذر الاتصال بالمدرس الذكي."))
            }
        }
    }

    fun analyzePronunciation(
        targetSentence: String,
        recognizedText: String,
        language: TargetLanguage,
        dialect: Dialect,
        cefrLevel: CefrLevel
    ) {
        if (recognizedText.isBlank()) {
            _pronunciationState.value = AiOpState.Error("يرجى تسجيل صوتك أو إدخال النص المنطوق أولًا.")
            return
        }
        viewModelScope.launch {
            _pronunciationState.value = AiOpState.Loading
            try {
                val eval = aiRepository.evaluatePronunciation(
                    targetSentence = targetSentence,
                    recognizedText = recognizedText,
                    language = language,
                    dialect = dialect,
                    cefrLevel = cefrLevel
                )
                repository.savePronunciationLog(
                    language = language.code,
                    dialect = dialect.code,
                    targetSentence = targetSentence,
                    recognizedText = recognizedText,
                    accuracyScore = eval.accuracyScore,
                    stressScore = eval.stressScore,
                    rhythmScore = eval.rhythmScore,
                    fluencyScore = eval.fluencyScore,
                    clarityScore = eval.clarityScore,
                    arabicCoaching = "${eval.dialectNoteAr}\n\n${eval.arabicCoaching}"
                )
                _pronunciationState.value = AiOpState.Success(eval)
            } catch (e: TimeoutCancellationException) {
                _pronunciationState.value = AiOpState.Error("انتهت مهلة تحليل النطق. يرجى المحاولة مرة أخرى.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Pronunciation analysis failed", e)
                _pronunciationState.value = AiOpState.Error(formatAiError(e, "تعذر تحليل النطق."))
            }
        }
    }

    fun generateStory(
        language: TargetLanguage,
        dialect: Dialect,
        cefrLevel: CefrLevel,
        genreAr: String
    ) {
        viewModelScope.launch {
            _generatedStoryState.value = AiOpState.Loading
            try {
                val story = aiRepository.generateInteractiveStory(language, dialect, cefrLevel, genreAr)
                repository.saveGeneratedStory(story)
                _generatedStoryState.value = AiOpState.Success(story)
            } catch (e: TimeoutCancellationException) {
                _generatedStoryState.value = AiOpState.Error("انتهت مهلة توليد القصة. حاول مرة أخرى.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Story generation failed", e)
                _generatedStoryState.value = AiOpState.Error(formatAiError(e, "تعذر توليد القصة بالذكاء الاصطناعي."))
            }
        }
    }

    fun gradeWriting(
        language: TargetLanguage,
        cefrLevel: CefrLevel,
        promptTask: String,
        learnerText: String,
        onGraded: ((Int, String) -> Unit)? = null
    ) {
        if (learnerText.trim().length < 10) {
            _writingGradeState.value = AiOpState.Error("يرجى كتابة جملة أو فقرة واضحة لا تقل عن 10 أحرف.")
            return
        }
        viewModelScope.launch {
            _writingGradeState.value = AiOpState.Loading
            try {
                val result = aiRepository.gradeWritingSubmission(language, cefrLevel, promptTask, learnerText)
                _writingGradeState.value = AiOpState.Success(result)
                onGraded?.invoke(result.first, result.second)
            } catch (e: TimeoutCancellationException) {
                _writingGradeState.value = AiOpState.Error("انتهت مهلة تصحيح الكتابة. حاول مرة أخرى.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Writing grade failed", e)
                _writingGradeState.value = AiOpState.Error(formatAiError(e, "تعذر تقييم الكتابة."))
            }
        }
    }

    fun resetWritingGrade() {
        _writingGradeState.value = AiOpState.Idle
    }

    private fun formatAiError(error: Throwable, fallbackAr: String): String {
        val raw = error.localizedMessage.orEmpty()
        return when {
            raw.startsWith("APPCHECK_TIMEOUT") ->
                "انتهت مهلة التحقق الأمني (reCAPTCHA) على الشبكة الحالية ($raw). تأكد من تحديث خدمات Google Play أو جرب شبكة أخرى."
            raw.startsWith("APPCHECK_ERROR") ->
                "تعذر إكمال التحقق الأمني من التطبيق ($raw). تأكد من اتصال الإنترنت وضبط الوقت تلقائياً."
            raw.contains("allotted time", ignoreCase = true) ||
                raw.contains("timeout", ignoreCase = true) ->
                "الاتصال بخادم الذكاء الاصطناعي بطيء على الشبكة الحالية. اضغط إرسال مجددًا أو جرب الواي فاي."
            raw.contains("App Check", ignoreCase = true) || raw.contains("403") ->
                "تعذر التحقق الأمني من الاتصال ($raw). تأكد من ضبط الوقت تلقائياً وحاول مجددًا."
            raw.contains("Unable to resolve host", ignoreCase = true) ||
                raw.contains("network", ignoreCase = true) ->
                "تأكد من اتصال الإنترنت على هاتفك ثم حاول مجددًا."
            raw.isNotBlank() -> "$fallbackAr ($raw)"
            else -> fallbackAr
        }
    }
}
