# API 文档

## 核心类

### `AliyunASR`

阿里云实时语音识别核心类，提供单例模式访问。

#### 静态方法

##### `getInstance(): AliyunASR`

获取 AliyunASR 单例实例。

```typescript
const asr = AliyunASR.getInstance();
```

#### 实例方法

##### `initialize(config: ASRInitConfig): Promise<void>`

初始化 SDK。必须在调用其他方法前完成初始化。

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| config.appKey | string | 是 | 阿里云 AppKey |
| config.token | string | 是 | 访问令牌 |
| config.url | string | 否 | 服务器地址，默认使用阿里云官方地址 |
| config.deviceId | string | 否 | 设备唯一标识 |
| config.workspace | string | 否 | 工作目录，用于存储日志等 |
| config.sampleRate | number | 否 | 采样率 16000/8000，默认 16000 |
| config.format | 'opus' \| 'pcm' | 否 | 音频编码格式，默认 'opus' |
| config.enableVad | boolean | 否 | 是否启用 VAD，默认 true |
| config.logLevel | LogLevel | 否 | 日志级别，默认 INFO |
| config.saveLog | boolean | 否 | 是否保存日志到文件，默认 false |

**示例：**

```typescript
await asr.initialize({
  appKey: 'your-app-key',
  token: 'your-token',
  workspace: '/path/to/workspace',
  logLevel: LogLevel.INFO,
});
```

##### `release(): Promise<void>`

释放 SDK 资源。应用退出或不再需要语音识别时调用。

```typescript
await asr.release();
```

##### `startRecognition(vadMode?: VadMode, params?: ASRDialogParams): Promise<void>`

开始语音识别。

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| vadMode | VadMode | 否 | VAD 模式，默认 MODE_VAD |
| params.vocabularyId | string | 否 | 热词词表 ID |
| params.maxSentenceSilence | number | 否 | 句尾静默时长（毫秒） |

**示例：**

```typescript
// 基础使用
await asr.startRecognition();

// 使用 P2T 模式
await asr.startRecognition(VadMode.MODE_P2T);

// 带热词
await asr.startRecognition(VadMode.MODE_VAD, {
  vocabularyId: 'your-vocabulary-id',
});
```

##### `stopRecognition(): Promise<void>`

停止语音识别（不保证返回最终结果）。

```typescript
await asr.stopRecognition();
```

##### `cancelRecognition(force?: boolean): Promise<void>`

取消语音识别。

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| force | boolean | 否 | 是否强制取消，默认 false |

```typescript
// 普通取消（可能触发回调）
await asr.cancelRecognition();

// 强制取消（不触发回调）
await asr.cancelRecognition(true);
```

##### `setParam(key: string, value: string): Promise<void>`

设置参数。

```typescript
await asr.setParam('vocabulary_id', 'your-vocabulary-id');
```

##### `getParam(key: string): Promise<string>`

获取参数值。

```typescript
const value = await asr.getParam('vocabulary_id');
```

##### `on(event: ASREvent, callback: ASREventCallback): void`

订阅事件。

```typescript
asr.on(ASREvent.ASR_RESULT, (data) => {
  console.log('识别结果:', data.result?.text);
});
```

##### `off(event: ASREvent, callback: ASREventCallback): void`

取消订阅事件。

```typescript
const callback = (data) => console.log(data);
asr.on(ASREvent.ASR_RESULT, callback);
asr.off(ASREvent.ASR_RESULT, callback);
```

##### `once(event: ASREvent, callback: ASREventCallback): void`

一次性订阅事件，触发后自动取消订阅。

```typescript
asr.once(ASREvent.VAD_START, (data) => {
  console.log('语音开始');
});
```

---

## 枚举类型

### `VadMode`

VAD（语音活动检测）模式。

| 值 | 名称 | 说明 |
|----|------|------|
| 0 | MODE_VAD | 纯 VAD 模式，自动检测语音开始和结束（默认） |
| 1 | MODE_P2T | 按住说话模式（Push to Talk） |
| 5 | MODE_AUTO_CONTINUAL | 自动连续识别 |

### `ServiceType`

服务类型（本框架仅支持语音识别）。

| 值 | 名称 | 说明 |
|----|------|------|
| 0 | SERVICE_TYPE_ASR | 语音识别 |

### `LogLevel`

日志级别。

| 值 | 名称 | 说明 |
|----|------|------|
| 0 | VERBOSE | 详细日志 |
| 1 | DEBUG | 调试日志 |
| 2 | INFO | 信息日志（默认） |
| 3 | WARNING | 警告日志 |
| 4 | ERROR | 错误日志 |
| 5 | NONE | 关闭日志 |

### `ASREvent`

回调事件类型。

#### VAD 事件

| 值 | 名称 | 说明 |
|----|------|------|
| 0 | VAD_START | 检测到语音开始 |
| 1 | VAD_TIMEOUT | VAD 超时 |
| 2 | VAD_END | 检测到语音结束 |

#### ASR 事件

| 值 | 名称 | 说明 |
|----|------|------|
| 9 | ASR_PARTIAL_RESULT | 中间识别结果 |
| 10 | ASR_RESULT | 最终识别结果 |
| 11 | ASR_ERROR | 识别错误 |
| 40 | ASR_STARTED | 识别开始 |

#### 句子级事件

| 值 | 名称 | 说明 |
|----|------|------|
| 28 | SENTENCE_START | 句子开始 |
| 29 | SENTENCE_END | 句子结束 |
| 30 | SENTENCE_SEMANTICS | 语义结果 |

#### 其他事件

| 值 | 名称 | 说明 |
|----|------|------|
| 19 | MIC_ERROR | 麦克风错误 |
| 22 | BEFORE_CONNECTION | 连接前事件 |

### `ASRErrorCode`

错误码。

| 值 | 名称 | 说明 |
|----|------|------|
| 0 | SUCCESS | 成功 |
| 240001 | CONFIG_INVALID | 配置或文件无效 |
| 240002 | ILLEGAL_PARAM | 参数非法 |
| 240011 | SDK_NOT_INIT | SDK 未初始化 |
| 240012 | SDK_ALREADY_INIT | SDK 已初始化 |
| 240052 | MIC_ERROR | 麦克风错误 |
| 240070 | AUTH_FAILED | 认证失败（Token 过期） |
| 240091 | CONNECTION_TIMEOUT | 连接超时 |
| 240093 | ASR_TIMEOUT | 识别超时 |

---

## 类型定义

### `ASRInitConfig`

初始化配置对象。

```typescript
interface ASRInitConfig {
  appKey: string;           // 必填：阿里云 AppKey
  token: string;            // 必填：访问令牌
  url?: string;             // 可选：自定义服务器地址
  deviceId?: string;        // 可选：设备标识
  workspace?: string;       // 可选：日志目录
  sampleRate?: number;      // 可选：采样率，默认 16000
  format?: 'opus' | 'pcm';  // 可选：音频格式，默认 opus
  enableVad?: boolean;      // 可选：启用 VAD，默认 true
  logLevel?: LogLevel;      // 可选：日志级别，默认 INFO
  saveLog?: boolean;        // 可选：保存日志，默认 false
}
```

### `ASRDialogParams`

识别参数对象。

```typescript
interface ASRDialogParams {
  vocabularyId?: string;        // 可选：热词词表 ID
  maxSentenceSilence?: number;  // 可选：句尾静默时长（毫秒）
}
```

### `ASRResult`

识别结果对象。

```typescript
interface ASRResult {
  text: string;             // 识别文本
  confidence?: number;      // 置信度
  isFinal: boolean;         // 是否最终结果
  sentenceId?: number;      // 句子 ID（用于长语音）
}
```

### `ASREventData`

事件回调数据对象。

```typescript
interface ASREventData {
  event: ASREvent;
  result?: ASRResult;
  errorCode?: number;
  errorMessage?: string;
  dialogId?: number;
}
```

### `ASREventCallback`

事件回调函数类型。

```typescript
type ASREventCallback = (data: ASREventData) => void;
```

---

## 完整示例

```typescript
import React, { useEffect, useState } from 'react';
import { View, Text, Button } from 'react-native';
import { AliyunASR, VadMode, ASREvent, LogLevel } from '@gaozh1024/rn-aliyun-asr';

const asr = AliyunASR.getInstance();

function ASRScreen() {
  const [text, setText] = useState('');
  const [isRecording, setIsRecording] = useState(false);

  useEffect(() => {
    // 初始化 SDK
    asr.initialize({
      appKey: 'your-app-key',
      token: 'your-token',
    });

    // 监听临时结果
    asr.on(ASREvent.ASR_PARTIAL_RESULT, (data) => {
      setText(data.result?.text || '');
    });

    // 监听最终结果
    asr.on(ASREvent.ASR_RESULT, (data) => {
      setText(data.result?.text || '');
      setIsRecording(false);
    });

    // 错误处理
    asr.on(ASREvent.ASR_ERROR, (data) => {
      console.error('ASR 错误:', data.errorMessage);
      setIsRecording(false);
    });

    // 清理
    return () => {
      asr.release();
    };
  }, []);

  const start = () => {
    setIsRecording(true);
    asr.startRecognition(VadMode.MODE_VAD);
  };

  const stop = () => {
    asr.stopRecognition();
  };

  return (
    <View>
      <Text>{text || '点击开始识别'}</Text>
      <Button
        title={isRecording ? '停止' : '开始'}
        onPress={isRecording ? stop : start}
      />
    </View>
  );
}

export default ASRScreen;
```

---

## 错误处理建议

```typescript
asr.on(ASREvent.ASR_ERROR, (data) => {
  switch (data.errorCode) {
    case ASRErrorCode.AUTH_FAILED:
      // Token 过期，需要重新获取并初始化
      console.log('Token 过期，请重新获取');
      break;
    case ASRErrorCode.MIC_ERROR:
      // 麦克风错误，检查权限
      console.log('麦克风错误，请检查权限');
      break;
    case ASRErrorCode.CONNECTION_TIMEOUT:
      // 网络连接超时
      console.log('连接超时，请检查网络');
      break;
    default:
      console.error('ASR 错误:', data.errorMessage);
  }
});
```
