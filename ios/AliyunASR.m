// AliyunASR.m

#import "AliyunASR.h"
#import <React/RCTLog.h>

@interface AliyunASR ()
@property (nonatomic, strong) NeoNui *nuiSdk;
@property (nonatomic, assign) BOOL hasListeners;
@end

@implementation AliyunASR

RCT_EXPORT_MODULE(AliyunASRModule);

+ (instancetype)sharedInstance {
    static AliyunASR *instance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[self alloc] init];
    });
    return instance;
}

- (NSArray<NSString *> *)supportedEvents {
    return @[@"onASREvent", @"onASRAudioState"];
}

- (void)startObserving {
    _hasListeners = YES;
}

- (void)stopObserving {
    _hasListeners = NO;
}

RCT_EXPORT_METHOD(initialize:(NSString *)parameters
                  logLevel:(NSInteger)logLevel
                  saveLog:(BOOL)saveLog
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    dispatch_async(dispatch_get_main_queue(), ^{
        @try {
            self.nuiSdk = [NeoNui get_instance];
            self.nuiSdk.delegate = self;
            
            NuiSdkLogLevel level = (NuiSdkLogLevel)logLevel;
            int result = [self.nuiSdk nui_initialize:[parameters UTF8String]
                                            logLevel:level
                                             saveLog:saveLog];
            
            if (result == SUCCESS) {
                resolve(@(YES));
            } else {
                reject(@"INIT_ERROR", [NSString stringWithFormat:@"初始化失败，错误码: %d", result], nil);
            }
        } @catch (NSException *exception) {
            reject(@"INIT_EXCEPTION", exception.reason, nil);
        }
    });
}

RCT_EXPORT_METHOD(release:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.nuiSdk) {
            int result = [self.nuiSdk nui_release];
            self.nuiSdk = nil;
            
            if (result == SUCCESS) {
                resolve(@(YES));
            } else {
                reject(@"RELEASE_ERROR", [NSString stringWithFormat:@"释放失败，错误码: %d", result], nil);
            }
        } else {
            resolve(@(YES));
        }
    });
}

RCT_EXPORT_METHOD(startDialog:(NSInteger)vadMode
                  dialogParams:(NSString *)dialogParams
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (!self.nuiSdk) {
            reject(@"NOT_INITIALIZED", @"SDK 未初始化", nil);
            return;
        }
        
        NuiVadMode mode = (NuiVadMode)vadMode;
        int result = [self.nuiSdk nui_dialog_start:mode
                                       dialogParam:[dialogParams UTF8String]];
        
        if (result == SUCCESS) {
            resolve(@(YES));
        } else {
            reject(@"START_ERROR", [NSString stringWithFormat:@"开始识别失败，错误码: %d", result], nil);
        }
    });
}

RCT_EXPORT_METHOD(stopDialog:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (!self.nuiSdk) {
            reject(@"NOT_INITIALIZED", @"SDK 未初始化", nil);
            return;
        }
        
        int result = [self.nuiSdk nui_dialog_cancel:NO];
        
        if (result == SUCCESS) {
            resolve(@(YES));
        } else {
            reject(@"STOP_ERROR", [NSString stringWithFormat:@"停止识别失败，错误码: %d", result], nil);
        }
    });
}

RCT_EXPORT_METHOD(cancelDialog:(BOOL)force
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (!self.nuiSdk) {
            reject(@"NOT_INITIALIZED", @"SDK 未初始化", nil);
            return;
        }
        
        int result = [self.nuiSdk nui_dialog_cancel:force];
        
        if (result == SUCCESS) {
            resolve(@(YES));
        } else {
            reject(@"CANCEL_ERROR", [NSString stringWithFormat:@"取消识别失败，错误码: %d", result], nil);
        }
    });
}

RCT_EXPORT_METHOD(setParam:(NSString *)key
                  value:(NSString *)value
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (!self.nuiSdk) {
            reject(@"NOT_INITIALIZED", @"SDK 未初始化", nil);
            return;
        }
        
        int result = [self.nuiSdk nui_set_param:[key UTF8String] Value:[value UTF8String]];
        
        if (result == SUCCESS) {
            resolve(@(YES));
        } else {
            reject(@"SET_PARAM_ERROR", [NSString stringWithFormat:@"设置参数失败，错误码: %d", result], nil);
        }
    });
}

RCT_EXPORT_METHOD(getParam:(NSString *)key
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (!self.nuiSdk) {
            reject(@"NOT_INITIALIZED", @"SDK 未初始化", nil);
            return;
        }
        
        const char *value = [self.nuiSdk nui_get_param:[key UTF8String]];
        resolve([NSString stringWithUTF8String:value]);
    });
}

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
        @"isFinish": @(finish)
    } mutableCopy];
    
    if (asr_result != NULL) {
        NSString *resultText = [NSString stringWithUTF8String:asr_result];
        eventData[@"result"] = @{
            @"text": resultText,
            @"isFinal": @(nuiEvent == EVENT_ASR_RESULT || nuiEvent == EVENT_SENTENCE_END)
        };
    }
    
    if (code != 0) {
        eventData[@"errorMessage"] = [self getErrorMessage:code];
    }
    
    if (_hasListeners) {
        [self sendEventWithName:@"onASREvent" body:eventData];
    }
}

- (void)onNuiAudioStateChanged:(NuiAudioState)state {
    if (_hasListeners) {
        [self sendEventWithName:@"onASRAudioState" body:@{
            @"type": @"audioState",
            @"state": @(state)
        }];
    }
}

- (NSString *)getErrorMessage:(int)code {
    switch (code) {
        case 240001: return @"配置或文件无效";
        case 240002: return @"参数非法";
        case 240011: return @"SDK 未初始化";
        case 240012: return @"SDK 已初始化";
        case 240052: return @"麦克风错误";
        case 240070: return @"认证失败";
        case 240091: return @"连接超时";
        case 240093: return @"识别超时";
        default: return [NSString stringWithFormat:@"未知错误: %d", code];
    }
}

@end
