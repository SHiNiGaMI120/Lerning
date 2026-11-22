package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.core.*
import com.example.data.model.TutorSessionScorecard
import com.example.ui.screens.VoiceTutorPanel
import com.example.ui.theme.LingoWorldTheme
import com.example.ui.viewmodel.AiOpState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VoiceTutorUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun capture(name: String) {
        compose.waitForIdle()
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bmp))
            File(System.getProperty("java.io.tmpdir"), "voice-$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun initialScreenRequiresExplicitStart() {
        var starts = 0; var strict = false
        compose.setContent { LingoWorldTheme(darkTheme = false) {
            VoiceTutorPanel(VoiceCallState(), false, false,
                onStrict = { strict = it }, onFallback = {}, onStart = { starts++ }, onPause = {}, onEnd = {}, onClose = {}, onRetrySave = {}, onSaveSentence = { _, _, _ -> })
        } }
        assertEquals(0, starts)
        capture("ready")
        compose.onNodeWithTag("voice_strict").performClick(); assertTrue(strict)
        compose.onNodeWithTag("voice_start").performScrollTo().performClick(); assertEquals(1, starts)
    }

    @Test fun liveScreenLocksModesAndExposesStopControls() {
        var pauses = 0; var ends = 0
        compose.setContent { LingoWorldTheme(darkTheme = true) {
            VoiceTutorPanel(VoiceCallState(phase = VoicePhase.LIVE, seconds = 42, turns = listOf(VoiceTurn("user", "I would like a ticket, please."))), true, false,
                onStrict = {}, onFallback = {}, onStart = {}, onPause = { pauses++ }, onEnd = { ends++ }, onClose = {}, onRetrySave = {}, onSaveSentence = { _, _, _ -> })
        } }
        compose.onNodeWithTag("voice_strict").assertIsNotEnabled()
        compose.onNodeWithTag("voice_fallback_mode").assertIsNotEnabled()
        capture("live-dark")
        compose.onNodeWithTag("voice_pause").performScrollTo().performClick(); assertEquals(1, pauses)
        compose.onNodeWithTag("voice_end").performScrollTo().performClick(); assertEquals(1, ends)
    }

    @Test fun spanishDisablesLiveAndKeepsFallbackAvailable() {
        compose.setContent { LingoWorldTheme(darkTheme = false) {
            VoiceTutorPanel(VoiceCallState(), false, true, liveSupported = false,
                notice = "الإسباني متاح بنظام تكلّم وانتظر الرد.",
                onStrict = {}, onFallback = {}, onStart = {}, onPause = {}, onEnd = {}, onClose = {}, onRetrySave = {}, onSaveSentence = { _, _, _ -> })
        } }
        compose.onNodeWithTag("voice_live_mode").assertIsNotEnabled()
        compose.onNodeWithTag("voice_start").performScrollTo().assertIsEnabled()
        capture("fallback")
    }

    @Test fun finishedScreenRendersTranscriptAndSummaryWithoutClaimingAudioScore() {
        val summary = TutorSessionScorecard(70, 70, 60, 80, listOf("استخدم الماضي مع yesterday"), "جملك واضحة", "راجع الماضي البسيط", "I went to work yesterday.")
        compose.setContent { LingoWorldTheme(darkTheme = false) {
            VoiceTutorPanel(VoiceCallState(phase = VoicePhase.FINISHED, turns = listOf(VoiceTurn("user", "I go yesterday."), VoiceTurn("tutor", "Try: I went yesterday."))), false, false,
                summary = AiOpState.Success(summary), saveState = AiOpState.Success(Unit),
                onStrict = {}, onFallback = {}, onStart = {}, onPause = {}, onEnd = {}, onClose = {}, onRetrySave = {}, onSaveSentence = { _, _, _ -> })
        } }
        compose.onNodeWithTag("voice_call_screen").performScrollToNode(hasText("ملخص تدريبك"))
        capture("summary")
        compose.onNodeWithText("تقييم للنص، مش تحليل صوتي للنطق").assertExists()
    }
}
