Pod::Spec.new do |s|
  s.name             = 'XNGAdTracker'
  s.version          = '1.0.0'
  s.summary          = 'Conversion tracker for tracking iOS app installs triggered by app ads in the XING network.'

  s.description      = 'XNGAdTracker is used to track conversions for app install ads in the XING network. For more information see https://www.xing.com/xas'

  s.homepage         = 'https://www.xing.com/xas'
  s.license          = { :type => 'MIT', :file => 'LICENSE' }
  s.author           = { 'XING iOS Team' => 'iphonedev@xing.com' }
  s.source           = { :git => 'https://github.com/xing/xing_ad_tracker.git', :tag => s.version.to_s }
  s.public_header_files = 'XNGAdTracker/XNGAdTracker.h'

  s.ios.deployment_target = '8.0'

  s.frameworks = 'AdSupport'
end
