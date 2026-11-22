package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.core.*
import com.example.data.curriculum.CurriculumCatalog
import com.example.data.model.*
import com.example.ui.navigation.*
import com.example.ui.screens.*
import com.example.ui.theme.LingoWorldTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w390dp-h844dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VisualRedesignTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun capture(name: String) {
        compose.waitForIdle()
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(System.getProperty("java.io.tmpdir"), "studio-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    @Composable private fun Home(dark: Boolean) {
        LingoWorldTheme(darkTheme = dark) {
            Scaffold(topBar = { LingoTopBar(LingoDestination.HOME, {}, {}) },
                bottomBar = { LingoBottomNavigationBar(LingoDestination.HOME, {}) }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    HomeDashboardScreen(UserProfile(displayName = "محمد"), emptyList(), emptyList(), emptyList(), emptyList(), true,
                        onSwitchLanguage = {}, onContinueLastLesson = {}, onNavigate = {})
                }
            }
        }
    }
    @Test fun homeLightWithRealAppChrome() { compose.setContent { Home(false) }; capture("home-light") }
    @Test fun homeDarkWithRealAppChrome() { compose.setContent { Home(true) }; capture("home-dark") }
    @Test fun voiceStudioReady() {
        compose.setContent { LingoWorldTheme(darkTheme = false) {
            VoiceTutorPanel(VoiceCallState(), false, false, onStrict = {}, onFallback = {}, onStart = {}, onPause = {}, onEnd = {}, onClose = {}, onRetrySave = {}, onSaveSentence = { _, _, _ -> })
        } }
        capture("voice")
        compose.onNodeWithTag("voice_start").performScrollTo().assertIsDisplayed()
    }
    @Test fun revealThenBuildSentence() {
        val items = CurriculumCatalog.sentencesFor(TargetLanguage.EN, CefrLevel.A1, 1, 1)
        val speech = SpeechEngine(compose.activity)
        val recorder = AudioClipRecorder(compose.activity)
        compose.setContent { LingoWorldTheme(darkTheme = false) {
            Surface { LessonGameView("التعارف والتحية", items, Dialect.fromCode("en-US", TargetLanguage.EN), speech, recorder, {}, {}, {}, {}) }
        } }
        compose.onNodeWithText(items.first().arabicTranslation).assertDoesNotExist()
        capture("discover")
        compose.onNodeWithTag("sentence_reveal_translation").performScrollTo().performClick()
        compose.onNodeWithText(items.first().arabicTranslation).assertExists()
        capture("meaning")
        compose.onNodeWithTag("lesson_game_list").performScrollToNode(hasTestTag("round_discover_next_btn"))
        compose.onNodeWithTag("round_discover_next_btn").performClick()
        capture("word-game")
        compose.onNodeWithTag("word_builder_hint_audio_btn").assertExists()
        speech.release(); recorder.resetClip()
    }
}
