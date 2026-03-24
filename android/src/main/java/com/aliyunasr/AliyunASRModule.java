package com.aliyunasr;

import androidx.annotation.NonNull;
import com.alibaba.nuisdk.*;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import javax.annotation.Nullable;

public class AliyunASRModule extends ReactContextBaseJavaModule {
    private static final String MODULE_NAME = "AliyunASRModule";

    private NuiSdk nuiSdk;
    private NuiSdkListenerImpl listener;
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
            nuiSdk = new NuiSdk();
            listener = new NuiSdkListenerImpl(this);

            NuiSdkLogLevel level = NuiSdkLogLevel.values()[logLevel];
            NuiResultCode result = nuiSdk.nui_initialize(
                parameters, 
                listener, 
                null, 
                level, 
                saveLog
            );

            if (result == NuiResultCode.SUCCESS) {
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
        if (nuiSdk != null) {
            NuiResultCode result = nuiSdk.nui_release();
            nuiSdk = null;
            listener = null;
            
            if (result == NuiResultCode.SUCCESS) {
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
        if (nuiSdk == null) {
            promise.reject("NOT_INITIALIZED", "SDK 未初始化");
            return;
        }

        NuiVadMode mode = NuiVadMode.values()[vadMode];
        NuiResultCode result = nuiSdk.nui_dialog_start(mode, dialogParams);

        if (result == NuiResultCode.SUCCESS) {
            promise.resolve(null);
        } else {
            promise.reject("START_ERROR", "开始识别失败，错误码: " + result);
        }
    }

    @ReactMethod
    public void stopDialog(Promise promise) {
        if (nuiSdk == null) {
            promise.reject("NOT_INITIALIZED", "SDK 未初始化");
            return;
        }

        NuiResultCode result = nuiSdk.nui_dialog_cancel(false);

        if (result == NuiResultCode.SUCCESS) {
            promise.resolve(null);
        } else {
            promise.reject("STOP_ERROR", "停止识别失败，错误码: " + result);
        }
    }

    @ReactMethod
    public void cancelDialog(boolean force, Promise promise) {
        if (nuiSdk == null) {
            promise.reject("NOT_INITIALIZED", "SDK 未初始化");
            return;
        }

        NuiResultCode result = nuiSdk.nui_dialog_cancel(force);

        if (result == NuiResultCode.SUCCESS) {
            promise.resolve(null);
        } else {
            promise.reject("CANCEL_ERROR", "取消识别失败，错误码: " + result);
        }
    }

    @ReactMethod
    public void setParam(String key, String value, Promise promise) {
        if (nuiSdk == null) {
            promise.reject("NOT_INITIALIZED", "SDK 未初始化");
            return;
        }

        NuiResultCode result = nuiSdk.nui_set_param(key, value);

        if (result == NuiResultCode.SUCCESS) {
            promise.resolve(null);
        } else {
            promise.reject("SET_PARAM_ERROR", "设置参数失败，错误码: " + result);
        }
    }

    @ReactMethod
    public void getParam(String key, Promise promise) {
        if (nuiSdk == null) {
            promise.reject("NOT_INITIALIZED", "SDK 未初始化");
            return;
        }

        String value = nuiSdk.nui_get_param(key);
        promise.resolve(value);
    }

    void sendEvent(String eventName, @Nullable WritableMap params) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    ReactApplicationContext getReactContext() {
        return reactContext;
    }
}
