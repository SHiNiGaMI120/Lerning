package com.example.data.repository

import android.util.Log
import com.example.data.model.CefrLevel
import com.example.data.model.Dialect
import com.example.data.model.ExamAiGradingResult
import com.example.data.model.ExamQuestion
import com.example.data.model.GeneratedExamSet
import com.example.data.model.InteractiveStory
import com.example.data.model.PronunciationEvaluation
import com.example.data.model.SentenceLessonItem
import com.example.data.model.SkillType
import com.example.data.model.StoryChoice
import com.example.data.model.StoryTurn
import com.example.data.model.TargetLanguage
import com.example.data.model.TutorMessage
import com.example.data.model.TutorScenario
import com.example.data.model.TutorSessionScorecard
import com.example.core.activeAppCheckMode
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "LingoAiRepository"
private const val DEFAULT_MODEL = "gemini-3.8-flash"
private const val FAST_MODEL = "gemini-3.5-flash-lite"

class LingoAiRepository {

    private suspend fun ensureAppCheckReady() {
        try {
            withTimeout(12_000L) {
                FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "App Check token timed out after 12s (mode=$activeAppCheckMode)", e)
            throw IllegalStateException("APPCHECK_TIMEOUT:$activeAppCheckMode", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "App Check token failed (mode=$activeAppCheckMode)", e)
            throw IllegalStateException("APPCHECK_ERROR:$activeAppCheckMode:${e.localizedMessage ?: e.javaClass.simpleName}", e)
        }
    }

    // Fast multi-turn conversational model (official free-tier models, zero billing required)
    private fun createTutorModel(systemPrompt: String, deepThinking: Boolean, useFastModel: Boolean = !deepThinking) =
        if (deepThinking) {
            Firebase.ai.generativeModel(
                modelName = DEFAULT_MODEL,
                generationConfig = generationConfig {
                    thinkingConfig = thinkingConfig {
                        thinkingLevel = ThinkingLevel.MEDIUM
                    }
                },
                systemInstruction = content { text(systemPrompt) }
            )
        } else {
            Firebase.ai.generativeModel(
                modelName = if (useFastModel) FAST_MODEL else DEFAULT_MODEL,
                systemInstruction = content { text(systemPrompt) }
            )
        }

    // Structured JSON model for Pronunciation Lab Coaching
    private val pronunciationModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "accuracyScore" to Schema.integer("Phonetic accuracy 0-100"),
                "stressScore" to Schema.integer("Word/sentence stress score 0-100"),
                "rhythmScore" to Schema.integer("Natural rhythm and cadence score 0-100"),
                "fluencyScore" to Schema.integer("Speech smoothness/fluency score 0-100"),
                "clarityScore" to Schema.integer("Overall intelligibility/clarity 0-100"),
                "dialectNoteAr" to Schema.string("Arabic note distinguishing acceptable dialect variation from genuine pronunciation errors"),
                "arabicCoaching" to Schema.string("Clear Arabic feedback explaining specific weaknesses and tongue/mouth tips"),
                "correctiveDrillTarget" to Schema.string("A custom corrective practice sentence targeting the detected weakness")
            )
        )
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    // Structured JSON model for AI Interactive Story Generator
    private val storyGeneratorModel by lazy {
        val turnSchema = Schema.obj(
            properties = mapOf(
                "speakerName" to Schema.string("Character name"),
                "text" to Schema.string("Natural sentence in target language"),
                "arabicTranslation" to Schema.string("Accurate Arabic translation")
            )
        )
        val choiceSchema = Schema.obj(
            properties = mapOf(
                "replyText" to Schema.string("Learner reply in target language"),
                "arabicHint" to Schema.string("Short Arabic hint explaining the reply intent"),
                "outcomeSpeaker" to Schema.string("Responding character name"),
                "outcomeReply" to Schema.string("Story branch continuation in target language"),
                "outcomeArabic" to Schema.string("Arabic translation of outcomeReply"),
                "feedbackAr" to Schema.string("Arabic pedagogical feedback on why this choice worked")
            )
        )
        val storySchema = Schema.obj(
            properties = mapOf(
                "titleAr" to Schema.string("Arabic story title"),
                "subtitleTarget" to Schema.string("Story title in target language"),
                "turns" to Schema.array(turnSchema, "3 natural dialogue turns"),
                "choices" to Schema.array(choiceSchema, "2 branching learner reply choices"),
                "comprehensionQuestionAr" to Schema.string("Arabic comprehension question"),
                "comprehensionOptionsAr" to Schema.array(Schema.string(), "3 Arabic answer options"),
                "correctOptionIndex" to Schema.integer("Index 0..2 of the correct answer")
            )
        )
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = storySchema
            }
        )
    }

    // Writing & Exam Grader
    private val writingGraderModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "score" to Schema.integer("CEFR-calibrated writing score 0-100"),
                "arabicFeedback" to Schema.string("Detailed Arabic evaluation of grammar, cohesion, vocabulary, and CEFR alignment with corrected version")
            )
        )
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    // Structured JSON unseen CEFR Exam Generator (Listening + Reading + Speaking + Writing)
    private val examGeneratorModel by lazy {
        val mcqSchema = Schema.obj(
            properties = mapOf(
                "promptAr" to Schema.string("Clear exam question instruction in Arabic"),
                "stimulusTarget" to Schema.string("Authentic passage or spoken script in target language"),
                "options" to Schema.array(Schema.string(), "3 plausible Arabic answer choices"),
                "correctIndex" to Schema.integer("Index 0..2 of the correct option")
            )
        )
        val examSchema = Schema.obj(
            properties = mapOf(
                "listeningQuestions" to Schema.array(mcqSchema, "2 unseen CEFR listening questions"),
                "readingQuestions" to Schema.array(mcqSchema, "2 unseen CEFR reading comprehension questions"),
                "speakingPromptAr" to Schema.string("Arabic speaking task instruction for the learner"),
                "speakingTargetContext" to Schema.string("Short situation/question in target language to answer aloud"),
                "writingPromptAr" to Schema.string("Arabic writing task instruction"),
                "writingPromptTarget" to Schema.string("Writing task prompt in target language")
            )
        )
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = examSchema
            }
        )
    }

    // Official CEFR Exam Grader (Speaking + Writing + Placement recommendation)
    private val examGraderModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "speakingScore" to Schema.integer("CEFR-calibrated speaking score 0-100"),
                "writingScore" to Schema.integer("CEFR-calibrated writing score 0-100"),
                "recommendedCefrCode" to Schema.string("Recommended CEFR code strictly one of: A1, A2, B1, B2, C1, C2"),
                "arabicFeedback" to Schema.string("Comprehensive Arabic examiner report covering speaking, writing, grammar accuracy, vocabulary range, and concrete improvement plan")
            )
        )
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    // Structured JSON on-demand unit sentence lesson generator
    private val unitLessonGeneratorModel by lazy {
        val exampleSchema = Schema.obj(
            properties = mapOf(
                "target" to Schema.string("Similar natural sentence in target language"),
                "arabic" to Schema.string("Arabic translation of the similar sentence")
            )
        )
        val sentenceSchema = Schema.obj(
            properties = mapOf(
                "sentence" to Schema.string("Authentic full sentence in target language calibrated to CEFR and dialect"),
                "arabicTranslation" to Schema.string("Natural Arabic translation"),
                "dialectVariantAlt" to Schema.string("Natural equivalent phrasing in the other dialect (US/UK or ES/LatAm)"),
                "expressionNoteAr" to Schema.string("Clear Arabic explanation of key idioms/expressions in this sentence"),
                "grammarInContextAr" to Schema.string("Clear Arabic grammar explanation embedded in this exact sentence"),
                "realWorldUsageAr" to Schema.string("When and where native speakers actually use this sentence (in Arabic)"),
                "similarExamples" to Schema.array(exampleSchema, "2 similar real-life example sentences"),
                "comprehensionQuestionAr" to Schema.string("Arabic comprehension question testing real meaning/usage"),
                "optionsAr" to Schema.array(Schema.string(), "3 Arabic answer options"),
                "correctOptionIndex" to Schema.integer("Correct option index 0..2")
            )
        )
        val wrapperSchema = Schema.obj(
            properties = mapOf(
                "sentences" to Schema.array(sentenceSchema, "4 fresh, progressive contextual sentences")
            )
        )
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = wrapperSchema
            }
        )
    }

    // AI Tutor end-of-session scorecard & recurring mistakes tracker
    private val tutorScorecardModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "overallScore" to Schema.integer("Overall conversation mastery score 0-100"),
                "fluencyScore" to Schema.integer("Conversational flow & response naturalness 0-100"),
                "grammarScore" to Schema.integer("Grammatical accuracy 0-100"),
                "vocabularyScore" to Schema.integer("Vocabulary precision and CEFR appropriateness 0-100"),
                "recurringMistakesAr" to Schema.array(Schema.string(), "2 to 4 concrete recurring mistake patterns noticed across the learner's turns in Arabic, with brief corrections"),
                "strengthsAr" to Schema.string("Encouraging Arabic summary of what the learner did well"),
                "nextFocusTipAr" to Schema.string("Actionable Arabic study recommendation for the next session"),
                "correctivePracticeSentence" to Schema.string("One polished target-language practice sentence directly addressing the learner's main error")
            )
        )
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    /**
     * Multi-turn streaming AI Tutor chat maintaining full conversation history and scenario role.
     */
    fun streamTutorReply(
        scenario: TutorScenario,
        language: TargetLanguage,
        dialect: Dialect,
        cefrLevel: CefrLevel,
        history: List<TutorMessage>,
        userInput: String,
        deepThinking: Boolean
    ): Flow<String> = flow {
        val roleLine = if (language == TargetLanguage.EN) scenario.roleInstructionEn else scenario.roleInstructionEs
        val systemPrompt = buildString {
            appendLine("You are LingoWorld AI Tutor.")
            appendLine(roleLine)
            appendLine("Target language: ${language.nativeName} (${dialect.nativeLabel}).")
            appendLine("Learner CEFR level: ${cefrLevel.code}. Adapt your vocabulary, sentence length, and complexity strictly to ${cefrLevel.code}.")
            appendLine("Always format your reply clearly in TWO sections:")
            appendLine("1) Natural response in ${language.nativeName} continuing the scenario and asking a follow-up question.")
            appendLine("2) A separator line '---شرح وتصحيح بالعربية---' followed by concise Arabic feedback: correct any grammar/wording mistakes from the learner's message, explain one useful expression in context, and suggest one stronger native phrasing.")
            if (deepThinking) {
                appendLine("DEEP THINKING MODE ENABLED: Perform thorough linguistic, pragmatic, and contrastive Arabic-to-${language.nativeName} analysis of the learner's message and highlight recurring error patterns and tailored micro-drills.")
            }
        }

        // Step 1: Verify App Check token fast (max 12s) so we never hang silently for minutes
        ensureAppCheckReady()

        // Strictly alternate user/model turns so previous unanswered user messages don't break chat history
        val sanitizedHistory = mutableListOf<com.google.firebase.ai.type.Content>()
        var expectedRole = "user"
        history.takeLast(10).forEach { msg ->
            val role = if (msg.sender == "user") "user" else "model"
            if (role == expectedRole && msg.text.isNotBlank()) {
                sanitizedHistory += content(role = role) { text(msg.text) }
                expectedRole = if (expectedRole == "user") "model" else "user"
            }
        }
        if (sanitizedHistory.lastOrNull()?.role == "user") {
            sanitizedHistory.removeAt(sanitizedHistory.lastIndex)
        }

        // Step 2: Fast unary sendMessage (avoids SSE streaming stalls on mobile 4G networks)
        val primaryModel = createTutorModel(systemPrompt, deepThinking, useFastModel = !deepThinking)
        var replyText = ""
        try {
            val chat = primaryModel.startChat(history = sanitizedHistory)
            val response = withTimeout(if (deepThinking) 35_000L else 20_000L) {
                chat.sendMessage(userInput)
            }
            replyText = response.text.orEmpty().trim()
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Primary tutor model timed out; trying fallback model", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = e.localizedMessage.orEmpty()
            val retryable = msg.contains("allotted time", ignoreCase = true) ||
                msg.contains("timeout", ignoreCase = true) ||
                msg.contains("not found", ignoreCase = true) ||
                msg.contains("404", ignoreCase = true)
            if (!retryable) throw e
            Log.w(TAG, "Primary tutor model retryable error ($msg); trying fallback model", e)
        }

        if (replyText.isBlank()) {
            val fallbackModel = createTutorModel(systemPrompt, deepThinking = false, useFastModel = false)
            val fallbackChat = fallbackModel.startChat(history = sanitizedHistory)
            val reply = withTimeout(22_000L) {
                fallbackChat.sendMessage(userInput)
            }
            replyText = reply.text.orEmpty().trim()
        }

        if (replyText.isBlank()) {
            throw IllegalStateException("لم يُرجع المدرس الذكي ردًا، يرجى المحاولة مرة أخرى.")
        }
        emit(replyText)
    }

    /**
     * Deep Thinking pronunciation analysis distinguishing dialect variance from real errors.
     */
    suspend fun evaluatePronunciation(
        targetSentence: String,
        recognizedText: String,
        language: TargetLanguage,
        dialect: Dialect,
        cefrLevel: CefrLevel
    ): PronunciationEvaluation {
        val prompt = """
            Evaluate an Arabic learner's spoken attempt for Pronunciation Lab.
            Target language & dialect: ${language.nativeName} (${dialect.nativeLabel}), Level: ${cefrLevel.code}.
            Target sentence: "$targetSentence"
            Speech-to-text transcript captured: "$recognizedText"

            Important rules:
            - Do NOT treat raw speech-to-text accuracy as the sole measure of pronunciation quality.
            - Distinguish acceptable dialect variation (e.g., US vs UK, Spain vs Latin America) from real phonetic/stress/rhythm mistakes common for Arabic speakers (such as p/b, v/f, vowel length, Spanish rolled r, syllable stress).
            - Provide realistic 0-100 scores for accuracyScore, stressScore, rhythmScore, fluencyScore, clarityScore.
            - Provide clear Arabic coaching and a targeted corrective drill sentence in ${language.nativeName}.
        """.trimIndent()

        return try {
            ensureAppCheckReady()
            val response = withTimeout(60_000L) {
                pronunciationModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Empty pronunciation evaluation")
            val json = JSONObject(raw)
            PronunciationEvaluation(
                accuracyScore = json.optInt("accuracyScore", 75).coerceIn(0, 100),
                stressScore = json.optInt("stressScore", 72).coerceIn(0, 100),
                rhythmScore = json.optInt("rhythmScore", 74).coerceIn(0, 100),
                fluencyScore = json.optInt("fluencyScore", 73).coerceIn(0, 100),
                clarityScore = json.optInt("clarityScore", 76).coerceIn(0, 100),
                dialectNoteAr = json.optString("dialectNoteAr", "تم مراعاة خصائص اللهجة المختارة أثناء التقييم."),
                arabicCoaching = json.optString("arabicCoaching", "أداء جيد! ركّز على وضوح المقاطع المشددة وإيقاع الجملة."),
                correctiveDrillTarget = json.optString("correctiveDrillTarget", targetSentence)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Pronunciation AI evaluation failed", e)
            throw e
        }
    }

    /**
     * Generates a fresh branching interactive story calibrated to CEFR level and genre.
     */
    suspend fun generateInteractiveStory(
        language: TargetLanguage,
        dialect: Dialect,
        cefrLevel: CefrLevel,
        genreAr: String
    ): InteractiveStory {
        val prompt = """
            Create an original, engaging interactive branching dialogue story for an Arabic learner.
            Target language: ${language.nativeName} (${dialect.nativeLabel}).
            CEFR Level: ${cefrLevel.code} (strictly match ${cefrLevel.code} grammar and vocabulary).
            Genre: $genreAr.
            Include 3 vivid dialogue turns between 2 distinct characters, 2 meaningful learner reply choices that lead to different story outcomes, and 1 Arabic comprehension question with 3 options.
        """.trimIndent()

        return try {
            ensureAppCheckReady()
            val response = withTimeout(60_000L) {
                storyGeneratorModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Empty story JSON")
            val json = JSONObject(raw)
            val turnsArr = json.getJSONArray("turns")
            val turns = (0 until turnsArr.length()).map { i ->
                val obj = turnsArr.getJSONObject(i)
                StoryTurn(
                    speakerName = obj.optString("speakerName", "شخصية ${i + 1}"),
                    voicePitch = if (i % 2 == 0) 1.12f else 0.88f,
                    text = obj.getString("text"),
                    arabicTranslation = obj.getString("arabicTranslation")
                )
            }
            val choicesArr = json.getJSONArray("choices")
            val choices = (0 until choicesArr.length()).map { i ->
                val obj = choicesArr.getJSONObject(i)
                StoryChoice(
                    replyText = obj.getString("replyText"),
                    arabicHint = obj.getString("arabicHint"),
                    outcomeSpeaker = obj.optString("outcomeSpeaker", "الراوي"),
                    outcomeReply = obj.getString("outcomeReply"),
                    outcomeArabic = obj.getString("outcomeArabic"),
                    feedbackAr = obj.getString("feedbackAr")
                )
            }
            val optionsArr = json.getJSONArray("comprehensionOptionsAr")
            val options = (0 until optionsArr.length()).map { optionsArr.getString(it) }

            InteractiveStory(
                id = "ai_story_${System.currentTimeMillis()}",
                language = language,
                cefrLevel = cefrLevel,
                genreAr = "$genreAr • مولّدة بالذكاء الاصطناعي",
                titleAr = json.getString("titleAr"),
                subtitleTarget = json.getString("subtitleTarget"),
                turns = turns,
                choices = choices,
                comprehensionQuestionAr = json.getString("comprehensionQuestionAr"),
                comprehensionOptionsAr = options,
                correctOptionIndex = json.optInt("correctOptionIndex", 0).coerceIn(0, (options.size - 1).coerceAtLeast(0))
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI story generation failed", e)
            throw e
        }
    }

    /**
     * Deep-thinking CEFR Writing & Exam Grader.
     */
    suspend fun gradeWritingSubmission(
        language: TargetLanguage,
        cefrLevel: CefrLevel,
        promptTask: String,
        learnerText: String
    ): Pair<Int, String> {
        val prompt = """
            You are a strict CEFR ${cefrLevel.code} examiner for ${language.nativeName}.
            Task prompt: "$promptTask"
            Learner writing submission: "$learnerText"
            Grade rigorously on CEFR ${cefrLevel.code} standards (grammar accuracy, sentence naturalness, task completion, cohesion).
            Return a realistic integer score 0-100 and detailed constructive feedback in Arabic including a polished model version in ${language.nativeName}.
        """.trimIndent()

        return try {
            ensureAppCheckReady()
            val response = withTimeout(60_000L) {
                writingGraderModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Empty writing grade")
            val json = JSONObject(raw)
            val score = json.optInt("score", 70).coerceIn(0, 100)
            val feedback = json.optString("arabicFeedback", "تم تقييم كتابتك بنجاح.")
            score to feedback
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Writing AI grading failed", e)
            throw e
        }
    }

    /**
     * Generates a brand-new, unseen CEFR exam set with 2 listening MCQs, 2 reading MCQs,
     * a real-world speaking prompt, and an open writing task.
     */
    suspend fun generateUnseenExamSet(
        language: TargetLanguage,
        dialect: Dialect,
        cefrLevel: CefrLevel,
        examType: String
    ): GeneratedExamSet {
        val examLabel = when (examType) {
            "placement" -> "Adaptive CEFR Placement Test spanning A1 to C2 difficulty cues"
            "final" -> "Strict Final CEFR ${cefrLevel.code} Mastery Certification Exam"
            "sublevel" -> "CEFR ${cefrLevel.code} Sub-Level Progression Exam"
            else -> "CEFR ${cefrLevel.code} Unit Mastery Exam"
        }
        val prompt = """
            Generate a brand-new, rigorous, unseen exam for an Arabic-speaking learner.
            Exam type: $examLabel
            Target language & dialect: ${language.nativeName} (${dialect.nativeLabel})
            Target CEFR level: ${cefrLevel.code} (${cefrLevel.summaryAr})

            Requirements:
            - Create 2 unseen Listening questions with authentic spoken scripts (stimulusTarget) in ${language.nativeName}, Arabic question prompt (promptAr), and 3 plausible Arabic options.
            - Create 2 unseen Reading comprehension/inference questions with realistic passages (stimulusTarget) in ${language.nativeName}, Arabic question prompt (promptAr), and 3 plausible Arabic options.
            - Create 1 realistic Speaking prompt (Arabic instruction + target-language context prompt).
            - Create 1 realistic open Writing task (Arabic instruction + target-language prompt).
            - Ensure questions test real understanding and nuance rather than trivial vocabulary matching.
        """.trimIndent()

        return try {
            ensureAppCheckReady()
            val response = withTimeout(60_000L) {
                examGeneratorModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Empty exam JSON")
            val json = JSONObject(raw)
            val stamp = System.currentTimeMillis()

            fun parseMcqs(key: String, skill: SkillType): List<ExamQuestion> {
                val arr = json.getJSONArray(key)
                return (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    val optArr = obj.getJSONArray("options")
                    val opts = (0 until optArr.length()).map { optArr.getString(it) }
                    ExamQuestion(
                        id = "${key}_${stamp}_$i",
                        skill = skill,
                        promptAr = obj.getString("promptAr"),
                        stimulusTarget = obj.getString("stimulusTarget"),
                        options = opts,
                        correctIndex = obj.optInt("correctIndex", 0).coerceIn(0, (opts.size - 1).coerceAtLeast(0))
                    )
                }
            }

            GeneratedExamSet(
                id = "exam_$stamp",
                listeningQuestions = parseMcqs("listeningQuestions", SkillType.LISTENING),
                readingQuestions = parseMcqs("readingQuestions", SkillType.READING),
                speakingPromptAr = json.getString("speakingPromptAr"),
                speakingTargetContext = json.getString("speakingTargetContext"),
                writingPromptAr = json.getString("writingPromptAr"),
                writingPromptTarget = json.getString("writingPromptTarget")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unseen exam generation failed", e)
            throw e
        }
    }

    /**
     * Deep-thinking strict CEFR evaluation for Speaking + Writing + automatic CEFR level placement.
     */
    suspend fun evaluateExamSpeakingAndWriting(
        language: TargetLanguage,
        cefrLevel: CefrLevel,
        examType: String,
        listeningScore: Int,
        readingScore: Int,
        speakingPrompt: String,
        speakingTranscript: String,
        writingPrompt: String,
        writingText: String
    ): ExamAiGradingResult {
        val prompt = """
            You are a certified, strict CEFR examiner for ${language.nativeName}.
            Exam type: $examType | Target CEFR Level: ${cefrLevel.code}.
            Learner's objective scores so far: Listening = $listeningScore%, Reading = $readingScore%.

            SPEAKING TASK: "$speakingPrompt"
            LEARNER SPOKEN RESPONSE TRANSCRIPT: "$speakingTranscript"

            WRITING TASK: "$writingPrompt"
            LEARNER WRITING SUBMISSION: "$writingText"

            Evaluate strictly according to official CEFR ${cefrLevel.code} descriptors:
            1. Assign a realistic speakingScore (0-100) based on task relevance, natural phrasing, grammatical control, and complexity.
            2. Assign a realistic writingScore (0-100) based on coherence, grammar accuracy, vocabulary range, and task completion.
            3. Determine recommendedCefrCode (strictly one of A1, A2, B1, B2, C1, C2) reflecting the learner's overall demonstrated mastery across all 4 skills.
            4. Write a clear, constructive Arabic examiner report (arabicFeedback) highlighting strengths, exact corrections for mistakes, and next study priorities.
        """.trimIndent()

        return try {
            ensureAppCheckReady()
            val response = withTimeout(60_000L) {
                examGraderModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Empty exam grading JSON")
            val json = JSONObject(raw)
            val speakingScore = json.optInt("speakingScore", 70).coerceIn(0, 100)
            val writingScore = json.optInt("writingScore", 70).coerceIn(0, 100)
            val recCode = json.optString("recommendedCefrCode", cefrLevel.code).uppercase().trim()
            val recommendedLevel = CefrLevel.entries.firstOrNull { it.code == recCode } ?: cefrLevel
            val feedback = json.optString("arabicFeedback", "تم تقييم مهارتي التحدث والكتابة وفق معايير CEFR.")
            ExamAiGradingResult(
                speakingScore = speakingScore,
                writingScore = writingScore,
                recommendedCefrLevel = recommendedLevel,
                arabicFeedback = feedback
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Exam speaking/writing AI grading failed", e)
            throw e
        }
    }

    /**
     * Generates 4 brand-new contextual sentence lessons tailored to a specific CEFR unit and sub-level (1..10).
     */
    suspend fun generateUnitLessonSentences(
        language: TargetLanguage,
        dialect: Dialect,
        cefrLevel: CefrLevel,
        unitNumber: Int,
        unitTitleAr: String,
        subLevel: Int
    ): List<SentenceLessonItem> {
        val prompt = """
            Create 4 fresh, authentic, sentence-based language lessons (NEVER isolated single words) for an Arabic-speaking learner.
            Target Language & Dialect: ${language.nativeName} (${dialect.nativeLabel}).
            CEFR Level: ${cefrLevel.code} (${cefrLevel.summaryAr}).
            Sub-level progression inside ${cefrLevel.code}: Level $subLevel out of 10 (1 = foundational entry of ${cefrLevel.code}, 10 = mastery peak of ${cefrLevel.code}).
            Unit $unitNumber Topic: $unitTitleAr.

            For each of the 4 sentences provide:
            - Natural full sentence in ${language.nativeName} matching ${dialect.nativeLabel}
            - Accurate Arabic translation
            - Alternative natural phrasing in the sibling dialect (US/UK for English, Spain/Latin America for Spanish)
            - Arabic explanation of expressions, grammar in context, and real-world usage
            - 2 similar daily-life example sentences with Arabic translation
            - 1 Arabic comprehension check question with 3 options and correctOptionIndex (0..2).
        """.trimIndent()

        return try {
            ensureAppCheckReady()
            val response = withTimeout(60_000L) {
                unitLessonGeneratorModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Empty AI unit lesson JSON")
            val json = JSONObject(raw)
            val arr = json.getJSONArray("sentences")
            val stamp = System.currentTimeMillis()
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val exArr = obj.optJSONArray("similarExamples")
                val examples = if (exArr != null) {
                    (0 until exArr.length()).map { idx ->
                        val exObj = exArr.getJSONObject(idx)
                        exObj.optString("target", "") to exObj.optString("arabic", "")
                    }.filter { it.first.isNotBlank() }
                } else emptyList()
                val optArr = obj.getJSONArray("optionsAr")
                val opts = (0 until optArr.length()).map { optArr.getString(it) }
                SentenceLessonItem(
                    id = "ai_u${unitNumber}_sub${subLevel}_${stamp}_$i",
                    language = language,
                    cefrLevel = cefrLevel,
                    unitNumber = unitNumber,
                    lessonNumber = subLevel,
                    sentence = obj.getString("sentence"),
                    arabicTranslation = obj.getString("arabicTranslation"),
                    dialectVariantAlt = obj.optString("dialectVariantAlt", ""),
                    expressionNoteAr = obj.optString("expressionNoteAr", ""),
                    grammarInContextAr = obj.optString("grammarInContextAr", ""),
                    realWorldUsageAr = obj.optString("realWorldUsageAr", ""),
                    similarExamples = examples,
                    comprehensionQuestionAr = obj.getString("comprehensionQuestionAr"),
                    optionsAr = opts,
                    correctOptionIndex = obj.optInt("correctOptionIndex", 0).coerceIn(0, (opts.size - 1).coerceAtLeast(0))
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI unit sentence lesson generation failed", e)
            throw e
        }
    }

    /**
     * Deep-thinking end-of-session scorecard analyzing recurring learner mistakes across an AI Tutor conversation.
     */
    suspend fun evaluateTutorSession(
        language: TargetLanguage,
        dialect: Dialect,
        cefrLevel: CefrLevel,
        scenario: TutorScenario,
        messages: List<TutorMessage>
    ): TutorSessionScorecard {
        val transcript = messages.takeLast(24).joinToString("\n") { msg ->
            val role = if (msg.sender == "user") "LEARNER" else "TUTOR"
            "$role: ${msg.text}"
        }
        val prompt = """
            Analyze this completed language tutoring session for an Arabic-speaking learner and produce an official end-of-session scorecard.
            Target language & dialect: ${language.nativeName} (${dialect.nativeLabel})
            Learner CEFR level: ${cefrLevel.code}
            Roleplay scenario: ${scenario.titleAr} (${scenario.subtitleAr})

            CONVERSATION TRANSCRIPT:
            $transcript

            Tasks:
            1. Grade overallScore, fluencyScore, grammarScore, and vocabularyScore (0-100) honestly relative to CEFR ${cefrLevel.code}.
            2. Identify 2 to 4 specific recurring mistakes or language transfer habits from Arabic found in the LEARNER's turns (written clearly in Arabic with the corrected phrasing).
            3. Summarize learner strengths in Arabic and give 1 concrete next focus tip in Arabic.
            4. Provide one polished correctivePracticeSentence in ${language.nativeName} that the learner can practice and save for spaced repetition.
        """.trimIndent()

        return try {
            ensureAppCheckReady()
            val response = withTimeout(60_000L) {
                tutorScorecardModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Empty tutor scorecard JSON")
            val json = JSONObject(raw)
            val mistakesArr = json.optJSONArray("recurringMistakesAr")
            val mistakes = if (mistakesArr != null) {
                (0 until mistakesArr.length()).map { mistakesArr.getString(it) }.filter { it.isNotBlank() }
            } else emptyList()

            TutorSessionScorecard(
                overallScore = json.optInt("overallScore", 76).coerceIn(0, 100),
                fluencyScore = json.optInt("fluencyScore", 75).coerceIn(0, 100),
                grammarScore = json.optInt("grammarScore", 74).coerceIn(0, 100),
                vocabularyScore = json.optInt("vocabularyScore", 77).coerceIn(0, 100),
                recurringMistakesAr = mistakes.ifEmpty {
                    listOf("مراجعة تركيب الجمل وزمن الأفعال في السياقات الحوارية الطبيعية.")
                },
                strengthsAr = json.optString("strengthsAr", "تفاعل ممتاز وجرأة واضحة في بناء جمل كاملة داخل الموقف."),
                nextFocusTipAr = json.optString("nextFocusTipAr", "استمر في استخدام الروابط الطبيعية وإعادة صياغة الجمل الطويلة."),
                correctivePracticeSentence = json.optString(
                    "correctivePracticeSentence",
                    if (language == TargetLanguage.EN) "Could you clarify what you mean so we can move forward smoothly?"
                    else "¿Podrías aclarar a qué te refieres para que podamos avanzar sin problemas?"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Tutor session scorecard failed", e)
            throw e
        }
    }
}
