# CCU
Central Control Unit (CCU) is at the core of 75F platform. It is responsible for registration of new Sites, commissioning of new modules, aggregation of sensor data and controlling the building, triggering alerts, syncing building data to cloud etc. It is primarily a Java project with few android/kotlin modules.
---

<details open="open">
   <summary>Table of Contents</summary>
   <ol>
      <li><a href="#getting-started">Getting Started</a></li>
      <li><a href=“#build-apk”>Building apk</a></li>
      <li><a href=“#connect-ccu“>Connecting to CCU</a></li>
      <li><a href=“#installation”>Installation</a> </li>
    
      <li><a href="#testing">Testing</a></li>
      <ul>
         <li><a href=“#android-studio”>Android Studio</a></li>
      </ul>
   </ol>
</details>


## Getting Started

---
Follow the steps to set up authentication with the DevOps Artifact repo: https://docs.microsoft.com/en-us/azure/devops/artifacts/gradle/publish-package-gradle?view=azure-devops#set-up-authentication
* Use organization "75fdevelopment"
* Selecting "Custom defined" for the expiration will allow you to set a date up to one year in the future
* Select "Full access" for the scope of the token

## Building apk

---
Android application package (apk) is the application binary file ready for installation on an Android device.

The easiest way to start CCU development or to build an apk is by using Android Studio.
https://developer.android.com/studio/

It is an extension of IntelliJ IDEA with Android specific customizations. Most of the tools and plugins come pre-installed or there are simple wizards to guide you through completing the set up. 

CCU project supports 6 different build variants.

* Local
* Dev
* QA
* Staging
* Prod
* Daikin Prod

Easiest Option to build is to select the appropriate build variant from Android Studio’s “Build Variants” window and hitting the Play button.

If you choose to build from terminal , here are commands for various builds.

> ./gradlew assembleLocal
> ./gradlew assembleDev
> ./gradlew assembleQA
> ./gradlew assembleStaging
> ./gradlew assembleProd
> ./gradlew assembleDaikinProd


##  Connectioning to CCU

---

Android Debug Bridge (adb) is a versatile command-line tool that lets you communicate with an Android device.  

The adb command facilitates a variety of device actions, such as installing and debugging apps, and it provides access to a Unix shell that you can use to run a variety of commands on a device. 

adb is included in the Android SDK Platform-Tools package. 

https://developer.android.com/studio/releases/platform-tools 

To use adb with a device connected over USB, you must enable USB debugging in the device system settings, under Developer options. 

On Android 4.2 and higher, the Developer options screen is hidden by default. To make it visible, go to Settings > About phone and tap Build number seven times. Return to the previous screen to find Developer options at the bottom. 

You can now connect your device to PC with a micro USB cable.  

You can verify that your device is connected by executing “adb devices” command from the android_sdk/platform-tools/ directory. If connected, you'll see the device name listed as a "device." 

We can also connect to android devices without having a USB cable , by using wireless adb 

https://m.apkpure.com/adb-wireless-no-root/za.co.henry.hsu.adbwirelessbyhenry 

-> Download and install the application package from above link 

-> run adb connect command from PC as shown in the app screen. 

## Installation

---
* `$ adb push Renatus.apk /system/priv-app/
  - This is required only the very first time when we receive tablet from the vendor.
  - Before executing this, make sure application apk is removed from any other path if the 
    vendor has pre-installed in a different path. (example - vendor/other/apps)
  - Android permissions are assigned to application packages. Package name does not change 
    between releases. 
  - Subsequent updates can be done by regular install command as below. 
  - If the CCU app crashes with SecurityException when CM is connected , it is an 
    indication that the app is installed without all the required permissions.
* `$ adb install -r Renatus.apk`
Install command installs application into the data partition. 
  - */system/*     is read only partition in android file system. 
  - */data/*         is the user partition with RW permission. 


## Testing

---

### Android Studio
Right-click the `ccu` source or test directory in the project tree and select the "Run Tests ..." Option

