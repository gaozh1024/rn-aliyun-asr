# 变更日志 (Changelog)

所有版本的重要变更都会记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [1.0.1] - 2024-03-24

### 🐛 修复 (Fixed)

- **Android 构建兼容性修复**
  - 修复 `flatDir` 在 Gradle 8.x + React Native 0.73+ 中不生效的问题
  - 将 AAR 依赖方式从 `implementation(name: 'nuisdk-release', ext: 'aar')` 改为 `implementation files('libs/nuisdk-release.aar')`
  - 用户无需再手动在主项目 `android/build.gradle` 中添加 `flatDir` 配置

### 📚 文档 (Changed)

- 更新《开发指南-Android工程师》和《原生层实现》文档，反映新的 AAR 引用方式

---

## [1.0.0] - 2024-03-24

### ✨ 新增 (Added)

- 首个正式发布版本
- 阿里云实时语音识别功能
  - 实时语音识别 (Real-time ASR)
  - 语音活动检测 (VAD)
  - 中间结果实时返回
  - 长文本连续识别
  - 热词定制
- 双平台支持
  - Android (基于阿里云 NUI SDK)
  - iOS (基于阿里云 NUI SDK)
- TypeScript 完整类型支持
- 单例模式设计
- 事件驱动架构

### 📦 依赖

- React Native >= 0.60.0
- Android minSdkVersion 21
- iOS 11.0+

---
