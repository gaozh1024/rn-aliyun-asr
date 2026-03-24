import {
  NativeModules,
  NativeEventEmitter,
  EmitterSubscription,
} from 'react-native';
import type {
  ASRInitConfig,
  ASRDialogParams,
  ASREventData,
  ASREventCallback,
} from './types';
import { ASREvent, VadMode, LogLevel } from './types';

const { AliyunASRModule } = NativeModules;
const eventEmitter = new NativeEventEmitter(AliyunASRModule);

/**
 * 阿里云实时语音识别 SDK
 *
 * 使用示例：
 * ```typescript
 * const asr = AliyunASR.getInstance();
 * await asr.initialize({ appKey: 'xxx', token: 'xxx' });
 * await asr.startRecognition();
 * ```
 */
export class AliyunASR {
  private static instance: AliyunASR;
  private subscriptions: EmitterSubscription[] = [];
  private eventCallbacks: Map<string, Set<ASREventCallback>> = new Map();
  private isInitialized = false;

  private constructor() {}

  /**
   * 获取单例实例
   */
  static getInstance(): AliyunASR {
    if (!AliyunASR.instance) {
      AliyunASR.instance = new AliyunASR();
    }
    return AliyunASR.instance;
  }

  /**
   * 初始化 SDK
   * @param config 初始化配置
   */
  async initialize(config: ASRInitConfig): Promise<void> {
    if (this.isInitialized) {
      throw new Error('SDK 已经初始化');
    }

    const initParams = this.buildInitParams(config);

    await AliyunASRModule.initialize(
      initParams,
      config.logLevel ?? LogLevel.INFO,
      config.saveLog ?? false,
    );

    this.isInitialized = true;
    this.setupEventListeners();
  }

  /**
   * 释放 SDK 资源
   */
  async release(): Promise<void> {
    if (!this.isInitialized) {
      return;
    }

    this.removeEventListeners();
    await AliyunASRModule.release();
    this.isInitialized = false;
  }

  /**
   * 开始语音识别
   * @param vadMode VAD 模式，默认 MODE_VAD
   * @param params 识别参数（可选）
   */
  async startRecognition(
    vadMode: VadMode = VadMode.MODE_VAD,
    params?: ASRDialogParams,
  ): Promise<void> {
    this.ensureInitialized();

    const dialogParams = this.buildDialogParams(params);
    await AliyunASRModule.startDialog(vadMode, dialogParams);
  }

  /**
   * 停止语音识别
   */
  async stopRecognition(): Promise<void> {
    this.ensureInitialized();
    await AliyunASRModule.stopDialog();
  }

  /**
   * 取消语音识别（不会返回结果）
   * @param force 是否强制取消，默认 false
   */
  async cancelRecognition(force: boolean = false): Promise<void> {
    this.ensureInitialized();
    await AliyunASRModule.cancelDialog(force);
  }

  /**
   * 设置参数
   * @param key 参数名
   * @param value 参数值
   */
  async setParam(key: string, value: string): Promise<void> {
    this.ensureInitialized();
    await AliyunASRModule.setParam(key, value);
  }

  /**
   * 获取参数
   * @param key 参数名
   */
  async getParam(key: string): Promise<string> {
    this.ensureInitialized();
    return await AliyunASRModule.getParam(key);
  }

  /**
   * 订阅事件
   * @param event 事件类型
   * @param callback 回调函数
   */
  on(event: ASREvent, callback: ASREventCallback): void {
    const eventName = event.toString();
    if (!this.eventCallbacks.has(eventName)) {
      this.eventCallbacks.set(eventName, new Set());
    }
    this.eventCallbacks.get(eventName)!.add(callback);
  }

  /**
   * 取消订阅事件
   * @param event 事件类型
   * @param callback 回调函数
   */
  off(event: ASREvent, callback: ASREventCallback): void {
    const eventName = event.toString();
    const callbacks = this.eventCallbacks.get(eventName);
    if (callbacks) {
      callbacks.delete(callback);
    }
  }

  /**
   * 一次性订阅事件
   * @param event 事件类型
   * @param callback 回调函数
   */
  once(event: ASREvent, callback: ASREventCallback): void {
    const wrapper = (data: ASREventData) => {
      this.off(event, wrapper);
      callback(data);
    };
    this.on(event, wrapper);
  }

  // ============ 私有方法 ============

  private ensureInitialized(): void {
    if (!this.isInitialized) {
      throw new Error('SDK 未初始化，请先调用 initialize()');
    }
  }

  private buildInitParams(config: ASRInitConfig): string {
    const params: any = {
      app_key: config.appKey,
      token: config.token,
      sample_rate: config.sampleRate || 16000,
      format: config.format || 'opus',
      service_mode: 'kModeFullCloud',
      enable_vad: config.enableVad !== false,
      vocab_default_weight: 2,
    };

    if (config.url) {
      params.url = config.url;
    }

    if (config.deviceId) {
      params.device_id = config.deviceId;
    }

    if (config.workspace) {
      params.debug_path = config.workspace;
    }

    return JSON.stringify(params);
  }

  private buildDialogParams(params?: ASRDialogParams): string {
    if (!params) {
      return '{}';
    }

    const dialogParams: any = {};

    if (params.vocabularyId) {
      dialogParams.vocabulary_id = params.vocabularyId;
    }

    if (params.maxSentenceSilence) {
      dialogParams.max_sentence_silence = params.maxSentenceSilence;
    }

    return JSON.stringify(dialogParams);
  }

  private setupEventListeners(): void {
    const subscription = eventEmitter.addListener(
      'onASREvent',
      (nativeEvent: any) => {
        const eventData: ASREventData = {
          event: nativeEvent.event,
          result: nativeEvent.result,
          errorCode: nativeEvent.errorCode,
          errorMessage: nativeEvent.errorMessage,
          wakeWord: nativeEvent.wakeWord,
          dialogId: nativeEvent.dialogId,
          isFinish: nativeEvent.isFinish,
        };

        // 触发特定事件类型的回调
        const callbacks = this.eventCallbacks.get(nativeEvent.event.toString());
        if (callbacks) {
          callbacks.forEach((cb) => {
            try {
              cb(eventData);
            } catch (e) {
              console.error('ASR 回调错误:', e);
            }
          });
        }
      },
    );

    this.subscriptions.push(subscription);
  }

  private removeEventListeners(): void {
    this.subscriptions.forEach((sub) => sub.remove());
    this.subscriptions = [];
    this.eventCallbacks.clear();
  }
}

// 导出便捷函数
export const createASR = () => AliyunASR.getInstance();
