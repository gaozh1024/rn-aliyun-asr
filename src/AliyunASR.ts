import {
  NativeModules,
  NativeEventEmitter,
  EmitterSubscription,
} from 'react-native';
import type {
  ASRInitConfig,
  ASRDialogParams,
  ASRResult,
  ASREventData,
  ASREventCallback,
  ASRAudioStateData,
  ASRAudioStateCallback,
} from './types';
import { ASREvent, VadMode, LogLevel } from './types';

const { AliyunASRModule } = NativeModules;
const eventEmitter = new NativeEventEmitter(AliyunASRModule);
const NATIVE_EVENT_NAME_TO_EVENT: Record<string, ASREvent> = {
  EVENT_VAD_START: ASREvent.VAD_START,
  EVENT_VAD_TIMEOUT: ASREvent.VAD_TIMEOUT,
  EVENT_VAD_END: ASREvent.VAD_END,
  EVENT_WUW: ASREvent.WUW,
  EVENT_WUW_TRUSTED: ASREvent.WUW_TRUSTED,
  EVENT_WUW_CONFIRMED: ASREvent.WUW_CONFIRMED,
  EVENT_WUW_REJECTED: ASREvent.WUW_REJECTED,
  EVENT_WUW_END: ASREvent.WUW_END,
  EVENT_ASR_PARTIAL_RESULT: ASREvent.ASR_PARTIAL_RESULT,
  EVENT_ASR_RESULT: ASREvent.ASR_RESULT,
  EVENT_ASR_ERROR: ASREvent.ASR_ERROR,
  EVENT_DIALOG_ERROR: ASREvent.DIALOG_ERROR,
  EVENT_ONESHOT_TIMEOUT: ASREvent.ONESHOT_TIMEOUT,
  EVENT_DIALOG_RESULT: ASREvent.DIALOG_RESULT,
  EVENT_WUW_HINT: ASREvent.WUW_HINT,
  EVENT_VPR_RESULT: ASREvent.VPR_RESULT,
  EVENT_TEXT2ACTION_DIALOG_RESULT: ASREvent.TEXT2ACTION_DIALOG_RESULT,
  EVENT_TEXT2ACTION_ERROR: ASREvent.TEXT2ACTION_ERROR,
  EVENT_ATTR_RESULT: ASREvent.ATTR_RESULT,
  EVENT_MIC_ERROR: ASREvent.MIC_ERROR,
  EVENT_DIALOG_EX: ASREvent.DIALOG_EX,
  EVENT_WUW_ERROR: ASREvent.WUW_ERROR,
  EVENT_BEFORE_CONNECTION: ASREvent.BEFORE_CONNECTION,
  EVENT_SENTENCE_START: ASREvent.SENTENCE_START,
  EVENT_SENTENCE_END: ASREvent.SENTENCE_END,
  EVENT_SENTENCE_SEMANTICS: ASREvent.SENTENCE_SEMANTICS,
  EVENT_RESULT_TRANSLATED: ASREvent.RESULT_TRANSLATED,
  EVENT_TRANSCRIBER_COMPLETE: ASREvent.TRANSCRIBER_COMPLETE,
  EVENT_FILE_TRANS_CONNECTED: ASREvent.FILE_TRANS_CONNECTED,
  EVENT_FILE_TRANS_UPLOADED: ASREvent.FILE_TRANS_UPLOADED,
  EVENT_FILE_TRANS_RESULT: ASREvent.FILE_TRANS_RESULT,
  EVENT_FILE_TRANS_UPLOAD_PROGRESS: ASREvent.FILE_TRANS_UPLOAD_PROGRESS,
  EVENT_TRANSCRIBER_STARTED: ASREvent.TRANSCRIBER_STARTED,
  EVENT_ASR_STARTED: ASREvent.ASR_STARTED,
  EVENT_FILE_TRANS_QUERY_RESULT: ASREvent.FILE_TRANS_QUERY_RESULT,
  EVENT_WUW_START: ASREvent.WUW_START,
  EVENT_WUW_DATA: ASREvent.WUW_DATA,
};

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
  private allEventCallbacks: Set<ASREventCallback> = new Set();
  private audioStateCallbacks: Set<ASRAudioStateCallback> = new Set();
  private isInitialized = false;
  private logAllEvents = true;

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
      await this.release();
    }

    this.logAllEvents = config.logAllEvents ?? true;

    const initParams = this.buildInitParams(config);

    await AliyunASRModule.initialize(
      initParams,
      config.logLevel ?? LogLevel.INFO,
      config.saveLog ?? false,
    );

    this.setupEventListeners();
    await AliyunASRModule.setParams(this.buildRecognitionParams(config));
    this.isInitialized = true;
  }

  /**
   * 释放 SDK 资源
   */
  async release(): Promise<void> {
    if (!this.isInitialized) {
      return;
    }

    this.removeNativeEventListeners();
    await AliyunASRModule.release();
    this.isInitialized = false;
  }

  /**
   * 开始语音识别
   * @param vadMode VAD 模式，默认 MODE_P2T
   * @param params 识别参数（可选）
   */
  async startRecognition(
    vadMode: VadMode = VadMode.MODE_P2T,
    params?: ASRDialogParams,
  ): Promise<void> {
    this.ensureInitialized();

    const dialogParams = this.buildDialogParams(params);
    await AliyunASRModule.startDialog(vadMode, dialogParams);
  }

  /**
   * 停止语音识别（会返回最终结果）
   */
  async stopRecognition(): Promise<void> {
    this.ensureInitialized();
    await AliyunASRModule.stopDialog();
  }

  /**
   * 取消语音识别（不会返回结果）
   * @param force 是否强制取消，默认 true
   */
  async cancelRecognition(force: boolean = true): Promise<void> {
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
   * 批量设置参数
   * @param params 参数对象
   */
  async setParams(params: Record<string, unknown>): Promise<void> {
    this.ensureInitialized();
    await AliyunASRModule.setParams(JSON.stringify(params));
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

  /**
   * 订阅全部 ASR 事件
   */
  onAllEvents(callback: ASREventCallback): void {
    this.allEventCallbacks.add(callback);
  }

  /**
   * 取消订阅全部 ASR 事件
   */
  offAllEvents(callback: ASREventCallback): void {
    this.allEventCallbacks.delete(callback);
  }

  /**
   * 订阅录音状态
   */
  onAudioStateChange(callback: ASRAudioStateCallback): void {
    this.audioStateCallbacks.add(callback);
  }

  /**
   * 取消订阅录音状态
   */
  offAudioStateChange(callback: ASRAudioStateCallback): void {
    this.audioStateCallbacks.delete(callback);
  }

  /**
   * 清空全部 JS 回调
   */
  removeAllCallbacks(): void {
    this.eventCallbacks.clear();
    this.allEventCallbacks.clear();
    this.audioStateCallbacks.clear();
  }

  // ============ 私有方法 ============

  private ensureInitialized(): void {
    if (!this.isInitialized) {
      throw new Error('SDK 未初始化，请先调用 initialize()');
    }
  }

  private buildInitParams(config: ASRInitConfig): string {
    const params: Record<string, unknown> = {
      app_key: config.appKey,
      token: config.token,
      sample_rate: config.sampleRate || 16000,
      format: config.format || 'opus',
      service_mode: 'kModeFullCloud',
      enable_vad: config.enableVad !== false,
      enable_recorder_by_user: config.enableRecorderByUser ?? false,
      vocab_default_weight: 2,
    };

    if (config.androidAudioConfig) {
      params.android_audio_config = config.androidAudioConfig;
    }

    if (config.url) {
      params.url = config.url;
    }

    if (config.deviceId) {
      params.device_id = config.deviceId;
    }

    if (config.workspace) {
      params.workspace = config.workspace;
      params.debug_path = config.workspace;
    }

    return JSON.stringify(params);
  }

  private buildDialogParams(params?: ASRDialogParams): string {
    if (!params) {
      return '{}';
    }

    const dialogParams: any = {};

    if (params.token) {
      dialogParams.token = params.token;
    }

    if (params.vocabularyId) {
      dialogParams.vocabulary_id = params.vocabularyId;
    }

    if (params.maxSentenceSilence) {
      dialogParams.max_sentence_silence = params.maxSentenceSilence;
    }

    return JSON.stringify(dialogParams);
  }

  private buildRecognitionParams(config: ASRInitConfig): string {
    const serviceType = config.serviceType === 'speechTranscriber' ? 4 : 0;

    return JSON.stringify({
      service_type: serviceType,
      nls_config: {
        enable_intermediate_result: true,
        sample_rate: config.sampleRate || 16000,
        sr_format: config.format || 'opus',
      },
    });
  }

  private setupEventListeners(): void {
    this.removeNativeEventListeners();

    const asrSubscription = eventEmitter.addListener(
      'onASREvent',
      (nativeEvent: any) => {
        const resolvedEvent =
          (nativeEvent.eventName &&
            NATIVE_EVENT_NAME_TO_EVENT[nativeEvent.eventName]) ??
          nativeEvent.event;
        const eventData: ASREventData = {
          event: resolvedEvent,
          eventName: nativeEvent.eventName,
          result: this.normalizeResult(nativeEvent.result, resolvedEvent),
          errorCode: nativeEvent.errorCode,
          errorMessage: nativeEvent.errorMessage,
          wakeWord: nativeEvent.wakeWord,
          dialogId: nativeEvent.dialogId,
          isFinish: nativeEvent.isFinish,
        };

        if (this.logAllEvents) {
          const logger =
            resolvedEvent === ASREvent.MIC_ERROR ||
            resolvedEvent === ASREvent.ASR_ERROR ||
            resolvedEvent === ASREvent.DIALOG_ERROR
              ? console.error
              : console.log;
          logger('[AliyunASR] onASREvent', eventData);
        }

        // 触发特定事件类型的回调
        const callbacks = this.eventCallbacks.get(resolvedEvent.toString());
        if (callbacks) {
          callbacks.forEach((cb) => {
            try {
              cb(eventData);
            } catch (e) {
              console.error('ASR 回调错误:', e);
            }
          });
        }

        this.allEventCallbacks.forEach((cb) => {
          try {
            cb(eventData);
          } catch (e) {
            console.error('ASR 全量事件回调错误:', e);
          }
        });
      },
    );

    const audioStateSubscription = eventEmitter.addListener(
      'onASRAudioState',
      (nativeAudioState: any) => {
        const audioStateData: ASRAudioStateData = {
          state: nativeAudioState.state as ASRAudioStateData['state'],
          stateName: nativeAudioState.stateName,
          sampleRate16kBufferSize: nativeAudioState.sampleRate16kBufferSize,
          sampleRate8kBufferSize: nativeAudioState.sampleRate8kBufferSize,
          hasRecordAudioPermission: nativeAudioState.hasRecordAudioPermission,
          usingUserRecorder: nativeAudioState.usingUserRecorder,
          currentRecorderSource: nativeAudioState.currentRecorderSource,
          recorderState: nativeAudioState.recorderState,
          recorderRecordingState: nativeAudioState.recorderRecordingState,
        };

        if (this.logAllEvents) {
          console.log('[AliyunASR] onASRAudioState', audioStateData);
        }

        this.audioStateCallbacks.forEach((cb) => {
          try {
            cb(audioStateData);
          } catch (e) {
            console.error('ASR 录音状态回调错误:', e);
          }
        });
      },
    );

    this.subscriptions.push(asrSubscription, audioStateSubscription);
  }

  private normalizeResult(
    nativeResult: any,
    event: ASREvent,
  ): ASRResult | undefined {
    if (!nativeResult) {
      return undefined;
    }

    const rawText =
      typeof nativeResult.text === 'string' || nativeResult.text == null
        ? nativeResult.text
        : String(nativeResult.text);
    const rawJson = this.tryParseResultJson(rawText);
    const payload = rawJson?.payload as Record<string, unknown> | undefined;
    const parsedText =
      typeof payload?.result === 'string' ? payload.result : undefined;
    const parsedDuration =
      typeof payload?.duration === 'number' ? payload.duration : undefined;

    return {
      text: parsedText ?? rawText,
      confidence:
        typeof nativeResult.confidence === 'number'
          ? nativeResult.confidence
          : undefined,
      isFinal:
        typeof nativeResult.isFinal === 'boolean'
          ? nativeResult.isFinal
          : event === ASREvent.ASR_RESULT || event === ASREvent.SENTENCE_END,
      sentenceId:
        typeof nativeResult.sentenceId === 'number'
          ? nativeResult.sentenceId
          : undefined,
      rawText,
      rawJson,
      duration:
        typeof nativeResult.duration === 'number'
          ? nativeResult.duration
          : parsedDuration,
    };
  }

  private tryParseResultJson(
    value: string | null | undefined,
  ): Record<string, unknown> | null {
    if (!value) {
      return null;
    }

    const trimmed = value.trim();
    if (
      !(
        (trimmed.startsWith('{') && trimmed.endsWith('}')) ||
        (trimmed.startsWith('[') && trimmed.endsWith(']'))
      )
    ) {
      return null;
    }

    try {
      const parsed = JSON.parse(trimmed);
      return parsed && typeof parsed === 'object' ? parsed : null;
    } catch {
      return null;
    }
  }

  private removeNativeEventListeners(): void {
    this.subscriptions.forEach((sub) => sub.remove());
    this.subscriptions = [];
  }
}

// 导出便捷函数
export const createASR = () => AliyunASR.getInstance();
