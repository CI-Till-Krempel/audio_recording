package de.cologneintelligence.audio_recording.recorder

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeRecorderTest {

    private var recorder: FakeRecorder? = null

    @AfterTest
    fun tearDown() {
        recorder?.close()
    }

    @Test
    fun startFromIdle_succeeds_and_recordsElapsed() = runBlocking {
        val r = FakeRecorder().also { recorder = it }
        val res = r.start(RecordingConfig())
        assertNull(res.exceptionOrNull(), "start() should succeed from Idle")
        assertTrue(r.state.value is RecordingState.Recording)

        delay(220) // ticker period is 100ms; wait for ~2 ticks
        val after = r.state.value
        assertTrue(after is RecordingState.Recording)
        assertTrue(after.elapsedMs >= 150, "elapsed should advance while recording, was ${if (after is RecordingState.Recording) after.elapsedMs else -1}")
    }

    @Test
    fun pause_resume_cycle_behaves_and_elapsed_rules_hold() = runBlocking {
        val r = FakeRecorder().also { recorder = it }
        r.start(RecordingConfig())
        delay(150)
        val pauseRes = r.pause()
        assertNull(pauseRes.exceptionOrNull(), "pause() should succeed from Recording")
        val paused = r.state.value as RecordingState.Paused
        val pausedElapsed = paused.elapsedMs
        delay(200)
        val stillPaused = r.state.value as RecordingState.Paused
        assertEquals(pausedElapsed, stillPaused.elapsedMs, "elapsed should freeze while Paused")

        val resumeRes = r.resume()
        assertNull(resumeRes.exceptionOrNull(), "resume() should succeed from Paused")
        delay(150)
        val rec = r.state.value as RecordingState.Recording
        assertTrue(rec.elapsedMs > pausedElapsed, "elapsed should increase after resume")
    }

    @Test
    fun stop_fromRecording_returnsResult_and_setsIdle() = runBlocking {
        val r = FakeRecorder().also { recorder = it }
        r.start(RecordingConfig())
        delay(180)
        val result = r.stop()
        val rr = result.getOrNull()
        assertNotNull(rr, "stop() should succeed from Recording")
        assertTrue(rr.durationMs >= 100, "duration should reflect elapsed time")
        assertTrue(r.state.value is RecordingState.Idle, "state should be Idle after stop()")
    }

    @Test
    fun stop_fromIdle_fails() = runBlocking {
        val r = FakeRecorder().also { recorder = it }
        val res = r.stop()
        assertNotNull(res.exceptionOrNull(), "stop() from Idle should fail")
        assertTrue(r.state.value is RecordingState.Idle)
    }

    @Test
    fun invalidTransitions_fail_and_do_not_change_state() = runBlocking {
        val r = FakeRecorder().also { recorder = it }
        // pause from Idle
        assertNotNull(r.pause().exceptionOrNull(), "pause() from Idle must fail")
        assertTrue(r.state.value is RecordingState.Idle)

        // resume from Idle
        assertNotNull(r.resume().exceptionOrNull(), "resume() from Idle must fail")
        assertTrue(r.state.value is RecordingState.Idle)

        // start and then start again
        assertNull(r.start(RecordingConfig()).exceptionOrNull())
        assertTrue(r.state.value is RecordingState.Recording)
        assertNotNull(r.start(RecordingConfig()).exceptionOrNull(), "start() while Recording must fail")
        assertTrue(r.state.value is RecordingState.Recording)

        // resume while Recording
        assertNotNull(r.resume().exceptionOrNull(), "resume() while Recording must fail")
        assertTrue(r.state.value is RecordingState.Recording)

        // pause and then pause again
        assertNull(r.pause().exceptionOrNull())
        assertTrue(r.state.value is RecordingState.Paused)
        assertNotNull(r.pause().exceptionOrNull(), "pause() while Paused must fail")
        assertTrue(r.state.value is RecordingState.Paused)
    }
}
