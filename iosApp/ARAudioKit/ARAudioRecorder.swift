import Foundation
import AVFoundation

@objcMembers
class ARRecordingResult: NSObject {
    let url: URL
    let durationMs: Int64
    let bytes: Int64

    init(url: URL, durationMs: Int64, bytes: Int64) {
        self.url = url
        self.durationMs = durationMs
        self.bytes = bytes
    }
}

@objcMembers
class ARAudioRecorder: NSObject {
    private var audioSession: AVAudioSession = .sharedInstance()
    private var recorder: AVAudioRecorder?
    private var startedAt: Date?
    private var accumulatedMs: Int64 = 0
    private var activeStart: Date?
    private(set) var isPaused: Bool = false
    private var outputURL: URL?

    /// Start a new recording; returns the output file URL.
    func start(sampleRate: Int, bitRate: Int) throws -> URL {
        if recorder != nil { throw NSError(domain: "ARAudioRecorder", code: 1, userInfo: [NSLocalizedDescriptionKey: "Recorder already running"]) }

        // Configure session (baseline for C1; category may be revisited in C2)
        try audioSession.setCategory(.record, mode: .default, options: [])
        try audioSession.setPreferredSampleRate(Double(sampleRate))
        try audioSession.setActive(true)

        let caches = try FileManager.default.url(for: .cachesDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
        let dir = caches.appendingPathComponent("recordings", isDirectory: true)
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMdd_HHmmss"
        let name = formatter.string(from: Date())
        let url = dir.appendingPathComponent("REC_\(name).m4a")
        outputURL = url

        let settings: [String: Any] = [
            AVFormatIDKey: kAudioFormatMPEG4AAC,
            AVSampleRateKey: sampleRate,
            AVNumberOfChannelsKey: 1,
            AVEncoderBitRateKey: bitRate
        ]

        recorder = try AVAudioRecorder(url: url, settings: settings)
        guard let r = recorder else { throw NSError(domain: "ARAudioRecorder", code: 2, userInfo: [NSLocalizedDescriptionKey: "Failed to create AVAudioRecorder"]) }
        r.isMeteringEnabled = false
        r.record()

        startedAt = Date()
        accumulatedMs = 0
        activeStart = startedAt
        isPaused = false
        return url
    }

    /// Pause the recording (no-op if already paused)
    func pause() {
        guard let r = recorder, r.isRecording else { return }
        r.pause()
        if let active = activeStart {
            accumulatedMs += Int64(Date().timeIntervalSince(active) * 1000)
        }
        activeStart = nil
        isPaused = true
    }

    /// Resume a paused recording
    func resume() throws {
        guard let r = recorder else { throw NSError(domain: "ARAudioRecorder", code: 3, userInfo: [NSLocalizedDescriptionKey: "No active recorder"]) }
        if r.isRecording { return }
        let ok = r.record()
        if !ok { throw NSError(domain: "ARAudioRecorder", code: 4, userInfo: [NSLocalizedDescriptionKey: "Failed to resume recording"]) }
        activeStart = Date()
        isPaused = false
    }

    /// Stop the recording and return the result.
    func stop() -> ARRecordingResult {
        // Finalize elapsed time and stop the recorder before reading file size
        if let r = recorder {
            if r.isRecording, let active = activeStart {
                accumulatedMs += Int64(Date().timeIntervalSince(active) * 1000)
            }
            r.stop()
        }

        let url = outputURL ?? recorder?.url ?? URL(fileURLWithPath: "")
        let attributes = try? FileManager.default.attributesOfItem(atPath: url.path)
        let bytes: Int64 = (attributes?[.size] as? NSNumber)?.int64Value ?? 0

        // Cleanup session and internal state
        recorder = nil
        activeStart = nil
        isPaused = false
        try? audioSession.setActive(false, options: .notifyOthersOnDeactivation)

        return ARRecordingResult(url: url, durationMs: accumulatedMs, bytes: bytes)
    }

    /// Current elapsed time in ms (approximate)
    func currentElapsedMs() -> Int64 {
        if let start = activeStart, recorder?.isRecording == true {
            let nowMs = Int64(Date().timeIntervalSince(start) * 1000)
            return accumulatedMs + max(0, nowMs)
        }
        return accumulatedMs
    }
}
