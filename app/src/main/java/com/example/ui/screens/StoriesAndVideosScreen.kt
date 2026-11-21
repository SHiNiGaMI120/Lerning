package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.SpeechEngine
import com.example.data.curriculum.StoriesVideosCatalog
import com.example.data.model.CefrLevel
import com.example.data.model.CmsContentItem
import com.example.data.model.Dialect
import com.example.data.model.InteractiveStory
import com.example.data.model.StoryChoice
import com.example.data.model.StoryTurn
import com.example.data.model.TargetLanguage
import com.example.data.model.TimedSubtitleLine
import com.example.data.model.UserProfile
import com.example.data.model.VideoLesson
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.LtrTargetText
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.viewmodel.AiOpState
import com.example.ui.viewmodel.AiTutorViewModel

@Composable
fun InteractiveStoriesScreen(
    profile: UserProfile,
    savedStories: List<InteractiveStory>,
    cmsItems: List<CmsContentItem>,
    aiViewModel: AiTutorViewModel,
    speechEngine: SpeechEngine,
    onStoryCompleted: (InteractiveStory) -> Unit,
    onDeleteSavedStory: (String) -> Unit,
    onSaveSentence: (String, String, String) -> Unit
) {
    val activeLang = profile.currentLanguageEnum()
    val activeLevel = profile.currentLevelEnum()
    val activeDialect = Dialect.fromCode(profile.currentDialectCode(), activeLang)

    val generatedState by aiViewModel.generatedStoryState.collectAsStateWithLifecycle()
    var selectedGenre by remember { mutableStateOf("الكل") }
    var activeStory by remember { mutableStateOf<InteractiveStory?>(null) }

    val savedStoryIds = remember(savedStories) { savedStories.map { it.id }.toSet() }
    val cmsStories = remember(cmsItems, activeLang) {
        cmsItems.filter { it.published && it.contentType == "story" && it.language == activeLang.code }
            .map { cms -> cmsToInteractiveStory(cms) }
    }

    val allStories = remember(activeLang, savedStories, cmsStories) {
        val cloudSaved = savedStories.filter { it.language == activeLang }
        val builtIn = StoriesVideosCatalog.stories.filter { it.language == activeLang }
        (cloudSaved + cmsStories + builtIn).distinctBy { it.id }
    }
    val genres = listOf("الكل", "السفر", "العمل", "الغموض", "الحياة اليومية", "الكوميديا")

    if (activeStory != null) {
        StoryReaderView(
            story = activeStory!!,
            dialect = activeDialect,
            speechEngine = speechEngine,
            onSaveSentence = onSaveSentence,
            onComplete = {
                onStoryCompleted(activeStory!!)
                activeStory = null
            },
            onBack = { activeStory = null }
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_stories_banner_1795256214202),
                    contentDescription = "مكتبة القصص التفاعلية",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC111827)))
                        )
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                ) {
                    Text(
                        text = "مكتبة القصص التفاعلية (${activeLang.arabicName})",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "أصوات مستقلة لكل شخصية • Shadowing • قراراتك تغيّر أحداث القصة",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Genre Filter + AI Story Generator
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(genres) { genre ->
                    FilterChip(
                        selected = selectedGenre == genre,
                        onClick = { selectedGenre = genre },
                        label = { Text(genre) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    val targetGenre = if (selectedGenre == "الكل") "مغامرة يومية واقعية" else selectedGenre
                    aiViewModel.generateStory(activeLang, activeDialect, activeLevel, targetGenre)
                },
                enabled = generatedState !is AiOpState.Loading,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("generate_ai_story_btn")
            ) {
                if (generatedState is AiOpState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                    Text("جاري تأليف قصة تفاعلية مخصصة لمستواك ${activeLevel.code}...")
                } else {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("إنشاء قصة تفاعلية جديدة بالذكاء الاصطناعي (${activeLevel.code})")
                }
            }
            if (generatedState is AiOpState.Error) {
                Spacer(Modifier.height(8.dp))
                ErrorBannerCard((generatedState as AiOpState.Error).message)
            }
        }

        val filtered = if (selectedGenre == "الكل") allStories
        else allStories.filter { it.genreAr.contains(selectedGenre.removePrefix("ال")) }

        items(filtered, key = { it.id }) { story ->
            val isUserSaved = savedStoryIds.contains(story.id)
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activeStory = story }
                    .testTag("story_card_${story.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill(story.cefrLevel.code)
                            StatusPill(
                                text = story.genreAr,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (isUserSaved) {
                            IconButton(
                                onClick = { onDeleteSavedStory(story.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "حذف القصة المحفوظة",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Text(story.titleAr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LtrTargetText(story.subtitleTarget, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${story.turns.size} حوارات صوتية • ${story.choices.size} مسارات تفاعلية" +
                            if (isUserSaved) " • محفوظة سحابيًا ✓" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun cmsToInteractiveStory(cms: CmsContentItem): InteractiveStory {
    val lines = cms.bodyText.lines().map { it.trim() }.filter { it.isNotBlank() }
    val arLines = cms.arabicTranslation.lines().map { it.trim() }.filter { it.isNotBlank() }
    val turns = lines.mapIndexed { idx, line ->
        StoryTurn(
            speakerName = if (idx % 2 == 0) "المتحدث أ" else "المتحدث ب",
            voicePitch = if (idx % 2 == 0) 1.1f else 0.9f,
            text = line,
            arabicTranslation = arLines.getOrElse(idx) { cms.arabicTranslation }
        )
    }.ifEmpty {
        listOf(StoryTurn("الراوي", 1.0f, cms.bodyText, cms.arabicTranslation))
    }
    val replyTarget = if (cms.language == "ES") {
        "Entiendo perfectamente, sigamos hablando de este tema."
    } else {
        "I completely understand, let's keep discussing this."
    }
    return InteractiveStory(
        id = "cms_story_${cms.id}",
        language = TargetLanguage.fromCode(cms.language),
        cefrLevel = CefrLevel.fromCode(cms.cefrLevel),
        genreAr = "مكتبة المنصة السحابية (CMS)",
        titleAr = cms.title,
        subtitleTarget = lines.firstOrNull()?.take(60) ?: cms.title,
        turns = turns,
        choices = listOf(
            StoryChoice(
                replyText = replyTarget,
                arabicHint = "إظهار الفهم ومواصلة الحوار بأسلوب طبيعي",
                outcomeSpeaker = "الراوي",
                outcomeReply = lines.lastOrNull() ?: cms.bodyText,
                outcomeArabic = arLines.lastOrNull() ?: cms.arabicTranslation,
                feedbackAr = cms.grammarNote.ifBlank { "أحسنت! ردّ طبيعي ومناسب للسياق." }
            )
        ),
        comprehensionQuestionAr = "ما الهدف الرئيسي من هذا الحوار؟",
        comprehensionOptionsAr = listOf(
            "ممارسة التعبير الطبيعي داخل الموقف: ${cms.title}",
            "حفظ كلمات منفصلة بلا سياق"
        ),
        correctOptionIndex = 0
    )
}

@Composable
private fun StoryReaderView(
    story: InteractiveStory,
    dialect: Dialect,
    speechEngine: SpeechEngine,
    onSaveSentence: (String, String, String) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    var selectedChoice by remember(story.id) { mutableStateOf<StoryChoice?>(null) }
    var selectedQuizOption by remember(story.id) { mutableStateOf<Int?>(null) }
    var showTranslations by remember { mutableStateOf(true) }
    var shadowingSlow by remember { mutableStateOf(false) }

    DisposableEffect(story.id) {
        onDispose { speechEngine.stopSpeaking() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onBack) { Text("المكتبة") }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = shadowingSlow,
                        onClick = { shadowingSlow = !shadowingSlow },
                        label = { Text("Shadowing بطيء") }
                    )
                    FilterChip(
                        selected = showTranslations,
                        onClick = { showTranslations = !showTranslations },
                        label = { Text(if (showTranslations) "الترجمة ظاهرة" else "إخفاء الترجمة") }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            SectionHeader(story.titleAr, story.subtitleTarget, story.cefrLevel.code)
        }

        items(story.turns) { turn ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(turn.speakerName)
                        Row {
                            IconButton(
                                onClick = {
                                    speechEngine.speak(
                                        text = turn.text,
                                        dialectCode = dialect.code,
                                        slow = shadowingSlow,
                                        pitch = turn.voicePitch
                                    )
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "تشغيل صوت الشخصية", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = {
                                    onSaveSentence(turn.text, turn.arabicTranslation, "من قصة: ${story.titleAr}")
                                }
                            ) {
                                Icon(Icons.Filled.Repeat, contentDescription = "حفظ الجملة للمراجعة")
                            }
                        }
                    }
                    LtrTargetText(turn.text, bold = true)
                    if (showTranslations) {
                        Text(turn.arabicTranslation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Branching Interactive Choices
        item {
            SectionHeader("اختر ردك لتغيير أحداث القصة", "ردّك يحدد استجابة الشخصية وتطور الموقف")
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                story.choices.forEach { choice ->
                    val chosen = selectedChoice == choice
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (chosen) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedChoice = choice
                                speechEngine.speak(choice.outcomeReply, dialect.code, slow = shadowingSlow, pitch = 1.1f)
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LtrTargetText(choice.replyText, bold = true)
                            Text(choice.arabicHint, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        if (selectedChoice != null) {
            val branch = selectedChoice!!
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("تطوّر القصة بعد قرارك — ${branch.outcomeSpeaker}:", fontWeight = FontWeight.Bold)
                        LtrTargetText(branch.outcomeReply, bold = true)
                        Text(branch.outcomeArabic, style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider()
                        Text(branch.feedbackAr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Story Comprehension Question
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("سؤال فهم القصة:", fontWeight = FontWeight.Bold)
                    Text(story.comprehensionQuestionAr)
                    story.comprehensionOptionsAr.forEachIndexed { idx, opt ->
                        val picked = selectedQuizOption == idx
                        val correct = idx == story.correctOptionIndex
                        val color = when {
                            selectedQuizOption == null -> MaterialTheme.colorScheme.surface
                            picked && correct -> MaterialTheme.colorScheme.primaryContainer
                            picked -> MaterialTheme.colorScheme.errorContainer
                            correct -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = color),
                            modifier = Modifier.fillMaxWidth().clickable { selectedQuizOption = idx }
                        ) {
                            Text(opt, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onComplete,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("إتمام القصة وإضافة +40 XP", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RealVideosScreen(
    profile: UserProfile,
    cmsItems: List<CmsContentItem>,
    speechEngine: SpeechEngine,
    onSaveSentence: (String, String, String) -> Unit,
    onCompleteVideo: (VideoLesson) -> Unit
) {
    val context = LocalContext.current
    val activeLang = profile.currentLanguageEnum()
    val videos = remember(activeLang, cmsItems) {
        val cmsVideos = cmsItems
            .filter { it.published && it.contentType == "video" && it.language == activeLang.code && it.youtubeVideoId.isNotBlank() }
            .map { cms -> cmsToVideoLesson(cms) }
        cmsVideos + StoriesVideosCatalog.videoLessons.filter { it.language == activeLang }
    }
    var selectedVideo by remember(activeLang, videos.size) { mutableStateOf(videos.first()) }
    var hideTranscript by remember { mutableStateOf(false) }
    var loopSingleLine by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var activeLineIndex by remember(selectedVideo.id) { mutableStateOf(0) }
    var quizChoice by remember(selectedVideo.id) { mutableStateOf<Int?>(null) }
    var showEmbeddedPlayer by remember(selectedVideo.id) { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun stopWebViewVideo() {
        webViewRef?.let { wv ->
            runCatching {
                wv.onPause()
                wv.loadUrl("about:blank")
                wv.destroy()
            }
        }
        webViewRef = null
        showEmbeddedPlayer = false
    }

    fun speakSingleLineCleanly(index: Int) {
        val line = selectedVideo.lines.getOrNull(index) ?: return
        loopSingleLine = false
        activeLineIndex = index
        // Strict single-audio-source rule: NEVER allow YouTube audio and TTS to play together
        if (showEmbeddedPlayer || webViewRef != null) {
            stopWebViewVideo()
        }
        speechEngine.stopSpeaking()
        speechEngine.speak(
            text = line.text,
            dialectCode = selectedVideo.dialect.code,
            slow = playbackSpeed < 1f
        )
    }

    // Stop all audio cleanly whenever the user switches video or leaves this screen
    DisposableEffect(selectedVideo.id) {
        onDispose {
            speechEngine.stopSpeaking()
            stopWebViewVideo()
        }
    }

    // Optional clean TTS loop for the active sentence (never overlaps with YouTube)
    LaunchedEffect(loopSingleLine, activeLineIndex, selectedVideo.id, playbackSpeed, showEmbeddedPlayer) {
        if (!loopSingleLine || showEmbeddedPlayer) return@LaunchedEffect
        val line = selectedVideo.lines.getOrNull(activeLineIndex) ?: return@LaunchedEffect
        while (loopSingleLine && !showEmbeddedPlayer) {
            speechEngine.speak(line.text, selectedVideo.dialect.code, slow = playbackSpeed < 1f)
            val waitMs = ((line.text.length * 75L) + 2200L).coerceIn(3200L, 8000L)
            delay(waitMs)
        }
    }

    fun openInNativeYouTube(videoId: String) {
        speechEngine.stopSpeaking()
        loopSingleLine = false
        stopWebViewVideo()
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
        runCatching { context.startActivity(appIntent) }
            .onFailure { runCatching { context.startActivity(webIntent) } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "التعلم من فيديوهات حقيقية",
                subtitle = "شاهد الفيديو أو استمع للجمل المترجمة واحدة تلو الأخرى بدون تداخل صوتي",
                badge = selectedVideo.cefrLevel.code
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(videos, key = { it.id }) { vid ->
                    FilterChip(
                        selected = selectedVideo.id == vid.id,
                        onClick = {
                            speechEngine.stopSpeaking()
                            loopSingleLine = false
                            stopWebViewVideo()
                            selectedVideo = vid
                        },
                        label = { Text("${vid.dialect.arabicLabel} (${vid.cefrLevel.code})") }
                    )
                }
            }
        }

        // Video Stage: Clean Poster Card OR Direct Mobile YouTube WebView (never black iframe)
        item(key = "video_stage_${selectedVideo.youtubeVideoId}_$showEmbeddedPlayer") {
            ElevatedCard(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (showEmbeddedPlayer) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setBackgroundColor(android.graphics.Color.BLACK)
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                                    CookieManager.getInstance().setAcceptCookie(true)
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    webChromeClient = WebChromeClient()
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val host = request?.url?.host.orEmpty()
                                            return !(host.contains("youtube.com") || host.contains("youtu.be") || host.contains("google.com"))
                                        }
                                    }
                                    loadUrl("https://m.youtube.com/watch?v=${selectedVideo.youtubeVideoId}")
                                    webViewRef = this
                                }
                            },
                            update = { view -> webViewRef = view },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { stopWebViewVideo() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("video_close_embed_btn")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("إغلاق المشغّل")
                            }
                            Button(
                                onClick = { openInNativeYouTube(selectedVideo.youtubeVideoId) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("video_open_youtube_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("فتح في يوتيوب")
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clickable { openInNativeYouTube(selectedVideo.youtubeVideoId) }
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_stories_banner_1795256214202),
                                contentDescription = selectedVideo.titleAr,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0x55000000), Color(0xCC0F172A))
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = "تشغيل الفيديو",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = selectedVideo.titleAr,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${selectedVideo.dialect.arabicLabel} • ${selectedVideo.cefrLevel.code} • ${selectedVideo.lines.size} جمل تفاعلية مشروحة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { openInNativeYouTube(selectedVideo.youtubeVideoId) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("video_open_youtube_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("مشاهدة الفيديو في تطبيق YouTube (بأعلى جودة)", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    speechEngine.stopSpeaking()
                                    loopSingleLine = false
                                    showEmbeddedPlayer = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("video_embed_toggle_btn")
                            ) {
                                Icon(Icons.Filled.OndemandVideo, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("أو تشغيل صفحة يوتيوب داخل التطبيق")
                            }
                        }
                    }
                }
            }
        }

        // Clean Single-Voice Audio Controls (Speed, Loop, Stop Audio, Hide Transcript)
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(listOf(0.75f, 1.0f)) { rate ->
                    FilterChip(
                        selected = playbackSpeed == rate,
                        onClick = { playbackSpeed = rate },
                        label = { Text(if (rate < 1f) "نطق بطيء" else "سرعة طبيعية") }
                    )
                }
                item {
                    FilterChip(
                        selected = loopSingleLine,
                        onClick = {
                            loopSingleLine = !loopSingleLine
                            if (loopSingleLine) {
                                stopWebViewVideo()
                            } else {
                                speechEngine.stopSpeaking()
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Loop, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(if (loopSingleLine) "إيقاف التكرار" else "تكرار الجملة") },
                        modifier = Modifier.testTag("video_loop_toggle_chip")
                    )
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = {
                            loopSingleLine = false
                            speechEngine.stopSpeaking()
                            stopWebViewVideo()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("إيقاف الصوت") },
                        modifier = Modifier.testTag("video_stop_audio_chip")
                    )
                }
                item {
                    FilterChip(
                        selected = hideTranscript,
                        onClick = { hideTranscript = !hideTranscript },
                        leadingIcon = {
                            Icon(
                                if (hideTranscript) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(if (hideTranscript) "النص مخفي" else "إخفاء النص") }
                    )
                }
            }
        }

        // Timestamped Subtitle Lines (Clean Single-Voice Practice)
        items(selectedVideo.lines.indices.toList()) { idx ->
            val line = selectedVideo.lines[idx]
            val isActive = idx == activeLineIndex
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { speakSingleLineCleanly(idx) }
                    .testTag("video_line_$idx")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill("الجملة ${idx + 1} • ${line.startSeconds}ث - ${line.endSeconds}ث")
                        Row {
                            IconButton(
                                onClick = { speakSingleLineCleanly(idx) }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "سماع نطق الجملة",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = {
                                    activeLineIndex = idx
                                    stopWebViewVideo()
                                    loopSingleLine = true
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Loop,
                                    contentDescription = "تكرار هذه الجملة باستمرار",
                                    tint = if (isActive && loopSingleLine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    onSaveSentence(line.text, line.arabicTranslation, line.expressionNoteAr)
                                }
                            ) {
                                Icon(Icons.Filled.Repeat, contentDescription = "حفظ الجملة للمراجعة")
                            }
                        }
                    }
                    if (!hideTranscript) {
                        LtrTargetText(line.text, bold = true)
                        Text(line.arabicTranslation, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "شرح التعبير: ${line.expressionNoteAr}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "وضع الاستماع المركز: اضغط لسماع الجملة واختبر فهمك دون قراءة النص.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Video Comprehension Quiz
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اختبار فهم الفيديو:", fontWeight = FontWeight.Bold)
                    Text(selectedVideo.quizQuestionAr)
                    selectedVideo.quizOptionsAr.forEachIndexed { idx, opt ->
                        val picked = quizChoice == idx
                        val correct = idx == selectedVideo.correctOptionIndex
                        val bg = when {
                            quizChoice == null -> MaterialTheme.colorScheme.surface
                            picked && correct -> MaterialTheme.colorScheme.primaryContainer
                            picked -> MaterialTheme.colorScheme.errorContainer
                            correct -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = bg),
                            modifier = Modifier.fillMaxWidth().clickable { quizChoice = idx }
                        ) {
                            Text(opt, modifier = Modifier.padding(12.dp))
                        }
                    }
                    Button(
                        onClick = {
                            speechEngine.stopSpeaking()
                            loopSingleLine = false
                            stopWebViewVideo()
                            onCompleteVideo(selectedVideo)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إتمام درس الفيديو (+40 XP)")
                    }
                }
            }
        }
    }
}

private fun cmsToVideoLesson(cms: CmsContentItem): VideoLesson {
    val lang = TargetLanguage.fromCode(cms.language)
    val dialect = Dialect.fromCode(cms.dialect, lang)
    val rawLines = cms.bodyText.lines().map { it.trim() }.filter { it.isNotBlank() }
    val arLines = cms.arabicTranslation.lines().map { it.trim() }.filter { it.isNotBlank() }
    val parsedLines = rawLines.mapIndexed { idx, line ->
        val parts = line.split("|").map { it.trim() }
        if (parts.size >= 2 && parts[0].contains("-")) {
            val times = parts[0].split("-")
            val start = times.getOrNull(0)?.toIntOrNull() ?: (idx * 8)
            val end = times.getOrNull(1)?.toIntOrNull() ?: (start + 8)
            TimedSubtitleLine(
                startSeconds = start,
                endSeconds = end.coerceAtLeast(start + 2),
                text = parts[1],
                arabicTranslation = parts.getOrElse(2) { arLines.getOrElse(idx) { cms.arabicTranslation } },
                expressionNoteAr = parts.getOrElse(3) { cms.grammarNote.ifBlank { "فيديو مضاف من مكتبة المنصة" } }
            )
        } else {
            val start = idx * 8
            TimedSubtitleLine(
                startSeconds = start,
                endSeconds = start + 8,
                text = line,
                arabicTranslation = arLines.getOrElse(idx) { cms.arabicTranslation },
                expressionNoteAr = cms.grammarNote.ifBlank { "فيديو مضاف من مكتبة المنصة" }
            )
        }
    }.ifEmpty {
        listOf(TimedSubtitleLine(0, 10, cms.bodyText, cms.arabicTranslation, cms.grammarNote))
    }
    return VideoLesson(
        id = "cms_video_${cms.id}",
        youtubeVideoId = cms.youtubeVideoId.trim(),
        titleAr = "${cms.title} • (CMS)",
        language = lang,
        dialect = dialect,
        cefrLevel = CefrLevel.fromCode(cms.cefrLevel),
        lines = parsedLines,
        quizQuestionAr = "ما الفكرة الأساسية التي تناولها هذا الفيديو؟",
        quizOptionsAr = listOf(cms.title, "موضوع غير مرتبط بالفيديو"),
        correctOptionIndex = 0
    )
}
