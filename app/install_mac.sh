#!/bin/bash

app_package="a75f.io.renatus"
dir_app_name="app-debug"
MAIN_ACTIVITY="SplashActivity"

ADB=adb
ADB_SH="$ADB shell su -c"

#$ADB "shell su -c"
#//$ADB root 2> /dev/null

path_sysapp=/system/priv-app # assuming the app is priviledged
apk_host=./build/outputs/apk/dev/debug/
apk_name=$dir_app_name.apk
apk_target_dir=$path_sysapp
apk_target_sys=$apk_target_dir/$apk_name

echo "force stop app package"
$ADB shell am force-stop $app_package


# Delete previous APK
rm -rf $apk_host

# Compile the APK: you can adapt this for production build, flavors, etc.
../gradlew assembleLocal #|| exit -1 # exit on failure


$ADB remount # mount system

$ADB shell rm -f /system/priv-app/app-debug.apk
#$ADB push $apk_host/$apk_name $apk_target_dir

echo "push apk ..........."
adb push ./build/outputs/apk/dev/debug/app-debug.apk /system/priv-app/

# Give permissions
$ADB_SH chmod 755 $apk_target_dir
$ADB_SH chmod 644 $apk_target_sys

# Reinstall app
#$ADB_SH pm install -r $apk_target_sys

#Unmount system
$ADB_SH mount -o remount,ro /

# Re execute the app
$ADB shell am start -n \"$app_package/$app_package.$MAIN_ACTIVITY\" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER

