package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AudioClipRecorder
import com.example.core.SpeechEngine
import com.example.data.model.Dialect
import com.example.data.model.SentenceLessonItem
import com.example.ui.components.AudioSpeedButtons
import com.example.ui.components.LtrTargetText
import com.example.ui.components.SentenceRevealCard
import kotlin.random.Random

private data class WordTile(
    val id: Int,
    val display: String,
    val normalized: String
)

private enum class BuilderCheckState {
    IDLE,
    CORRECT,
    WRONG
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonGameView(
    lessonTitleAr: String,
    sentences: List<SentenceLessonItem>,
    dialect: Dialect,
    speechEngine: SpeechEngine,
    audioRecorder: AudioClipRecorder,
    onSaveToReview: (SentenceLessonItem) -> Unit,
    onPracticePronunciation: (String) -> Unit,
    onCompleteLesson: (Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    if (sentences.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("لا توجد جمل متاحة في هذا الدرس حالياً.")
                Button(onClick = onBack) { Text("العودة للخريطة") }
            }
        }
        return
    }

    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(0) }
    var roundStep by remember(currentIndex) { mutableIntStateOf(0) } // 0=Discover, 1=Word Builder, 2=Quiz+Voice
    var showVictory by remember { mutableStateOf(false) }

    // Game stats across the lesson
    var hearts by remember { mutableIntStateOf(5) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var sessionXp by remember { mutableIntStateOf(0) }
    var correctChallenges by remember { mutableIntStateOf(0) }
    var totalChallengesAttempted by remember { mutableIntStateOf(0) }

    // Per-sentence state
    var showGrammarDrawer by remember(currentIndex) { mutableStateOf(false) }
    var savedFeedback by remember(currentIndex) { mutableStateOf(false) }
    var voiceBonusClaimed by remember(currentIndex) { mutableStateOf(false) }
    var selectedQuizAnswer by remember(currentIndex) { mutableStateOf<Int?>(null) }

    val item = sentences[currentIndex]

    // Word builder tokens for Round 1
    val targetTiles = remember(item.id, item.sentence) { tokenizeSentence(item.sentence) }
    val shuffledBank = remember(item.id, item.sentence) { shuffleTilesDeterministically(targetTiles, item.id) }
    var selectedTileIds by remember(currentIndex) { mutableStateOf<List<Int>>(emptyList()) }
    var builderCheckState by remember(currentIndex) { mutableStateOf(BuilderCheckState.IDLE) }

    LaunchedEffect(currentIndex) {
        audioRecorder.resetClip()
    }

    val isRecordingClip by audioRecorder.isRecording.collectAsStateWithLifecycle()
    val isPlayingClip by audioRecorder.isPlaying.collectAsStateWithLifecycle()
    val hasRecordedClip by audioRecorder.hasRecording.collectAsStateWithLifecycle()
    val recordingSeconds by audioRecorder.recordingSeconds.collectAsStateWithLifecycle()
    val recorderError by audioRecorder.error.collectAsStateWithLifecycle()

    LaunchedEffect(hasRecordedClip) {
        if (hasRecordedClip && !voiceBonusClaimed) {
            voiceBonusClaimed = true
            sessionXp += 10
        }
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            audioRecorder.startRecording("sent_${item.id}")
        }
    }

    if (showVictory) {
        val accuracy = if (totalChallengesAttempted > 0) {
            ((correctChallenges * 100) / totalChallengesAttempted).coerceIn(50, 100)
        } else {
            90
        }
        val finalScore = accuracy.coerceIn(70, 100)
        val stars = when {
            accuracy >= 90 && hearts >= 4 -> 3
            accuracy >= 70 && hearts >= 2 -> 2
            else -> 1
        }
        LessonVictoryCard(
            lessonTitleAr = lessonTitleAr,
            stars = stars,
            sessionXp = sessionXp.coerceAtLeast(40),
            accuracy = accuracy,
            heartsRemaining = hearts,
            maxCombo = maxCombo,
            onClaimReward = { onCompleteLesson(finalScore) }
        )
        return
    }

    val totalSteps = (sentences.size * 3).coerceAtLeast(1)
    val completedSteps = currentIndex * 3 + roundStep + if (roundStep == 2 && selectedQuizAnswer != null) 1 else 0
    val animatedProgress by animateFloatAsState(
        targetValue = (completedSteps.toFloat() / totalSteps.toFloat()).coerceIn(0.05f, 1f),
        label = "lesson_game_progress"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("lesson_game_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1) Game HUD Bar
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(48.dp).testTag("lesson_game_exit_btn")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "الخروج من الدرس")
                        }

                        // Hearts & Streak & Live XP
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (combo >= 2) {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.LocalFireDepartment,
                                            contentDescription = "سلسلة صحيحة",
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "x$combo",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = "نقاط الخبرة",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "+$sessionXp XP",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        if (hearts > 0) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "القلوب المتبقية",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "$hearts",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(100.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الجملة ${currentIndex + 1} من ${sentences.size} • ${dialect.arabicLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when (roundStep) {
                                0 -> "الجولة 1/3: اسمع وافهم"
                                1 -> "الجولة 2/3: ركّب الجملة"
                                else -> "الجولة 3/3: تحدي النجوم"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 3 Round step pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RoundStepPill(
                            label = "1. اسمع وافهم",
                            active = roundStep == 0,
                            done = roundStep > 0,
                            modifier = Modifier.weight(1f)
                        )
                        RoundStepPill(
                            label = "2. ركّب الجملة",
                            active = roundStep == 1,
                            done = roundStep > 1,
                            modifier = Modifier.weight(1f)
                        )
                        RoundStepPill(
                            label = "3. تحدي النجوم",
                            active = roundStep == 2,
                            done = roundStep == 2 && selectedQuizAnswer != null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2) Active Round Stage
        when (roundStep) {
            0 -> {
                // ROUND 0: Listen & Discover
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "استمع للجملة جيداً واحفظ إيقاعها قبل بدء اللعب:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            SentenceRevealCard(item.sentence, item.arabicTranslation, item.id)

                            AudioSpeedButtons(
                                onPlayNormal = { speechEngine.speak(item.sentence, dialect.code, slow = false) },
                                onPlaySlow = { speechEngine.speak(item.sentence, dialect.code, slow = true) }
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        onSaveToReview(item)
                                        savedFeedback = true
                                    },
                                    modifier = Modifier.weight(1f).testTag("save_sentence_fsrs_btn")
                                ) {
                                    Icon(
                                        if (savedFeedback) Icons.Filled.CheckCircle else Icons.Filled.BookmarkAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.size(4.dp))
                                    Text(if (savedFeedback) "تم الحفظ ✓" else "حفظ للمراجعة")
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

                // Collapsible Grammar & Real-World Explanation Drawer
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showGrammarDrawer = !showGrammarDrawer }
                                    .padding(vertical = 4.dp)
                                    .testTag("toggle_grammar_drawer_btn"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (showGrammarDrawer) "إخفاء شرح القاعدة والأمثلة" else "عرض شرح التعبير والقواعد والأمثلة",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    imageVector = if (showGrammarDrawer) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = "تبديل الشرح"
                                )
                            }

                            AnimatedVisibility(visible = showGrammarDrawer) {
                                Column(
                                    modifier = Modifier.padding(top = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HorizontalDivider()
                                    Text("شرح التعبير:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(item.expressionNoteAr, style = MaterialTheme.typography.bodyMedium)
                                    HorizontalDivider()
                                    Text("القواعد داخل السياق:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(item.grammarInContextAr, style = MaterialTheme.typography.bodyMedium)
                                    HorizontalDivider()
                                    Text("الاستخدام الواقعي:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(item.realWorldUsageAr, style = MaterialTheme.typography.bodyMedium)
                                    if (item.similarExamples.isNotEmpty()) {
                                        HorizontalDivider()
                                        Text("أمثلة مشابهة (اضغط للاستماع):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                        }
                    }
                }

                // CTA to start Round 1 Game
                item {
                    Button(
                        onClick = {
                            sessionXp += 5
                            roundStep = 1
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("round_discover_next_btn")
                    ) {
                        Text(
                            text = "فهمتها! ابدأ لعبة التركيب (+5 XP)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            1 -> {
                // ROUND 1: Interactive Word-Tile Sentence Builder Game
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "رتّب الكلمات لتكوين الجملة:",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedButton(
                                    onClick = { speechEngine.speak(item.sentence, dialect.code, slow = true) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("word_builder_hint_audio_btn")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "تلميح صوتي",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("تلميح", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = item.arabicTranslation,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }

                            // Answer Tray (LTR for target English/Spanish sentence order)
                            val trayBorderColor = when (builderCheckState) {
                                BuilderCheckState.CORRECT -> MaterialTheme.colorScheme.primary
                                BuilderCheckState.WRONG -> MaterialTheme.colorScheme.error
                                BuilderCheckState.IDLE -> MaterialTheme.colorScheme.outlineVariant
                            }
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.5.dp, trayBorderColor),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 84.dp)
                            ) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    if (selectedTileIds.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "اضغط على الكلمات بالأسفل بالترتيب الصحيح...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        FlowRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            selectedTileIds.forEach { tileId ->
                                                val tile = targetTiles.firstOrNull { it.id == tileId } ?: return@forEach
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier
                                                        .heightIn(min = 42.dp)
                                                        .clickable(enabled = builderCheckState != BuilderCheckState.CORRECT) {
                                                            selectedTileIds = selectedTileIds.filterNot { it == tileId }
                                                            if (builderCheckState == BuilderCheckState.WRONG) {
                                                                builderCheckState = BuilderCheckState.IDLE
                                                            }
                                                        }
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                                    ) {
                                                        Text(
                                                            text = tile.display,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Shuffled Word Bank (LTR)
                            Text(
                                text = "بنك الكلمات:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    shuffledBank.forEach { tile ->
                                        val isUsed = selectedTileIds.contains(tile.id)
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isUsed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isUsed) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier
                                                .heightIn(min = 44.dp)
                                                .clickable(enabled = !isUsed && builderCheckState != BuilderCheckState.CORRECT) {
                                                    selectedTileIds = selectedTileIds + tile.id
                                                    if (builderCheckState == BuilderCheckState.WRONG) {
                                                        builderCheckState = BuilderCheckState.IDLE
                                                    }
                                                }
                                                .testTag("word_tile_${tile.id}")
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                            ) {
                                                Text(
                                                    text = tile.display,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isUsed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                                    else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Result Feedback Banner
                            when (builderCheckState) {
                                BuilderCheckState.CORRECT -> {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Column {
                                                Text(
                                                    "ممتاز! تركيب صحيح 100% (+15 XP)",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    "انتقل الآن للجولة الأخيرة لكسب النجوم!",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                }

                                BuilderCheckState.WRONG -> {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Cancel,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                                Text(
                                                    "قريب جداً! الترتيب الصحيح هو:",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                            LtrTargetText(
                                                item.sentence,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                bold = true
                                            )
                                        }
                                    }
                                }

                                BuilderCheckState.IDLE -> Unit
                            }

                            // Action Controls
                            when (builderCheckState) {
                                BuilderCheckState.CORRECT -> {
                                    Button(
                                        onClick = { roundStep = 2 },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .testTag("word_builder_continue_btn")
                                    ) {
                                        Text(
                                            "التالي: تحدي الفهم والنجوم",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                BuilderCheckState.WRONG -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                selectedTileIds = emptyList()
                                                builderCheckState = BuilderCheckState.IDLE
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .testTag("word_builder_retry_btn")
                                        ) {
                                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("أعد المحاولة")
                                        }
                                        Button(
                                            onClick = { roundStep = 2 },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .testTag("word_builder_skip_btn")
                                        ) {
                                            Text("التحدي التالي")
                                        }
                                    }
                                }

                                BuilderCheckState.IDLE -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { selectedTileIds = emptyList() },
                                            enabled = selectedTileIds.isNotEmpty(),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.height(52.dp)
                                        ) {
                                            Text("مسح")
                                        }
                                        Button(
                                            onClick = {
                                                val chosenNorm = selectedTileIds.mapNotNull { id ->
                                                    targetTiles.firstOrNull { it.id == id }?.normalized
                                                }
                                                val expectedNorm = targetTiles.map { it.normalized }
                                                totalChallengesAttempted++
                                                if (chosenNorm == expectedNorm) {
                                                    builderCheckState = BuilderCheckState.CORRECT
                                                    correctChallenges++
                                                    combo++
                                                    if (combo > maxCombo) maxCombo = combo
                                                    sessionXp += 15 + if (combo >= 2) 5 else 0
                                                    speechEngine.speak(item.sentence, dialect.code, slow = false)
                                                } else {
                                                    builderCheckState = BuilderCheckState.WRONG
                                                    hearts = (hearts - 1).coerceAtLeast(0)
                                                    combo = 0
                                                }
                                            },
                                            enabled = selectedTileIds.size == targetTiles.size,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .testTag("word_builder_check_btn")
                                        ) {
                                            Text(
                                                if (selectedTileIds.size == targetTiles.size) "تحقق من التركيب"
                                                else "اختر باقي الكلمات (${selectedTileIds.size}/${targetTiles.size})",
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

            else -> {
                // ROUND 2: Comprehension Challenge + Optional Bonus Voice XP
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "تحدي الفهم السريع (+15 XP):",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(12.dp)) {
                                    LtrTargetText(item.sentence, bold = true)
                                }
                            }
                            Text(
                                text = item.comprehensionQuestionAr,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )

                            item.optionsAr.forEachIndexed { idx, opt ->
                                val picked = selectedQuizAnswer == idx
                                val isCorrect = idx == item.correctOptionIndex
                                val bgColor = when {
                                    selectedQuizAnswer == null -> MaterialTheme.colorScheme.surfaceContainerLow
                                    picked && isCorrect -> MaterialTheme.colorScheme.primaryContainer
                                    picked && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                                    isCorrect -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                                }
                                val borderColor = when {
                                    selectedQuizAnswer == null -> MaterialTheme.colorScheme.outlineVariant
                                    isCorrect -> MaterialTheme.colorScheme.primary
                                    picked && !isCorrect -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                }

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = bgColor),
                                    border = BorderStroke(1.2.dp, borderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp)
                                        .clickable(enabled = selectedQuizAnswer == null) {
                                            selectedQuizAnswer = idx
                                            totalChallengesAttempted++
                                            if (idx == item.correctOptionIndex) {
                                                correctChallenges++
                                                combo++
                                                if (combo > maxCombo) maxCombo = combo
                                                sessionXp += 15 + if (combo >= 2) 5 else 0
                                            } else {
                                                hearts = (hearts - 1).coerceAtLeast(0)
                                                combo = 0
                                            }
                                        }
                                        .testTag("quiz_option_$idx")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = opt,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (picked || (selectedQuizAnswer != null && isCorrect)) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (selectedQuizAnswer != null) {
                                            when {
                                                isCorrect -> Icon(
                                                    Icons.Filled.CheckCircle,
                                                    contentDescription = "إجابة صحيحة",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                picked -> Icon(
                                                    Icons.Filled.Cancel,
                                                    contentDescription = "إجابة خاطئة",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Optional Bonus Voice Challenge (+10 XP)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مكافأة النطق الإضافية",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = if (voiceBonusClaimed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                ) {
                                    Text(
                                        text = if (voiceBonusClaimed) "تم +10 XP ✓" else "+10 XP إضافية",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (voiceBonusClaimed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = "انطق الجملة بصوتك وقارنها بالنطق الأصلي لكسب نقاط إضافية:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                                Text(
                                    recorderError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Next Sentence or Finish Lesson
                item {
                    Button(
                        onClick = {
                            if (currentIndex < sentences.lastIndex) {
                                currentIndex++
                            } else {
                                showVictory = true
                            }
                        },
                        enabled = selectedQuizAnswer != null,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("sentence_next_or_finish_btn")
                    ) {
                        Text(
                            text = when {
                                selectedQuizAnswer == null -> "اختر إجابة أولاً للمتابعة"
                                currentIndex < sentences.lastIndex -> "الجملة التالية (${currentIndex + 2} من ${sentences.size})"
                                else -> "عرض النتيجة واستلام الجوائز"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundStepPill(
    label: String,
    active: Boolean,
    done: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = when {
        active -> MaterialTheme.colorScheme.primary
        done -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val fg = when {
        active -> MaterialTheme.colorScheme.onPrimary
        done -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        modifier = modifier
    ) {
        Text(
            text = if (done && !active) "✓ $label" else label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (active || done) FontWeight.Bold else FontWeight.Normal,
            color = fg,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun LessonVictoryCard(
    lessonTitleAr: String,
    stars: Int,
    sessionXp: Int,
    accuracy: Int,
    heartsRemaining: Int,
    maxCombo: Int,
    onClaimReward: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "كأس الفوز",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // 3 Stars Row
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..3).forEach { starIndex ->
                        Icon(
                            imageVector = if (starIndex <= stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "نجمة $starIndex",
                            tint = if (starIndex <= stars) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Text(
                    text = "بطل! أنهيت الدرس بنجاح",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = lessonTitleAr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VictoryStatBox(
                        label = "نقاط الخبرة",
                        value = "+$sessionXp XP",
                        modifier = Modifier.weight(1f)
                    )
                    VictoryStatBox(
                        label = "الدقة",
                        value = "$accuracy%",
                        modifier = Modifier.weight(1f)
                    )
                    VictoryStatBox(
                        label = "أطول سلسلة",
                        value = "x${maxCombo.coerceAtLeast(1)}",
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "القلوب المتبقية: $heartsRemaining من 5",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Button(
                    onClick = onClaimReward,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("lesson_victory_claim_btn")
                ) {
                    Text(
                        text = "استلام الجوائز والمتابعة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun VictoryStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun tokenizeSentence(sentence: String): List<WordTile> {
    val rawTokens = sentence
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    return rawTokens.mapIndexed { idx, token ->
        val cleaned = token
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}']"), "")
            .ifBlank { token.lowercase() }
        WordTile(
            id = idx,
            display = token,
            normalized = cleaned
        )
    }
}

private fun shuffleTilesDeterministically(tiles: List<WordTile>, seedKey: String): List<WordTile> {
    if (tiles.size <= 1) return tiles
    val rng = Random(seedKey.hashCode())
    var candidate = tiles.shuffled(rng)
    var attempts = 0
    while (candidate.map { it.normalized } == tiles.map { it.normalized } && attempts < 6) {
        candidate = tiles.shuffled(Random(seedKey.hashCode() + attempts + 17))
        attempts++
    }
    if (candidate.map { it.normalized } == tiles.map { it.normalized } && candidate.size >= 2) {
        candidate = candidate.drop(1) + candidate.first()
    }
    return candidate
}
