package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.viewmodel.AiOpState
import com.example.ui.viewmodel.AiTutorViewModel

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

    var selectedUnitNumber by remember(activeLang, activeLevel) { mutableIntStateOf(1) }
    var selectedLessonNumber by remember(activeLang, activeLevel, selectedUnitNumber) { mutableIntStateOf(1) }
    var activeSentenceStudio by remember { mutableStateOf<List<SentenceLessonItem>?>(null) }
    var generatingUnitNo by remember { mutableIntStateOf(0) }

    LaunchedEffect(resumeLessonRequest) {
        val req = resumeLessonRequest ?: return@LaunchedEffect
        selectedUnitNumber = req.unitNumber
        selectedLessonNumber = req.lessonNumber
        activeSentenceStudio = CurriculumCatalog.sentencesFor(
            language = req.language,
            level = req.level,
            unitNumber = req.unitNumber,
            lessonNumber = req.lessonNumber
        )
        onConsumeResumeRequest()
    }

    val aiUnitLessonState by aiViewModel.generatedUnitLessonState.collectAsStateWithLifecycle()

    val units = remember(activeLang, activeLevel) {
        CurriculumCatalog.unitsFor(activeLang, activeLevel)
    }
    val completedIds = remember(lessonProgress) {
        lessonProgress.filter { it.completed }.map { it.id }.toSet()
    }
    val publishedSentencesFromCms = remember(cmsItems, activeLang, activeLevel) {
        cmsItems.filter {
            it.published &&
                it.contentType == "sentence" &&
                it.language == activeLang.code &&
                it.cefrLevel == activeLevel.code
        }
    }

    if (activeSentenceStudio != null) {
        SentenceStudioView(
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
        item {
            SectionHeader(
                title = "منهج CEFR المتكامل",
                subtitle = "12 وحدة × 10 دروس × 10 ليفلات لكل مستوى (A1 إلى C2)",
                badge = "${activeLang.arabicName} • ${activeLevel.code}"
            )
        }

        // Language & Dialect Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            }
        }

        // CEFR A1-C2 Selector
        item {
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
            Spacer(Modifier.height(6.dp))
            Text(
                text = "معيار ${activeLevel.code}: ${activeLevel.summaryAr}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Interactive 10-SubLevel Progression Ladder inside the active CEFR Level
        item {
            val tierLabelAr = when (currentSubLevel) {
                in 1..3 -> "مرحلة التأسيس وبناء الجمل الأساسية"
                in 4..6 -> "مرحلة الترسيخ والتوسّع في المواقف اليومية"
                in 7..9 -> "مرحلة الطلاقة والتعامل مع الحوارات المركّبة"
                else -> "مرحلة الإتقان — جاهز للاختبار النهائي للمستوى!"
            }
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سُلّم الليفلات داخل ${activeLevel.code}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        StatusPill("الليفل $currentSubLevel / 10")
                    }
                    LinearProgressIndicator(
                        progress = { (currentSubLevel / 10f).coerceIn(0.1f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(100.dp))
                    )
                    Text(
                        text = tierLabelAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

        // Admin-Published Cloud CMS Sentences (if any exist for this track/level)
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

        // 12 CEFR Units with 10 Lessons each
        items(units, key = { it.number }) { unit ->
            val isExpanded = selectedUnitNumber == unit.number
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedUnitNumber = if (isExpanded) 0 else unit.number
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatusPill("الوحدة ${unit.number}")
                                StatusPill(
                                    text = unit.themeTagAr,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = unit.titleAr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "عرض دروس الوحدة"
                        )
                    }

                    if (isExpanded) {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(10.dp))
                        val isGeneratingThisUnit = aiUnitLessonState is AiOpState.Loading && generatingUnitNo == unit.number
                        Button(
                            onClick = {
                                generatingUnitNo = unit.number
                                selectedLessonNumber = currentSubLevel
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
                                .testTag("generate_unit_lesson_u${unit.number}")
                        ) {
                            if (isGeneratingThisUnit) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("جاري توليد 4 جمل جديدة بالذكاء الاصطناعي...")
                            } else {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("توليد درس جمل جديد بالذكاء الاصطناعي (ليفل $currentSubLevel)")
                            }
                        }
                        if (aiUnitLessonState is AiOpState.Error && generatingUnitNo == unit.number) {
                            Spacer(Modifier.height(6.dp))
                            ErrorBannerCard((aiUnitLessonState as AiOpState.Error).message)
                        }
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            unit.lessonTitlesAr.forEachIndexed { idx, lessonTitle ->
                                val lessonNo = idx + 1
                                val progressId = "${activeLang.code.lowercase()}_${activeLevel.code.lowercase()}_u${unit.number}_l$lessonNo"
                                val done = completedIds.contains(progressId)
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (done) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedLessonNumber = lessonNo
                                            activeSentenceStudio = CurriculumCatalog.sentencesFor(
                                                activeLang, activeLevel, unit.number, lessonNo
                                            )
                                        }
                                        .testTag("lesson_u${unit.number}_l$lessonNo")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.PlayArrow,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = lessonTitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = if (done) "مكتمل" else "ابدأ",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SentenceStudioView(
    sentences: List<SentenceLessonItem>,
    dialect: Dialect,
    speechEngine: SpeechEngine,
    audioRecorder: AudioClipRecorder,
    onSaveToReview: (SentenceLessonItem) -> Unit,
    onPracticePronunciation: (String) -> Unit,
    onCompleteLesson: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember(currentIndex) { mutableStateOf<Int?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    val item = sentences[currentIndex]

    LaunchedEffect(currentIndex) {
        audioRecorder.resetClip()
    }

    val isRecordingClip by audioRecorder.isRecording.collectAsStateWithLifecycle()
    val isPlayingClip by audioRecorder.isPlaying.collectAsStateWithLifecycle()
    val hasRecordedClip by audioRecorder.hasRecording.collectAsStateWithLifecycle()
    val recordingSeconds by audioRecorder.recordingSeconds.collectAsStateWithLifecycle()
    val recorderError by audioRecorder.error.collectAsStateWithLifecycle()

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            audioRecorder.startRecording("sent_${item.id}")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onBack) {
                    Text("العودة للوحدات")
                }
                StatusPill("الجملة ${currentIndex + 1} من ${sentences.size} • ${dialect.arabicLabel}")
            }
        }

        // Core Sentence Card (Sentence-Based Learning — NEVER isolated words)
        item {
            ElevatedCard(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LtrTargetText(item.sentence, bold = true)
                    Text(
                        text = item.arabicTranslation,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "بديل اللهجة الأخرى: ${item.dialectVariantAlt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AudioSpeedButtons(
                        onPlayNormal = { speechEngine.speak(item.sentence, dialect.code, slow = false) },
                        onPlaySlow = { speechEngine.speak(item.sentence, dialect.code, slow = true) }
                    )

                    // Inline Learner Voice Recording & Playback inside Sentence Lesson
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (isRecordingClip) {
                                    audioRecorder.stopRecording()
                                } else {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        audioRecorder.startRecording("sent_${item.id}")
                                    } else {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecordingClip) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).testTag("sentence_record_voice_btn")
                        ) {
                            Icon(
                                if (isRecordingClip) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                                contentDescription = "تسجيل صوتك",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(if (isRecordingClip) "إيقاف (${recordingSeconds}ث)" else "سجّل صوتك")
                        }
                        OutlinedButton(
                            onClick = {
                                if (isPlayingClip) audioRecorder.stopPlayback() else audioRecorder.playRecording()
                            },
                            enabled = hasRecordedClip && !isRecordingClip,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).testTag("sentence_play_voice_btn")
                        ) {
                            Icon(
                                if (isPlayingClip) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = "سماع تسجيلك",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(if (isPlayingClip) "إيقاف" else "اسمع تسجيلك")
                        }
                    }
                    if (!recorderError.isNullOrBlank()) {
                        Text(recorderError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onSaveToReview(item) },
                            modifier = Modifier.weight(1f).testTag("save_sentence_fsrs_btn")
                        ) {
                            Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("حفظ للمراجعة")
                        }
                        OutlinedButton(
                            onClick = { onPracticePronunciation(item.sentence) },
                            modifier = Modifier.weight(1f).testTag("sentence_to_pronunciation_btn")
                        ) {
                            Icon(Icons.Filled.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("مختبر النطق")
                        }
                    }
                }
            }
        }

        // Contextual Grammar, Expressions & Real-World Usage
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("شرح التعبيرات:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(item.expressionNoteAr, style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()
                    Text("القواعد داخل السياق:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(item.grammarInContextAr, style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()
                    Text("الاستخدام الواقعي:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(item.realWorldUsageAr, style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()
                    Text("أمثلة مشابهة من الحياة اليومية:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    item.similarExamples.forEach { (enOrEs, ar) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { speechEngine.speak(enOrEs, dialect.code, slow = false) }
                                .padding(vertical = 4.dp)
                        ) {
                            LtrTargetText(enOrEs)
                            Text(ar, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Comprehension Check
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اختبار فهم الجملة:", fontWeight = FontWeight.Bold)
                    Text(item.comprehensionQuestionAr, style = MaterialTheme.typography.bodyMedium)
                    item.optionsAr.forEachIndexed { idx, opt ->
                        val picked = selectedAnswer == idx
                        val isCorrect = idx == item.correctOptionIndex
                        val bgColor = when {
                            selectedAnswer == null -> MaterialTheme.colorScheme.surface
                            picked && isCorrect -> MaterialTheme.colorScheme.primaryContainer
                            picked && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                            isCorrect -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = selectedAnswer == null) {
                                    selectedAnswer = idx
                                    if (idx == item.correctOptionIndex) correctCount++
                                }
                        ) {
                            Text(opt, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    if (currentIndex < sentences.lastIndex) {
                        currentIndex++
                    } else {
                        val finalScore = ((correctCount.coerceAtLeast(1) * 100) / sentences.size).coerceIn(70, 100)
                        onCompleteLesson(finalScore)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("sentence_next_or_finish_btn")
            ) {
                Text(
                    text = if (currentIndex < sentences.lastIndex) "الجملة التالية" else "إنهاء الدرس وتسجيل الإنجاز (+40 XP)",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
