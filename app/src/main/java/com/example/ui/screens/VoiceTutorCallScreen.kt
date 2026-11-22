package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.*
import com.example.data.model.*
import com.example.ui.viewmodel.*
import java.util.UUID
import com.example.ui.components.LtrTargetText
import com.example.ui.components.StudioVoiceBanner
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun VoiceTutorCallScreen(
    profile: UserProfile,
    scenario: TutorScenario,
    aiViewModel: AiTutorViewModel,
    speech: SpeechEngine,
    onClose: () -> Unit,
    onSaveSentence: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember(scope) { VoiceTutorController(scope) }
    val live by controller.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    var strict by remember { mutableStateOf(false) }
    var fallback by remember { mutableStateOf(profile.currentLanguageEnum() != TargetLanguage.EN) }
    var permissionError by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(true) }
    var fallbackGeneration by remember { mutableStateOf(0) }
    var fallbackEnded by remember { mutableStateOf(false) }
    var fallbackTurns by remember { mutableStateOf(emptyList<VoiceTurn>()) }
    var submitted by remember { mutableStateOf(false) }
    val callId = remember { UUID.randomUUID().toString() }
    val stream by aiViewModel.tutorStreamState.collectAsStateWithLifecycle()
    val listening by speech.isListening.collectAsStateWithLifecycle()
    val speechError by speech.speechError.collectAsStateWithLifecycle()
    val summary by aiViewModel.sessionScorecardState.collectAsStateWithLifecycle()
    val saved by aiViewModel.voiceSaveState.collectAsStateWithLifecycle()
    val dialect = Dialect.fromCode(profile.currentDialectCode(), profile.currentLanguageEnum())
    val instruction = tutorVoiceInstruction(profile.currentLevelEnum().code, dialect.nativeLabel, scenario.titleAr, strict)

    fun stopAll() {
        visible = false
        ++fallbackGeneration
        controller.stop()
        speech.stopListening(); speech.stopSpeaking()
        aiViewModel.cancelTutorRequest()
    }

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) stopAll()
            if (event == Lifecycle.Event.ON_RESUME) visible = true
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); stopAll() }
    }
    LaunchedEffect(Unit) { aiViewModel.resetVoiceResult(); speech.stopSpeaking(); speech.stopListening() }
    LaunchedEffect(live.phase) {
        if (live.phase == VoicePhase.FINISHED && !submitted) {
            submitted = true
            aiViewModel.finishVoiceCall(callId, live.turns, profile, scenario)
        }
    }

    fun startWithPermission() {
        visible = true
        permissionError = null
        speech.stopSpeaking()
        if (fallback) {
            if (listening) { speech.stopListening(); return }
            val token = ++fallbackGeneration
            speech.startListening(dialect.code) { recognized ->
                if (visible && token == fallbackGeneration && !fallbackEnded && recognized.isNotBlank()) {
                    val history = fallbackTurns.map { TutorMessage(sender = it.sender, text = it.text) }
                    fallbackTurns = (fallbackTurns + VoiceTurn("user", recognized)).takeLast(30)
                    val persona = if (strict) " Be a playful strict coach, demand another attempt without insults or humiliation." else " Be patient and encouraging."
                    val adjusted = scenario.copy(roleInstructionEn = scenario.roleInstructionEn + persona, roleInstructionEs = scenario.roleInstructionEs + persona)
                    aiViewModel.sendTutorMessage(adjusted, profile.currentLanguageEnum(), dialect, profile.currentLevelEnum(), history, recognized, false) { reply ->
                        if (visible && token == fallbackGeneration && !fallbackEnded) {
                            fallbackTurns = (fallbackTurns + VoiceTurn("tutor", reply)).takeLast(30)
                            speech.speak(reply, dialect.code)
                        }
                    }
                }
            }
        } else {
            speech.stopListening()
            controller.start(instruction + if (live.turns.isEmpty()) "" else "\nPrevious practice context:\n" + live.turns.takeLast(4).joinToString("\n") { "${it.sender}: ${it.text}" }.takeLast(2000))
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && visible && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) startWithPermission()
        else permissionError = "المحادثة محتاجة إذن الميكروفون. لو الإذن مرفوض نهائيًا، فعّله من إعدادات التطبيق. الكتابة لسه متاحة بعد الرجوع."
    }
    val busy = stream is AiOpState.Loading || stream is AiOpState.Streaming
    val displayState = if (fallback) VoiceCallState(
        phase = if (fallbackEnded) VoicePhase.FINISHED else VoicePhase.IDLE,
        turns = fallbackTurns,
        error = (stream as? AiOpState.Error)?.message ?: speechError
    ) else live

    VoiceTutorPanel(
        state = displayState, strict = strict, fallback = fallback,
        listening = listening, busy = busy,
        notice = permissionError ?: if (profile.currentLanguageEnum() != TargetLanguage.EN) "الصوت المباشر بالإنجليزي أولًا. الإسباني متاح بنظام تكلّم وانتظر الرد." else null,
        summary = summary, saveState = if (fallback) AiOpState.Idle else saved,
        liveSupported = profile.currentLanguageEnum() == TargetLanguage.EN,
        onStrict = { strict = it },
        onFallback = { if (!it && profile.currentLanguageEnum() != TargetLanguage.EN) return@VoiceTutorPanel; ++fallbackGeneration; controller.stop(); speech.stopListening(); speech.stopSpeaking(); aiViewModel.cancelTutorRequest(); fallback = it },
        onStart = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startWithPermission()
            else permission.launch(Manifest.permission.RECORD_AUDIO)
        },
        onPause = { ++fallbackGeneration; controller.stop(); speech.stopListening(); speech.stopSpeaking(); aiViewModel.cancelTutorRequest() },
        onEnd = {
            ++fallbackGeneration
            speech.stopListening(); speech.stopSpeaking(); aiViewModel.cancelTutorRequest()
            if (fallback) {
                fallbackEnded = true
                aiViewModel.generateSessionScorecard(profile.currentLanguageEnum(), dialect, profile.currentLevelEnum(), scenario, fallbackTurns.map { TutorMessage(sender = it.sender, text = it.text) })
            } else controller.stop(VoicePhase.FINISHED)
        },
        onClose = { stopAll(); onClose() },
        onRetrySave = { aiViewModel.finishVoiceCall(callId, live.turns, profile, scenario) },
        onSaveSentence = onSaveSentence
    )
}

@Composable
fun VoiceTutorPanel(
    state: VoiceCallState,
    strict: Boolean,
    fallback: Boolean,
    listening: Boolean = false,
    busy: Boolean = false,
    notice: String? = null,
    summary: AiOpState<TutorSessionScorecard> = AiOpState.Idle,
    saveState: AiOpState<Unit> = AiOpState.Idle,
    liveSupported: Boolean = true,
    onStrict: (Boolean) -> Unit,
    onFallback: (Boolean) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onEnd: () -> Unit,
    onClose: () -> Unit,
    onRetrySave: () -> Unit,
    onSaveSentence: (String, String, String) -> Unit
) {
    val active = state.phase == VoicePhase.LIVE || state.phase == VoicePhase.CONNECTING
    val finished = state.phase == VoicePhase.FINISHED
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().testTag("voice_call_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("المدرس الصوتي", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onClose, modifier = Modifier.testTag("voice_close")) { Text("رجوع") }
                }
                Text("المايك بيشتغل بس لما تبدأ. الصوت بيتبعت لـ Google للرد، والنص يتحفظ في سجل التطبيق بعد إنهاء المكالمة. الخروج أو قفل الشاشة بيوقف المايك.", style = MaterialTheme.typography.bodySmall)
            }
            item { StudioVoiceBanner() }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !strict, onClick = { onStrict(false) }, enabled = !active && !busy && !listening && !finished, label = { Text("مدرس صبور") }, modifier = Modifier.testTag("voice_patient"))
                    FilterChip(selected = strict, onClick = { onStrict(true) }, enabled = !active && !busy && !listening && !finished, label = { Text("كابتن صارم 😄") }, modifier = Modifier.testTag("voice_strict"))
                }
                Text("صارم في التمرين، مش إهانة أو تجريح.", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !fallback, onClick = { onFallback(false) }, enabled = !active && !busy && !listening && !finished && liveSupported, label = { Text("صوت مباشر") }, modifier = Modifier.testTag("voice_live_mode"))
                    FilterChip(selected = fallback, onClick = { onFallback(true) }, enabled = !active && !busy && !listening && !finished, label = { Text("تكلّم وانتظر الرد") }, modifier = Modifier.testTag("voice_fallback_mode"))
                }
            }
            notice?.let { item { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(when {
                            finished -> "انتهى التدريب، برافو عليك!"
                            busy -> "المدرس بيجهز الرد..."
                            listening -> "بسجّل كلامك..."
                            state.phase == VoicePhase.CONNECTING -> "بنتصل بالمدرس..."
                            state.phase == VoicePhase.LIVE -> "المحادثة شغالة، قول Hi عشان تبدأ"
                            state.phase == VoicePhase.PAUSED -> "متوقف، المايك مقفول"
                            state.phase == VoicePhase.ERROR -> "الصوت المباشر غير متاح دلوقتي"
                            else -> "جاهز تتمرن على الكلام؟"
                        }, style = MaterialTheme.typography.titleMedium)
                        if (!fallback) Text("${state.seconds / 60}:${(state.seconds % 60).toString().padStart(2, '0')} • مكالمة لحد ٥ دقائق", style = MaterialTheme.typography.labelMedium)
                        if (state.phase == VoicePhase.CONNECTING || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (!active && !finished) Button(onClick = onStart, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("voice_start")) {
                            Text(if (fallback) if (listening) "وقف التسجيل" else "اضغط واتكلم" else if (state.phase == VoicePhase.PAUSED) "ابدأ اتصال جديد وكمل" else "ابدأ المكالمة")
                        }
                        if (active || listening || busy) OutlinedButton(onClick = onPause, modifier = Modifier.fillMaxWidth().testTag("voice_pause")) { Text("إيقاف مؤقت للمايك والصوت") }
                        if (!finished && (active || state.turns.isNotEmpty())) Button(onClick = onEnd, modifier = Modifier.fillMaxWidth().testTag("voice_end")) { Text("إنهاء وملخص التدريب") }
                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Text("الصوت المباشر تجريبي وبيحتاج إنترنت. الحدود والتكلفة حسب إعدادات مشروعك. مفيش تسجيل صوتي محفوظ هنا. السماعات بتقلل الصدى. لو مش متاح اختار تكلّم وانتظر الرد.", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (finished) {
                item {
                    when (saveState) {
                        is AiOpState.Loading -> Text("بنحفظ نص المكالمة...")
                        is AiOpState.Success -> Text("تم حفظ نص المكالمة في السجل")
                        is AiOpState.Error -> Column {
                            Text(saveState.message, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onRetrySave) { Text("إعادة الحفظ") }
                        }
                        else -> Unit
                    }
                    when (summary) {
                        is AiOpState.Loading -> Text("بنجهز ملخصك من نص المحادثة...")
                        is AiOpState.Error -> Text(summary.message, color = MaterialTheme.colorScheme.error)
                        is AiOpState.Success -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("ملخص تدريبك", style = MaterialTheme.typography.titleLarge)
                                Text("تقييم للنص، مش تحليل صوتي للنطق", style = MaterialTheme.typography.labelSmall)
                                summary.data.recurringMistakesAr.forEach { Text("• $it") }
                                Text(summary.data.strengthsAr)
                                Text(summary.data.nextFocusTipAr)
                                LtrTargetText(summary.data.correctivePracticeSentence)
                                Button(onClick = { onSaveSentence(summary.data.correctivePracticeSentence, summary.data.nextFocusTipAr, summary.data.recurringMistakesAr.joinToString(" • ")) }) { Text("احفظ جملة التدريب للمراجعة") }
                            }
                        }
                        else -> if (state.turns.none { it.sender == "user" }) Text("مفيش كلام كفاية لملخص. ارجع وجرب محادثة قصيرة.")
                    }
                }
            }
            if (state.turns.isNotEmpty()) item { Text("نص المحادثة (ممكن التفريغ يغلط)", style = MaterialTheme.typography.titleMedium) }
            items(state.turns) { turn ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(if (turn.sender == "user") "أنت" else "المدرس", fontWeight = FontWeight.Bold)
                        LtrTargetText(turn.text)
                    }
                }
            }
        }
    }
}
