set app_package=a75f.io.renatus
set dir_app_name=ui-debug
set MAIN_ACTIVITY=RenatusLandingActivity

set ADB=adb
set ADB_SH=%ADB% shell su -c

set path_sysapp=/system/priv-app
set apk_host=../ui/build/outputs/apk
set apk_name=%dir_app_name%.apk
set apk_target_dir=%path_sysapp%/
set apk_target_sys=%apk_target_dir%%apk_name%

ECHO Delete previous APK
del /F /Q %apk_host%

ECHO Stop the app
%ADB% shell am force-stop %app_package%

%ADB% root


ECHO Compile the APK
call ..\gradlew assembleDebug

ECHO  mount system
%ADB% push %apk_host%/%apk_name% %apk_target_dir%


ECHO Reinstall app %ADB_SH% 'pm install -r %apk_target_sys%'
%ADB_SH% 'pm install -r %apk_target_sys%'

ECHO Start the app
%ADB% shell "am start -n %app_package%/%app_package%.%MAIN_ACTIVITY% -a android.intent.action.MAIN -c android.intent.category.LAUNCHER"

