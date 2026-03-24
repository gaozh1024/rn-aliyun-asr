# @gaozh1024/rn-aliyun-asr

React Native 阿里云实时语音识别 SDK

## 功能特性

- ✅ 实时语音识别 (Real-time ASR)
- ✅ 语音活动检测 (VAD)
- ✅ 中间结果实时返回
- ✅ 长文本连续识别
- ✅ 热词定制
- ✅ Android & iOS 双平台

## 安装

```bash
npm install @gaozh1024/rn-aliyun-asr
# 或
yarn add @gaozh1024/rn-aliyun-asr
```

### iOS 额外配置

```bash
cd ios && pod install
```

### 权限配置

**Android** - `android/app/src/main/AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
```

**iOS** - `ios/YourApp/Info.plist`

```xml
<key>NSMicrophoneUsageDescription</key>
<string>需要麦克风权限进行语音识别</string>
```

## 快速开始

```typescript
import { AliyunASR, VadMode, ASREvent } from '@gaozh1024/rn-aliyun-asr';

const asr = AliyunASR.getInstance();

// 初始化
await asr.initialize({
  appKey: 'your-app-key',
  token: 'your-token',
});

// 监听识别结果
asr.on(ASREvent.ASR_RESULT, (data) => {
  console.log('识别结果:', data.result?.text);
});

// 开始识别
await asr.startRecognition(VadMode.MODE_VAD);
```

## 文档

- [API 文档](./docs/API.md)
- [使用指南](./docs/USAGE.md)

## 许可证

MIT
