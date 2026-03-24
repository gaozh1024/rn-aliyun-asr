package com.aliyunasr;

import com.alibaba.idst.nui.*;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

public class NuiCallbackImpl implements INativeNuiCallback {
    private final AliyunASRModule module;

    public NuiCallbackImpl(AliyunASRModule module) {
        this.module = module;
    }

    @Override
    public void onEventCallback(Constants.NuiEvent event, int resultCode, long dialog, String wuw, 
                                String asrResult, boolean finish, int code, String allResponse) {
        WritableMap params = Arguments.createMap();
        params.putInt("event", event.ordinal());
        params.putInt("errorCode", code);
        params.putBoolean("isFinish", finish);

        // ASR 结果处理
        if (asrResult != null && !asrResult.isEmpty()) {
            WritableMap resultMap = Arguments.createMap();
            resultMap.putString("text", asrResult);
            // 判断是否为最终结果
            boolean isFinal = (event == Constants.NuiEvent.EVENT_ASR_RESULT || 
                              event == Constants.NuiEvent.EVENT_SENTENCE_END);
            resultMap.putBoolean("isFinal", isFinal);
            params.putMap("result", resultMap);
        }

        // 错误信息
        if (code != 0) {
            params.putString("errorMessage", getErrorMessage(code));
        }

        module.sendEvent("onASREvent", params);
    }

    @Override
    public int onAudioDataCallback(byte[] buffer, int len) {
        // 音频数据回调，返回 0 表示成功
        return 0;
    }

    @Override
    public void onAudioStateChanged(Constants.AudioState state) {
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
