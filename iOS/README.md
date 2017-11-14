# XNGAdTracker
Conversion tracker for tracking iOS app installs triggered by app ads in the XING network.

## Example
To run the example project, clone the repo, and run `pod install` from the Example directory first.

## Adding XNGAdTracker to your project
XNGAdTracker is available through [CocoaPods](http://cocoapods.org). To install
it, simply add the following line to your Podfile and run `pod install`:

```ruby
pod "XNGAdTracker", :git => 'https://github.com/xing/xing_ad_tracker.git', :tag => '1.0.0'
```

If you want to manually install the library download the repository into your project via git or as a zip and drag the XNGAdTracker folder into your Xcode project. If you want to use XNGAdTracker within Swift files don't forget to add `XNGAdTracker.h` to your bridging header.

## Tracking Code Integration
You should integrate the tracking code directly after the app starts i.e. in the `didFinishLaunching` or `didFinishLaunchingWithOptions` method of your app delegate:
### Swift:
```swift
import XNGAdTracker //if project was integrated via cocoapods

func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool {
    XNGAdTracker.activate()
    return true
}
```

### Objective-C:
```objc
#import "XNGAdTracker.h"
//@import XNGAdTracker; if integrated via cocoapods

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    [XNGAdTracker activate];
    return YES;
}
```

Note: You can also use `activateWithCompletion:` instead of `activate`, if you want to react to the answer of the tracking request or for debugging purposes.


# Author
XING SE

## License
XNGAdTracker is available under the Apache License, Version 2.0. See the [LICENSE](License) file for more info.
