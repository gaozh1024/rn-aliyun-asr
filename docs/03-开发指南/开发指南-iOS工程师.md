# iOS 工程师开发指南

## 角色定位
负责 iOS 原生模块开发，包括 SDK Framework 集成、Objective-C 代码实现和 Podspec 配置。

## Day 3 详细任务

### 09:00-10:00 环境准备

**前置条件：** TS 层已完成，Android 模块开发中或完成

```bash
# 1. 克隆仓库（如未克隆）
git clone https://github.com/gaozh1024/rn-aliyun-asr.git
cd rn-aliyun-asr

# 2. 创建分支
git checkout -b feat/ios-module

# 3. 创建目录
mkdir -p ios/Frameworks
```

---

### 10:00-10:30 复制 SDK 文件

**来源：** `docs/ios/`

```bash
# 复制 Framework
cp -r docs/ios/Release/nuisdk.framework ios/Frameworks/

# 确认文件
ls -la ios/Frameworks/
```

**检查清单：**
- [ ] `ios/Frameworks/nuisdk.framework` 存在

```bash
git add ios/Frameworks/
git commit -m "chore: add iOS SDK and assets"
```

---

### 10:30-12:00 AliyunASR.h

**创建文件：** `ios/AliyunASR.h`

```objc
// AliyunASR.h

#import <React/RCTEventEmitter.h>
#import <React/RCTBridgeModule.h>
#import <nuisdk/NeoNui.h>

@interface AliyunASR : RCTEventEmitter <RCTBridgeModule, NeoNuiSdkDelegate>

@end
```

**关键点：**
- 继承 `RCTEventEmitter`
- 实现 `RCTBridgeModule` 协议
- 实现 `NeoNuiSdkDelegate` 协议

```bash
git add ios/AliyunASR.h
git commit -m "feat: add AliyunASR.h"
```

---

### 13:00-16:00 AliyunASR.m

**创建文件：** `ios/AliyunASR.m`

**核心方法：**

1. **模块导出**
```objc
RCT_EXPORT_MODULE(AliyunASRModule);
```

2. **initialize**
```objc
RCT_EXPORT_METHOD(initialize:(NSString *)parameters
                  logLevel:(NSInteger)logLevel
                  saveLog:(BOOL)saveLog
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    self.nuiSdk = [NeoNui get_instance];
    self.nuiSdk.delegate = self;
    
    int result = [self.nuiSdk nui_initialize:[parameters UTF8String]
                                    logLevel:level
                                     saveLog:saveLog];
    // ...
}
```

4. **startDialog**
```objc
RCT_EXPORT_METHOD(startDialog:(NSInteger)vadMode
                  dialogParams:(NSString *)dialogParams
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    NuiVadMode mode = (NuiVadMode)vadMode;
    int result = [self.nuiSdk nui_dialog_start:mode
                                   dialogParam:[dialogParams UTF8String]];
    // ...
}
```

5. **事件回调**
```objc
- (void)onNuiEventCallback:(NuiCallbackEvent)nuiEvent
                    dialog:(long)dialog
                 kwsResult:(const char *)wuw
                 asrResult:(const char *)asr_result
                  ifFinish:(BOOL)finish
                   retCode:(int)code {
    NSMutableDictionary *eventData = [@{
        @"event": @(nuiEvent),
        @"dialogId": @(dialog),
        @"errorCode": @(code),
    } mutableCopy];
    
    if (asr_result != NULL) {
        NSString *resultText = [NSString stringWithUTF8String:asr_result];
        eventData[@"result"] = @{
            @"text": resultText,
            @"isFinal": @(nuiEvent == EVENT_ASR_RESULT)
        };
    }
    
    [self sendEventWithName:@"onASREvent" body:eventData];
}
```

**关键：** 事件名称必须是 `onASREvent`

```bash
git add ios/AliyunASR.m
git commit -m "feat: add AliyunASR.m"
```

---

### 16:00-17:00 rn-aliyun-asr.podspec

**创建文件：** `rn-aliyun-asr.podspec`

```ruby
require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = "rn-aliyun-asr"
  s.version      = package['version']
  s.summary      = "React Native 阿里云实时语音识别"
  s.description  = package['description']
  s.homepage     = package['homepage']
  s.license      = package['license']
  s.author       = { "author" => "author@example.com" }
  s.platform     = :ios, "11.0"
  s.source       = { :git => "https://github.com/gaozh1024/rn-aliyun-asr.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m}"
  
  # 引入阿里云 NUI SDK
  s.vendored_frameworks = 'ios/Frameworks/nuisdk.framework'

  s.dependency "React-Core"
  
  # 需要的系统框架
  s.frameworks = 'Foundation', 'UIKit', 'AudioToolbox', 'AVFoundation'
  
  # 库
  s.libraries = 'c++', 'z'
end
```

**关键点：**
- `s.vendored_frameworks` 指向 Framework
- `s.frameworks` 包含必要的系统框架
- `s.libraries` 包含 C++ 标准库

```bash
git add rn-aliyun-asr.podspec
git commit -m "chore: add podspec"

# 推送
git push origin feat/ios-module
```

---

## 调试指南

### 本地测试

```bash
cd example/ios

# 1. 安装依赖
pod install

# 2. 打开 Xcode
open AliyunASRExample.xcworkspace

# 3. 运行
npx react-native run-ios
```

### 常见问题

**Q1: Framework 找不到？**

检查 `podspec`：
```ruby
s.vendored_frameworks = 'ios/Frameworks/nuisdk.framework'
```

**Q2: 头文件找不到？**
```objc
#import <nuisdk/NeoNui.h>
```

**Q3: 事件不触发？**

检查事件名称：`onASREvent`（必须与 TS 层一致）

**Q4: 资源文件找不到？**

需要在 Xcode 中确认 `Resources.bundle`（随 `nuisdk.framework`）已添加到：
- Build Phases -> Copy Bundle Resources

---

## Xcode 配置

### 添加资源文件到 Bundle

1. 打开 `AliyunASRExample.xcworkspace`
2. 选中项目
3. Build Phases
4. Copy Bundle Resources
5. 点击 `+`
6. 确认 `Resources.bundle`（随 `nuisdk.framework`）已被正确拷贝到 App Bundle

### Info.plist 权限

确保添加了麦克风权限：
```xml
<key>NSMicrophoneUsageDescription</key>
<string>需要麦克风权限进行语音识别</string>
```

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

```objc
NSLog(@"AliyunASR: initialize called");
NSLog(@"AliyunASR: onNuiEventCallback: %d", nuiEvent);
```

---

## 交付标准

- [ ] Framework 已正确引用
- [ ] Resources.bundle 已随 Framework 集成
- [ ] 所有方法导出正确
- [ ] 事件转发测试通过
- [ ] 代码通过 PL 审核

**完成后通知 PL 进行 Code Review**
