
set app_package=a75f.io.renatus
set dir_app_name=ui-debug
set MAIN_ACTIVITY=RenatusLandingActivity

set ADB="adb"
::ADB_SH="%ADB% shell" # this script assumes using `adb root`. for `adb su`
::see `Caveats`

set path_sysapp=/system/priv-app
set apk_host="../ui/build/outputs/apk/"
set apk_name=%dir_app_name%.apk
set apk_target_dir=%path_sysapp%/
set apk_target_sys=%apk_target_dir%/

:: Delete previous APK
del %apk_host%

:: Compile the APK: you can adapt this for production build, flavors, etc.
call ..\gradlew assembleDebug

set ADB_SH=%ADB% shell su -c

:: Install APK: using adb su
%ADB_SH% mount -o rw,remount /system
%ADB_SH% chmod 777 /system/lib/
%ADB_SH% mkdir -p /sdcard/tmp
%ADB_SH% mkdir -p %apk_target_dir%
%ADB% push %apk_host% /sdcard/tmp/
%ADB_SH% cp /sdcard/tmp/apk/%apk_name% /system/priv-app/
%ADB_SH% rm -rf /sdcard/tmp/%apk_name%

:: Give permissions
%ADB_SH% chmod 755 %apk_target_dir%
%ADB_SH% chmod 644 %apk_target_sys%
%ADB_SH% chmod 644 /system/priv-app/ui-debug.apk
::Unmount system
%ADB_SH% mount -o remount,ro /

:: Stop the app
::%ADB% shell am force-stop %app_package%

::%ADB_SH% pm install -r /system/priv-app/ui-debug.apk

::%ADB_SH% mount -o remount,ro /

:: Re execute the app
%ADB% shell am start -n \"%app_package%/%app_package%.%MAIN_ACTIVITY%\" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER