package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.AudioClipRecorder
import com.example.core.SpeechEngine
import com.example.core.ThemePreferences
import com.example.core.configureAppCheck
import com.example.data.model.UserProfile
import com.example.data.repository.LingoAiRepository
import com.example.data.repository.LingoRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.performSignOut
import com.example.ui.components.ErrorBannerCard
import com.example.ui.navigation.LingoBottomNavigationBar
import com.example.ui.navigation.LingoNavHost
import com.example.ui.navigation.LingoNavigationRail
import com.example.ui.navigation.LingoTopBar
import com.example.ui.navigation.navigateToDestination
import com.example.ui.navigation.navigateToTopLevel
import com.example.ui.navigation.routeToDestination
import com.example.ui.screens.ActiveLessonResume
import com.example.ui.screens.LingoDestination
import com.example.ui.theme.LingoWorldTheme
import com.example.ui.viewmodel.AiTutorViewModel
import com.example.ui.viewmodel.DataState
import com.example.ui.viewmodel.LingoViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1 (MANDATORY): Configure Firebase App Check with launch intent BEFORE UI/ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            val themePrefs = remember { ThemePreferences(applicationContext) }
            val storedDark by themePrefs.isDarkMode.collectAsStateWithLifecycle(initialValue = null)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = storedDark ?: systemDark
            val scope = rememberCoroutineScope()

            LingoWorldTheme(darkTheme = darkTheme) {
                LingoWorldAppRoot(
                    darkTheme = darkTheme,
                    onToggleDarkTheme = { enabled ->
                        scope.launch { themePrefs.setDarkMode(enabled) }
                    }
                )
            }
        }
    }
}

@Composable
private fun LingoWorldAppRoot(
    darkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val firebaseReady = remember(context) {
        FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
    }

    if (!firebaseReady) {
        MissingFirebaseConfigScreen()
        return
    }

    LaunchedEffect(Unit) {
        com.example.core.warmUpAppCheckTokenOffMainThread()
    }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        key(user.uid) {
            AuthenticatedLingoShell(
                currentUserId = user.uid,
                initialDisplayName = user.displayName ?: "متعلم LingoWorld",
                darkTheme = darkTheme,
                onToggleDarkTheme = onToggleDarkTheme
            )
        }
    }
}

@Composable
private fun MissingFirebaseConfigScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ملف إعدادات Firebase غير مرفق بهذه النسخة",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "تم بناء ملف APK هذا بدون ملف google-services.json، لذلك لا يمكن بدء تسجيل الدخول بحساب Google أو الاتصال بقاعدة البيانات والذكاء الاصطناعي.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "لإصلاح ذلك في GitHub:\n1) افتح Settings ← Secrets and variables ← Actions\n2) أضف سرًا باسم GOOGLE_SERVICES_JSON يحتوي على محتوى ملف app/google-services.json\n3) أضف سرًا باسم DEBUG_KEYSTORE_BASE64 يحتوي على محتوى debug.keystore.base64\n4) أعد تشغيل بناء Build LingoWorld AI APK وحمّل النسخة الجديدة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedLingoShell(
    currentUserId: String,
    initialDisplayName: String,
    darkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val lingoViewModel: LingoViewModel = viewModel(
        key = "lingo_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                LingoViewModel(LingoRepository(db), currentUserId, initialDisplayName)
            }
        }
    )

    val aiViewModel: AiTutorViewModel = viewModel(
        key = "ai_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                AiTutorViewModel(LingoRepository(db), LingoAiRepository(), currentUserId)
            }
        }
    )

    val speechEngine = remember(context) { SpeechEngine(context) }
    DisposableEffect(speechEngine) {
        onDispose { speechEngine.release() }
    }

    val audioRecorder = remember(context) { AudioClipRecorder(context) }
    DisposableEffect(audioRecorder) {
        onDispose { audioRecorder.release() }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = routeToDestination(navBackStackEntry?.destination?.route)
    var pronunciationTargetSentence by remember { mutableStateOf<String?>(null) }
    var resumeLessonRequest by remember { mutableStateOf<ActiveLessonResume?>(null) }

    val profileState by lingoViewModel.profileState.collectAsStateWithLifecycle()
    val reviewState by lingoViewModel.reviewCardsState.collectAsStateWithLifecycle()
    val progressState by lingoViewModel.lessonProgressState.collectAsStateWithLifecycle()
    val examState by lingoViewModel.examAttemptsState.collectAsStateWithLifecycle()
    val pronState by lingoViewModel.pronunciationLogsState.collectAsStateWithLifecycle()
    val cmsState by lingoViewModel.cmsContentState.collectAsStateWithLifecycle()
    val savedStoriesState by lingoViewModel.savedStoriesState.collectAsStateWithLifecycle()
    val leaderboardState by lingoViewModel.leaderboardState.collectAsStateWithLifecycle()
    val adminLearnersState by lingoViewModel.adminLearnersState.collectAsStateWithLifecycle()
    val statusBanner by lingoViewModel.statusBanner.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        lingoViewModel.clearBanner()
    }

    val profile = (profileState as? DataState.Success)?.data
        ?: UserProfile(userId = currentUserId, displayName = initialDisplayName)
    val reviewCards = (reviewState as? DataState.Success)?.data.orEmpty()
    val lessonProgress = (progressState as? DataState.Success)?.data.orEmpty()
    val examAttempts = (examState as? DataState.Success)?.data.orEmpty()
    val pronLogs = (pronState as? DataState.Success)?.data.orEmpty()
    val cmsItems = (cmsState as? DataState.Success)?.data.orEmpty()
    val savedStories = (savedStoriesState as? DataState.Success)?.data.orEmpty()
    val leaderboardEntries = (leaderboardState as? DataState.Success)?.data.orEmpty()
    val adminLearners = (adminLearnersState as? DataState.Success)?.data.orEmpty()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 680.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                LingoTopBar(
                    currentDestination = currentDestination,
                    onNavigateBack = { navController.navigateUp() },
                    onNavigate = { dest -> navController.navigateToDestination(dest) }
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    LingoBottomNavigationBar(
                        currentDestination = currentDestination,
                        onSelectTab = { dest -> navController.navigateToTopLevel(dest) }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    LingoNavigationRail(
                        currentDestination = currentDestination,
                        onSelectTab = { dest -> navController.navigateToTopLevel(dest) }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp)
                    ) {
                        if (profileState is DataState.Loading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(stringResource(R.string.nav_syncing_profile))
                                }
                            }
                        } else {
                            if (profileState is DataState.Error) {
                                Box(Modifier.padding(16.dp)) {
                                    ErrorBannerCard((profileState as DataState.Error).message)
                                }
                            }

                            LingoNavHost(
                                navController = navController,
                                profile = profile,
                                reviewCards = reviewCards,
                                lessonProgress = lessonProgress,
                                examAttempts = examAttempts,
                                pronunciationLogs = pronLogs,
                                cmsItems = cmsItems,
                                savedStories = savedStories,
                                leaderboardEntries = leaderboardEntries,
                                adminLearners = adminLearners,
                                darkTheme = darkTheme,
                                pronunciationTargetSentence = pronunciationTargetSentence,
                                onSetPronunciationTarget = { pronunciationTargetSentence = it },
                                resumeLessonRequest = resumeLessonRequest,
                                onContinueLastLesson = { activeLesson ->
                                    if (profile.currentLanguageEnum() != activeLesson.language) {
                                        lingoViewModel.switchLanguage(activeLesson.language)
                                    }
                                    resumeLessonRequest = activeLesson
                                    navController.navigateToTopLevel(LingoDestination.LEARNING)
                                },
                                onConsumeResumeRequest = { resumeLessonRequest = null },
                                lingoViewModel = lingoViewModel,
                                aiViewModel = aiViewModel,
                                speechEngine = speechEngine,
                                audioRecorder = audioRecorder,
                                onToggleDarkTheme = onToggleDarkTheme,
                                onSignOut = { performSignOut(credentialManager, scope) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
