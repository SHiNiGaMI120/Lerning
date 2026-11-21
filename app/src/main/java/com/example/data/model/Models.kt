package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import org.json.JSONArray
import org.json.JSONObject
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TargetLanguage(val code: String, val arabicName: String, val nativeName: String) {
    EN("EN", "الإنجليزية", "English"),
    ES("ES", "الإسبانية", "Español");

    companion object {
        fun fromCode(raw: String?): TargetLanguage =
            entries.firstOrNull { it.code == raw } ?: EN
    }
}

enum class Dialect(
    val code: String,
    val language: TargetLanguage,
    val arabicLabel: String,
    val nativeLabel: String
) {
    EN_US("en-US", TargetLanguage.EN, "الإنجليزية الأمريكية", "American English"),
    EN_GB("en-GB", TargetLanguage.EN, "الإنجليزية البريطانية", "British English"),
    ES_ES("es-ES", TargetLanguage.ES, "الإسبانية الأوروبية", "Español de España"),
    ES_MX("es-MX", TargetLanguage.ES, "الإسبانية اللاتينية", "Español Latinoamericano");

    companion object {
        fun fromCode(raw: String?, fallbackLang: TargetLanguage = TargetLanguage.EN): Dialect =
            entries.firstOrNull { it.code == raw }
                ?: if (fallbackLang == TargetLanguage.ES) ES_ES else EN_US
    }
}

enum class CefrLevel(val code: String, val arabicTitle: String, val summaryAr: String) {
    A1("A1", "مبتدئ تأسيسي", "جمل الحياة اليومية والتعريف بالنفس والطلبات البسيطة"),
    A2("A2", "مبتدئ متقدم", "حوارات السفر والتسوق والعمل اليومي ووصف التجارب"),
    B1("B1", "متوسط", "التعبير عن الآراء وسرد الأحداث والتعامل مع المواقف المفاجئة"),
    B2("B2", "فوق المتوسط", "النقاشات المهنية وفهم الفروق الدقيقة واللهجات الطبيعية"),
    C1("C1", "متقدم", "الطلاقة الأكاديمية والتعبيرات الاصطلاحية والمعاني الضمنية"),
    C2("C2", "إتقان احترافي", "الإقناع البلاغي وتحليل الخطاب المتخصص كالمتحدث الأصلي");

    companion object {
        fun fromCode(raw: String?): CefrLevel =
            entries.firstOrNull { it.code == raw } ?: A1

        fun nextLevel(current: CefrLevel): CefrLevel {
            val idx = entries.indexOf(current)
            return entries.getOrElse(idx + 1) { C2 }
        }
    }
}

enum class SkillType(val id: String, val arabicName: String, val subtitleAr: String) {
    LISTENING("listening", "الاستماع", "فهم الحوارات واللهجات والمعاني الضمنية"),
    SPEAKING("speaking", "التحدث", "المحادثة ووصف المواقف وسرد القصص"),
    READING("reading", "القراءة", "تحليل القصص والمقالات والنصوص المهنية"),
    WRITING("writing", "الكتابة", "بناء الجمل والفقرات والرسائل والحجج")
}

// --- Firestore Entities (1:1 parity with firebase-blueprint.json & firestore.rules) ---

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "متعلم LingoWorld",
    val activeLanguage: String = "EN",
    val englishDialect: String = "en-US",
    val spanishDialect: String = "es-ES",
    val englishLevel: String = "A1",
    val spanishLevel: String = "A1",
    val englishSubLevel: Long = 1L,
    val spanishSubLevel: Long = 1L,
    val xp: Long = 0L,
    val streakDays: Long = 1L,
    val dailyGoalMinutes: Long = 15L,
    val listeningMinutes: Long = 0L,
    val completedLessonsCount: Long = 0L,
    val role: String = "learner",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun currentLanguageEnum(): TargetLanguage = TargetLanguage.fromCode(activeLanguage)
    fun currentDialectCode(): String =
        if (currentLanguageEnum() == TargetLanguage.ES) spanishDialect else englishDialect
    fun currentLevelEnum(): CefrLevel =
        CefrLevel.fromCode(if (currentLanguageEnum() == TargetLanguage.ES) spanishLevel else englishLevel)
    fun currentSubLevel(): Int =
        (if (currentLanguageEnum() == TargetLanguage.ES) spanishSubLevel else englishSubLevel).toInt().coerceIn(1, 10)
}

@Keep
@IgnoreExtraProperties
data class ReviewCard(
    @DocumentId val id: String = "",
    val sentenceText: String = "",
    val arabicTranslation: String = "",
    val language: String = "EN",
    val dialect: String = "en-US",
    val cefrLevel: String = "A1",
    val grammarNote: String = "",
    val masteryScore: Long = 20L,
    val stabilityDays: Double = 1.0,
    val difficulty: Double = 5.0,
    val reviewCount: Long = 0L,
    val lapseCount: Long = 0L,
    val pronunciationScore: Long = 0L,
    val nextReviewEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TutorMessage(
    @DocumentId val id: String = "",
    val scenarioId: String = "free_chat",
    val language: String = "EN",
    val sender: String = "user",
    val text: String = "",
    val arabicExplanation: String = "",
    val deepThinking: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExamAttempt(
    @DocumentId val id: String = "",
    val language: String = "EN",
    val cefrLevel: String = "A1",
    val examType: String = "unit",
    val listeningScore: Long = 0L,
    val speakingScore: Long = 0L,
    val readingScore: Long = 0L,
    val writingScore: Long = 0L,
    val totalScore: Long = 0L,
    val passed: Boolean = false,
    val aiFeedback: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PronunciationLog(
    @DocumentId val id: String = "",
    val language: String = "EN",
    val dialect: String = "en-US",
    val targetSentence: String = "",
    val recognizedText: String = "",
    val accuracyScore: Long = 0L,
    val stressScore: Long = 0L,
    val rhythmScore: Long = 0L,
    val fluencyScore: Long = 0L,
    val clarityScore: Long = 0L,
    val arabicCoaching: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LessonProgress(
    @DocumentId val id: String = "",
    val language: String = "EN",
    val cefrLevel: String = "A1",
    val unitNumber: Long = 1L,
    val lessonNumber: Long = 1L,
    val contentType: String = "lesson",
    val completed: Boolean = false,
    val score: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CmsContentItem(
    @DocumentId val id: String = "",
    val language: String = "EN",
    val dialect: String = "en-US",
    val cefrLevel: String = "A1",
    val unitNumber: Long = 1L,
    val contentType: String = "sentence",
    val title: String = "",
    val bodyText: String = "",
    val arabicTranslation: String = "",
    val grammarNote: String = "",
    val youtubeVideoId: String = "",
    val published: Boolean = true,
    val authorId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// --- Rich Curriculum Domain Models ---

data class SentenceLessonItem(
    val id: String,
    val language: TargetLanguage,
    val cefrLevel: CefrLevel,
    val unitNumber: Int,
    val lessonNumber: Int,
    val sentence: String,
    val dialectVariantAlt: String,
    val arabicTranslation: String,
    val expressionNoteAr: String,
    val grammarInContextAr: String,
    val realWorldUsageAr: String,
    val similarExamples: List<Pair<String, String>>,
    val comprehensionQuestionAr: String,
    val optionsAr: List<String>,
    val correctOptionIndex: Int
)

data class CurriculumUnit(
    val number: Int,
    val cefrLevel: CefrLevel,
    val language: TargetLanguage,
    val titleAr: String,
    val themeTagAr: String,
    val lessonTitlesAr: List<String>
)

data class StoryTurn(
    val speakerName: String,
    val voicePitch: Float,
    val text: String,
    val arabicTranslation: String
)

data class StoryChoice(
    val replyText: String,
    val arabicHint: String,
    val outcomeSpeaker: String,
    val outcomeReply: String,
    val outcomeArabic: String,
    val feedbackAr: String
)

data class InteractiveStory(
    val id: String,
    val language: TargetLanguage,
    val cefrLevel: CefrLevel,
    val genreAr: String,
    val titleAr: String,
    val subtitleTarget: String,
    val turns: List<StoryTurn>,
    val choices: List<StoryChoice>,
    val comprehensionQuestionAr: String,
    val comprehensionOptionsAr: List<String>,
    val correctOptionIndex: Int
)

data class TimedSubtitleLine(
    val startSeconds: Int,
    val endSeconds: Int,
    val text: String,
    val arabicTranslation: String,
    val expressionNoteAr: String
)

data class VideoLesson(
    val id: String,
    val youtubeVideoId: String,
    val titleAr: String,
    val language: TargetLanguage,
    val dialect: Dialect,
    val cefrLevel: CefrLevel,
    val lines: List<TimedSubtitleLine>,
    val quizQuestionAr: String,
    val quizOptionsAr: List<String>,
    val correctOptionIndex: Int
)

data class TutorScenario(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val roleInstructionEn: String,
    val roleInstructionEs: String,
    val starterEn: String,
    val starterEs: String
)

data class ExamQuestion(
    val id: String,
    val skill: SkillType,
    val promptAr: String,
    val stimulusTarget: String,
    val options: List<String>,
    val correctIndex: Int
)

data class PronunciationEvaluation(
    val accuracyScore: Int,
    val stressScore: Int,
    val rhythmScore: Int,
    val fluencyScore: Int,
    val clarityScore: Int,
    val dialectNoteAr: String,
    val arabicCoaching: String,
    val correctiveDrillTarget: String
)

data class GeneratedExamSet(
    val id: String,
    val listeningQuestions: List<ExamQuestion>,
    val readingQuestions: List<ExamQuestion>,
    val speakingPromptAr: String,
    val speakingTargetContext: String,
    val writingPromptAr: String,
    val writingPromptTarget: String
)

data class ExamAiGradingResult(
    val speakingScore: Int,
    val writingScore: Int,
    val recommendedCefrLevel: CefrLevel,
    val arabicFeedback: String
)

data class TutorSessionScorecard(
    val overallScore: Int,
    val fluencyScore: Int,
    val grammarScore: Int,
    val vocabularyScore: Int,
    val recurringMistakesAr: List<String>,
    val strengthsAr: String,
    val nextFocusTipAr: String,
    val correctivePracticeSentence: String
)

data class SavedStoryDoc(
    @DocumentId val id: String = "",
    val language: String = "EN",
    val cefrLevel: String = "A1",
    val genreAr: String = "",
    val titleAr: String = "",
    val subtitleTarget: String = "",
    val storyJson: String = "",
    val createdAt: Timestamp? = null
) {
    fun toInteractiveStory(): InteractiveStory? = try {
        val json = JSONObject(storyJson)
        val turnsArr = json.optJSONArray("turns") ?: JSONArray()
        val turns = (0 until turnsArr.length()).map { i ->
            val o = turnsArr.getJSONObject(i)
            StoryTurn(
                speakerName = o.optString("speakerName", "شخصية ${i + 1}"),
                voicePitch = o.optDouble("voicePitch", if (i % 2 == 0) 1.1 else 0.9).toFloat(),
                text = o.optString("text", ""),
                arabicTranslation = o.optString("arabicTranslation", "")
            )
        }.filter { it.text.isNotBlank() }
        val choicesArr = json.optJSONArray("choices") ?: JSONArray()
        val choices = (0 until choicesArr.length()).map { i ->
            val o = choicesArr.getJSONObject(i)
            StoryChoice(
                replyText = o.optString("replyText", ""),
                arabicHint = o.optString("arabicHint", ""),
                outcomeSpeaker = o.optString("outcomeSpeaker", "الراوي"),
                outcomeReply = o.optString("outcomeReply", ""),
                outcomeArabic = o.optString("outcomeArabic", ""),
                feedbackAr = o.optString("feedbackAr", "")
            )
        }.filter { it.replyText.isNotBlank() }
        val optsArr = json.optJSONArray("comprehensionOptionsAr") ?: JSONArray()
        val opts = (0 until optsArr.length()).map { optsArr.getString(it) }
        if (turns.isEmpty()) null else InteractiveStory(
            id = id.ifBlank { "saved_${titleAr.hashCode()}" },
            language = TargetLanguage.fromCode(language),
            cefrLevel = CefrLevel.fromCode(cefrLevel),
            genreAr = genreAr.ifBlank { "محفوظة في السحابة" },
            titleAr = titleAr,
            subtitleTarget = subtitleTarget,
            turns = turns,
            choices = choices,
            comprehensionQuestionAr = json.optString("comprehensionQuestionAr", "ما الفكرة الأساسية في القصة؟"),
            comprehensionOptionsAr = opts.ifEmpty { listOf("التواصل الطبيعي في الموقف", "موضوع مختلف") },
            correctOptionIndex = json.optInt("correctOptionIndex", 0)
        )
    } catch (_: Exception) {
        null
    }

    companion object {
        fun encodeStoryJson(story: InteractiveStory): String {
            val turnsArr = JSONArray()
            story.turns.forEach { t ->
                turnsArr.put(
                    JSONObject().apply {
                        put("speakerName", t.speakerName)
                        put("voicePitch", t.voicePitch.toDouble())
                        put("text", t.text)
                        put("arabicTranslation", t.arabicTranslation)
                    }
                )
            }
            val choicesArr = JSONArray()
            story.choices.forEach { c ->
                choicesArr.put(
                    JSONObject().apply {
                        put("replyText", c.replyText)
                        put("arabicHint", c.arabicHint)
                        put("outcomeSpeaker", c.outcomeSpeaker)
                        put("outcomeReply", c.outcomeReply)
                        put("outcomeArabic", c.outcomeArabic)
                        put("feedbackAr", c.feedbackAr)
                    }
                )
            }
            val optsArr = JSONArray()
            story.comprehensionOptionsAr.forEach { optsArr.put(it) }
            return JSONObject().apply {
                put("turns", turnsArr)
                put("choices", choicesArr)
                put("comprehensionQuestionAr", story.comprehensionQuestionAr)
                put("comprehensionOptionsAr", optsArr)
                put("correctOptionIndex", story.correctOptionIndex)
            }.toString()
        }
    }
}

data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val activeLanguage: String = "EN",
    val activeLevel: String = "A1",
    val xp: Long = 0L,
    val streakDays: Long = 1L,
    val updatedAt: Timestamp? = null
)


