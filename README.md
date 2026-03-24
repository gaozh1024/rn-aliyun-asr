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

### Android 额外配置

**由于 Gradle 8.x 限制，需要手动在主项目中添加 AAR 依赖：**

编辑 `android/app/build.gradle`：

```gradle
dependencies {
    // ... 其他依赖
    
    // 阿里云语音识别 SDK AAR（必须手动添加）
    implementation files("${rootDir}/../node_modules/@gaozh1024/rn-aliyun-asr/android/libs/nuisdk-release.aar")
}
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

文档已按功能分组，便于快速查找：

### 📚 入门指南
- [项目说明](./docs/01-入门指南/项目说明.md) - 项目介绍、功能特性
- [使用文档](./docs/01-入门指南/使用文档.md) - 详细使用指南、API 示例、常见问题

### 📖 API 参考
- [API文档](./docs/02-API文档/API文档.md) - 完整 API 方法、类型定义、错误码

### 💻 开发指南（开发者）
- [架构设计](./docs/03-开发指南/架构设计.md) - 整体架构设计
- [原生层实现](./docs/03-开发指南/原生层实现.md) - Android/iOS 原生代码实现

### 👥 项目管理
- [README-团队](./docs/04-团队管理/README-团队.md) - 团队总览

## 快速导航

| 角色 | 推荐文档 |
|------|----------|
| **使用者** | [使用文档](./docs/01-入门指南/使用文档.md) |
| **开发者** | [架构设计](./docs/03-开发指南/架构设计.md) + [原生层实现](./docs/03-开发指南/原生层实现.md) |
| **项目负责人** | [README-团队](./docs/04-团队管理/README-团队.md) |

## 示例

查看 [example/App.tsx](./example/App.tsx) 获取完整示例代码。

## 变更日志

查看 [版本记录](./docs/05-版本记录) 了解每个版本的详细变更。

| 版本 | 日期 | 说明 |
|------|------|------|
| [v1.0.3](./docs/05-版本记录/v1.0.3.md) | 2024-03-24 | Android AAR 手动配置方案 |
| [v1.0.2](./docs/05-版本记录/v1.0.2.md) | 2024-03-24 | Android AAR 依赖传递修复（已废弃） |
| [v1.0.1](./docs/05-版本记录/v1.0.1.md) | 2024-03-24 | Android Gradle 8.x 兼容性修复（已废弃） |
| [v1.0.0](./docs/05-版本记录/v1.0.0.md) | 2024-03-24 | 首个正式版本 |

## 许可证

MIT
