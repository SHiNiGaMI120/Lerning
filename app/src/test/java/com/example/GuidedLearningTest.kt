package com.example

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import android.graphics.Canvas
import com.example.data.model.*
import com.example.ui.screens.*
import com.example.ui.theme.LingoWorldTheme
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Calendar
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GuidedLearningTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val file = File(System.getProperty("java.io.tmpdir"), "lingo-$name.png")
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        println("SCREENSHOT: ${file.absolutePath}")
    }

    @Test fun setupPreservesExistingChoicesAndAllowsBack() {
        var answer = ""
        compose.setContent { LingoWorldTheme(darkTheme = false) {
            OnboardingScreen(UserProfile(activeLanguage = "ES", spanishLevel = "B1", dailyGoalMinutes = 30), false, null,
                { language, level, minutes -> answer = "${language.code}:${level.code}:$minutes" }, {})
        } }
        screenshot("setup-language")
        compose.onNodeWithTag("onboarding_next_button").performClick()
        screenshot("setup-level")
        compose.onNodeWithTag("onboarding_next_button").performClick()
        screenshot("setup-goal")
        compose.onNodeWithText("رجوع").performClick()
        compose.onNodeWithTag("onboarding_next_button").performClick()
        compose.onNodeWithTag("onboarding_next_button").performClick()
        assertEquals("ES:B1:30", answer)
    }

    @Test fun homeGuidesToActualNextActivityAndKeepsAdminVisible() {
        var destination: LingoDestination? = null
        compose.setContent { LingoWorldTheme(darkTheme = false) {
            HomeDashboardScreen(UserProfile(displayName = "محمد"), emptyList(), emptyList(), emptyList(), emptyList(), true,
                onSwitchLanguage = {}, onContinueLastLesson = { destination = LingoDestination.LEARNING }, onNavigate = { destination = it })
        } }
        screenshot("home")
        compose.onNodeWithTag("home_next_step_button").performScrollTo().performClick()
        assertEquals(LingoDestination.LEARNING, destination)
        compose.onNodeWithTag("home_dashboard_list").performScrollToNode(hasText("لوحة الإدارة والمحتوى"))
        compose.onNodeWithText("لوحة الإدارة والمحتوى").performClick()
        assertEquals(LingoDestination.ADMIN, destination)
        compose.onNodeWithTag("home_dashboard_list").performScrollToNode(hasTestTag("home_explore_toggle"))
        compose.onNodeWithTag("home_explore_toggle").performClick()
        compose.onNodeWithTag("home_dashboard_list").performScrollToNode(hasTestTag("hub_tile_stories"))
        compose.onNodeWithTag("hub_tile_stories").assertExists()
    }

    @Test fun darkHomeAndNextStepBarRender() {
        compose.setContent { LingoWorldTheme(darkTheme = true) {
            HomeDashboardScreen(UserProfile(displayName = "محمد"), emptyList(), emptyList(), emptyList(), emptyList(), false,
                onSwitchLanguage = {}, onContinueLastLesson = {}, onNavigate = {})
        } }
        screenshot("home-dark")
    }

    @Test fun nextStepIsNavigationNotCompletion() {
        var destination: LingoDestination? = null
        compose.setContent { LingoWorldTheme(darkTheme = false) {
            LearningNextStepBar(LingoDestination.PRONUNCIATION) { destination = it }
        } }
        compose.onNodeWithTag("learning_next_step_button").performClick()
        assertEquals(LingoDestination.AI_TUTOR, destination)
        screenshot("next-step")
    }

    @Test fun completionIgnoresOldOtherLanguageAndFutureActivity() {
        val now = System.currentTimeMillis()
        val today = Timestamp(Date(now))
        val yesterday = Timestamp(Date(now - 86400000L))
        val future = Timestamp(Date(now + 86400000L))
        val oldLesson = LessonProgress(language = "EN", completed = true, updatedAt = yesterday)
        assertEquals(listOf(false, false, false, false), dailyJourneyCompletion("EN", listOf(oldLesson),
            listOf(PronunciationLog(language = "ES", createdAt = today)),
            listOf(TutorMessage(language = "EN", sender = "assistant", createdAt = today)),
            listOf(ReviewCard(language = "EN", reviewCount = 0, updatedAt = today)), now))
        assertEquals(listOf(true, true, true, true), dailyJourneyCompletion("EN",
            listOf(oldLesson.copy(updatedAt = today)), listOf(PronunciationLog(createdAt = today)),
            listOf(TutorMessage(sender = "user", createdAt = today)), listOf(ReviewCard(reviewCount = 1, updatedAt = today)), now))
        assertEquals(listOf(false, false, false, false), dailyJourneyCompletion("EN",
            listOf(oldLesson.copy(updatedAt = future)), listOf(PronunciationLog(createdAt = future)),
            listOf(TutorMessage(createdAt = future)), listOf(ReviewCard(reviewCount = 1, updatedAt = future)), now))
    }
}
