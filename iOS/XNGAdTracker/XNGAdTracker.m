#import <UIKit/UIKit.h>
#import <AdSupport/ASIdentifierManager.h>
#import "XNGAdTracker.h"
#import "XNGAdTracker-Private.h"

NS_ASSUME_NONNULL_BEGIN;

static NSString *const kAPIBaseURL = @"https://xing.com/rest/xas/ads/";
static NSString *const kUserAgentName = @"XING-TRACK-IOS";
static NSString *const kAppInstallTrackedKey = @"XING-APP-INSTALL-TRACKED";
static NSErrorDomain XNGAdTrackerErrorDomain = @"XNGAdTrackerErrorDomain";

@interface XNGAdTrackerLogger: NSObject
+ (void)logError:(NSString*)message;
+ (void)logMessage:(NSString*)message;
+ (void)setEnabled:(BOOL)enabled;
@end

@implementation XNGAdTrackerLogger
static BOOL _enabled = NO;

+ (void)setEnabled:(BOOL)enabled {
    _enabled = enabled;
}

+ (void)logError:(NSString*)message {
    if (_enabled) {
        NSLog(@"XNGAdTracker - Error: %@",message);
    }
}

+ (void)logMessage:(NSString*)message {
    if (_enabled) {
        NSLog(@"XNGAdTracker - %@",message);
    }
}
@end

@interface XNGAdTracker ()
@property(nonatomic, readonly) NSString *adID;
@property(nonatomic, readonly) NSString *appID;
@property (nonatomic) id<XNGURLSession> urlSession;
@property (nonatomic) id<XNGUserDefaults> userDefaults;
@end
@implementation XNGAdTracker

+ (instancetype)getInstance {
    static XNGAdTracker *defaultInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        defaultInstance = [[self alloc] init];
        [XNGAdTrackerLogger setEnabled:NO];
    });
    
    return defaultInstance;
}

+ (void)activate {
    [XNGAdTracker activateWithCompletionHandler:nil];
}

+ (void)activateWithCompletionHandler:(nullable void (^)(BOOL, NSError *))completionHandler {
    [[XNGAdTracker getInstance] trackInstallWithCompletionHandler:completionHandler];
}

+ (void)reset {
    [[[XNGAdTracker getInstance] userDefaults] setBool:false forKey:kAppInstallTrackedKey];
}

- (instancetype)init {
    self = [super init];
    if (self != nil){
        _urlSession = (id<XNGURLSession>)[NSURLSession sharedSession];
        _userDefaults = (id<XNGUserDefaults>)[NSUserDefaults standardUserDefaults];
    }
    
    return self;
}

- (void)trackInstallWithCompletionHandler:(nullable void (^)(BOOL success, NSError * _Nullable error))completionHandler {
    if ([[self userDefaults] boolForKey:kAppInstallTrackedKey]) {
        return;
    }
    
    NSURL *url = [self installURLWithIDFA: self.adID
                        adTrackingEnabled:[ASIdentifierManager sharedManager].advertisingTrackingEnabled
                                    appID:self.appID];
    if (url == nil) {
        [XNGAdTrackerLogger logError:@"Malformed tracking url."];
        if (completionHandler) {
            completionHandler(NO, [NSError errorWithDomain:XNGAdTrackerErrorDomain code:0 userInfo:@{NSLocalizedDescriptionKey:@"Malformed tracking url."}]);
        }
        return;
    }
    [XNGAdTrackerLogger logMessage:[NSString stringWithFormat:@"Tracking: %@", url.absoluteString]];
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:url];
    [request setValue:[self userAgentString] forHTTPHeaderField:@"User-Agent"];

    [[self.urlSession dataTaskWithRequest:request completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        BOOL success = [self installTrackingCompletionHandlerWithResponse:response withError:error];
        if (completionHandler) {
            completionHandler(success, error);
        }
    }] resume];
}

- (void)persistTrackingState {
    [[self userDefaults] setBool:true forKey:kAppInstallTrackedKey];
}

#pragma mark: Private Methods

- (BOOL)installTrackingCompletionHandlerWithResponse:(NSURLResponse * _Nullable) response withError:(NSError * _Nullable) error {
    NSHTTPURLResponse* httpResponse = (NSHTTPURLResponse*)response;
    
    if (error) {
        [XNGAdTrackerLogger logError:error.localizedDescription];
        return NO;
    } else if (httpResponse.statusCode >= 200 && httpResponse.statusCode < 300) {
        [XNGAdTrackerLogger logMessage:@"App install tracking succeeded."];
        [self persistTrackingState];
        return YES;
    }
    
    [XNGAdTrackerLogger logError:[NSString stringWithFormat:@"Server responded with staus code %@", @(httpResponse.statusCode)]];
    return NO;
}

- (NSString *)userAgentString {
    NSString *appVersion = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleShortVersionString"];
    UIDevice *device = [UIDevice currentDevice];
    NSString *deviceName = [device model];
    NSString *OSVersion = [device systemVersion];
    
    return [NSString stringWithFormat:@"%@/%@ Device/%@ OS-Version/%@", kUserAgentName, appVersion, deviceName, OSVersion];
}

- (NSURL*)installURLWithIDFA:(NSString *)idfa
           adTrackingEnabled:(BOOL)trackingEnabled
                       appID:(NSString*)appID {
    NSString *urlString = [[NSURL URLWithString:kAPIBaseURL] URLByAppendingPathComponent:@"install"].absoluteString;
    NSURLComponents *components = [[NSURLComponents alloc] initWithString:urlString];
    components.query = [NSString stringWithFormat:@"idfa=%@&lat=%@&appid=%@", idfa, trackingEnabled ? @"0":@"1", appID];

    return components.URL;
}

- (NSString*)adID {
    return [[[ASIdentifierManager sharedManager] advertisingIdentifier] UUIDString];
}

- (NSString*)appID {
    return [NSString stringWithFormat:@"%@", [NSBundle mainBundle].bundleIdentifier];
}

@end
NS_ASSUME_NONNULL_END;
