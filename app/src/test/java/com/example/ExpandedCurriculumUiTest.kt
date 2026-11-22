package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.core.AudioClipRecorder
import com.example.core.SpeechEngine
import com.example.data.curriculum.CurriculumCatalog
import com.example.data.model.*
import com.example.ui.screens.LessonGameView
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
@Config(sdk = [35], qualifiers = "en-rUS-w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExpandedCurriculumUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun show(language: TargetLanguage, level: CefrLevel, unit: Int, dark: Boolean, name: String) {
        val items = CurriculumCatalog.sentencesFor(language, level, unit, 1)
        val speech = SpeechEngine(compose.activity)
        val recorder = AudioClipRecorder(compose.activity)
        var saved = ""
        compose.setContent { LingoWorldTheme(darkTheme = dark) {
            LessonGameView("تدريب جديد ${level.code}", items, Dialect.fromCode(if (language == TargetLanguage.EN) "en-US" else "es-ES", language),
                speech, recorder, { saved = it.sentence }, {}, {}, {})
        } }
        compose.onNodeWithText(items.first().sentence).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(items.first().arabicTranslation).assertExists()
        compose.waitForIdle()
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(System.getProperty("java.io.tmpdir"), "content-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        compose.onNodeWithTag("save_sentence_fsrs_btn").performScrollTo().performClick()
        assertEquals(items.first().sentence, saved)
        compose.onNodeWithTag("toggle_grammar_drawer_btn").performScrollTo().performClick()
        compose.onNodeWithText(items.first().expressionNoteAr).assertExists()
        speech.release()
        recorder.resetClip()
    }
    @Test fun englishA1NewContentRendersAndCanBeSaved() = show(TargetLanguage.EN, CefrLevel.A1, 11, false, "english-a1")
    @Test fun spanishC2LongContentRendersInDarkModeAndCanBeSaved() = show(TargetLanguage.ES, CefrLevel.C2, 4, true, "spanish-c2-dark")

    @Test fun fallbackExamActuallyUsesSevenQuestionsPerSkill() {
        val app = com.google.firebase.FirebaseApp.initializeApp(compose.activity,
            com.google.firebase.FirebaseOptions.Builder()
                .setApplicationId("1:1234567890:android:0123456789abcdef")
                .setProjectId("lingo-offline-render-test")
                .setApiKey("AIza" + "x".repeat(35)).build(), "offline-exam-render")
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance(app)
        db.disableNetwork()
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance(app)
        val vm = com.example.ui.viewmodel.AiTutorViewModel(
            com.example.data.repository.LingoRepository(db, auth),
            com.example.data.repository.LingoAiRepository(), "offline-render-only")
        val speech = SpeechEngine(compose.activity)
        val recorder = AudioClipRecorder(compose.activity)
        compose.setContent { LingoWorldTheme(darkTheme = true) {
            com.example.ui.screens.HardCefrExamsScreen(
                UserProfile(activeLanguage = "ES", spanishLevel = "C2"), emptyList(), emptyList(), vm,
                speech, recorder, { _, _, _, _, _, _, _, _, _, _ -> })
        } }
        compose.onNodeWithTag("cefr_exam_list").performScrollToNode(hasText("1) الاستماع (7 أسئلة) — 25%"))
        compose.onNodeWithText("1) الاستماع (7 أسئلة) — 25%").assertExists()
        val target = CurriculumCatalog.examQuestionsFor(TargetLanguage.ES, CefrLevel.C2)
            .first { it.skill == SkillType.READING && it.id.startsWith("expanded") }.stimulusTarget
        compose.onNodeWithTag("cefr_exam_list").performScrollToNode(hasText(target))
        compose.onNodeWithText(target).assertIsDisplayed()
        compose.onNodeWithText("2) القراءة والاستنتاج (7 أسئلة) — 25%").assertExists()
        compose.waitForIdle()
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(System.getProperty("java.io.tmpdir"), "content-exam-spanish-c2.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        speech.release()
        recorder.resetClip()
        app.delete()
    }
}
