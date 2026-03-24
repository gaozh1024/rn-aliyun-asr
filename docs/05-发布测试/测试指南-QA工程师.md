# QA 工程师测试指南

## 角色定位
负责测试用例设计、功能测试、兼容性测试和发布验证。

## Day 4-5 详细任务

### Day 4 上午：测试用例设计

**创建文件：** `docs/TEST_PLAN.md`

```markdown
# 测试计划

## 测试范围
- 实时语音识别功能
- Android/iOS 双平台

## 测试环境
- Android: API 21+
- iOS: 11+
- React Native: 0.60+

## 测试用例

### 1. 初始化测试
| 用例ID | 测试项 | 步骤 | 预期结果 |
|--------|--------|------|----------|
| TC001 | 正常初始化 | 调用 initialize | 成功，无错误 |
| TC002 | 重复初始化 | 再次调用 initialize | 抛出错误 |
| TC003 | 错误 AppKey | 使用错误 AppKey | 返回 AUTH_FAILED |
| TC004 | 错误 Token | 使用过期 Token | 返回 AUTH_FAILED |

### 2. 识别测试
| 用例ID | 测试项 | 步骤 | 预期结果 |
|--------|--------|------|----------|
| TC010 | 开始识别 | 调用 startRecognition | ASR_STARTED 事件 |
| TC011 | 临时结果 | 说话中 | ASR_PARTIAL_RESULT 事件 |
| TC012 | 最终结果 | 说完一句话 | ASR_RESULT 事件 |
| TC013 | 停止识别 | 调用 stopRecognition | 停止当前识别并返回最终结果 |
| TC014 | 取消识别 | 调用 cancelRecognition | 不返回结果 |

### 3. 事件测试
| 用例ID | 测试项 | 预期结果 |
|--------|--------|----------|
| TC020 | VAD_START | 检测到语音开始时触发 |
| TC021 | VAD_END | 检测到语音结束时触发 |
| TC022 | ASR_ERROR | 错误时触发，包含错误码 |
```

---

### Day 4 下午：联调测试

**参与联调，验证功能：**

#### Android 联调检查清单

```bash
cd example/android
npx react-native run-android
```

- [ ] App 正常启动
- [ ] 初始化按钮点击后无错误
- [ ] 开始识别后 ASR_STARTED 事件触发
- [ ] 说话时 ASR_PARTIAL_RESULT 有文字返回
- [ ] 说完后 ASR_RESULT 有最终结果
- [ ] 停止按钮正常工作
- [ ] 取消按钮正常工作
- [ ] 释放按钮正常工作

#### iOS 联调检查清单

```bash
cd example/ios
pod install
npx react-native run-ios
```

- [ ] App 正常启动
- [ ] 初始化按钮点击后无错误
- [ ] 开始识别后 ASR_STARTED 事件触发
- [ ] 说话时 ASR_PARTIAL_RESULT 有文字返回
- [ ] 说完后 ASR_RESULT 有最终结果
- [ ] 停止按钮正常工作
- [ ] 取消按钮正常工作
- [ ] 释放按钮正常工作

---

### Day 5：全面测试

#### 功能测试

**测试场景 1：正常识别流程**
1. 点击"初始化 SDK"
2. 点击"开始识别"
3. 说"你好，世界"
4. 等待识别结果
5. 点击"停止识别"

**预期：** 中间结果显示"你好，世界"，最终结果正确

**测试场景 2：长文本识别**
1. 开始识别
2. 连续说多句话
3. 检查句子级事件

**预期：** 每句话都有 SENTENCE_START 和 SENTENCE_END

**测试场景 3：热词测试**
1. 使用 vocabularyId 开始识别
2. 说包含热词的句子

**预期：** 热词识别准确率更高

#### 异常测试

**测试场景 4：网络异常**
1. 开启飞行模式
2. 开始识别

**预期：** 触发 ASR_ERROR，错误码为 CONNECTION_TIMEOUT

**测试场景 5：麦克风权限拒绝**
1. 拒绝麦克风权限
2. 开始识别

**预期：** 触发 ASR_ERROR，错误码为 MIC_ERROR

#### 兼容性测试

| 设备 | Android 版本 | iOS 版本 | 测试结果 |
|------|-------------|----------|----------|
| 小米 10 | Android 11 | - | ⬜ |
| 华为 P40 | Android 10 | - | ⬜ |
| iPhone 12 | - | iOS 15 | ⬜ |
| iPhone X | - | iOS 14 | ⬜ |

---

## Bug 报告模板

```markdown
## Bug 标题
[Android/iOS] 简要描述

## 复现步骤
1. 
2. 
3. 

## 预期结果

## 实际结果

## 环境信息
- 设备型号：
- 系统版本：
- RN 版本：
- SDK 版本：

## 截图/日志
```

---

## Day 5 下午：发布验证

### NPM 包验证

```bash
# 1. 安装测试
npm install @gaozh1024/rn-aliyun-asr@latest

# 2. 检查文件
ls node_modules/@gaozh1024/rn-aliyun-asr/
# 应有：src/, android/, ios/, package.json

# 3. iOS 安装测试
cd example/ios
pod install
# 检查是否正确安装
```

### 最终检查清单

- [ ] NPM 包可正常安装
- [ ] Android 可正常构建
- [ ] iOS 可正常构建
- [ ] 示例应用可正常运行
- [ ] 识别功能正常
- [ ] 无 P0/P1 Bug

---

## 测试报告模板

```markdown
# 测试报告

## 测试时间
2026-XX-XX

## 测试范围
实时语音识别功能

## 测试环境
- Android: API 21-33
- iOS: 11-16

## 测试结果
- 测试用例总数：XX
- 通过：XX
- 失败：XX
- 通过率：XX%

## Bug 列表
| Bug ID | 描述 | 严重程度 | 状态 |
|--------|------|----------|------|
| BUG001 | ... | P0 | 已修复 |

## 发布建议
⬜ 建议发布
⬜ 不建议发布（存在阻塞性问题）
```

---

## 交付物

- [ ] 测试计划文档
- [ ] 测试用例文档
- [ ] Bug 列表
- [ ] 测试报告
- [ ] 发布验证报告

**完成后通知 PL 进行发布评审**
