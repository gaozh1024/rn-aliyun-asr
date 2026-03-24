// AliyunASR.h

#import <React/RCTEventEmitter.h>
#import <React/RCTBridgeModule.h>
#import <nuisdk/NeoNui.h>

@interface AliyunASR : RCTEventEmitter <RCTBridgeModule, NeoNuiSdkDelegate>

@end
