# XNGAdTracker
Damit Sie sehen zu können wie viele Installationen ihre Ad generiert hat, müssen Sie den XNGAdTracker in ihre iOS App integrieren.

## XNGAdTracker herunterladen
Laden Sie [XNGAdTracker]() herunter und extrahieren Sie die .zip Datei in einen Ordner ihrer Wahl.

## XNGAdTracker zum Projekt hinzufügen
Um `XNGAdTracker` in ihrem Projekt zu nutzen, gibt es zwei Möglichkeiten:

### Variante I: Integration über CocoaPods
Wenn Sie in ihrem Projekt CocoaPods nutzen können Sie `XNGAdTracker` als lokalen Pod einbinden. Verschieben Sie dazu den Ordner `iOS` in ihr Projektverzeichnis und nennen Sie ihn in `XNGAdTracker` um. Fügen Sie Ihrem Podfile anschließend folgende Zeile hinzu: `pod 'XNGAdTracker', :git => 'https://github.com/xing/xing_ad_tracker.git'` und führen Sie anschließend `pod install` aus.

### Variante II: Direkte Integration der Quelldateien
Ziehen Sie die Dateien `XNGAdTracker.h` und `XNGAdTracker.m` aus dem Verzeichnis `XNGAdTracker` per Drag and Drop in ihr Xcode Projekt. Wählen Sie `Copy items if needed` aus bevor sie auf `Finish` klicken. Falls Xcode Sie fragt, ob Sie einen Objective-C Bridging Header erstellen wollen, bejahen Sie, indem Sie `Create Bridging Header` auswählen. Möchten Sie  `XNGAdTracker` in Swift Dateien verwenden, fügen sie die Zeile `#import "XNGAdTracker.h"` zu ihrer Bridging Header Datei hinzu.

## Integration des Tracking Codes
Der Tracking Code sollte direkt nach dem App Start ausgeführt werden. Hierzu eignet sich am besten die `didFinishLaunching` bzw. `didFinishLaunchingWithOptions` methode, die sich im AppDelegate befindet:
### Swift:
```swift
import XNGAdTracker //nur wenn das projekt mit cocoapods integriert wurde

func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool {
    XNGAdTracker.activate()
    return true
}
```

### Objective-C:
```objc
#import "XNGAdTracker.h"
//oder @import XNGAdTracker; bei integration via cocoapods cocoapods

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    [XNGAdTracker activate];
    return YES;
}
```

Anmerkung: Sie können auch `activateWithCompletion:` anstatt `activate` wählen, falls sie auf die Antwort des  Tracking Requests reagieren wollen.

# English Version
In order for you to find out how many app installs your ad generated you have to integrate `XNGAdTracker` into your iOS app.

## Get XNGAdTracker
Download [XNGAdTracker]() and extract the archive into a directory of your choice.

## Add XNGAdTracker to your project
In order to use `XNGAdTracker` in your project you can choose between two possibilties:

### Variant I: Integration via CocoaPods
If you are using CocoaPods within your project, you can integrate `XNGAdTracker` as a local pod. In order to do so move the folder `iOS` into you project directory and rename it to `XNGAdTracker`. Then add the following line to your podfile: `pod 'XNGAdTracker', :path => './XNGAdTracker/'` and run `pod install`.

### Variant II: Direct integration of source files
Drag and drop `XNGAdTracker.h` and `XNGAdTracker.m` into your Xcode project. Check `Copy items if needed` before pressing `Finish`. If Xcode asks if you would like to create an Objective-C bridging header, confirm by choosing `Create Bridging Header`. If you want to use `XNGAdTracker` in Swift files add the line `#import "XNGAdTracker.h"` to your bridging header file.

## Integrate the tracking code
The tracking code should be integrated directly after the app starts i.e. in the `didFinishLaunching` or `didFinishLaunchingWithOptions` method of your app delegate:
### Swift:
```swift
import XNGAdTracker //only needed if integrated via cocoapods

func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool {
    XNGAdTracker.activate()
    return true
}
```

### Objective-C:
```objc
#import "XNGAdTracker.h"
//or @import XNGAdTracker; if integrated via cocoapods

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    [XNGAdTracker activate];
    return YES;
}
```

Note: You can also choose to use `activateWithCompletion:` instead of `activate` if you wish to react to a successfull tracking call.
