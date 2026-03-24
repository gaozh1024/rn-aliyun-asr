import React, { useState, useEffect, useCallback } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  SafeAreaView,
  Alert,
  TextInput,
} from 'react-native';
import {
  AliyunASR,
  VadMode,
  ASREvent,
  LogLevel,
} from '@gaozh1024/rn-aliyun-asr';

// 配置信息（请替换为实际的 appKey 和 token）
const CONFIG = {
  appKey: '', // 请填写你的阿里云 AppKey
  token: '', // 请填写你的阿里云 Token
};

const asr = AliyunASR.getInstance();

export default function App() {
  const [isInitialized, setIsInitialized] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [results, setResults] = useState<string[]>([]);
  const [currentText, setCurrentText] = useState('');
  const [status, setStatus] = useState('未初始化');
  const [appKey, setAppKey] = useState(CONFIG.appKey);
  const [token, setToken] = useState(CONFIG.token);

  // 初始化 SDK
  const initialize = useCallback(async () => {
    if (!appKey || !token) {
      Alert.alert('错误', '请先填写 AppKey 和 Token');
      return;
    }

    try {
      setStatus('初始化中...');
      await asr.initialize({
        appKey: appKey,
        token: token,
        logLevel: LogLevel.INFO,
        saveLog: false,
      });
      setIsInitialized(true);
      setStatus('已初始化');
    } catch (error) {
      setStatus(`初始化失败: ${error}`);
      Alert.alert('初始化失败', String(error));
    }
  }, [appKey, token]);

  // 释放 SDK
  const release = useCallback(async () => {
    try {
      await asr.release();
      setIsInitialized(false);
      setStatus('已释放');
    } catch (error) {
      Alert.alert('释放失败', String(error));
    }
  }, []);

  // 开始识别
  const startRecognition = useCallback(async () => {
    try {
      await asr.startRecognition(VadMode.MODE_VAD);
      setIsRecording(true);
      setCurrentText('');
      setStatus('识别中...');
    } catch (error) {
      Alert.alert('开始识别失败', String(error));
    }
  }, []);

  // 停止识别
  const stopRecognition = useCallback(async () => {
    try {
      await asr.stopRecognition();
      setIsRecording(false);
      setStatus('已停止');
    } catch (error) {
      Alert.alert('停止识别失败', String(error));
    }
  }, []);

  // 监听识别事件
  useEffect(() => {
    // 临时结果
    asr.on(ASREvent.ASR_PARTIAL_RESULT, (data) => {
      if (data.result?.text) {
        setCurrentText(data.result.text);
      }
    });

    // 最终结果
    asr.on(ASREvent.ASR_RESULT, (data) => {
      if (data.result?.text) {
        setResults((prev) => [...prev, data.result!.text]);
        setCurrentText('');
      }
    });

    // 错误处理
    asr.on(ASREvent.ASR_ERROR, (data) => {
      setStatus(`错误: ${data.errorMessage}`);
      setIsRecording(false);
    });

    // VAD 事件
    asr.on(ASREvent.VAD_START, () => {
      setStatus('检测到语音...');
    });

    asr.on(ASREvent.VAD_END, () => {
      setStatus('语音结束');
    });

    return () => {
      asr.release().catch(console.error);
    };
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>阿里云实时语音识别 Demo</Text>

      {/* 配置输入 */}
      {!isInitialized && (
        <View style={styles.configContainer}>
          <TextInput
            style={styles.input}
            placeholder="请输入 AppKey"
            value={appKey}
            onChangeText={setAppKey}
          />
          <TextInput
            style={styles.input}
            placeholder="请输入 Token"
            value={token}
            onChangeText={setToken}
            secureTextEntry
          />
        </View>
      )}

      <Text style={styles.status}>状态: {status}</Text>

      <View style={styles.buttonContainer}>
        {!isInitialized ? (
          <TouchableOpacity style={styles.button} onPress={initialize}>
            <Text style={styles.buttonText}>初始化 SDK</Text>
          </TouchableOpacity>
        ) : (
          <>
            <TouchableOpacity
              style={[styles.button, isRecording && styles.buttonActive]}
              onPress={isRecording ? stopRecognition : startRecognition}
            >
              <Text style={styles.buttonText}>
                {isRecording ? '停止识别' : '开始识别'}
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.button, styles.dangerButton]}
              onPress={release}
            >
              <Text style={styles.buttonText}>释放 SDK</Text>
            </TouchableOpacity>
          </>
        )}
      </View>

      {currentText ? (
        <View style={styles.currentContainer}>
          <Text style={styles.currentLabel}>当前识别:</Text>
          <Text style={styles.currentText}>{currentText}</Text>
        </View>
      ) : null}

      <Text style={styles.resultLabel}>识别历史:</Text>
      <ScrollView style={styles.resultContainer}>
        {results.map((text, index) => (
          <View key={index} style={styles.resultItem}>
            <Text style={styles.resultIndex}>{index + 1}.</Text>
            <Text style={styles.resultText}>{text}</Text>
          </View>
        ))}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
    backgroundColor: '#f5f5f5',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 8,
  },
  configContainer: {
    marginBottom: 16,
  },
  input: {
    backgroundColor: '#fff',
    padding: 12,
    borderRadius: 8,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  status: {
    fontSize: 14,
    color: '#666',
    textAlign: 'center',
    marginBottom: 16,
  },
  buttonContainer: {
    gap: 12,
    marginBottom: 16,
  },
  button: {
    backgroundColor: '#1890ff',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
    alignItems: 'center',
  },
  buttonActive: {
    backgroundColor: '#ff4d4f',
  },
  dangerButton: {
    backgroundColor: '#ff4d4f',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  currentContainer: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 8,
    marginBottom: 16,
  },
  currentLabel: {
    fontSize: 14,
    color: '#666',
    marginBottom: 4,
  },
  currentText: {
    fontSize: 18,
    color: '#1890ff',
  },
  resultLabel: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
  },
  resultContainer: {
    flex: 1,
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 12,
  },
  resultItem: {
    flexDirection: 'row',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  resultIndex: {
    width: 30,
    color: '#999',
  },
  resultText: {
    flex: 1,
    fontSize: 15,
  },
});
