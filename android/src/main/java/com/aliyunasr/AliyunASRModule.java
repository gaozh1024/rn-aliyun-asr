package com.aliyunasr;

import androidx.annotation.NonNull;
import com.alibaba.idst.nui.*;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import javax.annotation.Nullable;

public class AliyunASRModule extends ReactContextBaseJavaModule {
    private static final String MODULE_NAME = "AliyunASRModule";

    private NativeNui nativeNui;
    private NuiCallbackImpl callback;
    private final ReactApplicationContext reactContext;

    public AliyunASRModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @NonNull
    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @ReactMethod
    public void initialize(String parameters, int logLevel, boolean saveLog, Promise promise) {
        try {
            nativeNui = new NativeNui();
            callback = new NuiCallbackImpl(this);

            // 转换日志级别
            Constants.LogLevel[] levels = Constants.LogLevel.values();
            int safeLogLevel = Math.max(0, Math.min(logLevel, levels.length - 1));
            Constants.LogLevel level = levels[safeLogLevel];
            
            // 初始化
            int result = nativeNui.nui_initialize(
                parameters, 
                callback, 
                null,  // 外部音频实例（不使用）
                level.ordinal(), 
                saveLog
            );

            if (result == 0) {
                promise.resolve(null);
            } else {
                promise.reject("INIT_ERROR", "初始化失败，错误码: " + result);
            }
        } catch (Exception e) {
            promise.reject("INIT_EXCEPTION", e);
        }
    }

    @ReactMethod
    public void release(Promise promise) {
        if (nativeNui != null) {
            int result = nativeNui.nui_release();
            nativeNui = null;
            callback = null;
            
            if (result == 0) {
                promise.resolve(null);
            } else {
                promise.reject("RELEASE_ERROR", "释放失败，错误码: " + result);
            }
        } else {
            promise.resolve(null);
        }
    }

    @ReactMethod
    public void startDialog(int vadMode, String dialogParams, Promise promise) {
        if (nativeNui == null) {
            promise.reject("NOT_INITIALIZED", "SDK 未初始化");
            return;
        }

        try {
            int result = nativeNui.nui_dialog_start(vadMode, dialogParams);

            if (result == 0) {
                promise.resolve(null);
            } else {
                promise.reject("START_ERROR", "开始识别失败，错误码: " + result);
            }
        } catch (Exception e) {
            promise.reject("START_EXCEPTION", e);
        }
    }

    @ReactMethod
    public void stopDialog(Promise promise) {
        if (nativeNui == null) {
            promise.reject("NOT_INITIALIZED", "SDK 未初始化");
            return;
        }

        try {
            int result = nativeNui.nui_dialog_cancel(false);

            if (result == 0) {
                promise.resolve(null);
            } else {
                promise.reject("STOP_ERROR", "停止识别失败，错误码: " + result);
            }
        } catch (Exception e) {
            promise.reject("STOP_EXCEPTION", e);
        }
    }

    @ReactMethod
    public void cancelDialog(boolean force, Promise promise) {
        if (nativeNui == null) {
            promise.reject("NOT_INITIALIZED", "SDK 未初始化");
            return;
        }

        try {
            int result = nativeNui.nui_dialog_cancel(force);

            if (result == 0) {
                promise.resolve(null);
            } else {
                promise.reject("CANCEL_ERROR", "取消识别失败，错误码: " + result);
            }
        } catch (Exception e) {
            promise.reject("CANCEL_EXCEPTION", e);
        }
    }

    @ReactMethod
    public void setParam(String key, String value, Promise promise) {
        if (nativeNui == null) {
            promise.reject("NOT_INITIALIZED", "SDK 未初始化");
            return;
        }

        try {
            int result = nativeNui.nui_set_param(key, value);

            if (result == 0) {
                promise.resolve(null);
            } else {
                promise.reject("SET_PARAM_ERROR", "设置参数失败，错误码: " + result);
            }
        } catch (Exception e) {
            promise.reject("SET_PARAM_EXCEPTION", e);
        }
    }

    @ReactMethod
    public void getParam(String key, Promise promise) {
        if (nativeNui == null) {
            promise.reject("NOT_INITIALIZED", "SDK 未初始化");
            return;
        }

        try {
            String value = nativeNui.nui_get_param(key);
            promise.resolve(value);
        } catch (Exception e) {
            promise.reject("GET_PARAM_EXCEPTION", e);
        }
    }

    void sendEvent(String eventName, @Nullable WritableMap params) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    ReactApplicationContext getReactContext() {
        return reactContext;
    }

    @ReactMethod
    public void addListener(String eventName) {}

    @ReactMethod
    public void removeListeners(double count) {}
}
