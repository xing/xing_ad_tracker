@import XCTest;
@import XNGAdTracker;
@import AdSupport;
#import "XNGAdTracker-Private.h"

@interface XNGAdTracker ()
@property (nonatomic) id<XNGURLSession> urlSession;
@property (nonatomic) id<XNGUserDefaults> userDefaults;
- (void)installTrackingCompletionHandlerWithResponse:(NSURLResponse * _Nullable) response withError:(NSError * _Nullable) error;
@end

#pragma mark Stubs

@interface StubURLResponse : NSHTTPURLResponse
@property (nonatomic) BOOL failing;
@end
@implementation StubURLResponse
- (NSInteger)statusCode {
    if (self.failing) {
        return 400;
    } else {
        return 200;
    }
}
@end

@interface StubURLSessionDataTask : NSURLSessionDataTask
typedef void(^DataTaskCompletionHandler)(NSData * _Nullable, NSURLResponse * _Nullable, NSError * _Nullable);
@property (nonatomic) NSURLRequest *request;
@property (nonatomic) BOOL shouldFail;
@end

@implementation StubURLSessionDataTask

- (void)resume {
    StubURLResponse *urlResponse;
    NSError *error;
    if (self.shouldFail) {
        error = [NSError errorWithDomain:NSURLErrorDomain code:404 userInfo:@{@"description":@"Error"}];
    } else {
        urlResponse = [[StubURLResponse alloc] init];
        urlResponse.failing = self.shouldFail;
    }
    
    [[XNGAdTracker getInstance] installTrackingCompletionHandlerWithResponse:urlResponse withError:error];
}
@end

@interface StubURLSession : NSObject <XNGURLSession>
@property (nonatomic) StubURLSessionDataTask* dataTask;
@property (nonatomic) BOOL shouldFail;
@end

@implementation StubURLSession
- (NSURLSessionDataTask *)dataTaskWithRequest:(NSURLRequest *)request completionHandler:(void (^)(NSData * _Nullable, NSURLResponse * _Nullable, NSError * _Nullable))completionHandler {
    self.dataTask = [[StubURLSessionDataTask alloc] init];
    self.dataTask.request = request;
    self.dataTask.shouldFail = self.shouldFail;
    if (completionHandler) {
        NSHTTPURLResponse *response;
        if (self.shouldFail) {
            response = [[NSHTTPURLResponse alloc] initWithURL:request.URL statusCode:404 HTTPVersion:@"" headerFields:@{}];
        } else {
            response = [[NSHTTPURLResponse alloc] initWithURL:request.URL statusCode:200 HTTPVersion:@"" headerFields:@{}];
        }
        completionHandler(nil, response, nil);
    }
    return self.dataTask;
}
@end

@interface StubUserDefaults : NSObject <XNGUserDefaults>
@end

@implementation StubUserDefaults
NSMutableDictionary *dictionary;

- (instancetype)init {
    self = [super init];
    if (self) {
        dictionary = [NSMutableDictionary dictionary];
    }
    return self;
}

- (void)setBool:(BOOL)value forKey:(NSString *)defaultName {
    [dictionary setValue:[NSNumber numberWithBool:value] forKey:defaultName];
}

-(BOOL)boolForKey:(NSString *)defaultName {
    return [(NSNumber*)[dictionary valueForKey:defaultName] boolValue];
}
@end

#pragma mark Tests

@interface Tests : XCTestCase
@end

@implementation Tests

- (void)setUp
{
    [super setUp];
    [XNGAdTracker reset];
}

- (void)testActivationSucceeds
{
    StubURLSession *session = [[StubURLSession alloc] init];
    [XNGAdTracker getInstance].urlSession = session;
    
    [XNGAdTracker activate];
    
    NSString *urlString = [NSString stringWithFormat:@"https://xing.com/rest/xas/ads/install?idfa=%@&lat=%@&appid=%@",
                           [ASIdentifierManager sharedManager].advertisingIdentifier.UUIDString,
                           [ASIdentifierManager sharedManager].isAdvertisingTrackingEnabled ? @"0":@"1",
                           [NSBundle mainBundle].bundleIdentifier];
    NSString *userAgentString = session.dataTask.request.allHTTPHeaderFields[@"User-Agent"];
    XCTAssertTrue([userAgentString containsString:@"XING-TRACK-IOS"]);
    XCTAssertTrue([urlString isEqualToString: session.dataTask.request.URL.absoluteString]);
}

- (void)testActivationSucceedsWithCompletion
{
    StubURLSession *session = [[StubURLSession alloc] init];
    [XNGAdTracker getInstance].urlSession = session;
    NSString *urlString = [NSString stringWithFormat:@"https://xing.com/rest/xas/ads/install?idfa=%@&lat=%@&appid=%@",
                           [ASIdentifierManager sharedManager].advertisingIdentifier.UUIDString,
                           [ASIdentifierManager sharedManager].isAdvertisingTrackingEnabled ? @"0":@"1",
                           [NSBundle mainBundle].bundleIdentifier];
    
    
    XCTestExpectation *expectation = [[XCTestExpectation alloc] initWithDescription:@"completion handler gets called"];
    
    [XNGAdTracker activateWithCompletionHandler:^(BOOL success, NSError *error){
        NSString *userAgentString = session.dataTask.request.allHTTPHeaderFields[@"User-Agent"];
        if ([userAgentString containsString:@"XING-TRACK-IOS"] &&
            [urlString isEqualToString: session.dataTask.request.URL.absoluteString]) {
            [expectation fulfill];
        }
    }];
    
    [self waitForExpectations:@[expectation] timeout:2.0];
    
}

- (void)testIgnoresSecondActivationIfFirstSucceeds {
    [XNGAdTracker getInstance].urlSession = [[StubURLSession alloc] init];
    [XNGAdTracker getInstance].userDefaults = [[StubUserDefaults alloc] init];
    [XNGAdTracker activate];
    StubURLSession *session = [[StubURLSession alloc] init];
    [XNGAdTracker getInstance].urlSession = session;
    [XNGAdTracker activate];

    XCTAssertTrue(session.dataTask.request == nil);
}

- (void)testWontIgnoreSecondActivationIfFirstFails {
    StubURLSession *session = [[StubURLSession alloc] init];
    session.shouldFail = YES;
    [XNGAdTracker getInstance].urlSession = session;
    [XNGAdTracker getInstance].userDefaults = [[StubUserDefaults alloc] init];
    [XNGAdTracker activate];
    
    session = [[StubURLSession alloc] init];
    session.shouldFail = NO;
    [XNGAdTracker getInstance].urlSession = session;
    [XNGAdTracker activate];
    
    XCTAssertTrue(session.dataTask.request != nil);
}

@end

