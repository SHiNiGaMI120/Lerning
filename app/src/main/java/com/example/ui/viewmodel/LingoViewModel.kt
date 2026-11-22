package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CefrLevel
import com.example.data.model.CmsContentItem
import com.example.data.model.Dialect
import com.example.data.model.ExamAttempt
import com.example.data.model.InteractiveStory
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LessonProgress
import com.example.data.model.PronunciationLog
import com.example.data.model.ReviewCard
import com.example.data.model.TargetLanguage
import com.example.data.model.UserProfile
import com.example.core.WeekKey
import com.example.data.repository.LingoRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "LingoViewModel"

sealed interface DataState<out T> {
    data object Loading : DataState<Nothing>
    data class Success<T>(val data: T) : DataState<T>
    data class Error(val message: String) : DataState<Nothing>
}

class LingoViewModel(
    private val repository: LingoRepository,
    private val currentUserId: String,
    initialDisplayName: String
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    val profileState: StateFlow<DataState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .toDataState("فشل تحميل ملف المتعلم")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading)

    val reviewCardsState: StateFlow<DataState<List<ReviewCard>>> =
        repository.observeReviewCards(currentUserId)
            .toDataState("فشل تحميل بطاقات المراجعة")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading)

    val examAttemptsState: StateFlow<DataState<List<ExamAttempt>>> =
        repository.observeExamAttempts(currentUserId)
            .toDataState("فشل تحميل سجل الاختبارات")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading)

    val pronunciationLogsState: StateFlow<DataState<List<PronunciationLog>>> =
        repository.observePronunciationLogs(currentUserId)
            .toDataState("فشل تحميل سجل النطق")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading)

    val lessonProgressState: StateFlow<DataState<List<LessonProgress>>> =
        repository.observeLessonProgress(currentUserId)
            .toDataState("فشل تحميل تقدم الدروس")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val cmsContentState: StateFlow<DataState<List<CmsContentItem>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAllCmsContentAdmin(currentUserId)
            else repository.observePublishedCmsContent(currentUserId)
        }
            .toDataState("فشل تحميل مكتبة المحتوى السحابية")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading)

    val savedStoriesState: StateFlow<DataState<List<InteractiveStory>>> =
        repository.observeSavedStories(currentUserId)
            .toDataState("فشل تحميل القصص المحفوظة")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Success(emptyList()))

    val leaderboardState: StateFlow<DataState<List<LeaderboardEntry>>> =
        repository.observeLeaderboard(currentUserId)
            .toDataState("فشل تحميل لوحة المتصدرين")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Success(emptyList()))

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminLearnersState: StateFlow<DataState<List<UserProfile>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) {
                repository.observeAllLearnersAdmin(currentUserId)
                    .toDataState("فشل تحميل تقارير المتعلمين")
            } else {
                flowOf<DataState<List<UserProfile>>>(DataState.Success(emptyList()))
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Success(emptyList()))

    init {
        viewModelScope.launch {
            _isAdmin.value = repository.isCurrentUserAdmin()
        }
        viewModelScope.launch {
            val result = repository.ensureUserProfile(initialDisplayName)
            result.onFailure {
                _statusBanner.value = "تعذر تهيئة الملف السحابي: ${it.localizedMessage ?: "خطأ غير معروف"}"
            }
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun showBanner(message: String) {
        _statusBanner.value = message
    }

    fun switchLanguage(language: TargetLanguage) {
        viewModelScope.launch {
            repository.updateProfileFields(mapOf("activeLanguage" to language.code))
                .onSuccess { _statusBanner.value = "تم التبديل إلى مسار ${language.arabicName}" }
                .onFailure { _statusBanner.value = "تعذر تحديث اللغة: ${it.localizedMessage}" }
        }
    }

    fun selectDialect(dialect: Dialect) {
        val field = if (dialect.language == TargetLanguage.ES) "spanishDialect" else "englishDialect"
        viewModelScope.launch {
            repository.updateProfileFields(mapOf(field to dialect.code))
                .onSuccess { _statusBanner.value = "تم اختيار لهجة: ${dialect.arabicLabel}" }
                .onFailure { _statusBanner.value = "تعذر تحديث اللهجة: ${it.localizedMessage}" }
        }
    }

    fun selectLevel(language: TargetLanguage, level: CefrLevel) {
        val field = if (language == TargetLanguage.ES) "spanishLevel" else "englishLevel"
        viewModelScope.launch {
            repository.updateProfileFields(mapOf(field to level.code))
                .onSuccess { _statusBanner.value = "تم تعيين مستوى ${language.arabicName} إلى ${level.code}" }
                .onFailure { _statusBanner.value = "تعذر تحديث المستوى: ${it.localizedMessage}" }
        }
    }

    fun selectSubLevel(language: TargetLanguage, subLevel: Int) {
        val safeSub = subLevel.coerceIn(1, 10)
        val field = if (language == TargetLanguage.ES) "spanishSubLevel" else "englishSubLevel"
        viewModelScope.launch {
            repository.updateProfileFields(mapOf(field to safeSub.toLong()))
                .onSuccess { _statusBanner.value = "تم الانتقال إلى الليفل $safeSub من 10 في ${language.arabicName}" }
                .onFailure { _statusBanner.value = "تعذر تحديث الليفل: ${it.localizedMessage}" }
        }
    }

    fun updateDailyGoal(minutes: Int) {
        viewModelScope.launch {
            repository.updateProfileFields(mapOf("dailyGoalMinutes" to minutes.coerceIn(5, 180).toLong()))
                .onSuccess { _statusBanner.value = "تم تحديث الهدف اليومي إلى $minutes دقيقة" }
                .onFailure { _statusBanner.value = "تعذر تحديث الهدف اليومي: ${it.localizedMessage}" }
        }
    }

    fun saveSentenceToReview(
        sentence: String,
        arabic: String,
        language: TargetLanguage,
        dialectCode: String,
        level: CefrLevel,
        grammarNote: String
    ) {
        viewModelScope.launch {
            repository.saveReviewCard(
                sentenceText = sentence,
                arabicTranslation = arabic,
                language = language.code,
                dialect = dialectCode,
                cefrLevel = level.code,
                grammarNote = grammarNote
            ).onSuccess {
                _statusBanner.value = "تم حفظ الجملة في نظام المراجعة الذكية (FSRS)"
            }.onFailure {
                _statusBanner.value = "تعذر حفظ الجملة: ${it.localizedMessage}"
            }
        }
    }

    fun submitFsrsReview(card: ReviewCard, rating: Int, responseTimeMs: Long) {
        viewModelScope.launch {
            repository.submitCardReview(card, rating, responseTimeMs)
                .onSuccess {
                    val label = when (rating) {
                        1 -> "ستُعاد الجملة قريبًا لتثبيتها"
                        2 -> "تمت الجدولة (صعب) +5 XP"
                        3 -> "ممتاز! تمت الجدولة الذكية +15 XP"
                        else -> "إتقان رائع! فترة مراجعة أطول +15 XP"
                    }
                    _statusBanner.value = label
                }
                .onFailure { _statusBanner.value = "تعذر حفظ نتيجة المراجعة: ${it.localizedMessage}" }
        }
    }

    fun completeLessonItem(
        progressId: String,
        language: TargetLanguage,
        level: CefrLevel,
        unitNumber: Int,
        lessonNumber: Int,
        contentType: String,
        score: Int
    ) {
        viewModelScope.launch {
            repository.saveLessonProgress(
                progressId = progressId,
                language = language.code,
                cefrLevel = level.code,
                unitNumber = unitNumber,
                lessonNumber = lessonNumber,
                contentType = contentType,
                score = score
            ).onSuccess {
                _statusBanner.value = "أحسنت! تم تسجيل الإنجاز وإضافة +40 XP"
            }.onFailure {
                _statusBanner.value = "تعذر حفظ التقدم: ${it.localizedMessage}"
            }
        }
    }

    fun recordExamResult(
        language: TargetLanguage,
        level: CefrLevel,
        examType: String,
        listening: Int,
        speaking: Int,
        reading: Int,
        writing: Int,
        feedback: String,
        recommendedPlacementLevel: CefrLevel? = null,
        currentSubLevel: Int = 1
    ) {
        viewModelScope.launch {
            repository.saveExamAttempt(
                language = language.code,
                cefrLevel = level.code,
                examType = examType,
                listeningScore = listening,
                speakingScore = speaking,
                readingScore = reading,
                writingScore = writing,
                aiFeedback = feedback
            ).onSuccess { passed ->
                val levelField = if (language == TargetLanguage.ES) "spanishLevel" else "englishLevel"
                val subField = if (language == TargetLanguage.ES) "spanishSubLevel" else "englishSubLevel"
                when {
                    examType == "placement" && recommendedPlacementLevel != null -> {
                        repository.updateProfileFields(
                            mapOf(
                                levelField to recommendedPlacementLevel.code,
                                subField to 1L
                            )
                        )
                        _statusBanner.value = "تم تحديد مستواك تلقائيًا في ${language.arabicName}: ${recommendedPlacementLevel.code} (${recommendedPlacementLevel.arabicTitle})"
                    }
                    passed && examType == "final" -> {
                        val next = CefrLevel.nextLevel(level)
                        repository.updateProfileFields(
                            mapOf(
                                levelField to next.code,
                                subField to 1L
                            )
                        )
                        _statusBanner.value = "مبروك! اجتزت الاختبار النهائي وانتقلت إلى ${next.code} وتم إصدار شهادتك (+120 XP)"
                    }
                    passed && examType == "sublevel" -> {
                        val nextSub = (currentSubLevel + 1).coerceAtMost(10)
                        repository.updateProfileFields(mapOf(subField to nextSub.toLong()))
                        _statusBanner.value = "ممتاز! تمت ترقيتك إلى الليفل $nextSub من 10 في مستوى ${level.code} (+120 XP)"
                    }
                    passed -> {
                        _statusBanner.value = "ممتاز! اجتزت الاختبار بنجاح (≥80% إجمالي و≥70% لكل مهارة) +120 XP"
                    }
                    else -> {
                        _statusBanner.value = "تم حفظ النتيجة (+35 XP). تذكّر: شرط الاجتياز 80% إجماليًا و70% لكل مهارة."
                    }
                }
            }.onFailure {
                _statusBanner.value = "تعذر حفظ نتيجة الاختبار: ${it.localizedMessage}"
            }
        }
    }

    fun createCmsContent(
        language: TargetLanguage,
        dialectCode: String,
        level: CefrLevel,
        unitNumber: Int,
        contentType: String,
        title: String,
        bodyText: String,
        arabicTranslation: String,
        grammarNote: String,
        youtubeVideoId: String,
        published: Boolean
    ) {
        if (title.isBlank() || bodyText.isBlank() || arabicTranslation.isBlank()) {
            _statusBanner.value = "يرجى ملء العنوان والنص والترجمة العربية أولًا."
            return
        }
        viewModelScope.launch {
            repository.createCmsContent(
                language = language.code,
                dialect = dialectCode,
                cefrLevel = level.code,
                unitNumber = unitNumber,
                contentType = contentType,
                title = title,
                bodyText = bodyText,
                arabicTranslation = arabicTranslation,
                grammarNote = grammarNote,
                youtubeVideoId = youtubeVideoId,
                published = published
            ).onSuccess {
                _statusBanner.value = "تمت إضافة المحتوى بنجاح إلى قاعدة البيانات السحابية"
            }.onFailure {
                _statusBanner.value = "تعذر حفظ المحتوى (تأكد من صلاحية المدير): ${it.localizedMessage}"
            }
        }
    }

    fun toggleCmsPublished(item: CmsContentItem) {
        viewModelScope.launch {
            repository.toggleCmsPublished(item.id, !item.published)
                .onSuccess {
                    _statusBanner.value = if (!item.published) "تم نشر المحتوى للمتعلمين" else "تم تحويل المحتوى إلى مسودة"
                }
                .onFailure { _statusBanner.value = "تعذر تعديل حالة النشر: ${it.localizedMessage}" }
        }
    }

    fun deleteCmsItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteCmsContent(itemId)
                .onSuccess { _statusBanner.value = "تم حذف عنصر المحتوى" }
                .onFailure { _statusBanner.value = "تعذر الحذف: ${it.localizedMessage}" }
        }
    }

    fun deleteSavedStory(storyId: String) {
        viewModelScope.launch {
            repository.deleteSavedStory(storyId)
                .onSuccess { _statusBanner.value = "تم حذف القصة من مكتبتك السحابية" }
                .onFailure { _statusBanner.value = "تعذر حذف القصة: ${it.localizedMessage}" }
        }
    }

    fun toggleLeaderboardOptIn(profile: UserProfile, optIn: Boolean) {
        viewModelScope.launch {
            repository.setLeaderboardOptIn(profile, optIn)
                .onSuccess {
                    _statusBanner.value = if (optIn) "تمت مشاركة تقدمك في لوحة المتصدرين الاختيارية!"
                    else "تم إخفاء اسمك من لوحة المتصدرين بنجاح"
                }
                .onFailure { _statusBanner.value = "تعذر تحديث إعداد لوحة المتصدرين: ${it.localizedMessage}" }
        }
    }

    fun claimWeeklyChallenge(
        challengeId: String,
        language: TargetLanguage,
        level: CefrLevel,
        bonusXp: Long = 50L
    ) {
        viewModelScope.launch {
            // Week-keyed claim id: the challenge can be claimed once per ISO week.
            val progressId = "weekly_${WeekKey.current()}_$challengeId"
            repository.claimWeeklyChallenge(
                progressId = progressId,
                language = language.code,
                cefrLevel = level.code
            ).onSuccess { granted ->
                if (granted) {
                    // Only the displayed weekly bonus is granted; the claim
                    // record itself never triggers the normal lesson reward.
                    repository.rewardXp(xpDelta = bonusXp)
                    _statusBanner.value = "ممتاز! تم إتمام التحدي الأسبوعي وإضافة +$bonusXp XP"
                } else {
                    _statusBanner.value = "تم استلام مكافأة هذا التحدي لهذا الأسبوع بالفعل"
                }
            }.onFailure {
                _statusBanner.value = "تعذر استلام مكافأة التحدي: ${it.localizedMessage}"
            }
        }
    }

    private fun <T> Flow<T>.toDataState(fallbackMessage: String): Flow<DataState<T>> =
        this.map<T, DataState<T>> { DataState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackMessage, rootError)
                emit(DataState.Error(rootError.localizedMessage ?: fallbackMessage))
            }
}
