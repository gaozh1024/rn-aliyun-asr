# TypeScript 工程师开发指南

## 角色定位
负责 TypeScript 层开发，包括类型定义、核心类实现和示例应用开发。

## Day 1 详细任务

### 09:00-10:00 环境搭建

```bash
# 创建项目目录
mkdir rn-aliyun-asr
cd rn-aliyun-asr

# 初始化 npm
npm init -y

# 安装依赖
npm install --save-dev typescript @types/react @types/react-native \
  react-native-builder-bob eslint prettier jest

# 创建目录
mkdir -p src
mkdir -p example
```

**检查清单：**
- [ ] Node.js >= 16
- [ ] npm 或 yarn
- [ ] TypeScript 安装成功

---

### 10:00-12:00 类型定义 (src/types.ts)

**复制来源：** 包配置.md

```typescript
// 完整复制包配置.md 中的 types.ts 内容

// 错误码
export enum ASRErrorCode {
  SUCCESS = 0,
  // ... 其他错误码
}

// VAD 模式
export enum VadMode {
  MODE_VAD = 0,
  MODE_P2T = 1,
  MODE_AUTO_CONTINUAL = 5,
}

// 其他类型...
```

**自测命令：**
```bash
npx tsc --noEmit src/types.ts
```

**检查清单：**
- [ ] 无 TypeScript 错误
- [ ] 所有枚举导出
- [ ] 所有接口导出

---

### 13:00-16:00 核心类实现 (src/AliyunASR.ts)

**复制来源：** 架构设计.md

**关键实现点：**

1. **单例模式**
```typescript
export class AliyunASR {
  private static instance: AliyunASR;
  
  static getInstance(): AliyunASR {
    if (!AliyunASR.instance) {
      AliyunASR.instance = new AliyunASR();
    }
    return AliyunASR.instance;
  }
}
```

2. **初始化方法**
```typescript
async initialize(config: ASRInitConfig): Promise<void> {
  // 构建参数 JSON
  const initParams = this.buildInitParams(config);
  // 调用原生模块
  await AliyunASRModule.initialize(initParams, logLevel, saveLog);
}
```

3. **事件系统**
```typescript
on(event: ASREvent, callback: ASREventCallback): void {
  // 存储回调
}

off(event: ASREvent, callback: ASREventCallback): void {
  // 移除回调
}
```

**自测命令：**
```bash
npx tsc --noEmit src/AliyunASR.ts
```

---

### 16:00-18:00 入口文件 (src/index.ts)

```typescript
// 类型导出
export {
  ASRInitConfig,
  ASRDialogParams,
  ASRResult,
  ASREventData,
  ASREventCallback,
} from './types';

// 枚举导出
export { ASRErrorCode, VadMode, LogLevel, ASREvent } from './types';

// 核心类导出
export { AliyunASR, createASR } from './AliyunASR';

// 默认导出
export { AliyunASR as default } from './AliyunASR';
```

---

## Day 4 示例应用开发

### 09:00-12:00 开发 example/App.tsx

**复制来源：** 包配置.md

**开发要点：**

1. **初始化流程**
```typescript
useEffect(() => {
  asr.initialize({
    appKey: CONFIG.appKey,
    token: CONFIG.token,
  });
  // ...
  return () => {
    asr.release();
  };
}, []);
```

2. **事件监听**
```typescript
asr.on(ASREvent.ASR_PARTIAL_RESULT, (data) => {
  setCurrentText(data.result?.text || '');
});

asr.on(ASREvent.ASR_RESULT, (data) => {
  setResults(prev => [...prev, data.result!.text]);
});
```

3. **UI 交互**
```typescript
<TouchableOpacity onPress={isRecording ? stop : start}>
  <Text>{isRecording ? '停止' : '开始'}</Text>
</TouchableOpacity>
```

---

## 代码提交规范

### 提交信息格式
```
<type>: <subject>

<body>
```

### 类型说明
- `feat`: 新功能
- `fix`: 修复
- `docs`: 文档
- `style`: 格式
- `refactor`: 重构
- `test`: 测试

### 示例
```
feat: 实现 AliyunASR 核心类

- 添加单例模式
- 实现 initialize/release 方法
- 添加事件系统
```

---

## 联调检查清单

### 与 Android 联调
- [ ] 初始化成功回调
- [ ] startRecognition 触发原生方法
- [ ] 事件接收正常
- [ ] 错误处理正确

### 与 iOS 联调
- [ ] 初始化成功回调
- [ ] startRecognition 触发原生方法
- [ ] 事件接收正常
- [ ] 错误处理正确

---

## 常见问题

### Q1: NativeModules 找不到？

**解决：** 确保原生模块已正确链接
```bash
cd example
npx react-native link
# 或
cd ios && pod install
```

### Q2: TypeScript 类型错误？

**解决：** 检查 tsconfig.json
```json
{
  "compilerOptions": {
    "esModuleInterop": true,
    "skipLibCheck": true
  }
}
```

### Q3: 事件不触发？

**调试：** 添加日志
```typescript
console.log('注册事件:', event);
console.log('收到事件:', nativeEvent);
```

---

## 交付标准

- [ ] TypeScript 无编译错误
- [ ] 所有类型导出正确
- [ ] 示例应用可运行
- [ ] 代码通过 ESLint
- [ ] 文档注释完整

**完成后通知 PL 进行 Code Review**
