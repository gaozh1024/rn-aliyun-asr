# Android 工程师开发指南

## 角色定位
负责 Android 原生模块开发，包括 SDK 集成、Java 代码实现和配置。

## Day 2 详细任务

### 09:00-10:00 环境准备

**前置条件：** TS 层已完成并合并到 main

```bash
# 1. 克隆仓库
git clone https://github.com/gaozh1024/rn-aliyun-asr.git
cd rn-aliyun-asr

# 2. 创建分支
git checkout -b feat/android-module

# 3. 创建目录
mkdir -p android/src/main/java/com/aliyunasr
mkdir -p android/src/main/assets
mkdir -p android/libs
```

---

### 10:00-10:30 复制 SDK 文件

**来源：** `docs/android/`

```bash
# 复制 AAR 包
cp docs/android/RelWithDebugInfo/nuisdk-release.aar android/libs/

# 复制资源文件（仅 ASR 必需）
cp docs/android/resources/nui.json android/src/main/assets/
cp docs/android/resources/cei.json android/src/main/assets/
cp docs/android/resources/vad.bin android/src/main/assets/

# 确认文件
cd android
find . -type f
```

**检查清单：**
- [ ] `android/libs/nuisdk-release.aar` 存在
- [ ] `android/src/main/assets/nui.json` 存在
- [ ] `android/src/main/assets/cei.json` 存在
- [ ] `android/src/main/assets/vad.bin` 存在

```bash
git add android/libs/ android/src/main/assets/
git commit -m "chore: add Android SDK and assets"
```

---

### 10:30-12:00 AliyunASRPackage.java

**创建文件：** `android/src/main/java/com/aliyunasr/AliyunASRPackage.java`

**复制来源：** `原生层实现.md`

**关键代码：**
```java
package com.aliyunasr;

public class AliyunASRPackage implements ReactPackage {
    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();
        modules.add(new AliyunASRModule(reactContext));
        return modules;
    }
}
```

```bash
git add android/src/main/java/com/aliyunasr/AliyunASRPackage.java
git commit -m "feat: add AliyunASRPackage"
```

---

### 13:00-15:00 AliyunASRModule.java

**创建文件：** `android/src/main/java/com/aliyunasr/AliyunASRModule.java`

**核心方法：**

1. **initialize**
```java
@ReactMethod
public void initialize(String parameters, int logLevel, boolean saveLog, Promise promise) {
    nuiSdk = new NuiSdk();
    listener = new NuiSdkListenerImpl(this);
    NuiResultCode result = nuiSdk.nui_initialize(parameters, listener, null, level, saveLog);
    // ...
}
```

2. **startDialog**
```java
@ReactMethod
public void startDialog(int vadMode, String dialogParams, Promise promise) {
    NuiVadMode mode = NuiVadMode.values()[vadMode];
    NuiResultCode result = nuiSdk.nui_dialog_start(mode, dialogParams);
    // ...
}
```

3. **stopDialog / cancelDialog**
```java
@ReactMethod
public void stopDialog(Promise promise) {
    NuiResultCode result = nuiSdk.nui_dialog_cancel(false);
    // ...
}

@ReactMethod
public void cancelDialog(boolean force, Promise promise) {
    NuiResultCode result = nuiSdk.nui_dialog_cancel(force);
    // ...
}
```

**检查清单：**
- [ ] 所有 `@ReactMethod` 注解正确
- [ ] Promise 正确 resolve/reject
- [ ] SDK 未初始化检查

```bash
git add android/src/main/java/com/aliyunasr/AliyunASRModule.java
git commit -m "feat: add AliyunASRModule"
```

---

### 15:00-17:00 NuiSdkListenerImpl.java

**创建文件：** `android/src/main/java/com/aliyunasr/NuiSdkListenerImpl.java`

**核心方法：**

```java
@Override
public void onEventCallback(NuiCallbackEvent event, long dialog, String wuw, 
                            String asrResult, boolean finish, int code, String allResponse) {
    WritableMap params = Arguments.createMap();
    params.putInt("event", event.ordinal());
    params.putDouble("dialogId", dialog);
    params.putInt("errorCode", code);
    
    if (asrResult != null && !asrResult.isEmpty()) {
        WritableMap resultMap = Arguments.createMap();
        resultMap.putString("text", asrResult);
        resultMap.putBoolean("isFinal", 
            event == NuiCallbackEvent.EVENT_ASR_RESULT);
        params.putMap("result", resultMap);
    }
    
    module.sendEvent("onASREvent", params);
}
```

**关键：** 事件名称必须是 `onASREvent`，与 TS 层对应

```bash
git add android/src/main/java/com/aliyunasr/NuiSdkListenerImpl.java
git commit -m "feat: add NuiSdkListenerImpl"
```

---

### 17:00-18:00 配置文件

**1. build.gradle**

创建 `android/build.gradle`

**关键点：**
```gradle
dependencies {
    implementation 'com.facebook.react:react-native:+'
    implementation(name: 'nuisdk-release', ext: 'aar')
}

repositories {
    flatDir {
        dirs 'libs'
    }
}
```

**2. AndroidManifest.xml**

创建 `android/src/main/AndroidManifest.xml`

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.aliyunasr">
    
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
</manifest>
```

```bash
git add android/build.gradle android/src/main/AndroidManifest.xml
git commit -m "chore: add Android build config"

# 推送
git push origin feat/android-module
```

---

## 调试指南

### 本地测试

```bash
cd example/android

# 1. 清理
./gradlew clean

# 2. 构建
./gradlew assembleDebug

# 3. 运行
npx react-native run-android
```

### 常见问题

**Q1: AAR 找不到？**

检查 `build.gradle`：
```gradle
repositories {
    flatDir {
        dirs 'libs'
    }
}

dependencies {
    implementation(name: 'nuisdk-release', ext: 'aar')
}
```

**Q2: 资源文件找不到？**

确保文件在 `android/src/main/assets/` 目录下

**Q3: 事件不触发？**

检查事件名称：`onASREvent`（必须与 TS 层一致）

---

## 联调检查清单

### 与 TS 层联调

- [ ] `initialize` 成功回调到 TS
- [ ] `startRecognition` 触发原生 `startDialog`
- [ ] `stopRecognition` 触发原生 `stopDialog`
- [ ] `ASR_PARTIAL_RESULT` 事件正常发送
- [ ] `ASR_RESULT` 事件正常发送
- [ ] `ASR_ERROR` 事件正常发送

### 日志调试

```java
// 在关键位置添加日志
Log.d("AliyunASR", "initialize called");
Log.d("AliyunASR", "onEventCallback: " + event);
```

---

## 交付标准

- [ ] 所有 Java 文件通过编译
- [ ] 无警告
- [ ] 资源文件完整
- [ ] 事件转发测试通过
- [ ] 代码通过 PL 审核

**完成后通知 PL 进行 Code Review**
