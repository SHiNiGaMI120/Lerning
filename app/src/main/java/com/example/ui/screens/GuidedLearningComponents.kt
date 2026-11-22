package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import java.util.Calendar

/** Only real activity in the selected language on this local calendar day counts. */
fun dailyJourneyCompletion(
    language: String,
    lessons: List<LessonProgress>,
    pronunciations: List<PronunciationLog>,
    messages: List<TutorMessage>,
    reviews: List<ReviewCard>,
    nowMs: Long = System.currentTimeMillis()
): List<Boolean> {
    val day = Calendar.getInstance().apply {
        timeInMillis = nowMs
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val start = day.timeInMillis
    day.add(Calendar.DAY_OF_YEAR, 1)
    val end = day.timeInMillis
    fun today(time: com.google.firebase.Timestamp?) = time != null && time.toDate().time in start until end
    return listOf(
        lessons.any { it.language == language && it.contentType == "lesson" && it.completed && today(it.updatedAt ?: it.createdAt) },
        pronunciations.any { it.language == language && today(it.createdAt) },
        messages.any { it.language == language && it.sender == "user" && today(it.createdAt) },
        reviews.any { it.language == language && it.reviewCount > 0 && today(it.updatedAt) }
    )
}

@Composable
fun LearningNextStepBar(destination: LingoDestination, onNavigate: (LingoDestination) -> Unit) {
    val next = when (destination) {
        LingoDestination.LEARNING -> LingoDestination.PRONUNCIATION
        LingoDestination.PRONUNCIATION -> LingoDestination.AI_TUTOR
        LingoDestination.AI_TUTOR -> LingoDestination.REVIEW
        LingoDestination.REVIEW, LingoDestination.STORIES, LingoDestination.VIDEOS, LingoDestination.SKILLS -> LingoDestination.HOME
        else -> return
    }
    Surface(tonalElevation = 3.dp, shadowElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("خلصت تدريبك؟ كمل على الخطوة اللي بعدها", style = MaterialTheme.typography.labelSmall)
            Button(
                onClick = { onNavigate(next) },
                modifier = Modifier.fillMaxWidth().testTag("learning_next_step_button")
            ) {
                Text(if (next == LingoDestination.HOME) "ارجع لخطة يومك" else "الخطوة الجاية: ${next.labelAr}")
            }
        }
    }
}
