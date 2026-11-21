package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.SpeechEngine
import com.example.data.curriculum.CurriculumCatalog
import com.example.data.model.Dialect
import com.example.data.model.TargetLanguage
import com.example.data.model.TutorMessage
import com.example.data.model.TutorSessionScorecard
import com.example.data.model.UserProfile
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.LtrTargetText
import com.example.ui.components.StatusPill
import com.example.ui.viewmodel.AiOpState
import com.example.ui.viewmodel.AiTutorViewModel
import com.example.ui.viewmodel.DataState

@Composable
fun AiTutorScreen(
    profile: UserProfile,
    aiViewModel: AiTutorViewModel,
    speechEngine: SpeechEngine,
    onSaveSentence: (String, String, String) -> Unit,
    onPracticePronunciation: (String) -> Unit
) {
    val context = LocalContext.current
    val activeLang = profile.currentLanguageEnum()
    val activeLevel = profile.currentLevelEnum()
    val activeDialect = Dialect.fromCode(profile.currentDialectCode(), activeLang)

    var selectedScenario by remember { mutableStateOf(CurriculumCatalog.tutorScenarios.first()) }
    var deepThinkingEnabled by remember { mutableStateOf(false) }
    var draftInput by remember { mutableStateOf("") }
    var showNewSessionConfirm by remember { mutableStateOf(false) }

    val messagesState by aiViewModel.messagesState.collectAsStateWithLifecycle()
    val streamState by aiViewModel.tutorStreamState.collectAsStateWithLifecycle()
    val scorecardState by aiViewModel.sessionScorecardState.collectAsStateWithLifecycle()
    val isListening by speechEngine.isListening.collectAsStateWithLifecycle()
    val speechError by speechEngine.speechError.collectAsStateWithLifecycle()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            speechEngine.startListening(activeDialect.code) { recognized ->
                draftInput = recognized
            }
        }
    }

    val allMessages = (messagesState as? DataState.Success)?.data.orEmpty()
    val scenarioMessages = remember(allMessages, selectedScenario.id, activeLang.code) {
        allMessages.filter { it.scenarioId == selectedScenario.id && it.language == activeLang.code }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(scenarioMessages.size, streamState) {
        val totalItems = scenarioMessages.size + 2
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems)
        }
    }

    val learnerTurnCount = remember(scenarioMessages) {
        scenarioMessages.count { it.sender == "user" }
    }

    LaunchedEffect(selectedScenario.id, activeLang.code) {
        aiViewModel.clearSessionScorecard()
    }

    if (showNewSessionConfirm) {
        AlertDialog(
            onDismissRequest = { showNewSessionConfirm = false },
            title = { Text("بدء جلسة جديدة نظيفة؟", fontWeight = FontWeight.Bold) },
            text = {
                Text("سيتم مسح رسائل هذا الموقف (${selectedScenario.titleAr}) للبدء من جديد بذاكرة محادثة نظيفة.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNewSessionConfirm = false
                        aiViewModel.startFreshTutorSession(scenarioMessages)
                    }
                ) {
                    Text("نعم، ابدأ جلسة جديدة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewSessionConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tutor Header + Deep Thinking Toggle
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_ai_tutor_avatar_1795256212805),
                        contentDescription = "المدرس الذكي LingoWorld AI",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "المدرس الذكي • ${activeLang.arabicName} (${activeLevel.code})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedScenario.titleAr} • ${activeDialect.arabicLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Psychology,
                            contentDescription = "وضع التفكير العميق",
                            tint = if (deepThinkingEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Switch(
                            checked = deepThinkingEnabled,
                            onCheckedChange = { deepThinkingEnabled = it },
                            modifier = Modifier.testTag("deep_thinking_toggle")
                        )
                    }
                }

                if (deepThinkingEnabled) {
                    StatusPill(
                        text = "وضع التفكير العميق مفعّل: تحليل نحوي وأسلوبي وتصحيح متقدم",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CurriculumCatalog.tutorScenarios, key = { it.id }) { scenario ->
                        FilterChip(
                            selected = selectedScenario.id == scenario.id,
                            onClick = { selectedScenario = scenario },
                            label = { Text(scenario.titleAr) },
                            modifier = Modifier.testTag("scenario_chip_${scenario.id}")
                        )
                    }
                }

                // Session Scorecard & Fresh Session Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            aiViewModel.generateSessionScorecard(
                                language = activeLang,
                                dialect = activeDialect,
                                cefrLevel = activeLevel,
                                scenario = selectedScenario,
                                messages = scenarioMessages
                            )
                        },
                        enabled = learnerTurnCount > 0 && scorecardState !is AiOpState.Loading,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).testTag("tutor_scorecard_btn")
                    ) {
                        Icon(Icons.Filled.Assessment, contentDescription = "تقرير الجلسة", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("تقرير الأخطاء والجلسة", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { showNewSessionConfirm = true },
                        enabled = scenarioMessages.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("tutor_new_session_btn")
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = "بدء جلسة جديدة", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("جلسة جديدة", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Scrollable Multi-Turn Conversation Thread
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Scenario Starter Card
            item(key = "starter_${selectedScenario.id}_${activeLang.code}") {
                val starter = if (activeLang == TargetLanguage.EN) selectedScenario.starterEn else selectedScenario.starterEs
                TutorBubble(
                    message = TutorMessage(
                        sender = "tutor",
                        text = starter,
                        arabicExplanation = "موقف: ${selectedScenario.subtitleAr} — أجب باللغة المستهدفة صوتيًا أو كتابيًا وسأصحح لك فورًا بالعربية!"
                    ),
                    onSpeak = { speechEngine.speak(starter, activeDialect.code, slow = false) }
                )
            }

            if (messagesState is DataState.Error) {
                item {
                    ErrorBannerCard((messagesState as DataState.Error).message)
                }
            }

            items(scenarioMessages, key = { it.id.ifBlank { "${it.sender}_${it.text.hashCode()}" } }) { msg ->
                if (msg.sender == "user") {
                    UserBubble(msg)
                } else {
                    TutorBubble(
                        message = msg,
                        onSpeak = { speechEngine.speak(msg.text, activeDialect.code, slow = false) }
                    )
                }
            }

            // Streaming / Loading / Error Live Feedback
            item(key = "stream_status") {
                when (val s = streamState) {
                    is AiOpState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(
                                text = if (deepThinkingEnabled) "جاري التفكير العميق وتحليل لغتك..." else "المدرس الذكي يكتب الرد...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    is AiOpState.Streaming -> {
                        TutorBubble(
                            message = TutorMessage(
                                sender = "tutor",
                                text = s.partialText,
                                deepThinking = deepThinkingEnabled
                            ),
                            onSpeak = {}
                        )
                    }
                    is AiOpState.Error -> ErrorBannerCard(s.message)
                    else -> Unit
                }
            }

            // End-of-Session Scorecard & Recurring Mistakes Tracker
            item(key = "session_scorecard") {
                when (val sc = scorecardState) {
                    is AiOpState.Loading -> {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                Text("جاري تحليل أخطائك المتكررة وإعداد تقرير الجلسة بالتفكير العميق...")
                            }
                        }
                    }
                    is AiOpState.Error -> ErrorBannerCard(sc.message)
                    is AiOpState.Success -> {
                        SessionScorecardCard(
                            scorecard = sc.data,
                            onSpeakPractice = {
                                speechEngine.speak(sc.data.correctivePracticeSentence, activeDialect.code, slow = false)
                            },
                            onSaveToFsrs = {
                                onSaveSentence(
                                    sc.data.correctivePracticeSentence,
                                    sc.data.nextFocusTipAr,
                                    sc.data.recurringMistakesAr.joinToString(" • ")
                                )
                            },
                            onPracticePronunciation = {
                                onPracticePronunciation(sc.data.correctivePracticeSentence)
                            },
                            onDismiss = { aiViewModel.clearSessionScorecard() }
                        )
                    }
                    else -> Unit
                }
            }
        }

        if (!speechError.isNullOrBlank()) {
            Text(
                text = speechError.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Composer Row (Voice + Text + Send)
        Surface(
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (isListening) {
                            speechEngine.stopListening()
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                speechEngine.startListening(activeDialect.code) { recognized ->
                                    draftInput = recognized
                                }
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        )
                        .testTag("tutor_voice_mic_btn")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = "تحدث بالصوت مع المدرس الذكي",
                        tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = draftInput,
                    onValueChange = { draftInput = it },
                    placeholder = {
                        Text(
                            if (activeLang == TargetLanguage.EN) "اكتب أو تحدث بالإنجليزية..."
                            else "اكتب أو تحدث بالإسبانية..."
                        )
                    },
                    maxLines = 3,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f).testTag("tutor_input_field")
                )

                val sending = streamState is AiOpState.Loading || streamState is AiOpState.Streaming
                IconButton(
                    enabled = draftInput.isNotBlank() && !sending,
                    onClick = {
                        val toSend = draftInput
                        draftInput = ""
                        aiViewModel.sendTutorMessage(
                            scenario = selectedScenario,
                            language = activeLang,
                            dialect = activeDialect,
                            cefrLevel = activeLevel,
                            existingMessages = scenarioMessages,
                            userInput = toSend,
                            deepThinking = deepThinkingEnabled
                        )
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("tutor_send_btn")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "إرسال الرسالة",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun UserBubble(message: TutorMessage) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LtrTargetText(message.text, color = MaterialTheme.colorScheme.onPrimary)
                if (message.deepThinking) {
                    Text(
                        text = "مع تحليل التفكير العميق",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorBubble(
    message: TutorMessage,
    onSpeak: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        text = if (message.deepThinking) "المدرس الذكي • تفكير عميق" else "المدرس الذكي",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    IconButton(onClick = onSpeak, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Filled.VolumeUp,
                            contentDescription = "الاستماع لرد المدرس",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                LtrTargetText(message.text)
                if (message.arabicExplanation.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = message.arabicExplanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionScorecardCard(
    scorecard: TutorSessionScorecard,
    onSpeakPractice: () -> Unit,
    onSaveToFsrs: () -> Unit,
    onPracticePronunciation: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth().testTag("tutor_scorecard_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تقرير الجلسة والأخطاء المتكررة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusPill(
                    text = "التقييم ${scorecard.overallScore}%",
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }

            ScoreAxisBar("الطلاقة وتدفق الحوار", scorecard.fluencyScore)
            ScoreAxisBar("دقة القواعد والتراكيب", scorecard.grammarScore)
            ScoreAxisBar("ثراء المفردات والملاءمة", scorecard.vocabularyScore)

            HorizontalDivider()
            Text("الأخطاء المتكررة المرصودة في محادثتك:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            scorecard.recurringMistakesAr.forEach { mistake ->
                Text("• $mistake", style = MaterialTheme.typography.bodySmall)
            }

            HorizontalDivider()
            Text("نقاط قوتك:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(scorecard.strengthsAr, style = MaterialTheme.typography.bodySmall)
            Text("الخطوة التالية الموصى بها:", fontWeight = FontWeight.Bold)
            Text(scorecard.nextFocusTipAr, style = MaterialTheme.typography.bodySmall)

            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("جملة تدريب تصحيحية مخصصة لك:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = onSpeakPractice, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = "الاستماع للجملة التصحيحية")
                }
            }
            LtrTargetText(scorecard.correctivePracticeSentence, bold = true)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onSaveToFsrs,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("حفظ للمراجعة")
                }
                OutlinedButton(
                    onClick = onPracticePronunciation,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("تدريب نطقها")
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("إخفاء التقرير")
            }
        }
    }
}

@Composable
private fun ScoreAxisBar(label: String, score: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("$score%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { (score / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(100.dp))
        )
    }
}
