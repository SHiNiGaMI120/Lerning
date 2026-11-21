package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AudioClipRecorder
import com.example.core.SpeechEngine
import com.example.data.curriculum.CurriculumCatalog
import com.example.data.model.CefrLevel
import com.example.data.model.CmsContentItem
import com.example.data.model.Dialect
import com.example.data.model.LessonProgress
import com.example.data.model.SentenceLessonItem
import com.example.data.model.TargetLanguage
import com.example.data.model.UserProfile
import com.example.ui.components.AudioSpeedButtons
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.LtrTargetText
import com.example.ui.components.StatusPill
import com.example.ui.viewmodel.AiOpState
import com.example.ui.viewmodel.AiTutorViewModel

private data class NextGameLessonTarget(
    val unitNumber: Int,
    val lessonNumber: Int,
    val unitTitleAr: String,
    val lessonTitleAr: String,
    val completedCountInLevel: Int
)

@Composable
fun CurriculumAndSentenceScreen(
    profile: UserProfile,
    lessonProgress: List<LessonProgress>,
    cmsItems: List<CmsContentItem>,
    aiViewModel: AiTutorViewModel,
    speechEngine: SpeechEngine,
    audioRecorder: AudioClipRecorder,
    resumeLessonRequest: ActiveLessonResume? = null,
    onConsumeResumeRequest: () -> Unit = {},
    onSwitchLanguage: (TargetLanguage) -> Unit,
    onSelectDialect: (Dialect) -> Unit,
    onSelectLevel: (TargetLanguage, CefrLevel) -> Unit,
    onSelectSubLevel: (TargetLanguage, Int) -> Unit,
    onSaveToReview: (SentenceLessonItem, String) -> Unit,
    onCompleteLesson: (String, TargetLanguage, CefrLevel, Int, Int, Int) -> Unit,
    onPracticeInPronunciation: (String) -> Unit
) {
    val activeLang = profile.currentLanguageEnum()
    val activeLevel = profile.currentLevelEnum()
    val activeDialect = Dialect.fromCode(profile.currentDialectCode(), activeLang)
    val currentSubLevel = profile.currentSubLevel()

    val units = remember(activeLang, activeLevel) {
        CurriculumCatalog.unitsFor(activeLang, activeLevel)
    }
    val completedIds = remember(lessonProgress) {
        lessonProgress.filter { it.completed }.map { it.id }.toSet()
    }
    val nextTarget = remember(units, completedIds, activeLang, activeLevel) {
        findNextLessonTarget(units, completedIds, activeLang, activeLevel)
    }

    var selectedUnitNumber by remember(activeLang, activeLevel) {
        mutableIntStateOf(nextTarget.unitNumber)
    }
    var selectedLessonNumber by remember(activeLang, activeLevel) {
        mutableIntStateOf(nextTarget.lessonNumber)
    }
    var activeLessonTitle by remember { mutableStateOf(nextTarget.lessonTitleAr) }
    var activeSentenceStudio by remember { mutableStateOf<List<SentenceLessonItem>?>(null) }
    var generatingUnitNo by remember { mutableIntStateOf(0) }
    var showTrackSettings by remember { mutableStateOf(false) }

    LaunchedEffect(resumeLessonRequest) {
        val req = resumeLessonRequest ?: return@LaunchedEffect
        selectedUnitNumber = req.unitNumber
        selectedLessonNumber = req.lessonNumber
        activeLessonTitle = req.lessonTitleAr
        activeSentenceStudio = CurriculumCatalog.sentencesFor(
            language = req.language,
            level = req.level,
            unitNumber = req.unitNumber,
            lessonNumber = req.lessonNumber
        )
        onConsumeResumeRequest()
    }

    val aiUnitLessonState by aiViewModel.generatedUnitLessonState.collectAsStateWithLifecycle()

    val publishedSentencesFromCms = remember(cmsItems, activeLang, activeLevel) {
        cmsItems.filter {
            it.published &&
                it.contentType == "sentence" &&
                it.language == activeLang.code &&
                it.cefrLevel == activeLevel.code
        }
    }

    if (activeSentenceStudio != null) {
        LessonGameView(
            lessonTitleAr = activeLessonTitle,
            sentences = activeSentenceStudio!!,
            dialect = activeDialect,
            speechEngine = speechEngine,
            audioRecorder = audioRecorder,
            onSaveToReview = { item -> onSaveToReview(item, activeDialect.code) },
            onPracticePronunciation = onPracticeInPronunciation,
            onCompleteLesson = { score ->
                val progressId = "${activeLang.code.lowercase()}_${activeLevel.code.lowercase()}_u${selectedUnitNumber}_l${selectedLessonNumber}"
                onCompleteLesson(progressId, activeLang, activeLevel, selectedUnitNumber, selectedLessonNumber, score)
                activeSentenceStudio = null
            },
            onBack = {
                audioRecorder.resetClip()
                activeSentenceStudio = null
            }
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1) Active Level Hero Game Card (First thing the user sees — clear Level 1 entry)
        item {
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.SportsEsports,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "رحلة التعلّم باللعب",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "${activeLang.arabicName} (${activeDialect.arabicLabel}) • ${activeLevel.code}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        StatusPill("ليفل $currentSubLevel / 10")
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "كيف تلعب؟ كل درس 3 جولات سريعة: استمع وافهم ← ركّب الكلمات كاللعبة ← اجتز تحدي الفهم واجمع النجوم!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    // Progress inside current CEFR level
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "تقدمك في مستوى ${activeLevel.code}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${nextTarget.completedCountInLevel} / 120 درس مكتمل",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (nextTarget.completedCountInLevel / 120f).coerceIn(0.03f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(100.dp))
                        )
                    }

                    // Primary Play Next Lesson CTA
                    Button(
                        onClick = {
                            selectedUnitNumber = nextTarget.unitNumber
                            selectedLessonNumber = nextTarget.lessonNumber
                            activeLessonTitle = nextTarget.lessonTitleAr
                            activeSentenceStudio = CurriculumCatalog.sentencesFor(
                                activeLang,
                                activeLevel,
                                nextTarget.unitNumber,
                                nextTarget.lessonNumber
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("play_next_lesson_btn")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "العب الآن: الوحدة ${nextTarget.unitNumber} • الدرس ${nextTarget.lessonNumber}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Compact Settings Toggle (keeps the map clean and uncluttered)
                    OutlinedButton(
                        onClick = { showTrackSettings = !showTrackSettings },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("toggle_track_settings_btn")
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (showTrackSettings) "إخفاء إعدادات اللغة والمستوى" else "تغيير اللغة أو اللهجة أو المستوى (${activeLevel.code})",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            if (showTrackSettings) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2) Collapsible Track Settings Panel (Language, Dialect, CEFR A1-C2, 10 SubLevels)
        item {
            AnimatedVisibility(visible = showTrackSettings) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("اختر اللغة واللهجة:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TargetLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = activeLang == lang,
                                    onClick = { onSwitchLanguage(lang) },
                                    label = { Text("${lang.arabicName} (${lang.nativeName})") },
                                    modifier = Modifier.testTag("lang_chip_${lang.code}")
                                )
                            }
                        }
                        val dialects = Dialect.entries.filter { it.language == activeLang }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            dialects.forEach { dialect ->
                                FilterChip(
                                    selected = activeDialect == dialect,
                                    onClick = { onSelectDialect(dialect) },
                                    label = { Text(dialect.arabicLabel) },
                                    modifier = Modifier.testTag("dialect_chip_${dialect.code}")
                                )
                            }
                        }

                        HorizontalDivider()

                        Text("مستوى CEFR (من A1 للمبتدئ حتى C2):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(CefrLevel.entries) { level ->
                                FilterChip(
                                    selected = activeLevel == level,
                                    onClick = { onSelectLevel(activeLang, level) },
                                    label = { Text("${level.code} • ${level.arabicTitle}") },
                                    modifier = Modifier.testTag("cefr_chip_${level.code}")
                                )
                            }
                        }
                        Text(
                            text = "معيار ${activeLevel.code}: ${activeLevel.summaryAr}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider()

                        Text("سُلّم الليفلات داخل ${activeLevel.code} (الليفل $currentSubLevel / 10):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items((1..10).toList()) { sub ->
                                val isCurrent = sub == currentSubLevel
                                val isPassed = sub < currentSubLevel
                                FilterChip(
                                    selected = isCurrent,
                                    onClick = { onSelectSubLevel(activeLang, sub) },
                                    label = {
                                        Text(
                                            text = if (isPassed) "✓ ليفل $sub" else "ليفل $sub",
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.testTag("sublevel_chip_$sub")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3) Admin-Published Cloud CMS Sentences (if any exist for this track/level)
        if (publishedSentencesFromCms.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("إضافات حصرية من مكتبة المنصة السحابية", fontWeight = FontWeight.Bold)
                        publishedSentencesFromCms.forEach { cms ->
                            LtrTargetText(cms.bodyText, bold = true)
                            Text(cms.arabicTranslation, style = MaterialTheme.typography.bodySmall)
                            if (cms.grammarNote.isNotBlank()) {
                                Text("شرح: ${cms.grammarNote}", style = MaterialTheme.typography.labelSmall)
                            }
                            AudioSpeedButtons(
                                onPlayNormal = { speechEngine.speak(cms.bodyText, activeDialect.code, slow = false) },
                                onPlaySlow = { speechEngine.speak(cms.bodyText, activeDialect.code, slow = true) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        // 4) Stepping Game Map Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "خريطة المراحل (${activeLevel.code})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    text = "12 مرحلة × 10 دروس",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 5) 12 Visual Game Units with Stepping Lesson Nodes
        items(units, key = { it.number }) { unit ->
            val isExpanded = selectedUnitNumber == unit.number
            val isCurrentUnit = unit.number == nextTarget.unitNumber
            val doneInUnit = (1..unit.lessonTitlesAr.size).count { lNo ->
                val pid = "${activeLang.code.lowercase()}_${activeLevel.code.lowercase()}_u${unit.number}_l$lNo"
                completedIds.contains(pid)
            }
            val unitCompleted = doneInUnit >= unit.lessonTitlesAr.size

            ElevatedCard(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isCurrentUnit -> MaterialTheme.colorScheme.surfaceContainerHigh
                        unitCompleted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedUnitNumber = if (isExpanded) 0 else unit.number
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Stage Number Node
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            unitCompleted -> MaterialTheme.colorScheme.primary
                                            isCurrentUnit -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (unitCompleted) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "مرحلة مكتملة",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text(
                                        text = "${unit.number}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isCurrentUnit) MaterialTheme.colorScheme.onTertiary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "المرحلة ${unit.number}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isCurrentUnit) {
                                        StatusPill(
                                            text = "أنت هنا",
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    } else {
                                        StatusPill(
                                            text = unit.themeTagAr,
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                Text(
                                    text = unit.titleAr,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { (doneInUnit.toFloat() / unit.lessonTitlesAr.size.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(100.dp))
                                    )
                                    Text(
                                        text = "$doneInUnit/${unit.lessonTitlesAr.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "عرض دروس المرحلة"
                        )
                    }

                    if (isExpanded) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))

                        // Stepping Game Lesson Nodes
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            unit.lessonTitlesAr.forEachIndexed { idx, lessonTitle ->
                                val lessonNo = idx + 1
                                val progressId = "${activeLang.code.lowercase()}_${activeLevel.code.lowercase()}_u${unit.number}_l$lessonNo"
                                val done = completedIds.contains(progressId)
                                val isNextUp = unit.number == nextTarget.unitNumber && lessonNo == nextTarget.lessonNumber

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            isNextUp -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                                            done -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                            else -> MaterialTheme.colorScheme.surfaceContainerLow
                                        }
                                    ),
                                    border = if (isNextUp) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .clickable {
                                            selectedUnitNumber = unit.number
                                            selectedLessonNumber = lessonNo
                                            activeLessonTitle = lessonTitle
                                            activeSentenceStudio = CurriculumCatalog.sentencesFor(
                                                activeLang, activeLevel, unit.number, lessonNo
                                            )
                                        }
                                        .testTag("lesson_u${unit.number}_l$lessonNo")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            done -> MaterialTheme.colorScheme.primary
                                                            isNextUp -> MaterialTheme.colorScheme.tertiary
                                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                when {
                                                    done -> Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = "مكتمل",
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    isNextUp -> Icon(
                                                        Icons.Filled.PlayArrow,
                                                        contentDescription = "دورك الآن",
                                                        tint = MaterialTheme.colorScheme.onTertiary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    else -> Text(
                                                        text = "$lessonNo",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = lessonTitle,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isNextUp) FontWeight.ExtraBold else FontWeight.Medium
                                                )
                                                if (isNextUp) {
                                                    Text(
                                                        text = "دورك الآن • 3 جولات تفاعلية (+40 XP)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        when {
                                            done -> Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.Star,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(2.dp))
                                                Text(
                                                    text = "مكتمل",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            isNextUp -> Surface(
                                                shape = RoundedCornerShape(100.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    text = "العب ▶",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                                )
                                            }
                                            else -> Text(
                                                text = "ابدأ",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // AI Bonus Challenge Generator for this Unit
                        val isGeneratingThisUnit = aiUnitLessonState is AiOpState.Loading && generatingUnitNo == unit.number
                        OutlinedButton(
                            onClick = {
                                generatingUnitNo = unit.number
                                selectedUnitNumber = unit.number
                                selectedLessonNumber = currentSubLevel
                                activeLessonTitle = "تحدي الذكاء الاصطناعي • ${unit.titleAr}"
                                aiViewModel.generateUnitLesson(
                                    language = activeLang,
                                    dialect = activeDialect,
                                    cefrLevel = activeLevel,
                                    unitNumber = unit.number,
                                    unitTitleAr = unit.titleAr,
                                    subLevel = currentSubLevel
                                ) { freshSentences ->
                                    activeSentenceStudio = freshSentences
                                }
                            },
                            enabled = aiUnitLessonState !is AiOpState.Loading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("generate_unit_lesson_u${unit.number}")
                        ) {
                            if (isGeneratingThisUnit) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("جاري تجهيز تحدي ذكي جديد...")
                            } else {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("تحدي إضافي بالذكاء الاصطناعي (4 جمل جديدة)")
                            }
                        }
                        if (aiUnitLessonState is AiOpState.Error && generatingUnitNo == unit.number) {
                            Spacer(Modifier.height(6.dp))
                            ErrorBannerCard((aiUnitLessonState as AiOpState.Error).message)
                        }
                    }
                }
            }
        }
    }
}

private fun findNextLessonTarget(
    units: List<com.example.data.model.CurriculumUnit>,
    completedIds: Set<String>,
    language: TargetLanguage,
    level: CefrLevel
): NextGameLessonTarget {
    var totalCompleted = 0
    var firstIncomplete: NextGameLessonTarget? = null

    units.forEach { unit ->
        unit.lessonTitlesAr.forEachIndexed { idx, lessonTitle ->
            val lessonNo = idx + 1
            val progressId = "${language.code.lowercase()}_${level.code.lowercase()}_u${unit.number}_l$lessonNo"
            if (completedIds.contains(progressId)) {
                totalCompleted++
            } else if (firstIncomplete == null) {
                firstIncomplete = NextGameLessonTarget(
                    unitNumber = unit.number,
                    lessonNumber = lessonNo,
                    unitTitleAr = unit.titleAr,
                    lessonTitleAr = lessonTitle,
                    completedCountInLevel = 0
                )
            }
        }
    }

    val fallbackUnit = units.firstOrNull()
    val chosen = firstIncomplete ?: NextGameLessonTarget(
        unitNumber = fallbackUnit?.number ?: 1,
        lessonNumber = 1,
        unitTitleAr = fallbackUnit?.titleAr ?: "الوحدة 1",
        lessonTitleAr = fallbackUnit?.lessonTitlesAr?.firstOrNull() ?: "الدرس 1",
        completedCountInLevel = totalCompleted
    )
    return chosen.copy(completedCountInLevel = totalCompleted)
}
