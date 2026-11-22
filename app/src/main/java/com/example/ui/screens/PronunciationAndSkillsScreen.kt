package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.data.curriculum.CurriculumCatalog
import com.example.data.model.Dialect
import com.example.data.model.PronunciationLog
import com.example.data.model.SkillType
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
fun PronunciationLabScreen(
    profile: UserProfile,
    initialSentence: String?,
    pronunciationLogs: List<PronunciationLog>,
    aiViewModel: AiTutorViewModel,
    speechEngine: SpeechEngine,
    audioRecorder: AudioClipRecorder
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeLang = profile.currentLanguageEnum()
    val activeLevel = profile.currentLevelEnum()
    val activeDialect = Dialect.fromCode(profile.currentDialectCode(), activeLang)

    val practiceSentences = remember(activeLang, activeLevel) {
        CurriculumCatalog.sentencesFor(activeLang, activeLevel, 1, 1).map { it.sentence }
    }

    var targetSentence by remember(initialSentence, activeLang, activeLevel) {
        mutableStateOf(initialSentence?.takeIf { it.isNotBlank() } ?: practiceSentences.first())
    }
    var spokenTranscript by remember { mutableStateOf("") }
    var pendingAudioAction by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(targetSentence) {
        audioRecorder.resetClip()
    }

    val evalState by aiViewModel.pronunciationState.collectAsStateWithLifecycle()
    val isListening by speechEngine.isListening.collectAsStateWithLifecycle()
    val speechError by speechEngine.speechError.collectAsStateWithLifecycle()

    val isRecordingClip by audioRecorder.isRecording.collectAsStateWithLifecycle()
    val isPlayingClip by audioRecorder.isPlaying.collectAsStateWithLifecycle()
    val hasRecordedClip by audioRecorder.hasRecording.collectAsStateWithLifecycle()
    val recordingSeconds by audioRecorder.recordingSeconds.collectAsStateWithLifecycle()
    val recorderError by audioRecorder.error.collectAsStateWithLifecycle()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            when (pendingAudioAction) {
                "clip" -> audioRecorder.startRecording("pron_${targetSentence.hashCode()}")
                else -> speechEngine.startListening(activeDialect.code) { recognized ->
                    spokenTranscript = recognized
                }
            }
        }
        pendingAudioAction = null
    }

    fun requestMicAndRun(action: String, block: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            block()
        } else {
            pendingAudioAction = action
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "مختبر النطق الاحترافي",
                subtitle = "تقييم الأصوات والتشديد والإيقاع والطلاقة والوضوح مع التمييز بين اللهجة والخطأ",
                badge = activeDialect.arabicLabel
            )
        }

        // Practice Sentence Picker
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(practiceSentences.indices.toList()) { idx ->
                    val s = practiceSentences[idx]
                    FilterChip(
                        selected = targetSentence == s,
                        onClick = { targetSentence = s },
                        label = { Text("جملة ${idx + 1}") }
                    )
                }
            }
        }

        // Target Model Card + Shadowing
        item {
            ElevatedCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("الجملة النموذجية للتدريب:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    LtrTargetText(targetSentence, bold = true)
                    AudioSpeedButtons(
                        onPlayNormal = { speechEngine.speak(targetSentence, activeDialect.code, slow = false) },
                        onPlaySlow = { speechEngine.speak(targetSentence, activeDialect.code, slow = true) }
                    )

                    HorizontalDivider()

                    // Real Voice Clip Recorder & Side-by-Side Comparison
                    Text(
                        text = "تسجيل صوتك الحقيقي ومقارنته بالنطق الأصلي:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (isRecordingClip) {
                                    audioRecorder.stopRecording()
                                } else {
                                    if (isListening) speechEngine.stopListening()
                                    requestMicAndRun("clip") {
                                        audioRecorder.startRecording("pron_${targetSentence.hashCode()}")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecordingClip) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).testTag("pron_clip_record_btn")
                        ) {
                            Icon(
                                if (isRecordingClip) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                                contentDescription = "تسجيل مقطع صوتي"
                            )
                            Spacer(Modifier.size(6.dp))
                            Text(if (isRecordingClip) "إيقاف (00:${recordingSeconds.toString().padStart(2, '0')})" else "سجّل صوتك")
                        }

                        OutlinedButton(
                            onClick = {
                                if (isPlayingClip) audioRecorder.stopPlayback() else audioRecorder.playRecording()
                            },
                            enabled = hasRecordedClip && !isRecordingClip,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).testTag("pron_clip_play_btn")
                        ) {
                            Icon(
                                if (isPlayingClip) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = "استمع لصوتك"
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(if (isPlayingClip) "إيقاف" else "اسمع صوتك")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            speechEngine.speak(targetSentence, activeDialect.code, slow = false)
                            scope.launch {
                                val waitMs = (targetSentence.length * 75L).coerceIn(2200L, 5200L)
                                delay(waitMs)
                                audioRecorder.playRecording()
                            }
                        },
                        enabled = hasRecordedClip && !isRecordingClip,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("pron_compare_btn")
                    ) {
                        Icon(Icons.Filled.CompareArrows, contentDescription = "مقارنة الصوت الأصلي بصوتك")
                        Spacer(Modifier.size(6.dp))
                        Text("مقارنة مباشرة: النطق الأصلي ثم صوتك")
                    }

                    if (!recorderError.isNullOrBlank()) {
                        Text(recorderError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }

                    HorizontalDivider()

                    // Live Speech-to-Text Capture for Deep Thinking AI Analysis
                    Text(
                        text = "التحليل الذكي بالذكاء الاصطناعي (5 محاور):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (isListening) {
                                    speechEngine.stopListening()
                                } else {
                                    if (isRecordingClip) audioRecorder.stopRecording()
                                    requestMicAndRun("stt") {
                                        speechEngine.startListening(activeDialect.code) { recognized ->
                                            spokenTranscript = recognized
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).testTag("pronunciation_record_btn")
                        ) {
                            Icon(if (isListening) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text(if (isListening) "إيقاف الالتقاط" else "التقاط النطق للتحليل")
                        }

                        OutlinedButton(
                            onClick = {
                                speechEngine.speak(targetSentence, activeDialect.code, slow = true)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تدريب Shadowing")
                        }
                    }

                    if (!speechError.isNullOrBlank()) {
                        Text(speechError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }

                    OutlinedTextField(
                        value = spokenTranscript,
                        onValueChange = { spokenTranscript = it },
                        label = { Text("ما تم التقاطه من صوتك (يمكنك إعادة التسجيل أو التعديل للتجربة)") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("pronunciation_transcript_field")
                    )

                    Button(
                        onClick = {
                            aiViewModel.analyzePronunciation(
                                targetSentence = targetSentence,
                                recognizedText = spokenTranscript,
                                language = activeLang,
                                dialect = activeDialect,
                                cefrLevel = activeLevel
                            )
                        },
                        enabled = spokenTranscript.isNotBlank() && evalState !is AiOpState.Loading,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("analyze_pronunciation_btn")
                    ) {
                        if (evalState is AiOpState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(Modifier.size(8.dp))
                            Text("جاري التحليل الصوتي العميق...")
                        } else {
                            Icon(Icons.Filled.Psychology, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text("تحليل النطق وتحديد نقاط الضعف بالعربية")
                        }
                    }
                }
            }
        }

        // Evaluation Results Card
        item {
            when (val state = evalState) {
                is AiOpState.Error -> ErrorBannerCard(state.message)
                is AiOpState.Success -> {
                    val eval = state.data
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("تقرير تقييم النطق الشامل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            MetricRow("دقة الأصوات (Accuracy)", eval.accuracyScore)
                            MetricRow("التشديد (Stress)", eval.stressScore)
                            MetricRow("الإيقاع (Rhythm)", eval.rhythmScore)
                            MetricRow("الطلاقة (Fluency)", eval.fluencyScore)
                            MetricRow("وضوح الكلام (Clarity)", eval.clarityScore)
                            HorizontalDivider()
                            Text("التمييز بين اللهجة والخطأ الحقيقي:", fontWeight = FontWeight.Bold)
                            Text(eval.dialectNoteAr, style = MaterialTheme.typography.bodySmall)
                            Text("شرح الأخطاء والتوجيه بالعربية:", fontWeight = FontWeight.Bold)
                            Text(eval.arabicCoaching, style = MaterialTheme.typography.bodyMedium)
                            HorizontalDivider()
                            Text("جملة تدريب تصحيحي مخصصة لك:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            LtrTargetText(eval.correctiveDrillTarget, bold = true)
                            OutlinedButton(
                                onClick = {
                                    targetSentence = eval.correctiveDrillTarget
                                    spokenTranscript = ""
                                    speechEngine.speak(eval.correctiveDrillTarget, activeDialect.code, slow = true)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ابدأ التدريب التصحيحي فورًا")
                            }
                        }
                    }
                }
                else -> Unit
            }
        }

        // Previous Pronunciation History from Firestore
        if (pronunciationLogs.isNotEmpty()) {
            item {
                SectionHeader("سجل تدريبات النطق المحفوظة", "تقدمك المسجّل في السحابة")
            }
            items(pronunciationLogs.take(5), key = { it.id }) { log ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatusPill(log.dialect)
                            Text(
                                text = "الدقة ${log.accuracyScore}% • الطلاقة ${log.fluencyScore}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LtrTargetText(log.targetSentence)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, score: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("$score%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { (score / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(100.dp))
        )
    }
}

@Composable
fun FourSkillsHubScreen(
    profile: UserProfile,
    aiViewModel: AiTutorViewModel,
    speechEngine: SpeechEngine,
    onNavigate: (LingoDestination) -> Unit
) {
    val activeLang = profile.currentLanguageEnum()
    val activeLevel = profile.currentLevelEnum()
    val activeDialect = Dialect.fromCode(profile.currentDialectCode(), activeLang)

    var selectedSkill by remember { mutableStateOf(SkillType.WRITING) }
    var writingDraft by remember { mutableStateOf("") }
    val writingGradeState by aiViewModel.writingGradeState.collectAsStateWithLifecycle()

    val writingPrompt = if (activeLang == TargetLanguage.EN) {
        "Write 3–4 natural sentences describing a challenge you solved recently at work, study, or travel (CEFR ${activeLevel.code})."
    } else {
        "Escribe 3 o 4 frases naturales describiendo un reto que resolviste recientemente en tu trabajo, estudio o viaje (MCER ${activeLevel.code})."
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "منظومة المهارات الأربع",
                subtitle = "تدريبات متوازنة على الاستماع والتحدث والقراءة والكتابة لمستوى ${activeLevel.code}",
                badge = activeLang.arabicName
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SkillType.entries) { skill ->
                    FilterChip(
                        selected = selectedSkill == skill,
                        onClick = { selectedSkill = skill },
                        label = { Text(skill.arabicName) }
                    )
                }
            }
        }

        item {
            ElevatedCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(selectedSkill.arabicName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(selectedSkill.subtitleAr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    when (selectedSkill) {
                        SkillType.LISTENING -> {
                            val sample = CurriculumCatalog.examQuestionsFor(activeLang, activeLevel).first { it.skill == SkillType.LISTENING }
                            Text("استمع للمقطع الصوتي باللهجة المختارة ثم أعد تشغيله ببطء للتحقق من التفاصيل:")
                            AudioSpeedButtons(
                                onPlayNormal = { speechEngine.speak(sample.stimulusTarget, activeDialect.code, slow = false) },
                                onPlaySlow = { speechEngine.speak(sample.stimulusTarget, activeDialect.code, slow = true) }
                            )
                            Button(onClick = { onNavigate(LingoDestination.VIDEOS) }, modifier = Modifier.fillMaxWidth()) {
                                Text("الانتقال لتدريبات الفيديوهات الواقعية")
                            }
                        }
                        SkillType.SPEAKING -> {
                            Text("تدرّب على سرد القصص والمناقشة بالصوت في مختبر النطق أو مع المدرس الذكي:")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onNavigate(LingoDestination.PRONUNCIATION) }, modifier = Modifier.weight(1f)) {
                                    Text("مختبر النطق")
                                }
                                OutlinedButton(onClick = { onNavigate(LingoDestination.AI_TUTOR) }, modifier = Modifier.weight(1f)) {
                                    Text("المدرس الذكي")
                                }
                            }
                        }
                        SkillType.READING -> {
                            val readingQ = CurriculumCatalog.examQuestionsFor(activeLang, activeLevel).first { it.skill == SkillType.READING }
                            LtrTargetText(readingQ.stimulusTarget, bold = true)
                            Text("اقرأ النص بتركيز ثم انتقل لمكتبة القصص التفاعلية لتدريبات الاستنتاج العميق.")
                            Button(onClick = { onNavigate(LingoDestination.STORIES) }, modifier = Modifier.fillMaxWidth()) {
                                Text("فتح القصص التفاعلية")
                            }
                        }
                        SkillType.WRITING -> {
                            Text("مهمة الكتابة المخصصة لمستوى ${activeLevel.code}:", fontWeight = FontWeight.Bold)
                            LtrTargetText(writingPrompt)
                            OutlinedTextField(
                                value = writingDraft,
                                onValueChange = { writingDraft = it },
                                label = { Text("اكتب إجابتك هنا باللغة المستهدفة...") },
                                minLines = 4,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().testTag("skills_writing_input")
                            )
                            Button(
                                onClick = {
                                    aiViewModel.gradeWriting(activeLang, activeLevel, writingPrompt, writingDraft)
                                },
                                enabled = writingDraft.trim().length >= 10 && writingGradeState !is AiOpState.Loading,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().testTag("grade_writing_btn")
                            ) {
                                if (writingGradeState is AiOpState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    Spacer(Modifier.size(8.dp))
                                    Text("جاري التصحيح بالتفكير العميق...")
                                } else {
                                    Text("تصحيح الكتابة بالتفكير العميق (AI)")
                                }
                            }
                            when (val ws = writingGradeState) {
                                is AiOpState.Error -> ErrorBannerCard(ws.message)
                                is AiOpState.Success -> {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("درجة الكتابة: ${ws.data.first}%", fontWeight = FontWeight.Bold)
                                            Text(ws.data.second, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}
