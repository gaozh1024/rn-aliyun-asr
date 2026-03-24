package com.aliyunasr;

import com.alibaba.nuisdk.*;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

public class NuiSdkListenerImpl extends NuiSdkListener {
    private final AliyunASRModule module;

    public NuiSdkListenerImpl(AliyunASRModule module) {
        this.module = module;
    }

    @Override
    public void onEventCallback(NuiCallbackEvent event, long dialog, String wuw, 
                                String asrResult, boolean finish, int code, String allResponse) {
        WritableMap params = Arguments.createMap();
        params.putInt("event", event.ordinal());
        params.putDouble("dialogId", dialog);
        params.putInt("errorCode", code);
        params.putBoolean("isFinish", finish);

        if (asrResult != null && !asrResult.isEmpty()) {
            WritableMap resultMap = Arguments.createMap();
            resultMap.putString("text", asrResult);
            resultMap.putBoolean("isFinal", 
                event == NuiCallbackEvent.EVENT_ASR_RESULT || 
                event == NuiCallbackEvent.EVENT_SENTENCE_END);
            params.putMap("result", resultMap);
        }

        if (code != 0) {
            params.putString("errorMessage", getErrorMessage(code));
        }

        module.sendEvent("onASREvent", params);
    }

    @Override
    public int onUserDataCallback(byte[] buffer, int len) {
        return 0;
    }

    @Override
    public void onAudioStateChanged(NuiAudioState state) {
        WritableMap params = Arguments.createMap();
        params.putString("type", "audioState");
        params.putInt("state", state.ordinal());
        module.sendEvent("onASRAudioState", params);
    }

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
