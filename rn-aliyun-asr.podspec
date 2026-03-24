require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = "rn-aliyun-asr"
  s.version      = package['version']
  s.summary      = "React Native 阿里云实时语音识别"
  s.description  = package['description']
  s.homepage     = package['homepage']
  s.license      = package['license']
  s.author       = { "author" => "gaozh1024" }
  s.platform     = :ios, "11.0"
  s.source       = { :git => "https://github.com/gaozh1024/rn-aliyun-asr.git", :tag => "v#{s.version}" }

  s.source_files = "ios/**/*.{h,m}"
  
  # 引入阿里云 NUI SDK
  s.vendored_frameworks = 'ios/Frameworks/nuisdk.framework'

  s.dependency "React-Core"
  
  # 需要的系统框架
  s.frameworks = 'Foundation', 'UIKit', 'AudioToolbox', 'AVFoundation'
  
  # 库
  s.libraries = 'c++', 'z'
end
