package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.FsrsScheduler
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.CmsContentItem
import com.example.data.model.ExamAttempt
import com.example.data.model.InteractiveStory
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LessonProgress
import com.example.data.model.PronunciationLog
import com.example.data.model.ReviewCard
import com.example.data.model.SavedStoryDoc
import com.example.data.model.TutorMessage
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val ADMIN_EMAIL = "liongang377@gmail.com"

class LingoRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب Google أولًا.")
    }

    fun isCurrentUserAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email.equals(ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified
    }

    // --- User Profile ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(displayName: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}"
        return try {
            val verifiedUid = requireUserId()
            val docRef = db.collection("users").document(verifiedUid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().ifBlank { "متعلم LingoWorld" }.take(80)
                val payload = mapOf(
                    "userId" to verifiedUid,
                    "displayName" to safeName,
                    "activeLanguage" to "EN",
                    "englishDialect" to "en-US",
                    "spanishDialect" to "es-ES",
                    "englishLevel" to "A1",
                    "spanishLevel" to "A1",
                    "englishSubLevel" to 1L,
                    "spanishSubLevel" to 1L,
                    "xp" to 0L,
                    "streakDays" to 1L,
                    "dailyGoalMinutes" to 15L,
                    "listeningMinutes" to 0L,
                    "completedLessonsCount" to 0L,
                    "role" to "learner",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                // Automatic daily streak progression check on launch
                val existingProfile = snap.toObject(UserProfile::class.java)
                if (existingProfile != null) {
                    val newStreak = computeUpdatedStreak(existingProfile.updatedAt, existingProfile.streakDays)
                    if (newStreak != existingProfile.streakDays) {
                        docRef.update(
                            mapOf(
                                "streakDays" to newStreak,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private fun computeUpdatedStreak(lastUpdated: Timestamp?, currentStreak: Long): Long {
        val base = currentStreak.coerceAtLeast(1L)
        val lastMs = lastUpdated?.toDate()?.time ?: return base
        val dayMs = 86_400_000L
        val lastDay = lastMs / dayMs
        val today = System.currentTimeMillis() / dayMs
        val diff = today - lastDay
        return when {
            diff <= 0L -> base
            diff == 1L -> (base + 1L).coerceAtMost(10000L)
            else -> 1L
        }
    }

    suspend fun updateProfileFields(updates: Map<String, Any>): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}"
        return try {
            val verifiedUid = requireUserId()
            val safeUpdates = updates.toMutableMap().apply {
                remove("userId")
                remove("createdAt")
                remove("role")
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(verifiedUid).update(safeUpdates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun rewardXp(xpDelta: Long, listeningDeltaMinutes: Long = 0L, lessonCompletedDelta: Long = 0L): Result<Unit> {
        val verifiedUid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val updates = mutableMapOf<String, Any>(
            "xp" to FieldValue.increment(xpDelta.coerceAtLeast(0L))
        )
        if (listeningDeltaMinutes > 0L) {
            updates["listeningMinutes"] = FieldValue.increment(listeningDeltaMinutes)
        }
        if (lessonCompletedDelta > 0L) {
            updates["completedLessonsCount"] = FieldValue.increment(lessonCompletedDelta)
        }

        // Automatically update daily streak & keep opt-in leaderboard synced
        try {
            val snap = db.collection("users").document(verifiedUid).get().await()
            val existingProfile = snap.toObject(UserProfile::class.java)
            if (existingProfile != null) {
                val updatedStreak = computeUpdatedStreak(existingProfile.updatedAt, existingProfile.streakDays)
                if (updatedStreak != existingProfile.streakDays) {
                    updates["streakDays"] = updatedStreak
                }
                val result = updateProfileFields(updates)
                if (result.isSuccess) {
                    syncLeaderboardIfOptedIn(
                        uid = verifiedUid,
                        displayName = existingProfile.displayName,
                        activeLanguage = existingProfile.activeLanguage,
                        activeLevel = existingProfile.currentLevelEnum().code,
                        xp = (existingProfile.xp + xpDelta.coerceAtLeast(0L)).coerceAtMost(10_000_000L),
                        streakDays = updatedStreak
                    )
                }
                return result
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Fall through to standard update
        }
        return updateProfileFields(updates)
    }

    private suspend fun syncLeaderboardIfOptedIn(
        uid: String,
        displayName: String,
        activeLanguage: String,
        activeLevel: String,
        xp: Long,
        streakDays: Long
    ) {
        try {
            val lbDoc = db.collection("leaderboard").document(uid)
            val snap = lbDoc.get().await()
            if (snap.exists()) {
                lbDoc.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to displayName.trim().ifBlank { "متعلم LingoWorld" }.take(80),
                        "activeLanguage" to if (activeLanguage == "ES") "ES" else "EN",
                        "activeLevel" to activeLevel,
                        "xp" to xp.coerceIn(0L, 10_000_000L),
                        "streakDays" to streakDays.coerceIn(0L, 10_000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Non-blocking sync
        }
    }

    // --- FSRS Review Cards ---

    fun observeReviewCards(userId: String): Flow<List<ReviewCard>> = flow {
        val path = "users/$userId/reviewCards"
        emitAll(
            db.collection("users").document(userId).collection("reviewCards")
                .orderBy("nextReviewEpochMs", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ReviewCard::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun saveReviewCard(
        sentenceText: String,
        arabicTranslation: String,
        language: String,
        dialect: String,
        cefrLevel: String,
        grammarNote: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/reviewCards"
        return try {
            val verifiedUid = requireUserId()
            val docRef = db.collection("users").document(verifiedUid).collection("reviewCards").document()
            val payload = mapOf(
                "sentenceText" to sentenceText.trim().take(500),
                "arabicTranslation" to arabicTranslation.trim().take(500),
                "language" to if (language == "ES") "ES" else "EN",
                "dialect" to dialect.take(16),
                "cefrLevel" to cefrLevel.take(2),
                "grammarNote" to grammarNote.trim().take(1000),
                "masteryScore" to 20L,
                "stabilityDays" to 1.0,
                "difficulty" to 5.0,
                "reviewCount" to 0L,
                "lapseCount" to 0L,
                "pronunciationScore" to 0L,
                "nextReviewEpochMs" to System.currentTimeMillis(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitCardReview(
        card: ReviewCard,
        rating: Int,
        responseTimeMs: Long
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/reviewCards/${card.id}"
        return try {
            val verifiedUid = requireUserId()
            require(card.id.isNotBlank()) { "Card ID must not be blank" }
            val next = FsrsScheduler.computeNext(
                currentMastery = card.masteryScore.toInt(),
                currentStability = card.stabilityDays,
                currentDifficulty = card.difficulty,
                reviewCount = card.reviewCount.toInt(),
                lapseCount = card.lapseCount.toInt(),
                pronunciationScore = card.pronunciationScore.toInt(),
                responseTimeMs = responseTimeMs,
                rating = rating
            )
            db.collection("users").document(verifiedUid)
                .collection("reviewCards").document(card.id)
                .update(
                    mapOf(
                        "masteryScore" to next.masteryScore.toLong(),
                        "stabilityDays" to next.stabilityDays,
                        "difficulty" to next.difficulty,
                        "reviewCount" to next.reviewCount.toLong(),
                        "lapseCount" to next.lapseCount.toLong(),
                        "nextReviewEpochMs" to next.nextReviewEpochMs,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            rewardXp(xpDelta = if (rating >= 3) 15L else 5L)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- AI Tutor Messages ---

    fun observeTutorMessages(userId: String): Flow<List<TutorMessage>> = flow {
        val path = "users/$userId/tutorMessages"
        emitAll(
            db.collection("users").document(userId).collection("tutorMessages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limitToLast(80)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TutorMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun addTutorMessage(
        scenarioId: String,
        language: String,
        sender: String,
        text: String,
        arabicExplanation: String = "",
        deepThinking: Boolean = false
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/tutorMessages"
        return try {
            val verifiedUid = requireUserId()
            val safeText = text.trim().take(8000)
            require(safeText.isNotEmpty()) { "Message text cannot be blank" }
            val docRef = db.collection("users").document(verifiedUid).collection("tutorMessages").document()
            val payload = mapOf(
                "scenarioId" to scenarioId.take(64),
                "language" to if (language == "ES") "ES" else "EN",
                "sender" to if (sender == "tutor") "tutor" else "user",
                "text" to safeText,
                "arabicExplanation" to arabicExplanation.trim().take(4000),
                "deepThinking" to deepThinking,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun clearTutorMessagesByIds(messageIds: List<String>): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/tutorMessages"
        return try {
            val verifiedUid = requireUserId()
            val validIds = messageIds.filter { it.isNotBlank() }
            if (validIds.isEmpty()) return Result.success(Unit)
            val col = db.collection("users").document(verifiedUid).collection("tutorMessages")
            validIds.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { msgId ->
                    batch.delete(col.document(msgId))
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Exams ---

    fun observeExamAttempts(userId: String): Flow<List<ExamAttempt>> = flow {
        val path = "users/$userId/examAttempts"
        emitAll(
            db.collection("users").document(userId).collection("examAttempts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(40)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ExamAttempt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun saveExamAttempt(
        language: String,
        cefrLevel: String,
        examType: String,
        listeningScore: Int,
        speakingScore: Int,
        readingScore: Int,
        writingScore: Int,
        aiFeedback: String
    ): Result<Boolean> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/examAttempts"
        return try {
            val verifiedUid = requireUserId()
            val l = listeningScore.coerceIn(0, 100)
            val s = speakingScore.coerceIn(0, 100)
            val r = readingScore.coerceIn(0, 100)
            val w = writingScore.coerceIn(0, 100)
            val total = (l + s + r + w) / 4
            // Mandatory LingoWorld pass rule: >= 80% total AND >= 70% in EVERY skill
            val passed = total >= 80 && l >= 70 && s >= 70 && r >= 70 && w >= 70

            val docRef = db.collection("users").document(verifiedUid).collection("examAttempts").document()
            val payload = mapOf(
                "language" to if (language == "ES") "ES" else "EN",
                "cefrLevel" to cefrLevel,
                "examType" to examType,
                "listeningScore" to l.toLong(),
                "speakingScore" to s.toLong(),
                "readingScore" to r.toLong(),
                "writingScore" to w.toLong(),
                "totalScore" to total.toLong(),
                "passed" to passed,
                "aiFeedback" to aiFeedback.trim().take(4000),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            rewardXp(xpDelta = if (passed) 120L else 35L)
            Result.success(passed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- Pronunciation Logs ---

    fun observePronunciationLogs(userId: String): Flow<List<PronunciationLog>> = flow {
        val path = "users/$userId/pronunciationLogs"
        emitAll(
            db.collection("users").document(userId).collection("pronunciationLogs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PronunciationLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun savePronunciationLog(
        language: String,
        dialect: String,
        targetSentence: String,
        recognizedText: String,
        accuracyScore: Int,
        stressScore: Int,
        rhythmScore: Int,
        fluencyScore: Int,
        clarityScore: Int,
        arabicCoaching: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/pronunciationLogs"
        return try {
            val verifiedUid = requireUserId()
            val docRef = db.collection("users").document(verifiedUid).collection("pronunciationLogs").document()
            val payload = mapOf(
                "language" to if (language == "ES") "ES" else "EN",
                "dialect" to dialect.take(16),
                "targetSentence" to targetSentence.trim().take(500),
                "recognizedText" to recognizedText.trim().take(500),
                "accuracyScore" to accuracyScore.coerceIn(0, 100).toLong(),
                "stressScore" to stressScore.coerceIn(0, 100).toLong(),
                "rhythmScore" to rhythmScore.coerceIn(0, 100).toLong(),
                "fluencyScore" to fluencyScore.coerceIn(0, 100).toLong(),
                "clarityScore" to clarityScore.coerceIn(0, 100).toLong(),
                "arabicCoaching" to arabicCoaching.trim().take(4000),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            rewardXp(xpDelta = 25L, listeningDeltaMinutes = 2L)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- Lesson Progress ---

    fun observeLessonProgress(userId: String): Flow<List<LessonProgress>> = flow {
        val path = "users/$userId/lessonProgress"
        emitAll(
            db.collection("users").document(userId).collection("lessonProgress")
                .snapshots()
                .map { snap ->
                    snap.toObjects(LessonProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun saveLessonProgress(
        progressId: String,
        language: String,
        cefrLevel: String,
        unitNumber: Int,
        lessonNumber: Int,
        contentType: String,
        score: Int
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val safeId = progressId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(100)
        val path = "users/${uid ?: "unauthenticated"}/lessonProgress/$safeId"
        return try {
            val verifiedUid = requireUserId()
            val docRef = db.collection("users").document(verifiedUid)
                .collection("lessonProgress").document(safeId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "completed" to true,
                        "score" to score.coerceIn(0, 100).toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "language" to if (language == "ES") "ES" else "EN",
                        "cefrLevel" to cefrLevel,
                        "unitNumber" to unitNumber.coerceIn(1, 20).toLong(),
                        "lessonNumber" to lessonNumber.coerceIn(1, 20).toLong(),
                        "contentType" to contentType,
                        "completed" to true,
                        "score" to score.coerceIn(0, 100).toLong(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                rewardXp(xpDelta = 40L, listeningDeltaMinutes = 4L, lessonCompletedDelta = 1L)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Admin CMS Content ---

    fun observePublishedCmsContent(userId: String): Flow<List<CmsContentItem>> = flow {
        val path = "cmsContent"
        // Client query MUST include whereEqualTo("published", true) to align with security rules
        emitAll(
            db.collection("cmsContent")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CmsContentItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeAllCmsContentAdmin(userId: String): Flow<List<CmsContentItem>> = flow {
        val path = "cmsContent"
        emitAll(
            db.collection("cmsContent")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CmsContentItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun createCmsContent(
        language: String,
        dialect: String,
        cefrLevel: String,
        unitNumber: Int,
        contentType: String,
        title: String,
        bodyText: String,
        arabicTranslation: String,
        grammarNote: String,
        youtubeVideoId: String,
        published: Boolean
    ): Result<Unit> {
        val path = "cmsContent"
        return try {
            val verifiedUid = requireUserId()
            val docRef = db.collection("cmsContent").document()
            val payload = mapOf(
                "language" to if (language == "ES") "ES" else "EN",
                "dialect" to dialect.take(16),
                "cefrLevel" to cefrLevel,
                "unitNumber" to unitNumber.coerceIn(1, 12).toLong(),
                "contentType" to contentType,
                "title" to title.trim().take(160),
                "bodyText" to bodyText.trim().take(5000),
                "arabicTranslation" to arabicTranslation.trim().take(5000),
                "grammarNote" to grammarNote.trim().take(2000),
                "youtubeVideoId" to youtubeVideoId.trim().take(32),
                "published" to published,
                "authorId" to verifiedUid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleCmsPublished(itemId: String, published: Boolean): Result<Unit> {
        val path = "cmsContent/$itemId"
        return try {
            requireUserId()
            require(itemId.isNotBlank()) { "CMS item ID required" }
            db.collection("cmsContent").document(itemId).update(
                mapOf(
                    "published" to published,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCmsContent(itemId: String): Result<Unit> {
        val path = "cmsContent/$itemId"
        return try {
            requireUserId()
            require(itemId.isNotBlank()) { "CMS item ID required" }
            db.collection("cmsContent").document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Saved AI Interactive Stories ---

    fun observeSavedStories(userId: String): Flow<List<InteractiveStory>> = flow {
        val path = "users/$userId/savedStories"
        emitAll(
            db.collection("users").document(userId).collection("savedStories")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SavedStoryDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .mapNotNull { it.toInteractiveStory() }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun saveGeneratedStory(story: InteractiveStory): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/savedStories"
        return try {
            val verifiedUid = requireUserId()
            val safeId = story.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(80).ifBlank { "ai_${System.currentTimeMillis()}" }
            val storyJson = SavedStoryDoc.encodeStoryJson(story).take(19500)
            val payload = mapOf(
                "language" to story.language.code,
                "cefrLevel" to story.cefrLevel.code,
                "genreAr" to story.genreAr.trim().ifBlank { "قصة ذكاء اصطناعي" }.take(120),
                "titleAr" to story.titleAr.trim().ifBlank { "قصة تفاعلية" }.take(200),
                "subtitleTarget" to story.subtitleTarget.trim().ifBlank { "Interactive Story" }.take(240),
                "storyJson" to storyJson,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(verifiedUid)
                .collection("savedStories").document(safeId)
                .set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSavedStory(storyId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/savedStories/$storyId"
        return try {
            val verifiedUid = requireUserId()
            require(storyId.isNotBlank()) { "Story ID required" }
            db.collection("users").document(verifiedUid)
                .collection("savedStories").document(storyId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Optional Opt-In Public Leaderboard ---

    fun observeLeaderboard(userId: String): Flow<List<LeaderboardEntry>> = flow {
        val path = "leaderboard"
        emitAll(
            db.collection("leaderboard")
                .orderBy("xp", Query.Direction.DESCENDING)
                .limit(20)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun setLeaderboardOptIn(profile: UserProfile, optIn: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "leaderboard/${uid ?: "unauthenticated"}"
        return try {
            val verifiedUid = requireUserId()
            val docRef = db.collection("leaderboard").document(verifiedUid)
            if (optIn) {
                docRef.set(
                    mapOf(
                        "userId" to verifiedUid,
                        "displayName" to profile.displayName.trim().ifBlank { "متعلم LingoWorld" }.take(80),
                        "activeLanguage" to if (profile.activeLanguage == "ES") "ES" else "EN",
                        "activeLevel" to profile.currentLevelEnum().code,
                        "xp" to profile.xp.coerceIn(0L, 10_000_000L),
                        "streakDays" to profile.streakDays.coerceIn(0L, 10_000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (optIn) OperationType.WRITE else OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Admin Platform Analytics (Admin-only user list) ---

    fun observeAllLearnersAdmin(userId: String): Flow<List<UserProfile>> = flow {
        val path = "users"
        emitAll(
            db.collection("users")
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        op: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }
}
