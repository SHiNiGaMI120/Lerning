package com.example

import com.example.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceTutorControllerTest {
    class FakeConnection : VoiceConnection {
        var begun = 0; var stopped = 0; var closed = 0; var active = true
        lateinit var transcript: (String?, String?) -> Unit
        lateinit var away: () -> Unit
        override suspend fun begin(onTranscript: (String?, String?) -> Unit, onGoingAway: () -> Unit) {
            begun++; transcript = onTranscript; away = onGoingAway
        }
        override fun stopAudio() { stopped++; active = false }
        override suspend fun close() { closed++ }
        override fun isActive() = active
    }

    @Test fun explicitStartDoubleTapAndPauseReleaseResources() = runTest {
        val fake = FakeConnection(); var connects = 0
        val controller = VoiceTutorController(this, { connects++; fake }, { testScheduler.currentTime })
        assertEquals(0, connects)
        controller.start("practice"); controller.start("practice"); runCurrent()
        assertEquals(1, connects); assertEquals(1, fake.begun)
        assertEquals(VoicePhase.LIVE, controller.state.value.phase)
        fake.transcript("Hello", null); fake.transcript(null, "Hi!"); runCurrent()
        assertEquals(listOf(VoiceTurn("user", "Hello"), VoiceTurn("tutor", "Hi!")), controller.state.value.turns)
        controller.stop()
        assertTrue(fake.stopped > 0)
        runCurrent(); assertEquals(1, fake.closed)
        assertEquals(VoicePhase.PAUSED, controller.state.value.phase)
        fake.transcript("late callback", null); runCurrent()
        assertEquals(2, controller.state.value.turns.size)
    }

    @Test fun cancellingWhileConnectingNeverOpensTheMicrophone() = runTest {
        val fake = FakeConnection(); val gate = CompletableDeferred<Unit>()
        val controller = VoiceTutorController(this, { gate.await(); fake }, { testScheduler.currentTime })
        controller.start("practice"); runCurrent(); controller.stop(); gate.complete(Unit); runCurrent()
        assertEquals(0, fake.begun); assertEquals(VoicePhase.PAUSED, controller.state.value.phase)
    }

    @Test fun automaticLimitClosesAudioAndConnection() = runTest {
        val fake = FakeConnection()
        val controller = VoiceTutorController(this, { fake }, { testScheduler.currentTime }, maxSeconds = 1)
        controller.start("practice"); runCurrent(); advanceTimeBy(1250); runCurrent()
        assertEquals(VoicePhase.FINISHED, controller.state.value.phase)
        assertEquals(1, fake.closed); assertTrue(fake.stopped > 0)
        controller.stop(); assertEquals(VoicePhase.FINISHED, controller.state.value.phase)
    }

    @Test fun goingAwayStopsInsteadOfSilentlyReconnecting() = runTest {
        val fake = FakeConnection(); var connects = 0
        val controller = VoiceTutorController(this, { connects++; fake }, { testScheduler.currentTime })
        controller.start("practice"); runCurrent(); fake.away(); runCurrent()
        assertEquals(VoicePhase.FINISHED, controller.state.value.phase)
        assertEquals(1, fake.closed); assertEquals(1, connects)
    }

    @Test fun connectionFailureIsRecoverableWithoutOpeningAudio() = runTest {
        val controller = VoiceTutorController(this, { throw IllegalStateException("unavailable") }, { testScheduler.currentTime })
        controller.start("practice"); runCurrent()
        assertEquals(VoicePhase.ERROR, controller.state.value.phase)
        assertTrue(controller.state.value.error!!.contains("تكلّم وانتظر الرد"))
    }

    @Test fun closedConnectionCleansUpAndReportsFailure() = runTest {
        val fake = FakeConnection()
        val controller = VoiceTutorController(this, { fake }, { testScheduler.currentTime })
        controller.start("practice"); runCurrent(); fake.active = false; advanceTimeBy(500); runCurrent()
        assertEquals(VoicePhase.ERROR, controller.state.value.phase); assertEquals(1, fake.closed)
    }

    @Test fun strictPromptHasSafetyAndLevelBoundaries() {
        val prompt = tutorVoiceInstruction("A2", "British", "Airport", true)
        assertTrue(prompt.contains("A2")); assertTrue(prompt.contains("Airport"))
        assertTrue(prompt.contains("Never insult")); assertTrue(prompt.contains("not claim to be a human"))
    }
}
