package com.aliyunasr;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

class UserAudioRecorder {
    private static final String LOG_TAG = "AliyunASR";
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int MAX_CONSECUTIVE_READ_FAILURES = 5;

    private final AndroidAudioConfig audioConfig;

    private AudioRecord audioRecord;
    private List<Integer> candidateSources = new ArrayList<>();
    private int currentSourceIndex = -1;
    private int currentAudioSource = -1;
    private int sampleRate = 16000;
    private int bufferSizeInBytes = 0;
    private int consecutiveReadFailures = 0;

    UserAudioRecorder(AndroidAudioConfig audioConfig, int sampleRate) {
        this.audioConfig = audioConfig;
        this.sampleRate = sampleRate;
    }

    synchronized void updateSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    synchronized boolean start() {
        stop();
        consecutiveReadFailures = 0;
        candidateSources = audioConfig.resolveAudioSources();
        currentSourceIndex = -1;

        if (!openNextAudioSource()) {
            Log.e(LOG_TAG, "user recorder start failed: no available audio source");
            return false;
        }

        return true;
    }

    synchronized void stop() {
        consecutiveReadFailures = 0;
        releaseCurrentRecorder();
    }

    synchronized boolean isActive() {
        return audioRecord != null
                && audioRecord.getState() == AudioRecord.STATE_INITIALIZED
                && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING;
    }

    synchronized String getCurrentRecorderSourceName() {
        return currentAudioSource == -1 ? null : AndroidAudioConfig.sourceToName(currentAudioSource);
    }

    synchronized int getRecorderState() {
        return audioRecord == null ? AudioRecord.STATE_UNINITIALIZED : audioRecord.getState();
    }

    synchronized int getRecorderRecordingState() {
        return audioRecord == null ? AudioRecord.RECORDSTATE_STOPPED : audioRecord.getRecordingState();
    }

    synchronized String getDebugSnapshot() {
        return "{"
                + "\"active\":" + isActive()
                + ",\"sampleRate\":" + sampleRate
                + ",\"bufferSizeInBytes\":" + bufferSizeInBytes
                + ",\"currentAudioSource\":\"" + getCurrentRecorderSourceName() + "\""
                + ",\"recorderState\":" + getRecorderState()
                + ",\"recordingState\":" + getRecorderRecordingState()
                + "}";
    }

    int read(byte[] buffer, int len) {
        int targetLength = Math.min(len, buffer.length);

        while (true) {
            AudioRecord currentRecorder = getCurrentRecorder();
            if (currentRecorder == null) {
                return 0;
            }

            int read;
            try {
                read = currentRecorder.read(buffer, 0, targetLength);
            } catch (IllegalStateException e) {
                Log.w(LOG_TAG, "AudioRecord read threw IllegalStateException", e);
                read = AudioRecord.ERROR_INVALID_OPERATION;
            }

            if (read > 0) {
                synchronized (this) {
                    consecutiveReadFailures = 0;
                }
                return read;
            }

            boolean switched = false;
            synchronized (this) {
                consecutiveReadFailures++;
                Log.w(
                        LOG_TAG,
                        "AudioRecord read failed, source=" + getCurrentRecorderSourceName()
                                + ", read=" + read
                                + ", consecutiveFailures=" + consecutiveReadFailures
                );

                if (consecutiveReadFailures >= MAX_CONSECUTIVE_READ_FAILURES) {
                    consecutiveReadFailures = 0;
                    switched = switchToNextAudioSource();
                    if (!switched) {
                        Log.e(LOG_TAG, "AudioRecord fallback exhausted, cannot provide more audio");
                    }
                }
            }

            if (!switched) {
                return 0;
            }
        }
    }

    private synchronized AudioRecord getCurrentRecorder() {
        return audioRecord;
    }

    private synchronized boolean switchToNextAudioSource() {
        releaseCurrentRecorder();
        return openNextAudioSource();
    }

    private synchronized boolean openNextAudioSource() {
        while (++currentSourceIndex < candidateSources.size()) {
            int source = candidateSources.get(currentSourceIndex);
            AudioRecord candidate = createAudioRecord(source);
            if (candidate != null) {
                audioRecord = candidate;
                currentAudioSource = source;
                return true;
            }
        }

        return false;
    }

    private AudioRecord createAudioRecord(int audioSource) {
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT);
        int desiredBufferSize = Math.max(minBufferSize, (sampleRate / 10) * 2);

        Log.i(
                LOG_TAG,
                "try AudioRecord source=" + AndroidAudioConfig.sourceToName(audioSource)
                        + ", sampleRate=" + sampleRate
                        + ", channelConfig=" + CHANNEL_CONFIG
                        + ", audioFormat=" + AUDIO_FORMAT
                        + ", minBufferSize=" + minBufferSize
                        + ", desiredBufferSize=" + desiredBufferSize
        );

        if (minBufferSize <= 0) {
            Log.w(LOG_TAG, "AudioRecord minBufferSize invalid: " + minBufferSize);
            return null;
        }

        AudioRecord candidate;
        try {
            candidate = new AudioRecord(
                    audioSource,
                    sampleRate,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    desiredBufferSize
            );
        } catch (IllegalArgumentException e) {
            Log.w(LOG_TAG, "AudioRecord init failed for source=" + AndroidAudioConfig.sourceToName(audioSource), e);
            return null;
        }

        Log.i(
                LOG_TAG,
                "AudioRecord init source=" + AndroidAudioConfig.sourceToName(audioSource)
                        + ", state=" + candidate.getState()
                        + ", recordingState=" + candidate.getRecordingState()
        );

        if (candidate.getState() != AudioRecord.STATE_INITIALIZED) {
            candidate.release();
            return null;
        }

        try {
            candidate.startRecording();
        } catch (IllegalStateException e) {
            Log.w(LOG_TAG, "AudioRecord startRecording failed for source=" + AndroidAudioConfig.sourceToName(audioSource), e);
            candidate.release();
            return null;
        }

        Log.i(
                LOG_TAG,
                "AudioRecord started source=" + AndroidAudioConfig.sourceToName(audioSource)
                        + ", state=" + candidate.getState()
                        + ", recordingState=" + candidate.getRecordingState()
        );

        if (candidate.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            candidate.release();
            return null;
        }

        bufferSizeInBytes = desiredBufferSize;
        return candidate;
    }

    private synchronized void releaseCurrentRecorder() {
        if (audioRecord == null) {
            currentAudioSource = -1;
            bufferSizeInBytes = 0;
            return;
        }

        try {
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop();
            }
        } catch (IllegalStateException e) {
            Log.w(LOG_TAG, "AudioRecord stop failed", e);
        }

        audioRecord.release();
        audioRecord = null;
        currentAudioSource = -1;
        bufferSizeInBytes = 0;
    }
}
