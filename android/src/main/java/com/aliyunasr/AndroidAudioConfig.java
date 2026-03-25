package com.aliyunasr;

import android.media.MediaRecorder;
import android.os.Build;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

class AndroidAudioConfig {
    private static final String STRATEGY_SDK = "sdk";
    private static final String STRATEGY_USER = "user";
    private static final String STRATEGY_AUTO = "auto";

    private final String recorderStrategy;
    private final String recorderSource;
    private final List<String> recorderSourceFallbacks;
    private final boolean huaweiCompatibility;

    private AndroidAudioConfig(
            String recorderStrategy,
            String recorderSource,
            List<String> recorderSourceFallbacks,
            boolean huaweiCompatibility
    ) {
        this.recorderStrategy = recorderStrategy;
        this.recorderSource = recorderSource;
        this.recorderSourceFallbacks = recorderSourceFallbacks;
        this.huaweiCompatibility = huaweiCompatibility;
    }

    static AndroidAudioConfig fromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return defaultConfig();
        }

        String strategy = normalizeStrategy(jsonObject.optString("recorderStrategy", STRATEGY_AUTO));
        String source = normalizeSourceName(jsonObject.optString("recorderSource", ""));
        boolean huaweiCompat = jsonObject.optBoolean("huaweiCompatibility", true);

        List<String> fallbacks = new ArrayList<>();
        JSONArray fallbackArray = jsonObject.optJSONArray("recorderSourceFallbacks");
        if (fallbackArray != null) {
            for (int i = 0; i < fallbackArray.length(); i++) {
                String fallback = normalizeSourceName(fallbackArray.optString(i, ""));
                if (!fallback.isEmpty()) {
                    fallbacks.add(fallback);
                }
            }
        }

        return new AndroidAudioConfig(strategy, source, fallbacks, huaweiCompat);
    }

    static AndroidAudioConfig defaultConfig() {
        return new AndroidAudioConfig(STRATEGY_AUTO, "", new ArrayList<String>(), true);
    }

    boolean shouldUseUserRecorder() {
        if (STRATEGY_USER.equals(recorderStrategy)) {
            return true;
        }
        if (STRATEGY_SDK.equals(recorderStrategy)) {
            return false;
        }
        return huaweiCompatibility && isHuaweiDevice();
    }

    List<Integer> resolveAudioSources() {
        LinkedHashSet<Integer> sources = new LinkedHashSet<>();

        if (!recorderSource.isEmpty()) {
            sources.add(toAudioSourceConstant(recorderSource));
        }

        for (String fallback : recorderSourceFallbacks) {
            sources.add(toAudioSourceConstant(fallback));
        }

        if (sources.isEmpty()) {
            if (huaweiCompatibility && isHuaweiDevice()) {
                sources.add(MediaRecorder.AudioSource.MIC);
                sources.add(MediaRecorder.AudioSource.VOICE_RECOGNITION);
                sources.add(MediaRecorder.AudioSource.DEFAULT);
                sources.add(MediaRecorder.AudioSource.CAMCORDER);
            } else {
                sources.add(MediaRecorder.AudioSource.VOICE_RECOGNITION);
                sources.add(MediaRecorder.AudioSource.MIC);
                sources.add(MediaRecorder.AudioSource.DEFAULT);
                sources.add(MediaRecorder.AudioSource.CAMCORDER);
            }
        }

        return new ArrayList<>(sources);
    }

    String toDebugString() {
        return "{"
                + "\"recorderStrategy\":\"" + recorderStrategy + "\""
                + ",\"recorderSource\":\"" + recorderSource + "\""
                + ",\"recorderSourceFallbacks\":\"" + recorderSourceFallbacks + "\""
                + ",\"huaweiCompatibility\":" + huaweiCompatibility
                + ",\"manufacturer\":\"" + safe(Build.MANUFACTURER) + "\""
                + ",\"brand\":\"" + safe(Build.BRAND) + "\""
                + ",\"model\":\"" + safe(Build.MODEL) + "\""
                + "}";
    }

    static String sourceToName(int audioSource) {
        switch (audioSource) {
            case MediaRecorder.AudioSource.VOICE_RECOGNITION:
                return "voiceRecognition";
            case MediaRecorder.AudioSource.MIC:
                return "mic";
            case MediaRecorder.AudioSource.DEFAULT:
                return "default";
            case MediaRecorder.AudioSource.CAMCORDER:
                return "camcorder";
            default:
                return "unknown(" + audioSource + ")";
        }
    }

    private static int toAudioSourceConstant(String sourceName) {
        switch (sourceName) {
            case "voiceRecognition":
                return MediaRecorder.AudioSource.VOICE_RECOGNITION;
            case "mic":
                return MediaRecorder.AudioSource.MIC;
            case "camcorder":
                return MediaRecorder.AudioSource.CAMCORDER;
            case "default":
            default:
                return MediaRecorder.AudioSource.DEFAULT;
        }
    }

    private static String normalizeStrategy(String strategy) {
        if (STRATEGY_SDK.equals(strategy) || STRATEGY_USER.equals(strategy)) {
            return strategy;
        }
        return STRATEGY_AUTO;
    }

    private static String normalizeSourceName(String source) {
        if ("voiceRecognition".equals(source)
                || "mic".equals(source)
                || "default".equals(source)
                || "camcorder".equals(source)) {
            return source;
        }
        return "";
    }

    private static boolean isHuaweiDevice() {
        String manufacturer = safe(Build.MANUFACTURER).toLowerCase();
        String brand = safe(Build.BRAND).toLowerCase();
        return manufacturer.contains("huawei")
                || manufacturer.contains("honor")
                || brand.contains("huawei")
                || brand.contains("honor");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
