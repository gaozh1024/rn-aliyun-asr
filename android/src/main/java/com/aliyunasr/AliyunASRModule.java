package com.aliyunasr;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.alibaba.idst.nui.*;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import java.io.File;
import org.json.JSONException;
import org.json.JSONObject;
import javax.annotation.Nullable;

public class AliyunASRModule extends ReactContextBaseJavaModule {
    private static final String MODULE_NAME = "AliyunASRModule";
    private static final String LOG_TAG = "AliyunASR";
    private static final String DEFAULT_WORKSPACE_DIR = "aliyun_asr";

    private NativeNui nativeNui;
    private NuiCallbackImpl callback;
    private final ReactApplicationContext reactContext;
    private String lastInitParams = "{}";
    private String lastRecognitionParams = "{}";
    private String lastDialogParams = "{}";
    private int lastVadModeCode = 1;
    private String lastVadModeName = "TYPE_P2T";
    private int currentSampleRate = 16000;
    private AndroidAudioConfig androidAudioConfig = AndroidAudioConfig.defaultConfig();
    private UserAudioRecorder userAudioRecorder;
    private boolean useUserRecorder = false;

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
            nativeNui = NativeNui.GetInstance();
            callback = new NuiCallbackImpl(this);
            String normalizedParameters = normalizeInitParams(parameters);
            String workspacePath = getWorkspacePath(normalizedParameters);
            lastInitParams = normalizedParameters;

            if (!CommonUtils.copyAssetsToExplicitPath(reactContext, workspacePath)) {
                promise.reject("ASSET_COPY_ERROR", "复制 Android 资源失败");
                return;
            }

            Constants.LogLevel[] levels = Constants.LogLevel.values();
            int safeLogLevel = Math.max(0, Math.min(logLevel, levels.length - 1));
            Constants.LogLevel level = levels[safeLogLevel];

            Log.i(
                    LOG_TAG,
                    "initialize sdkVersion=" + nativeNui.getVersion()
                            + ", logLevel=" + level.name()
                            + ", saveLog=" + saveLog
                            + ", params=" + normalizedParameters
            );

            int result = nativeNui.initialize(callback, normalizedParameters, level, saveLog);

            if (result == 240012) {
                Log.w(LOG_TAG, "SDK already initialized, releasing and retrying initialize");
                stopUserRecorder();
                nativeNui.release();
                result = nativeNui.initialize(callback, normalizedParameters, level, saveLog);
            }

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
            stopUserRecorder();
            int result = nativeNui.release();
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
            Constants.VadMode resolvedVadMode = Constants.VadMode.fromInt(vadMode);
            lastVadModeCode = vadMode;
            lastVadModeName = resolvedVadMode.name();
            lastDialogParams = dialogParams == null ? "{}" : dialogParams;

            Log.i(
                    LOG_TAG,
                    "startDialog requestedVadMode=" + vadMode
                            + ", resolvedVadMode=" + resolvedVadMode.name()
                            + ", resolvedVadCode=" + resolvedVadMode.getCode()
                            + ", dialogParams=" + lastDialogParams
                            + ", audioDebug=" + buildAudioDebugSnapshot()
            );

            int result = nativeNui.startDialog(resolvedVadMode, dialogParams);

            if (result == 0) {
                if (useUserRecorder && !startUserRecorder("startDialog")) {
                    nativeNui.cancelDialog();
                    promise.reject("START_ERROR", "开始识别失败，用户录音器启动失败");
                    return;
                }
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
            stopUserRecorder();
            int result = nativeNui.stopDialog();

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
            Log.d(LOG_TAG, "cancelDialog force=" + force);
            stopUserRecorder();
            int result = nativeNui.cancelDialog();

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
            int result = nativeNui.setParam(key, value);

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
    public void setParams(String params, Promise promise) {
        if (nativeNui == null) {
            promise.reject("NOT_INITIALIZED", "SDK 未初始化");
            return;
        }

        try {
            lastRecognitionParams = params;
            updateAudioConfigFromRecognitionParams(params);
            Log.i(LOG_TAG, "setParams params=" + params);
            int result = nativeNui.setParams(params);

            if (result == 0) {
                promise.resolve(null);
            } else {
                promise.reject("SET_PARAMS_ERROR", "批量设置参数失败，错误码: " + result);
            }
        } catch (Exception e) {
            promise.reject("SET_PARAMS_EXCEPTION", e);
        }
    }

    @ReactMethod
    public void getParam(String key, Promise promise) {
        if (nativeNui == null) {
            promise.reject("NOT_INITIALIZED", "SDK 未初始化");
            return;
        }

        try {
            String value = nativeNui.getParam(key);
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

    void onNativeAudioStateChanged(Constants.AudioState state) {
        if (!useUserRecorder) {
            return;
        }

        if (state == Constants.AudioState.STATE_OPEN) {
            startUserRecorder("audioStateOpen");
        } else if (state == Constants.AudioState.STATE_PAUSE || state == Constants.AudioState.STATE_CLOSE) {
            stopUserRecorder();
        }
    }

    int provideAudioData(byte[] buffer, int len) {
        if (!useUserRecorder || userAudioRecorder == null) {
            return 0;
        }

        if (!userAudioRecorder.isActive() && !startUserRecorder("needAudioData")) {
            return 0;
        }

        return userAudioRecorder.read(buffer, len);
    }

    void appendAudioStateDebug(WritableMap params) {
        params.putBoolean("usingUserRecorder", useUserRecorder);
        String currentRecorderSource =
                userAudioRecorder != null ? userAudioRecorder.getCurrentRecorderSourceName() : null;
        if (currentRecorderSource != null) {
            params.putString("currentRecorderSource", currentRecorderSource);
        } else {
            params.putNull("currentRecorderSource");
        }
        params.putInt(
                "recorderState",
                userAudioRecorder != null ? userAudioRecorder.getRecorderState() : AudioRecord.STATE_UNINITIALIZED
        );
        params.putInt(
                "recorderRecordingState",
                userAudioRecorder != null ? userAudioRecorder.getRecorderRecordingState() : AudioRecord.RECORDSTATE_STOPPED
        );
    }

    String buildAudioDebugSnapshot() {
        int bufferSize16k = AudioRecord.getMinBufferSize(
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        int bufferSize8k = AudioRecord.getMinBufferSize(
                8000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        boolean hasRecordAudioPermission =
                ContextCompat.checkSelfPermission(reactContext, Manifest.permission.RECORD_AUDIO)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED;

        return "{"
                + "\"hasRecordAudioPermission\":" + hasRecordAudioPermission
                + ",\"sampleRate16kBufferSize\":" + bufferSize16k
                + ",\"sampleRate8kBufferSize\":" + bufferSize8k
                + ",\"manufacturer\":\"" + Build.MANUFACTURER + "\""
                + ",\"brand\":\"" + Build.BRAND + "\""
                + ",\"model\":\"" + Build.MODEL + "\""
                + ",\"useUserRecorder\":" + useUserRecorder
                + ",\"lastVadModeCode\":" + lastVadModeCode
                + ",\"lastVadModeName\":\"" + lastVadModeName + "\""
                + ",\"androidAudioConfig\":" + JSONObject.quote(androidAudioConfig.toDebugString())
                + ",\"userAudioRecorder\":" + JSONObject.quote(userAudioRecorder != null ? userAudioRecorder.getDebugSnapshot() : "{}")
                + ",\"lastRecognitionParams\":" + JSONObject.quote(lastRecognitionParams)
                + ",\"lastInitParams\":" + JSONObject.quote(lastInitParams)
                + ",\"lastDialogParams\":" + JSONObject.quote(lastDialogParams)
                + "}";
    }

    private String normalizeInitParams(String parameters) throws JSONException {
        JSONObject params = new JSONObject(parameters);
        JSONObject audioConfigJson = params.optJSONObject("android_audio_config");
        androidAudioConfig = AndroidAudioConfig.fromJson(audioConfigJson);
        params.remove("android_audio_config");
        currentSampleRate = params.optInt("sample_rate", 16000);

        boolean explicitEnableRecorderByUser = params.optBoolean("enable_recorder_by_user", false);
        useUserRecorder = explicitEnableRecorderByUser || androidAudioConfig.shouldUseUserRecorder();
        params.put("enable_recorder_by_user", useUserRecorder);
        userAudioRecorder = useUserRecorder
                ? new UserAudioRecorder(androidAudioConfig, currentSampleRate)
                : null;

        String workspacePath = getWorkspacePath(params.toString());
        params.put("workspace", workspacePath);
        params.put("debug_path", workspacePath);

        Log.i(
                LOG_TAG,
                "normalizeInitParams useUserRecorder=" + useUserRecorder
                        + ", sampleRate=" + currentSampleRate
                        + ", androidAudioConfig=" + androidAudioConfig.toDebugString()
        );
        return params.toString();
    }

    private boolean startUserRecorder(String reason) {
        if (!useUserRecorder || nativeNui == null || userAudioRecorder == null) {
            return true;
        }

        if (userAudioRecorder.isActive()) {
            return true;
        }

        userAudioRecorder.updateSampleRate(currentSampleRate);
        boolean started = userAudioRecorder.start();
        Log.i(
                LOG_TAG,
                "startUserRecorder reason=" + reason
                        + ", started=" + started
                        + ", audioDebug=" + buildAudioDebugSnapshot()
        );
        return started;
    }

    private void stopUserRecorder() {
        if (userAudioRecorder != null) {
            userAudioRecorder.stop();
        }
    }

    private void updateAudioConfigFromRecognitionParams(String params) {
        try {
            JSONObject json = new JSONObject(params);
            JSONObject nlsConfig = json.optJSONObject("nls_config");
            if (nlsConfig != null && nlsConfig.has("sample_rate")) {
                currentSampleRate = nlsConfig.optInt("sample_rate", currentSampleRate);
                if (userAudioRecorder != null) {
                    userAudioRecorder.updateSampleRate(currentSampleRate);
                }
            }
        } catch (JSONException e) {
            Log.w(LOG_TAG, "parse recognition params failed: " + params, e);
        }
    }

    private String getWorkspacePath(String parameters) throws JSONException {
        JSONObject params = new JSONObject(parameters);
        if (params.has("workspace")) {
            String workspace = params.optString("workspace");
            if (workspace != null && !workspace.isEmpty()) {
                ensureDirectory(workspace);
                return workspace;
            }
        }

        if (params.has("debug_path")) {
            String debugPath = params.optString("debug_path");
            if (debugPath != null && !debugPath.isEmpty()) {
                ensureDirectory(debugPath);
                return debugPath;
            }
        }

        File workspaceDir = new File(reactContext.getFilesDir(), DEFAULT_WORKSPACE_DIR);
        ensureDirectory(workspaceDir.getAbsolutePath());
        return workspaceDir.getAbsolutePath();
    }

    private void ensureDirectory(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    @ReactMethod
    public void addListener(String eventName) {}

    @ReactMethod
    public void removeListeners(double count) {}
}
