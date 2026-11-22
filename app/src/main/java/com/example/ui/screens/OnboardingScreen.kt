package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.data.model.CefrLevel
import com.example.data.model.TargetLanguage

/**
 * First-run guided setup: 3 simple questions (language, level, daily goal) so the
 * app configures itself and the learner never lands on a screen without direction.
 */
@Composable
fun OnboardingScreen(
    profile: UserProfile,
    saving: Boolean,
    error: String?,
    onFinish: (TargetLanguage, CefrLevel, Int) -> Unit,
    onSkip: () -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var languageCode by rememberSaveable { mutableStateOf(profile.activeLanguage) }
    val selectedLanguage = TargetLanguage.fromCode(languageCode)
    var levelCode by rememberSaveable { mutableStateOf(profile.currentLevelEnum().code) }
    val selectedLevel = CefrLevel.fromCode(levelCode)
    var selectedGoalMinutes by rememberSaveable { mutableIntStateOf(profile.dailyGoalMinutes.toInt()) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "أهلاً بيك في LingoWorld AI 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "٣ أسئلة بسيطة والتطبيق يظبط نفسه على مستواك وهدفك",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { (step + 1) / 3f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp),
                )
                Text(
                    text = "خطوة ${step + 1} من 3",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when (step) {
                    0 -> {
                        OnboardingSectionTitle("عايز تتعلم أنهي لغة؟")
                        TargetLanguage.entries.forEach { language ->
                            OnboardingOptionCard(
                                title = language.arabicName,
                                subtitle = language.nativeName,
                                selected = selectedLanguage == language,
                                onClick = { languageCode = language.code; levelCode = if (language == TargetLanguage.EN) profile.englishLevel else profile.spanishLevel },
                                testTag = "onboarding_language_${language.code}"
                            )
                        }
                    }

                    1 -> {
                        OnboardingSectionTitle("مستواك الحالي إيه؟")
                        Text(
                            text = "متقلقش، تقدر تغيّره في أي وقت من صفحة تعلّمي",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        listOf(
                            CefrLevel.A1,
                            CefrLevel.A2,
                            CefrLevel.B1,
                            CefrLevel.B2,
                            CefrLevel.C1,
                            CefrLevel.C2
                        ).forEach { level ->
                            OnboardingOptionCard(
                                title = "${level.arabicTitle} (${level.code})",
                                subtitle = level.summaryAr,
                                selected = selectedLevel == level,
                                onClick = { levelCode = level.code },
                                testTag = "onboarding_level_${level.code}"
                            )
                        }
                    }

                    else -> {
                        OnboardingSectionTitle("هدفك اليومي قد إيه؟")
                        listOf(
                            Triple(5, "خفيف", "٥ دقائق يوميًا، عادة سهلة من غير ضغط"),
                            Triple(15, "متوازن", "ربع ساعة يوميًا، تقدم ثابت ومحسوس"),
                            Triple(30, "جاد", "نص ساعة يوميًا، وقت أطول للتدريب والمراجعة")
                        ).forEach { (minutes, title, subtitle) ->
                            OnboardingOptionCard(
                                title = "$title: $minutes دقيقة",
                                subtitle = subtitle,
                                selected = selectedGoalMinutes == minutes,
                                onClick = { selectedGoalMinutes = minutes },
                                testTag = "onboarding_goal_$minutes"
                            )
                        }
                    }
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { if (step > 0) step -= 1 else onSkip() },
                    enabled = !saving,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text(if (step > 0) "رجوع" else "مش دلوقتي")
                }
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = !saving,
                    onClick = {
                        if (step < 2) {
                            step += 1
                        } else {
                            onFinish(selectedLanguage, selectedLevel, selectedGoalMinutes)
                        }
                    },
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("onboarding_next_button")
                ) {
                    Text(
                        text = if (saving) "بنحفظ اختياراتك..." else if (step < 2) "التالي" else "يلا نبدأ 🚀",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun OnboardingOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "تم الاختيار",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
