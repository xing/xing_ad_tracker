#import "XNGAppDelegate.h"
@import XNGAdTracker;

@implementation XNGAppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    [XNGAdTracker activateWithCompletionHandler:^(BOOL success, NSError *error) {
        if (success) {
            NSLog(@"Success");
        } else if (error){
            NSLog(@"Error: %@", [error localizedDescription]);
        }
    }];
    return YES;
}

@end
