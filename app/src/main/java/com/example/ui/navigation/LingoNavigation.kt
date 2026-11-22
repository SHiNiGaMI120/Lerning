package com.example.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.AudioClipRecorder
import com.example.core.SpeechEngine
import com.example.data.model.CmsContentItem
import com.example.data.model.ExamAttempt
import com.example.data.model.InteractiveStory
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LessonProgress
import com.example.data.model.PronunciationLog
import com.example.data.model.ReviewCard
import com.example.data.model.UserProfile
import com.example.ui.screens.LearningNextStepBar
import com.example.ui.screens.ActiveLessonResume
import com.example.ui.screens.AdminCmsScreen
import com.example.ui.screens.AiTutorScreen
import com.example.ui.screens.CurriculumAndSentenceScreen
import com.example.ui.screens.FourSkillsHubScreen
import com.example.ui.screens.HardCefrExamsScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.InteractiveStoriesScreen
import com.example.ui.screens.LingoDestination
import com.example.ui.screens.ProgressGamificationScreen
import com.example.ui.screens.PronunciationLabScreen
import com.example.ui.screens.RealVideosScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmartReviewFsrsScreen
import com.example.ui.viewmodel.DataState
import com.example.ui.viewmodel.AiTutorViewModel
import com.example.ui.viewmodel.LingoViewModel

/**
 * Primary bottom-bar / navigation-rail sections ordered right-to-left under Arabic RTL:
 * Home (الرئيسية), My Learning (تعلمي), Progress (التقدم).
 */
enum class PrimaryNavTab(
    val destination: LingoDestination,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME(
        destination = LingoDestination.HOME,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_tab_home"
    ),
    MY_LEARNING(
        destination = LingoDestination.LEARNING,
        labelRes = R.string.nav_learning,
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook,
        testTag = "nav_tab_learning"
    ),
    PROGRESS(
        destination = LingoDestination.PROGRESS,
        labelRes = R.string.nav_progress,
        selectedIcon = Icons.Filled.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents,
        testTag = "nav_tab_progress"
    )
}

fun LingoDestination.parentPrimaryTab(): PrimaryNavTab = when (this) {
    LingoDestination.HOME,
    LingoDestination.SETTINGS,
    LingoDestination.ADMIN -> PrimaryNavTab.HOME

    LingoDestination.LEARNING,
    LingoDestination.STORIES,
    LingoDestination.VIDEOS,
    LingoDestination.PRONUNCIATION,
    LingoDestination.AI_TUTOR,
    LingoDestination.SKILLS,
    LingoDestination.REVIEW -> PrimaryNavTab.MY_LEARNING

    LingoDestination.PROGRESS,
    LingoDestination.TESTS -> PrimaryNavTab.PROGRESS
}

fun LingoDestination.isTopLevelTab(): Boolean =
    this == LingoDestination.HOME ||
        this == LingoDestination.LEARNING ||
        this == LingoDestination.PROGRESS

fun routeToDestination(route: String?): LingoDestination =
    LingoDestination.entries.firstOrNull { it.route == route } ?: LingoDestination.HOME

fun NavHostController.navigateToTopLevel(destination: LingoDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

fun NavHostController.navigateToDestination(destination: LingoDestination) {
    if (destination.isTopLevelTab()) {
        navigateToTopLevel(destination)
    } else {
        navigate(destination.route) {
            launchSingleTop = true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LingoTopBar(
    currentDestination: LingoDestination,
    onNavigateBack: () -> Unit,
    onNavigate: (LingoDestination) -> Unit
) {
    val showBackButton = !currentDestination.isTopLevelTab()
    val appTitle = stringResource(R.string.app_name)

    TopAppBar(
        title = {
            Text(
                text = if (currentDestination == LingoDestination.HOME) appTitle else currentDestination.labelAr,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.nav_back_cd)
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = { onNavigate(LingoDestination.AI_TUTOR) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("top_bar_ai_tutor_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = stringResource(R.string.nav_tutor),
                    tint = if (currentDestination == LingoDestination.AI_TUTOR) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            IconButton(
                onClick = { onNavigate(LingoDestination.STORIES) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("top_bar_stories_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoStories,
                    contentDescription = stringResource(R.string.nav_stories_cd),
                    tint = if (currentDestination == LingoDestination.STORIES) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            IconButton(
                onClick = { onNavigate(LingoDestination.SETTINGS) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("top_bar_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.nav_settings_cd),
                    tint = if (currentDestination == LingoDestination.SETTINGS) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun LingoBottomNavigationBar(
    currentDestination: LingoDestination,
    onSelectTab: (LingoDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTab = currentDestination.parentPrimaryTab()

    NavigationBar(
        modifier = modifier.testTag("main_bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        PrimaryNavTab.entries.forEach { tab ->
            val selected = activeTab == tab
            val label = stringResource(tab.labelRes)
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab.destination) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(tab.testTag),
                icon = {
                    AnimatedContent(
                        targetState = selected,
                        transitionSpec = {
                            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) togetherWith
                                fadeOut(animationSpec = tween(120))
                        },
                        label = "nav_icon_${tab.name}"
                    ) { isSelected ->
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = label
                        )
                    }
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun LingoNavigationRail(
    currentDestination: LingoDestination,
    onSelectTab: (LingoDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTab = currentDestination.parentPrimaryTab()

    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .testTag("main_navigation_rail"),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        PrimaryNavTab.entries.forEach { tab ->
            val selected = activeTab == tab
            val label = stringResource(tab.labelRes)
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(tab.destination) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("${tab.testTag}_rail"),
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun LingoNavHost(
    navController: NavHostController,
    profile: UserProfile,
    reviewCards: List<ReviewCard>,
    lessonProgress: List<LessonProgress>,
    examAttempts: List<ExamAttempt>,
    pronunciationLogs: List<PronunciationLog>,
    cmsItems: List<CmsContentItem>,
    savedStories: List<InteractiveStory>,
    leaderboardEntries: List<LeaderboardEntry>,
    adminLearners: List<UserProfile>,
    darkTheme: Boolean,
    pronunciationTargetSentence: String?,
    onSetPronunciationTarget: (String) -> Unit,
    resumeLessonRequest: ActiveLessonResume?,
    onContinueLastLesson: (ActiveLessonResume) -> Unit,
    onConsumeResumeRequest: () -> Unit,
    lingoViewModel: LingoViewModel,
    aiViewModel: AiTutorViewModel,
    speechEngine: SpeechEngine,
    audioRecorder: AudioClipRecorder,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAdmin by lingoViewModel.isAdmin.collectAsStateWithLifecycle()

    val tutorMessagesState by aiViewModel.messagesState.collectAsStateWithLifecycle()
    val tutorMessages = (tutorMessagesState as? DataState.Success)?.data.orEmpty()
    val entry by navController.currentBackStackEntryAsState()
    Column(modifier = modifier) {
    NavHost(
        navController = navController,
        startDestination = LingoDestination.HOME.route,
        modifier = Modifier.weight(1f).fillMaxSize(),
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(160)) }
    ) {
        composable(LingoDestination.HOME.route) {
            HomeDashboardScreen(
                profile = profile,
                reviewCards = reviewCards,
                lessonProgress = lessonProgress,
                examAttempts = examAttempts,
                pronunciationLogs = pronunciationLogs,
                isAdmin = isAdmin,
                tutorMessages = tutorMessages,
                onSwitchLanguage = lingoViewModel::switchLanguage,
                onContinueLastLesson = onContinueLastLesson,
                onNavigate = { navController.navigateToDestination(it) }
            )
        }

        composable(LingoDestination.LEARNING.route) {
            CurriculumAndSentenceScreen(
                profile = profile,
                lessonProgress = lessonProgress,
                cmsItems = cmsItems,
                aiViewModel = aiViewModel,
                speechEngine = speechEngine,
                audioRecorder = audioRecorder,
                resumeLessonRequest = resumeLessonRequest,
                onConsumeResumeRequest = onConsumeResumeRequest,
                onSwitchLanguage = lingoViewModel::switchLanguage,
                onSelectDialect = lingoViewModel::selectDialect,
                onSelectLevel = lingoViewModel::selectLevel,
                onSelectSubLevel = lingoViewModel::selectSubLevel,
                onSaveToReview = { item, dialect ->
                    lingoViewModel.saveSentenceToReview(
                        sentence = item.sentence,
                        arabic = item.arabicTranslation,
                        language = item.language,
                        dialectCode = dialect,
                        level = item.cefrLevel,
                        grammarNote = item.grammarInContextAr
                    )
                },
                onCompleteLesson = { id, lang, lvl, unit, lesson, score ->
                    lingoViewModel.completeLessonItem(id, lang, lvl, unit, lesson, "lesson", score)
                },
                onPracticeInPronunciation = { sentence ->
                    onSetPronunciationTarget(sentence)
                    navController.navigateToDestination(LingoDestination.PRONUNCIATION)
                }
            )
        }

        composable(LingoDestination.PROGRESS.route) {
            ProgressGamificationScreen(
                profile = profile,
                lessonProgress = lessonProgress,
                examAttempts = examAttempts,
                pronunciationLogs = pronunciationLogs,
                reviewCards = reviewCards,
                leaderboardEntries = leaderboardEntries,
                onToggleLeaderboardOptIn = { optIn ->
                    lingoViewModel.toggleLeaderboardOptIn(profile, optIn)
                },
                onClaimWeeklyChallenge = { challengeId, bonusXp ->
                    lingoViewModel.claimWeeklyChallenge(
                        challengeId = challengeId,
                        language = profile.currentLanguageEnum(),
                        level = profile.currentLevelEnum(),
                        bonusXp = bonusXp
                    )
                }
            )
        }

        composable(LingoDestination.STORIES.route) {
            InteractiveStoriesScreen(
                profile = profile,
                savedStories = savedStories,
                cmsItems = cmsItems,
                aiViewModel = aiViewModel,
                speechEngine = speechEngine,
                onStoryCompleted = { story ->
                    lingoViewModel.completeLessonItem(
                        progressId = "story_${story.id}",
                        language = story.language,
                        level = story.cefrLevel,
                        unitNumber = 1,
                        lessonNumber = 1,
                        contentType = "story",
                        score = 100
                    )
                },
                onDeleteSavedStory = lingoViewModel::deleteSavedStory,
                onSaveSentence = { text, ar, note ->
                    lingoViewModel.saveSentenceToReview(
                        sentence = text,
                        arabic = ar,
                        language = profile.currentLanguageEnum(),
                        dialectCode = profile.currentDialectCode(),
                        level = profile.currentLevelEnum(),
                        grammarNote = note
                    )
                }
            )
        }

        composable(LingoDestination.VIDEOS.route) {
            RealVideosScreen(
                profile = profile,
                cmsItems = cmsItems,
                speechEngine = speechEngine,
                onSaveSentence = { text, ar, note ->
                    lingoViewModel.saveSentenceToReview(
                        sentence = text,
                        arabic = ar,
                        language = profile.currentLanguageEnum(),
                        dialectCode = profile.currentDialectCode(),
                        level = profile.currentLevelEnum(),
                        grammarNote = note
                    )
                },
                onCompleteVideo = { vid ->
                    lingoViewModel.completeLessonItem(
                        progressId = "video_${vid.id}",
                        language = vid.language,
                        level = vid.cefrLevel,
                        unitNumber = 1,
                        lessonNumber = 1,
                        contentType = "video",
                        score = 100
                    )
                }
            )
        }

        composable(LingoDestination.PRONUNCIATION.route) {
            PronunciationLabScreen(
                profile = profile,
                initialSentence = pronunciationTargetSentence,
                pronunciationLogs = pronLogsOrEmpty(pronunciationLogs),
                aiViewModel = aiViewModel,
                speechEngine = speechEngine,
                audioRecorder = audioRecorder
            )
        }

        composable(LingoDestination.AI_TUTOR.route) {
            AiTutorScreen(
                profile = profile,
                aiViewModel = aiViewModel,
                speechEngine = speechEngine,
                onSaveSentence = { text, ar, note ->
                    lingoViewModel.saveSentenceToReview(
                        sentence = text,
                        arabic = ar,
                        language = profile.currentLanguageEnum(),
                        dialectCode = profile.currentDialectCode(),
                        level = profile.currentLevelEnum(),
                        grammarNote = note
                    )
                },
                onPracticePronunciation = { sentence ->
                    onSetPronunciationTarget(sentence)
                    navController.navigateToDestination(LingoDestination.PRONUNCIATION)
                }
            )
        }

        composable(LingoDestination.SKILLS.route) {
            FourSkillsHubScreen(
                profile = profile,
                aiViewModel = aiViewModel,
                speechEngine = speechEngine,
                onNavigate = { navController.navigateToDestination(it) }
            )
        }

        composable(LingoDestination.TESTS.route) {
            HardCefrExamsScreen(
                profile = profile,
                examAttempts = examAttempts,
                cmsItems = cmsItems,
                aiViewModel = aiViewModel,
                speechEngine = speechEngine,
                audioRecorder = audioRecorder,
                onSubmitExam = { lang, lvl, type, l, s, r, w, fb, recLevel, subLevel ->
                    lingoViewModel.recordExamResult(lang, lvl, type, l, s, r, w, fb, recLevel, subLevel)
                }
            )
        }

        composable(LingoDestination.REVIEW.route) {
            SmartReviewFsrsScreen(
                reviewCards = reviewCards,
                speechEngine = speechEngine,
                onSubmitRating = lingoViewModel::submitFsrsReview,
                onGoToCurriculum = { navController.navigateToTopLevel(LingoDestination.LEARNING) }
            )
        }

        composable(LingoDestination.SETTINGS.route) {
            SettingsScreen(
                profile = profile,
                darkThemeEnabled = darkTheme,
                isAdmin = isAdmin,
                onToggleDarkTheme = onToggleDarkTheme,
                onSelectDialect = lingoViewModel::selectDialect,
                onUpdateDailyGoal = lingoViewModel::updateDailyGoal,
                onOpenAdmin = { navController.navigateToDestination(LingoDestination.ADMIN) },
                onSignOut = onSignOut
            )
        }

        composable(LingoDestination.ADMIN.route) {
            AdminCmsScreen(
                isAdmin = isAdmin,
                cmsItems = cmsItems,
                adminLearners = adminLearners,
                onCreateCmsItem = lingoViewModel::createCmsContent,
                onTogglePublished = lingoViewModel::toggleCmsPublished,
                onDeleteItem = lingoViewModel::deleteCmsItem
            )
        }
    }
    LearningNextStepBar(routeToDestination(entry?.destination?.route)) { navController.navigateToDestination(it) }
    }
}

private fun pronLogsOrEmpty(logs: List<PronunciationLog>): List<PronunciationLog> = logs
