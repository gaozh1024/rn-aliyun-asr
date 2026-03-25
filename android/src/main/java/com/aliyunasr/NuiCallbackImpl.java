package com.aliyunasr;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;
import androidx.core.content.ContextCompat;
import com.alibaba.idst.nui.*;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

public class NuiCallbackImpl implements INativeNuiCallback {
    private static final String LOG_TAG = "AliyunASR";
    private final AliyunASRModule module;

    public NuiCallbackImpl(AliyunASRModule module) {
        this.module = module;
    }

    @Override
    public void onNuiEventCallback(
            Constants.NuiEvent event,
            int resultCode,
            int dialogId,
            KwsResult kwsResult,
            AsrResult asrResult
    ) {
        WritableMap params = Arguments.createMap();
        params.putInt("event", event.ordinal());
        params.putString("eventName", event.name());
        params.putInt("dialogId", dialogId);

        if (kwsResult != null && kwsResult.kws != null && !kwsResult.kws.isEmpty()) {
            params.putString("wakeWord", kwsResult.kws);
        }

        int errorCode = resultCode;

        if (asrResult != null) {
            errorCode = asrResult.resultCode;
            params.putBoolean("isFinish", asrResult.finish);

            WritableMap resultMap = Arguments.createMap();
            resultMap.putString("text", asrResult.asrResult);
            boolean isFinal = event == Constants.NuiEvent.EVENT_ASR_RESULT
                    || event == Constants.NuiEvent.EVENT_SENTENCE_END
                    || asrResult.finish;
            resultMap.putBoolean("isFinal", isFinal);
            params.putMap("result", resultMap);
        }

        params.putInt("errorCode", errorCode);

        if (errorCode != 0) {
            params.putString("errorMessage", getErrorMessage(errorCode));
        }

        Log.d(
                LOG_TAG,
                "event=" + event.name()
                        + ", ordinal=" + event.ordinal()
                        + ", resultCode=" + resultCode
                        + ", dialogId=" + dialogId
                        + ", finish=" + (asrResult != null && asrResult.finish)
                        + ", text=" + (asrResult != null ? asrResult.asrResult : "")
        );

        module.sendEvent("onASREvent", params);
    }

    @Override
    public int onNuiNeedAudioData(byte[] buffer, int len) {
        int read = module.provideAudioData(buffer, len);
        Log.d(LOG_TAG, "onNuiNeedAudioData len=" + len + ", read=" + read);
        return read;
    }

    @Override
    public void onNuiAudioStateChanged(Constants.AudioState state) {
        WritableMap params = Arguments.createMap();
        params.putString("type", "audioState");
        params.putInt("state", state.ordinal());
        params.putString("stateName", state.name());
        params.putInt(
                "sampleRate16kBufferSize",
                AudioRecord.getMinBufferSize(
                        16000,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                )
        );
        params.putInt(
                "sampleRate8kBufferSize",
                AudioRecord.getMinBufferSize(
                        8000,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                )
        );
        params.putBoolean(
                "hasRecordAudioPermission",
                ContextCompat.checkSelfPermission(
                        module.getReactContext(),
                        Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
        );
        module.onNativeAudioStateChanged(state);
        module.appendAudioStateDebug(params);
        Log.i(
                LOG_TAG,
                "audioState=" + state.name() + ", audioDebug=" + module.buildAudioDebugSnapshot()
        );
        module.sendEvent("onASRAudioState", params);
    }

    @Override
    public void onNuiAudioRMSChanged(float rms) {}

    @Override
    public void onNuiVprEventCallback(Constants.NuiVprEvent event) {}

    private String getErrorMessage(int code) {
        switch (code) {
            case 240001: return "配置或文件无效";
            case 240002: return "参数非法";
            case 240011: return "SDK 未初始化";
            case 240012: return "SDK 已初始化";
            case 240052: return "麦克风错误";
            case 240070: return "认证失败";
            case 240091: return "连接超时";
            case 240093: return "识别超时";
            default: return "未知错误: " + code;
        }
    }
}
