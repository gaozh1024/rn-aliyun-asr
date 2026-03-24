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
- [项目说明](https://github.com/gaozh1024/rn-aliyun-asr/blob/main/docs/01-%E5%85%A5%E9%97%A8%E6%8C%87%E5%8D%97/%E9%A1%B9%E7%9B%AE%E8%AF%B4%E6%98%8E.md) - 项目介绍、功能特性
- [使用文档](https://github.com/gaozh1024/rn-aliyun-asr/blob/main/docs/01-%E5%85%A5%E9%97%A8%E6%8C%87%E5%8D%97/%E4%BD%BF%E7%94%A8%E6%96%87%E6%A1%A3.md) - 详细使用指南、API 示例、常见问题

### 📖 API 参考
- [API文档](https://github.com/gaozh1024/rn-aliyun-asr/blob/main/docs/02-API%E6%96%87%E6%A1%A3/API%E6%96%87%E6%A1%A3.md) - 完整 API 方法、类型定义、错误码

### 💻 开发指南（开发者）
- [架构设计](https://github.com/gaozh1024/rn-aliyun-asr/blob/main/docs/03-%E5%BC%80%E5%8F%91%E6%8C%87%E5%8D%97/%E6%9E%B6%E6%9E%84%E8%AE%BE%E8%AE%A1.md) - 整体架构设计
- [原生层实现](https://github.com/gaozh1024/rn-aliyun-asr/blob/main/docs/03-%E5%BC%80%E5%8F%91%E6%8C%87%E5%8D%97/%E5%8E%9F%E7%94%9F%E5%B1%82%E5%AE%9E%E7%8E%B0.md) - Android/iOS 原生代码实现

### 👥 项目管理
- [README-团队](https://github.com/gaozh1024/rn-aliyun-asr/blob/main/docs/04-%E5%9B%A2%E9%98%9F%E7%AE%A1%E7%90%86/README-%E5%9B%A2%E9%98%9F.md) - 团队总览

## 快速导航

| 角色 | 推荐文档 |
|------|----------|
| **使用者** | [使用文档](./docs/01-入门指南/使用文档.md) |
| **开发者** | [架构设计](./docs/03-开发指南/架构设计.md) + [原生层实现](./docs/03-开发指南/原生层实现.md) |
| **项目负责人** | [README-团队](./docs/04-团队管理/README-团队.md) |

## 示例

查看 [example/App.tsx](https://github.com/gaozh1024/rn-aliyun-asr/blob/main/example/App.tsx) 获取完整示例代码。

## 变更日志

查看 [版本记录](https://github.com/gaozh1024/rn-aliyun-asr/tree/main/docs/05-%E7%89%88%E6%9C%AC%E8%AE%B0%E5%BD%95) 了解每个版本的详细变更。

| 版本 | 日期 | 说明 |
|------|------|------|
| [v1.0.5](https://github.com/gaozh1024/rn-aliyun-asr/blob/main/docs/05-%E7%89%88%E6%9C%AC%E8%AE%B0%E5%BD%95/v1.0.5.md) | 2026-03-24 | stop 语义修正、事件字段对齐、发布流程增强 |
| [v1.0.4](https://github.com/gaozh1024/rn-aliyun-asr/blob/main/docs/05-%E7%89%88%E6%9C%AC%E8%AE%B0%E5%BD%95/v1.0.4.md) | 2024-03-24 | **修复 Android 代码与 AAR 不匹配** |
| [v1.0.3](https://github.com/gaozh1024/rn-aliyun-asr/blob/main/docs/05-%E7%89%88%E6%9C%AC%E8%AE%B0%E5%BD%95/v1.0.3.md) | 2024-03-24 | Android AAR 手动配置方案 |
| [v1.0.2](https://github.com/gaozh1024/rn-aliyun-asr/blob/main/docs/05-%E7%89%88%E6%9C%AC%E8%AE%B0%E5%BD%95/v1.0.2.md) | 2024-03-24 | Android AAR 依赖传递修复（已废弃） |
| [v1.0.1](https://github.com/gaozh1024/rn-aliyun-asr/blob/main/docs/05-%E7%89%88%E6%9C%AC%E8%AE%B0%E5%BD%95/v1.0.1.md) | 2024-03-24 | Android Gradle 8.x 兼容性修复（已废弃） |
| [v1.0.0](https://github.com/gaozh1024/rn-aliyun-asr/blob/main/docs/05-%E7%89%88%E6%9C%AC%E8%AE%B0%E5%BD%95/v1.0.0.md) | 2024-03-24 | 首个正式版本 |

## 许可证

MIT
