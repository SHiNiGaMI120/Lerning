package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AudioClipRecorder
import com.example.core.SpeechEngine
import com.example.data.curriculum.CurriculumCatalog
import com.example.data.model.SkillType
import com.example.data.model.CefrLevel
import com.example.data.model.CmsContentItem
import com.example.data.model.Dialect
import com.example.data.model.ExamAttempt
import com.example.data.model.ReviewCard
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
fun HardCefrExamsScreen(
    profile: UserProfile,
    examAttempts: List<ExamAttempt>,
    cmsItems: List<CmsContentItem>,
    aiViewModel: AiTutorViewModel,
    speechEngine: SpeechEngine,
    audioRecorder: AudioClipRecorder,
    onSubmitExam: (TargetLanguage, CefrLevel, String, Int, Int, Int, Int, String, CefrLevel?, Int) -> Unit
) {
    val context = LocalContext.current
    val activeLang = profile.currentLanguageEnum()
    val activeLevel = profile.currentLevelEnum()
    val activeDialect = Dialect.fromCode(profile.currentDialectCode(), activeLang)
    val cmsExamQuestions = remember(cmsItems, activeLang, activeLevel) {
        cmsItems.filter {
            it.published &&
                it.contentType == "exam_question" &&
                it.language == activeLang.code &&
                it.cefrLevel == activeLevel.code
        }
    }

    val examTypes = listOf(
        "placement" to "تحديد المستوى التلقائي",
        "unit" to "اختبار وحدة",
        "sublevel" to "اختبار ليفل (${profile.currentSubLevel()}/10)",
        "final" to "اختبار CEFR نهائي"
    )
    var selectedType by remember { mutableStateOf("placement") }

    val examGenState by aiViewModel.examGeneratorState.collectAsStateWithLifecycle()
    val examGradingState by aiViewModel.examGradingState.collectAsStateWithLifecycle()
    val isListening by speechEngine.isListening.collectAsStateWithLifecycle()
    val speechError by speechEngine.speechError.collectAsStateWithLifecycle()
    val isRecordingClip by audioRecorder.isRecording.collectAsStateWithLifecycle()
    val isPlayingClip by audioRecorder.isPlaying.collectAsStateWithLifecycle()
    val hasRecordedClip by audioRecorder.hasRecording.collectAsStateWithLifecycle()
    val recordingSeconds by audioRecorder.recordingSeconds.collectAsStateWithLifecycle()

    val generatedExam = (examGenState as? AiOpState.Success)?.data
    val fallbackQuestions = remember(activeLang, activeLevel) {
        CurriculumCatalog.examQuestionsFor(activeLang, activeLevel)
    }

    val listeningQuestions = remember(generatedExam, fallbackQuestions) {
        generatedExam?.listeningQuestions?.takeIf { it.isNotEmpty() } ?: fallbackQuestions.filter { it.skill == SkillType.LISTENING }
    }
    val readingQuestions = remember(generatedExam, fallbackQuestions) {
        generatedExam?.readingQuestions?.takeIf { it.isNotEmpty() } ?: fallbackQuestions.filter { it.skill == SkillType.READING }
    }
    val speakingPromptAr = generatedExam?.speakingPromptAr
        ?: "تحدّث بصوتك وأجب بجملتين كاملتين وواضحتين عن موقف الاستماع باللغة المستهدفة:"
    val speakingTargetContext = generatedExam?.speakingTargetContext
        ?: listeningQuestions.first().stimulusTarget
    val writingPromptAr = generatedExam?.writingPromptAr
        ?: "اكتب فقرة قصيرة (2–4 جمل) تعبّر فيها عن رأيك في التعلم عبر المواقف الواقعية بدل حفظ القوائم:"
    val writingPromptTarget = generatedExam?.writingPromptTarget
        ?: "CEFR ${activeLevel.code} exam writing response on real-life contextual language learning"

    val examSessionKey = "${activeLang.code}_${activeLevel.code}_${selectedType}_${generatedExam?.id ?: "base"}"
    val selectedListening = remember(examSessionKey) { mutableStateMapOf<String, Int>() }
    val selectedReading = remember(examSessionKey) { mutableStateMapOf<String, Int>() }
    var speakingAnswer by remember(examSessionKey) { mutableStateOf("") }
    var writingAnswer by remember(examSessionKey) { mutableStateOf("") }
    var pendingMicAction by remember { mutableStateOf<String?>(null) }
    var certificateAttempt by remember { mutableStateOf<ExamAttempt?>(null) }

    LaunchedEffect(examSessionKey) {
        audioRecorder.resetClip()
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            when (pendingMicAction) {
                "clip" -> audioRecorder.startRecording("exam_speak_$examSessionKey")
                else -> speechEngine.startListening(activeDialect.code) { recognized ->
                    speakingAnswer = recognized
                }
            }
        }
        pendingMicAction = null
    }

    fun requestMic(action: String, onGranted: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) onGranted() else {
            pendingMicAction = action
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (certificateAttempt != null) {
        CefrCertificateDialog(
            learnerName = profile.displayName,
            attempt = certificateAttempt!!,
            onDismiss = { certificateAttempt = null }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("cefr_exam_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "الاختبارات الصعبة (${activeLevel.code})",
                subtitle = "التوزيع: 25% لكل مهارة • النجاح: 80% إجمالي و70% لكل مهارة • تحديد مستوى وترقية تلقائية",
                badge = activeLang.arabicName
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(examTypes) { (key, label) ->
                    FilterChip(
                        selected = selectedType == key,
                        onClick = { selectedType = key },
                        label = { Text(label) },
                        modifier = Modifier.testTag("exam_type_$key")
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    aiViewModel.generateUnseenExam(activeLang, activeDialect, activeLevel, selectedType)
                },
                enabled = examGenState !is AiOpState.Loading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("generate_unseen_exam_btn")
            ) {
                if (examGenState is AiOpState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("جاري توليد أسئلة اختبار جديدة غير مكررة...")
                } else {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("توليد اختبار جديد غير مكرر بالذكاء الاصطناعي")
                }
            }
            if (examGenState is AiOpState.Error) {
                Spacer(Modifier.height(8.dp))
                ErrorBannerCard((examGenState as AiOpState.Error).message)
            }
            if (generatedExam != null) {
                Spacer(Modifier.height(6.dp))
                StatusPill(
                    text = "نموذج اختبار جديد مولّد خصيصًا لك بالذكاء الاصطناعي ✓",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // 1. Listening (25%)
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusPill("1) الاستماع (${listeningQuestions.size} أسئلة) — 25%")
                    listeningQuestions.forEachIndexed { qIdx, q ->
                        if (qIdx > 0) HorizontalDivider()
                        Text("سؤال الاستماع ${qIdx + 1}: ${q.promptAr}", fontWeight = FontWeight.Bold)
                        AudioSpeedButtons(
                            onPlayNormal = { speechEngine.speak(q.stimulusTarget, activeDialect.code, slow = false) },
                            onPlaySlow = { speechEngine.speak(q.stimulusTarget, activeDialect.code, slow = true) }
                        )
                        q.options.forEachIndexed { idx, opt ->
                            val picked = selectedListening[q.id] == idx
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (picked) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth().clickable { selectedListening[q.id] = idx }
                            ) {
                                Text(opt, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            }
        }

        // 2. Reading (25%)
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusPill("2) القراءة والاستنتاج (${readingQuestions.size} أسئلة) — 25%")
                    readingQuestions.forEachIndexed { qIdx, q ->
                        if (qIdx > 0) HorizontalDivider()
                        Text("سؤال القراءة ${qIdx + 1}: ${q.promptAr}", fontWeight = FontWeight.Bold)
                        LtrTargetText(q.stimulusTarget, bold = true)
                        q.options.forEachIndexed { idx, opt ->
                            val picked = selectedReading[q.id] == idx
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (picked) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth().clickable { selectedReading[q.id] = idx }
                            ) {
                                Text(opt, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            }
        }

        // 3. Speaking (25%) — Real Voice Capture + Playback + Deep Thinking Grading
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusPill("3) التحدث الصوتي (تقييم ذكي عميق) — 25%")
                    Text(speakingPromptAr, fontWeight = FontWeight.Bold)
                    LtrTargetText(speakingTargetContext)
                    AudioSpeedButtons(
                        onPlayNormal = { speechEngine.speak(speakingTargetContext, activeDialect.code, slow = false) },
                        onPlaySlow = { speechEngine.speak(speakingTargetContext, activeDialect.code, slow = true) }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (isListening) {
                                    speechEngine.stopListening()
                                } else {
                                    if (isRecordingClip) audioRecorder.stopRecording()
                                    requestMic("stt") {
                                        speechEngine.startListening(activeDialect.code) { recognized ->
                                            speakingAnswer = recognized
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).testTag("exam_speaking_stt_btn")
                        ) {
                            Icon(if (isListening) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = "إجابة صوتية")
                            Spacer(Modifier.size(4.dp))
                            Text(if (isListening) "إيقاف" else "تحدّث للإجابة")
                        }

                        OutlinedButton(
                            onClick = {
                                if (isRecordingClip) {
                                    audioRecorder.stopRecording()
                                } else {
                                    if (isListening) speechEngine.stopListening()
                                    requestMic("clip") {
                                        audioRecorder.startRecording("exam_speak_$examSessionKey")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).testTag("exam_speaking_clip_btn")
                        ) {
                            Icon(
                                if (isRecordingClip) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                                contentDescription = "تسجيل صوتي للمراجعة"
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(if (isRecordingClip) "إيقاف (${recordingSeconds}ث)" else "تسجيل للمراجعة")
                        }
                    }

                    if (hasRecordedClip && !isRecordingClip) {
                        OutlinedButton(
                            onClick = {
                                if (isPlayingClip) audioRecorder.stopPlayback() else audioRecorder.playRecording()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("exam_speaking_play_btn")
                        ) {
                            Icon(if (isPlayingClip) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text(if (isPlayingClip) "إيقاف التشغيل" else "الاستماع لإجابتك الصوتية المسجّلة")
                        }
                    }

                    if (!speechError.isNullOrBlank()) {
                        Text(speechError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }

                    OutlinedTextField(
                        value = speakingAnswer,
                        onValueChange = { speakingAnswer = it },
                        label = { Text("نص إجابتك المنطوقة (يُملأ تلقائيًا بالتحدث أو يمكنك تعديله)") },
                        minLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("exam_speaking_input")
                    )
                }
            }
        }

        // 4. Writing (25%) + Deep Thinking 4-Skill Official Submission
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusPill("4) الكتابة المفتوحة (تصحيح ذكي عميق) — 25%")
                    Text(writingPromptAr, fontWeight = FontWeight.Bold)
                    if (generatedExam != null) {
                        LtrTargetText(writingPromptTarget)
                    }
                    OutlinedTextField(
                        value = writingAnswer,
                        onValueChange = { writingAnswer = it },
                        label = { Text("اكتب إجابتك الكاملة باللغة المستهدفة") },
                        minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("exam_writing_input")
                    )

                    if (examGradingState is AiOpState.Error) {
                        ErrorBannerCard((examGradingState as AiOpState.Error).message)
                    }

                    if (examGradingState is AiOpState.Success) {
                        val graded = (examGradingState as AiOpState.Success).data
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "تقييم الممتحن الذكي: تحدث ${graded.speakingScore}% • كتابة ${graded.writingScore}% • المستوى المقترح ${graded.recommendedCefrLevel.code}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(graded.arabicFeedback, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    val allListeningAnswered = listeningQuestions.all { selectedListening.containsKey(it.id) }
                    val allReadingAnswered = readingQuestions.all { selectedReading.containsKey(it.id) }

                    Button(
                        onClick = {
                            val listenCorrect = listeningQuestions.count { selectedListening[it.id] == it.correctIndex }
                            val readCorrect = readingQuestions.count { selectedReading[it.id] == it.correctIndex }
                            val listeningScore = ((listenCorrect * 100) / listeningQuestions.size).coerceIn(25, 100)
                            val readingScore = ((readCorrect * 100) / readingQuestions.size).coerceIn(25, 100)

                            aiViewModel.evaluateExamSpeakingAndWriting(
                                language = activeLang,
                                cefrLevel = activeLevel,
                                examType = selectedType,
                                listeningScore = listeningScore,
                                readingScore = readingScore,
                                speakingPrompt = speakingTargetContext,
                                speakingTranscript = speakingAnswer,
                                writingPrompt = writingPromptTarget,
                                writingText = writingAnswer
                            ) { result ->
                                onSubmitExam(
                                    activeLang,
                                    activeLevel,
                                    selectedType,
                                    listeningScore,
                                    result.speakingScore,
                                    readingScore,
                                    result.writingScore,
                                    result.arabicFeedback,
                                    result.recommendedCefrLevel,
                                    profile.currentSubLevel()
                                )
                            }
                        },
                        enabled = allListeningAnswered && allReadingAnswered &&
                            speakingAnswer.trim().length >= 5 &&
                            writingAnswer.trim().length >= 10 &&
                            examGradingState !is AiOpState.Loading,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_cefr_exam_btn")
                    ) {
                        if (examGradingState is AiOpState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("جاري تصحيح التحدث والكتابة بالتفكير العميق...")
                        } else {
                            Text("تسليم الاختبار وحساب النتيجة الرسمية", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Published Admin CMS Bonus Exam Questions (if any for this track/level)
        if (cmsExamQuestions.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("أسئلة تدريبية إضافية من مكتبة المنصة (CMS)", fontWeight = FontWeight.Bold)
                        cmsExamQuestions.forEach { q ->
                            Text(q.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            LtrTargetText(q.bodyText)
                            Text("الإجابة / التوضيح: ${q.arabicTranslation}", style = MaterialTheme.typography.bodySmall)
                            if (q.grammarNote.isNotBlank()) {
                                Text("ملاحظة الممتحن: ${q.grammarNote}", style = MaterialTheme.typography.labelSmall)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        // Exam History + Official Certificates for Passed Exams
        if (examAttempts.isNotEmpty()) {
            item { SectionHeader("سجل الاختبارات والشهادات", "محفوظة في السحابة مع شهادة معتمدة عند كل نجاح") }
            items(examAttempts.take(10), key = { it.id }) { attempt ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (attempt.passed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${attempt.language} • ${attempt.cefrLevel} (${attempt.examType})", fontWeight = FontWeight.Bold)
                            StatusPill(
                                text = if (attempt.passed) "ناجح ${attempt.totalScore}%" else "لم يجتز ${attempt.totalScore}%"
                            )
                        }
                        Text(
                            text = "استماع ${attempt.listeningScore}% | تحدث ${attempt.speakingScore}% | قراءة ${attempt.readingScore}% | كتابة ${attempt.writingScore}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                        if (attempt.aiFeedback.isNotBlank()) {
                            Text(attempt.aiFeedback, style = MaterialTheme.typography.bodySmall)
                        }
                        if (attempt.passed) {
                            OutlinedButton(
                                onClick = { certificateAttempt = attempt },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("view_certificate_${attempt.id}")
                            ) {
                                Icon(Icons.Filled.WorkspacePremium, contentDescription = "شهادة الإتمام")
                                Spacer(Modifier.size(6.dp))
                                Text("عرض شهادة إتمام CEFR الرسمية")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CefrCertificateDialog(
    learnerName: String,
    attempt: ExamAttempt,
    onDismiss: () -> Unit
) {
    val langLabel = if (attempt.language == "ES") "اللغة الإسبانية (Español)" else "اللغة الإنجليزية (English)"
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("إغلاق الشهادة")
            }
        },
        text = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WorkspacePremium,
                        contentDescription = "ختم الشهادة الرسمية",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "شهادة إنجاز رسمية • LingoWorld AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    HorizontalDivider()
                    Text(
                        text = "تشهد منصة LingoWorld AI بأن المتعلّم:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = learnerName.ifBlank { "متعلم LingoWorld AI" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "قد اجتاز بنجاح اختبار $langLabel بمستوى CEFR ${attempt.cefrLevel}",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    StatusPill(
                        text = "التقدير الإجمالي: ${attempt.totalScore}% — ناجح بامتياز",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    HorizontalDivider()
                    Text(
                        text = "الاستماع: ${attempt.listeningScore}%  •  التحدث: ${attempt.speakingScore}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "القراءة: ${attempt.readingScore}%  •  الكتابة: ${attempt.writingScore}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "رقم الشهادة السحابي: LW-${attempt.cefrLevel}-${attempt.id.take(8).uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

@Composable
fun SmartReviewFsrsScreen(
    reviewCards: List<ReviewCard>,
    speechEngine: SpeechEngine,
    onSubmitRating: (ReviewCard, Int, Long) -> Unit,
    onGoToCurriculum: () -> Unit
) {
    val now = System.currentTimeMillis()
    val dueCards = remember(reviewCards, now) {
        val due = reviewCards.filter { it.nextReviewEpochMs <= now }
        if (due.isNotEmpty()) due else reviewCards
    }
    var cardRevealed by remember(dueCards.firstOrNull()?.id) { mutableStateOf(false) }
    var cardStartTimeMs by remember(dueCards.firstOrNull()?.id) { mutableLongStateOf(System.currentTimeMillis()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "المراجعة الذكية (FSRS)",
                subtitle = "تُجدوَل الجمل تلقائيًا وفق الإتقان والأخطاء والصعوبة وسرعة الإجابة",
                badge = "${reviewCards.size} بطاقة"
            )
        }

        if (dueCards.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("لا توجد جمل محفوظة للمراجعة بعد!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "احفظ الجمل من الدروس أو القصص أو الفيديوهات لتبدأ خوارزمية FSRS بتثبيتها في ذاكرتك طويلة المدى.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onGoToCurriculum, shape = RoundedCornerShape(14.dp)) {
                            Text("استكشاف دروس الجمل الآن")
                        }
                    }
                }
            }
        } else {
            val current = dueCards.first()
            item(key = current.id) {
                ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatusPill("${current.language} • ${current.cefrLevel}")
                            Text(
                                text = "الإتقان: ${current.masteryScore}% • الاستقرار: ${current.stabilityDays} يوم",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LtrTargetText(current.sentenceText, bold = true)
                        AudioSpeedButtons(
                            onPlayNormal = { speechEngine.speak(current.sentenceText, current.dialect, slow = false) },
                            onPlaySlow = { speechEngine.speak(current.sentenceText, current.dialect, slow = true) }
                        )

                        if (!cardRevealed) {
                            Button(
                                onClick = { cardRevealed = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("fsrs_reveal_btn")
                            ) {
                                Text("إظهار الترجمة العربية والشرح")
                            }
                        } else {
                            HorizontalDivider()
                            Text(current.arabicTranslation, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            if (current.grammarNote.isNotBlank()) {
                                Text(current.grammarNote, style = MaterialTheme.typography.bodySmall)
                            }
                            Text("كيف كان تذكّرك للجملة؟", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1 to "أعدها", 2 to "صعب", 3 to "جيد", 4 to "سهل").forEach { (rating, label) ->
                                    OutlinedButton(
                                        onClick = {
                                            val elapsed = System.currentTimeMillis() - cardStartTimeMs
                                            onSubmitRating(current, rating, elapsed)
                                            cardRevealed = false
                                            cardStartTimeMs = System.currentTimeMillis()
                                        },
                                        modifier = Modifier.weight(1f).testTag("fsrs_rate_$rating"),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelMedium)
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
fun SettingsScreen(
    profile: UserProfile,
    darkThemeEnabled: Boolean,
    isAdmin: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSelectDialect: (Dialect) -> Unit,
    onUpdateDailyGoal: (Int) -> Unit,
    onOpenAdmin: () -> Unit,
    onSignOut: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader("الإعدادات والتخصيص", "المظهر واللهجات والهدف اليومي وإدارة الحساب")
        }

        item {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("الوضع الليلي (Dark Mode)", fontWeight = FontWeight.Bold)
                        Text("تبديل فوري بين الوضع الفاتح والداكن", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = darkThemeEnabled,
                        onCheckedChange = onToggleDarkTheme,
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اللهجات المفضلة", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Dialect.EN_US, Dialect.EN_GB).forEach { d ->
                            FilterChip(
                                selected = profile.englishDialect == d.code,
                                onClick = { onSelectDialect(d) },
                                label = { Text(d.arabicLabel) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Dialect.ES_ES, Dialect.ES_MX).forEach { d ->
                            FilterChip(
                                selected = profile.spanishDialect == d.code,
                                onClick = { onSelectDialect(d) },
                                label = { Text(d.arabicLabel) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("الهدف اليومي للتعلم (بالدقائق)", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 15, 30, 45).forEach { mins ->
                            FilterChip(
                                selected = profile.dailyGoalMinutes.toInt() == mins,
                                onClick = { onUpdateDailyGoal(mins) },
                                label = { Text("$mins د") }
                            )
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            item {
                Button(
                    onClick = onOpenAdmin,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("open_admin_cms_btn")
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("فتح لوحة الإدارة وإدارة المحتوى (CMS)")
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("sign_out_btn")
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("تسجيل الخروج من حساب Google")
            }
        }
    }
}

@Composable
fun AdminCmsScreen(
    isAdmin: Boolean,
    cmsItems: List<CmsContentItem>,
    adminLearners: List<UserProfile>,
    onCreateCmsItem: (TargetLanguage, String, CefrLevel, Int, String, String, String, String, String, String, Boolean) -> Unit,
    onTogglePublished: (CmsContentItem) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    if (!isAdmin) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            ErrorBannerCard("هذه اللوحة مخصصة لمدير المنصة فقط.")
        }
        return
    }

    var lang by remember { mutableStateOf(TargetLanguage.EN) }
    var level by remember { mutableStateOf(CefrLevel.A1) }
    var contentType by remember { mutableStateOf("sentence") }
    var unitNumber by remember { mutableIntStateOf(1) }
    var title by remember { mutableStateOf("") }
    var bodyText by remember { mutableStateOf("") }
    var arabicTranslation by remember { mutableStateOf("") }
    var grammarNote by remember { mutableStateOf("") }
    var youtubeId by remember { mutableStateOf("") }
    var publishNow by remember { mutableStateOf(true) }

    val publishedCount = cmsItems.count { it.published }
    val draftCount = cmsItems.size - publishedCount
    val totalPlatformXp = adminLearners.sumOf { it.xp }
    val totalLessonsCompleted = adminLearners.sumOf { it.completedLessonsCount }
    val enLearners = adminLearners.count { it.activeLanguage == "EN" }
    val esLearners = adminLearners.count { it.activeLanguage == "ES" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "لوحة الإدارة ونظام المحتوى (CMS)",
                subtitle = "إدارة الجمل والقصص والفيديوهات والاختبارات وتقارير الأداء من هاتفك",
                badge = "${cmsItems.size} عنصر"
            )
        }

        // Live Admin Performance Reports Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth().testTag("admin_analytics_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تقارير أداء المنصة والمتعلمين (مباشر)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي المتعلمين: ${adminLearners.size}", fontWeight = FontWeight.Bold)
                        Text("إجمالي XP: $totalPlatformXp", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = "مسار الإنجليزية: $enLearners متعلم • مسار الإسبانية: $esLearners متعلم",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "الدروس المكتملة بالمنصة: $totalLessonsCompleted • محتوى منشور: $publishedCount | مسودات: $draftCount",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("إضافة محتوى تعليمي جديد", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TargetLanguage.entries.forEach { l ->
                            FilterChip(selected = lang == l, onClick = { lang = l }, label = { Text(l.arabicName) })
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(CefrLevel.entries) { lv ->
                            FilterChip(selected = level == lv, onClick = { level = lv }, label = { Text(lv.code) })
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("sentence" to "جملة", "story" to "قصة", "video" to "فيديو", "exam_question" to "سؤال اختبار")) { (k, lbl) ->
                            FilterChip(selected = contentType == k, onClick = { contentType = k }, label = { Text(lbl) })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان العنصر") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = bodyText,
                        onValueChange = { bodyText = it },
                        label = {
                            Text(
                                if (contentType == "video") "أسطر الفيديو الموقّتة (مثال: 0-8|النص|الترجمة|الشرح)"
                                else "الجملة / النص باللغة المستهدفة"
                            )
                        },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(value = arabicTranslation, onValueChange = { arabicTranslation = it }, label = { Text("الترجمة العربية") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = grammarNote, onValueChange = { grammarNote = it }, label = { Text("شرح القواعد والتعبيرات (اختياري)") }, modifier = Modifier.fillMaxWidth())
                    if (contentType == "video") {
                        OutlinedTextField(value = youtubeId, onValueChange = { youtubeId = it }, label = { Text("معرّف فيديو YouTube (مثال: dQw4w9WgXcQ)") }, modifier = Modifier.fillMaxWidth())
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("نشر فوري للمتعلمين")
                        Switch(checked = publishNow, onCheckedChange = { publishNow = it })
                    }
                    Button(
                        onClick = {
                            val dialectCode = if (lang == TargetLanguage.ES) "es-ES" else "en-US"
                            onCreateCmsItem(lang, dialectCode, level, unitNumber, contentType, title, bodyText, arabicTranslation, grammarNote, youtubeId, publishNow)
                            title = ""
                            bodyText = ""
                            arabicTranslation = ""
                            grammarNote = ""
                            youtubeId = ""
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("cms_save_btn")
                    ) {
                        Text("حفظ ونشر في قاعدة البيانات السحابية")
                    }
                }
            }
        }

        items(cmsItems, key = { it.id }) { item ->
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        StatusPill("${item.language} • ${item.cefrLevel} • ${item.contentType}")
                        Row {
                            IconButton(onClick = { onTogglePublished(item) }) {
                                Icon(Icons.Filled.Publish, contentDescription = "تبديل النشر", tint = if (item.published) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onDeleteItem(item.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Text(item.title, fontWeight = FontWeight.Bold)
                    LtrTargetText(item.bodyText)
                    Text(item.arabicTranslation, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
