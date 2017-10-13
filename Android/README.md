XNGAdTracker
============

Conversion tracker for tracking Android app installs triggered by app ads in the XING network.

The ads tracker component tracks conversions of your Android app ads placed in the XING network.
App installs initiated from within XING app ads are associated with install referrer metadata which 
the PlayStore app passes on to the newly installed app at first launch.
This component can be used to easily notify the XING ad network so that the installation conversion
can be evaluated in the ad manager.

The component performs its job by listening to the `com.android.vending.INSTALL_REFERRER` broadcast 
intent sent by the PlayStore application upon first launch of the app.

When the app is not installed via the PlayStore app, this broadcast intent is not sent and 
conversion tracking will not occur.

  
Deployment
----------
First, the library can be added to an app project by adding the [library AAR](adtracker/aar-release) to the app project as a 
module as described here: 

https://developer.android.com/studio/projects/android-library.html#AddDependency
 
Alternatively, include the adtracker module from the sample project into your project in order to 
make the adtracker source part of your project (also described under the link above)

Next, the install referrer receiver has to be configured in the application's manifest. 
The install referrer intent is only sent to a single BroadcastReceiver per app. As your app project 
may already have a BroadcastReceiver listening to the install referrer intent, you have to pick from 
one of the following approaches:

1. If your app has no other BroadcastReceivers listening to the install referrer intent, simply add 
the following lines within the `<application>` tag of your app's manifest:

    ```xml
        <receiver android:name="com.xing.android.adtracker.XNGAdTrackerReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter>
                <action android:name="com.android.vending.INSTALL_REFERRER" />
            </intent-filter>
        </receiver>
    ```

2. If your app already has a BroadcastReceiver registered in its manifest listening to the install 
referrer intent, you have to pick one of the following two implementations:

    a) Remove your already existing install referrer BroadcastReceiver from the manifest and instead 
    refer to it from within the manifest tag of the XING tracking receiver as as follows using 
    meta-data tags as follows:
    
    ```xml
         <receiver android:name="com.xing.android.adtracker.XNGAdTrackerReceiver"
             android:enabled="true"
             android:exported="true">
             <meta-data android:name="forward.to.receiver.1" android:value="com.example.anyadvertisedapp.Receiver"/>
             <meta-data android:name="forward.to.receiver.2" android:value="com.example.yet.another.Receiver"/>
             <intent-filter>
                 <action android:name="com.android.vending.INSTALL_REFERRER" />
             </intent-filter>
         </receiver>
    ```
    You can add one or more additional install referrer receivers by adding one meta-data tag for 
    each, with `name` being an arbitrary, unique string and `value` being the class name of the 
    receiver implementation to invoke.
    
    The `XNGAdTrackerReceiver` implementation looks for these meta-data tags and invokes them when 
    receiving the install referrer broadcast intent. This option is especially suited for calling 
    third-party install referrer receivers, particularly when you do not have access to the source 
    code.
    
    b) If you have your own install referrer receiver implementation in your code already, you can 
    simply invoke the ad conversion tracking by calling    
    `XNGAdTrackerReceiver.handleBroadcastIntent(Context context, Intent intent)` from your 
    receiver's `onReceive(Context, Intent)` implementation:

    ```java
    package com.example.my.app;
 
    // imports ...
 
    public class MyInstallReferrerReceiver extends BroadcastReceiver{
       
         // ...
  
         @Override
         public void onReceive(Context context, Intent intent) {
             
           XNGAdTrackerReceiver.handleBroadcastIntent(context, intent);
            
           // ... 
         }
    }
    ```

Dependencies
------------
Except for the AndroidSDK, this project does not have any dependencies.

Permissions
-----------
In order for the tracking to be operational, the library requests the following permissions in its 
manifest, which will be merged into your application's manifest automatically during the build 
process:

```xml
    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        package="com.xing.android.adtracker">
       
        <uses-permission android:name="android.permission.INTERNET"/>
        <uses-permission android:name="android.permission.WAKE_LOCK"/>
        <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
        
     </manifest>
```

These permissions do not require any runtime permission management as they are permissions of normal
level.

Build AAR
---------
If you would like to build the AAR from the sources, you can invoke the exportReleaseAar task to 
build it
```sh
    ./gradlew exportReleaseAar
```

The release AAR will be built and copied to the [adtracker/aar-release](adtracker/aar-release) folder.

Author
------
XING SE

License
-------
XNGAdTracker is available under the Apache License, Version 2.0. See the [LICENSE.txt](LICENSE.txt) 
file for more info.

