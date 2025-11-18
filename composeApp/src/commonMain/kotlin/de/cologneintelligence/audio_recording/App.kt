package de.cologneintelligence.audio_recording

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.cologneintelligence.audio_recording.recorder.FakeRecorder
import de.cologneintelligence.audio_recording.recorder.RecordingResult
import de.cologneintelligence.audio_recording.recorder.RecordingState
import kotlinx.coroutines.launch

@Composable
fun App() {
    MaterialTheme {
        val recorder = remember { FakeRecorder() }
        val state by recorder.state.collectAsState()
        val scope = rememberCoroutineScope()
        var lastResult by remember { mutableStateOf<RecordingResult?>(null) }
        var errorText by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text("Audio Recorder (Spike)", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            // Elapsed time
            val elapsed = when (val s = state) {
                is RecordingState.Recording -> s.elapsedMs
                is RecordingState.Paused -> s.elapsedMs
                else -> 0L
            }
            Text("Elapsed: ${formatElapsed(elapsed)}")
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.Center) {
                Button(
                    onClick = {
                        scope.launch {
                            errorText = null
                            val result = recorder.start(config = de.cologneintelligence.audio_recording.recorder.RecordingConfig())
                            result.exceptionOrNull()?.let { errorText = it.message }
                        }
                    },
                    enabled = state is RecordingState.Idle || state is RecordingState.Error
                ) { Text("Record") }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            errorText = null
                            val res = if (state is RecordingState.Recording) recorder.pause() else recorder.resume()
                            res.exceptionOrNull()?.let { errorText = it.message }
                        }
                    },
                    enabled = (state is RecordingState.Recording || state is RecordingState.Paused) && recorder.capabilities.supportsPause
                ) { Text(if (state is RecordingState.Recording) "Pause" else "Resume") }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            errorText = null
                            val result = recorder.stop()
                            result.onSuccess { lastResult = it }
                            result.exceptionOrNull()?.let { errorText = it.message }
                        }
                    },
                    enabled = state is RecordingState.Recording || state is RecordingState.Paused
                ) { Text("Stop") }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.Center) {
                Button(
                    onClick = { /* TODO(A3): implement playback for lastResult */ },
                    enabled = lastResult != null
                ) { Text("Play") }
            }

            Spacer(Modifier.height(12.dp))
            lastResult?.let { res ->
                Text("Last file: ${res.uri}")
                Text("Duration: ${formatElapsed(res.durationMs)}  Size: ${res.bytes} bytes")
            }

            // Error surface
            val errorStateText = when (val s = state) {
                is RecordingState.Error -> s.message
                else -> null
            }
            val combinedError = errorText ?: errorStateText
            if (combinedError != null) {
                Spacer(Modifier.height(12.dp))
                Text("Error: $combinedError", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val mm = minutes.toString()
    val ss = seconds.toString().padStart(2, '0')
    return "$mm:$ss"
}