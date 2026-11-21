package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.curriculum.CurriculumCatalog
import com.example.data.model.CefrLevel
import com.example.data.model.ExamAttempt
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LessonProgress
import com.example.data.model.PronunciationLog
import com.example.data.model.ReviewCard
import com.example.data.model.TargetLanguage
import com.example.data.model.UserProfile
import com.example.ui.components.LtrTargetText
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.EnglishRoyalBlue
import com.example.ui.theme.SpanishTerracotta
import java.util.Calendar

enum class LingoDestination(val route: String, val labelAr: String) {
    HOME("home", "الرئيسية"),
    LEARNING("learning", "تعلمي والمنهج"),
    STORIES("stories", "القصص التفاعلية"),
    VIDEOS("videos", "فيديوهات حقيقية"),
    PRONUNCIATION("pronunciation", "مختبر النطق"),
    AI_TUTOR("ai_tutor", "المدرس الذكي"),
    SKILLS("skills", "المهارات الأربع"),
    TESTS("tests", "الاختبارات"),
    REVIEW("review", "المراجعة الذكية"),
    PROGRESS("progress", "التقدم والإنجازات"),
    SETTINGS("settings", "الإعدادات"),
    ADMIN("admin", "لوحة الإدارة")
}

data class ActiveLessonResume(
    val language: TargetLanguage,
    val level: CefrLevel,
    val subLevel: Int,
    val unitNumber: Int,
    val lessonNumber: Int,
    val unitTitleAr: String,
    val lessonTitleAr: String,
    val themeTagAr: String,
    val previewSentence: String,
    val previewArabic: String,
    val unitCompletedLessons: Int,
    val hasPriorProgress: Boolean
)

private fun resolveActiveLesson(
    profile: UserProfile,
    lessonProgress: List<LessonProgress>
): ActiveLessonResume {
    val activeLang = profile.currentLanguageEnum()
    val activeLevel = profile.currentLevelEnum()
    val subLevel = profile.currentSubLevel()

    val relevantLessons = lessonProgress.filter {
        it.language == activeLang.code &&
            it.cefrLevel == activeLevel.code &&
            it.contentType == "lesson"
    }
    val latest = relevantLessons.maxByOrNull { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }

    val (targetUnit, targetLesson) = if (latest == null) {
        1 to 1
    } else if (latest.completed) {
        val nextLesson = latest.lessonNumber.toInt() + 1
        if (nextLesson <= 10) {
            latest.unitNumber.toInt().coerceIn(1, 12) to nextLesson
        } else {
            (latest.unitNumber.toInt() + 1).coerceIn(1, 12) to 1
        }
    } else {
        latest.unitNumber.toInt().coerceIn(1, 12) to latest.lessonNumber.toInt().coerceIn(1, 10)
    }

    val units = CurriculumCatalog.unitsFor(activeLang, activeLevel)
    val unitObj = units.firstOrNull { it.number == targetUnit } ?: units.first()
    val lessonTitle = unitObj.lessonTitlesAr.getOrElse(targetLesson - 1) { "الدرس $targetLesson" }
    val previewItem = CurriculumCatalog.sentencesFor(activeLang, activeLevel, targetUnit, targetLesson).firstOrNull()
    val completedInUnit = relevantLessons.count {
        it.completed && it.unitNumber.toInt() == targetUnit
    }.coerceIn(0, 10)

    return ActiveLessonResume(
        language = activeLang,
        level = activeLevel,
        subLevel = subLevel,
        unitNumber = targetUnit,
        lessonNumber = targetLesson,
        unitTitleAr = unitObj.titleAr,
        lessonTitleAr = lessonTitle,
        themeTagAr = unitObj.themeTagAr,
        previewSentence = previewItem?.sentence.orEmpty(),
        previewArabic = previewItem?.arabicTranslation.orEmpty(),
        unitCompletedLessons = completedInUnit,
        hasPriorProgress = latest != null
    )
}

@Composable
fun HomeDashboardScreen(
    profile: UserProfile,
    reviewCards: List<ReviewCard>,
    lessonProgress: List<LessonProgress>,
    examAttempts: List<ExamAttempt>,
    pronunciationLogs: List<PronunciationLog>,
    isAdmin: Boolean,
    onSwitchLanguage: (TargetLanguage) -> Unit,
    onContinueLastLesson: (ActiveLessonResume) -> Unit,
    onNavigate: (LingoDestination) -> Unit
) {
    val activeLang = profile.currentLanguageEnum()
    val now = System.currentTimeMillis()
    val dueCardsCount = reviewCards.count { it.nextReviewEpochMs <= now }
    val enLessonsDone = lessonProgress.count { it.language == "EN" && it.completed }
    val esLessonsDone = lessonProgress.count { it.language == "ES" && it.completed }
    val avgPronunciation = if (pronunciationLogs.isEmpty()) 0 else {
        pronunciationLogs.map {
            (it.accuracyScore + it.stressScore + it.rhythmScore + it.fluencyScore + it.clarityScore) / 5
        }.average().toInt()
    }
    val activeLesson = remember(profile, lessonProgress) {
        resolveActiveLesson(profile, lessonProgress)
    }
    val greetingHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greetingText = when (greetingHour) {
        in 5..11 -> stringResource(R.string.home_greeting_morning, profile.displayName)
        in 12..17 -> stringResource(R.string.home_greeting_afternoon, profile.displayName)
        else -> stringResource(R.string.home_greeting_evening, profile.displayName)
    }
    val dailyGoalMinutes = profile.dailyGoalMinutes.coerceAtLeast(5L)
    val dailyProgressRatio = (profile.listeningMinutes.toFloat() / dailyGoalMinutes.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Welcoming Greeting Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("home_greeting_banner")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_lingo_1795256214321),
                    contentDescription = "ترحيب منصة LingoWorld AI",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xE6063827),
                                    Color(0xBF0A523B),
                                    Color(0x8815385A)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = greetingText,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "جاهز لخطوتك اليومية في ${activeLang.arabicName}؟",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                        Spacer(Modifier.size(8.dp))
                        Surface(
                            color = Color(0xFFFFB703),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.LocalFireDepartment,
                                    contentDescription = "سلسلة الأيام",
                                    tint = Color(0xFF3B1F00),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${profile.streakDays} يوم",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3B1F00)
                                )
                            }
                        }
                    }

                    // Daily Goal & XP Strip inside Greeting Banner
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الهدف اليومي: ${profile.listeningMinutes} / $dailyGoalMinutes دقيقة",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "${profile.xp} XP",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD166)
                                )
                            }
                            LinearProgressIndicator(
                                progress = { dailyProgressRatio.coerceAtLeast(0.06f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(100.dp)),
                                color = Color(0xFFFFD166),
                                trackColor = Color.White.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }
        }

        // 2. Prominent 'Continue Learning' Hero Card
        item {
            ContinueLearningHeroCard(
                activeLesson = activeLesson,
                onContinueClick = { onContinueLastLesson(activeLesson) },
                onOpenTutorClick = { onNavigate(LingoDestination.AI_TUTOR) }
            )
        }

        // 3. Quick Overview of Current Language Progress (English & Spanish)
        item {
            SectionHeader(
                title = stringResource(R.string.home_languages_title),
                subtitle = stringResource(R.string.home_languages_subtitle),
                badge = "${enLessonsDone + esLessonsDone} درس"
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LanguageTrackCard(
                    title = "الإنجليزية",
                    dialect = if (profile.englishDialect == "en-GB") "بريطانية UK" else "أمريكية US",
                    level = profile.englishLevel,
                    subLevel = profile.englishSubLevel.toInt().coerceIn(1, 10),
                    lessonsDone = enLessonsDone,
                    accent = EnglishRoyalBlue,
                    selected = activeLang == TargetLanguage.EN,
                    onClick = {
                        onSwitchLanguage(TargetLanguage.EN)
                        onNavigate(LingoDestination.LEARNING)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("track_english_card")
                )
                LanguageTrackCard(
                    title = "الإسبانية",
                    dialect = if (profile.spanishDialect == "es-MX") "لاتينية MX" else "أوروبية ES",
                    level = profile.spanishLevel,
                    subLevel = profile.spanishSubLevel.toInt().coerceIn(1, 10),
                    lessonsDone = esLessonsDone,
                    accent = SpanishTerracotta,
                    selected = activeLang == TargetLanguage.ES,
                    onClick = {
                        onSwitchLanguage(TargetLanguage.ES)
                        onNavigate(LingoDestination.LEARNING)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("track_spanish_card")
                )
            }
        }

        // Daily Stats & Smart Recommendation
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMiniCard(
                    icon = Icons.Filled.Replay,
                    value = "$dueCardsCount",
                    label = "مراجعة FSRS",
                    onClick = { onNavigate(LingoDestination.REVIEW) },
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    icon = Icons.Filled.Headphones,
                    value = "${profile.listeningMinutes} د",
                    label = "الاستماع",
                    onClick = { onNavigate(LingoDestination.VIDEOS) },
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    icon = Icons.Filled.GraphicEq,
                    value = if (avgPronunciation > 0) "$avgPronunciation%" else "جديد",
                    label = "دقة النطق",
                    onClick = { onNavigate(LingoDestination.PRONUNCIATION) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Smart AI Recommendation Banner
        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (dueCardsCount > 0) onNavigate(LingoDestination.REVIEW)
                        else if (examAttempts.isEmpty()) onNavigate(LingoDestination.TESTS)
                        else onNavigate(LingoDestination.STORIES)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "توصية ذكية لخطوتك الآن",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        val recText = when {
                            dueCardsCount > 0 -> "لديك $dueCardsCount جملة مستحقة للمراجعة الذكية اليوم لتثبيت الإتقان."
                            examAttempts.isEmpty() -> "ابدأ باختبار تحديد المستوى (Placement Test) لتحديد مستواك الدقيق في CEFR."
                            else -> "جرّب قصة تفاعلية جديدة أو جلسة تفكير عميق مع المدرس الذكي لرفع طلاقتك."
                        }
                        Text(
                            text = recText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.88f)
                        )
                    }
                }
            }
        }

        // Complete 13-Section Quick Access Hub
        item {
            SectionHeader(
                title = "أقسام المنصة الكاملة",
                subtitle = "وصول مباشر لكل أدوات التعلم من الهاتف"
            )
            Spacer(Modifier.height(10.dp))
            val quickItems = buildList {
                add(Triple(LingoDestination.LEARNING, Icons.Filled.MenuBook, "12 وحدة • جمل سياقية"))
                add(Triple(LingoDestination.STORIES, Icons.Filled.AutoStories, "قصص تفاعلية + AI"))
                add(Triple(LingoDestination.VIDEOS, Icons.Filled.OndemandVideo, "فيديوهات YouTube موقّتة"))
                add(Triple(LingoDestination.PRONUNCIATION, Icons.Filled.GraphicEq, "تقييم النطق + Shadowing"))
                add(Triple(LingoDestination.AI_TUTOR, Icons.Filled.AutoAwesome, "محادثة + تفكير عميق"))
                add(Triple(LingoDestination.SKILLS, Icons.Filled.Bolt, "المهارات الأربع"))
                add(Triple(LingoDestination.TESTS, Icons.Filled.FactCheck, "اختبارات CEFR الصعبة"))
                add(Triple(LingoDestination.REVIEW, Icons.Filled.Replay, "المراجعة الذكية FSRS"))
                add(Triple(LingoDestination.PROGRESS, Icons.Filled.TrendingUp, "التقدم والشارات"))
                add(Triple(LingoDestination.SETTINGS, Icons.Filled.Settings, "اللهجة والمظهر"))
                if (isAdmin) {
                    add(Triple(LingoDestination.ADMIN, Icons.Filled.AdminPanelSettings, "إدارة المحتوى CMS"))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                quickItems.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { (dest, icon, sub) ->
                            QuickFeatureTile(
                                title = dest.labelAr,
                                subtitle = sub,
                                icon = icon,
                                onClick = { onNavigate(dest) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hub_tile_${dest.route}")
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueLearningHeroCard(
    activeLesson: ActiveLessonResume,
    onContinueClick: () -> Unit,
    onOpenTutorClick: () -> Unit
) {
    val langAccent = if (activeLesson.language == TargetLanguage.EN) EnglishRoyalBlue else SpanishTerracotta
    val unitProgress = (activeLesson.unitCompletedLessons / 10f).coerceIn(0.05f, 1f)

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_continue_learning_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Accent top header strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                langAccent.copy(alpha = 0.14f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (activeLesson.hasPriorProgress) {
                                    stringResource(R.string.home_continue_badge)
                                } else {
                                    "ابدأ درسك الأول الآن"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${activeLesson.language.arabicName} • الوحدة ${activeLesson.unitNumber} من 12",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    StatusPill(
                        text = "${activeLesson.level.code} • ليفل ${activeLesson.subLevel}/10",
                        containerColor = langAccent.copy(alpha = 0.16f),
                        contentColor = langAccent
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = activeLesson.unitTitleAr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = activeLesson.lessonTitleAr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (activeLesson.previewSentence.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LtrTargetText(
                                text = activeLesson.previewSentence,
                                color = MaterialTheme.colorScheme.onSurface,
                                bold = true
                            )
                            Text(
                                text = activeLesson.previewArabic,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تقدم الوحدة ${activeLesson.unitNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${activeLesson.unitCompletedLessons} / 10 دروس",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { unitProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(100.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onContinueClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("home_continue_learning_btn")
                    ) {
                        Text(
                            text = stringResource(R.string.home_continue_cta),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.size(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenTutorClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("home_open_tutor_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = stringResource(R.string.home_tutor_quick_cta),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.home_tutor_quick_cta))
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageTrackCard(
    title: String,
    dialect: String,
    level: String,
    subLevel: Int,
    lessonsDone: Int,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressPct = ((lessonsDone % 20) * 5).coerceIn(if (lessonsDone > 0) 5 else 0, 100)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) BorderStroke(1.5.dp, accent.copy(alpha = 0.55f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusPill(
                    text = level,
                    containerColor = accent.copy(alpha = 0.16f),
                    contentColor = accent
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dialect,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (selected) {
                    Text(
                        text = stringResource(R.string.home_active_track_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            LinearProgressIndicator(
                progress = { (subLevel / 10f).coerceIn(0.1f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(100.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.18f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$lessonsDone درس • ليفل $subLevel/10",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$progressPct%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    icon: ImageVector,
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickFeatureTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ProgressGamificationScreen(
    profile: UserProfile,
    lessonProgress: List<LessonProgress>,
    examAttempts: List<ExamAttempt>,
    pronunciationLogs: List<PronunciationLog>,
    reviewCards: List<ReviewCard>,
    leaderboardEntries: List<LeaderboardEntry>,
    onToggleLeaderboardOptIn: (Boolean) -> Unit,
    onClaimWeeklyChallenge: (String, Long) -> Unit
) {
    val passedExams = examAttempts.filter { it.passed }
    val avgListening = examAttempts.map { it.listeningScore }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
    val avgSpeaking = examAttempts.map { it.speakingScore }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
    val avgReading = examAttempts.map { it.readingScore }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
    val avgWriting = examAttempts.map { it.writingScore }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
    val claimedIds = lessonProgress.map { it.id }.toSet()
    val isOptedIntoLeaderboard = leaderboardEntries.any { it.userId == profile.userId }

    data class WeeklyChallengeUi(
        val id: String,
        val title: String,
        val current: Int,
        val target: Int,
        val rewardXp: Long
    )

    val weeklyChallenges = listOf(
        WeeklyChallengeUi("lessons_3", "أكمل 3 دروس جمل أو قصص أو فيديوهات", lessonProgress.count { it.completed && !it.id.startsWith("weekly_") }, 3, 60L),
        WeeklyChallengeUi("pron_2", "سجّل وحلّل نطقك مرتين في مختبر النطق", pronunciationLogs.size, 2, 50L),
        WeeklyChallengeUi("fsrs_5", "احفظ 5 جمل في المراجعة الذكية FSRS", reviewCards.size, 5, 50L),
        WeeklyChallengeUi("exam_pass", "اجتز اختبار CEFR بنسبة ≥80%", passedExams.size, 1, 100L)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "لوحة التقدم والتحفيز",
                subtitle = "السلسلة اليومية التلقائية • التحديات الأسبوعية • لوحة المتصدرين الاختيارية",
                badge = "${profile.xp} XP"
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الهدف اليومي: ${profile.dailyGoalMinutes} دقيقة", fontWeight = FontWeight.Bold)
                        Text("السلسلة: ${profile.streakDays} يوم 🔥", fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { (profile.listeningMinutes.toFloat() / profile.dailyGoalMinutes.coerceAtLeast(1)).coerceIn(0.1f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(100.dp))
                    )
                    Text(
                        text = "الدروس المكتملة: ${profile.completedLessonsCount} • البطاقات المحفوظة: ${reviewCards.size} • تدريبات النطق: ${pronunciationLogs.size}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Weekly Challenges Section
        item {
            SectionHeader("التحديات الأسبوعية", "أنجز التحديات لاستلام نقاط XP إضافية وتثبيت مستواك")
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                weeklyChallenges.forEach { ch ->
                    val reached = ch.current >= ch.target
                    val claimed = claimedIds.contains("weekly_${ch.id}")
                    val progress = (ch.current.toFloat() / ch.target.coerceAtLeast(1)).coerceIn(0f, 1f)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (claimed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ch.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                StatusPill("+${ch.rewardXp} XP")
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(100.dp))
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "التقدم: ${ch.current.coerceAtMost(ch.target)} / ${ch.target}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                when {
                                    claimed -> Text("تم الاستلام ✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    reached -> Button(
                                        onClick = { onClaimWeeklyChallenge(ch.id, ch.rewardXp) },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("claim_challenge_${ch.id}")
                                    ) {
                                        Text("استلام المكافأة")
                                    }
                                    else -> Text("قيد التنفيذ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Optional Opt-In Leaderboard Section
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("leaderboard_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("لوحة المتصدرين الاختيارية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "خصوصيتك أولًا: فعّل الزر للمشاركة باسمك ونقاطك فقط",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isOptedIntoLeaderboard,
                            onCheckedChange = onToggleLeaderboardOptIn,
                            modifier = Modifier.testTag("leaderboard_opt_in_switch")
                        )
                    }

                    if (leaderboardEntries.isEmpty()) {
                        Text(
                            text = "كن أول المشاركين في لوحة المتصدرين بتفعيل زر المشاركة أعلاه!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            leaderboardEntries.take(10).forEachIndexed { idx, entry ->
                                val isMe = entry.userId == profile.userId
                                val medal = when (idx) {
                                    0 -> "🥇"
                                    1 -> "🥈"
                                    2 -> "🥉"
                                    else -> "#${idx + 1}"
                                }
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(medal, fontWeight = FontWeight.Bold)
                                            Column {
                                                Text(
                                                    text = if (isMe) "${entry.displayName} (أنت)" else entry.displayName,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = "${entry.activeLanguage} • ${entry.activeLevel} • سلسلة ${entry.streakDays} يوم",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        StatusPill("${entry.xp} XP")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("أداء المهارات الأربع في الاختبارات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SkillProgressBar("الاستماع (Listening)", avgListening)
                    SkillProgressBar("التحدث (Speaking)", avgSpeaking)
                    SkillProgressBar("القراءة (Reading)", avgReading)
                    SkillProgressBar("الكتابة (Writing)", avgWriting)
                }
            }
        }

        item {
            SectionHeader("الشارات والشهادات المكتسبة", "تُفتح المكافآت بالتقدم الحقيقي في التعلم")
            Spacer(Modifier.height(8.dp))
            val badges = listOf(
                Triple("بداية الطلاقة", "أكمل أول درس بالجمل", lessonProgress.isNotEmpty()),
                Triple("صوت واثق", "أكمل تحليل نطق بالذكاء الاصطناعي", pronunciationLogs.isNotEmpty()),
                Triple("ذاكرة فولاذية", "احفظ 3 جمل في FSRS", reviewCards.size >= 3),
                Triple("شهادة اجتياز CEFR", "اجتز اختبارًا بنسبة ≥80%", passedExams.isNotEmpty())
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                badges.forEach { (title, desc, unlocked) ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (unlocked) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = if (unlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, fontWeight = FontWeight.Bold)
                                Text(desc, style = MaterialTheme.typography.bodySmall)
                            }
                            FilterChip(
                                selected = unlocked,
                                onClick = {},
                                label = { Text(if (unlocked) "مكتسبة" else "قيد التقدم") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillProgressBar(label: String, percent: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$percent%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(100.dp))
        )
    }
}
