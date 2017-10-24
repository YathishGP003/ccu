cd C:\Users\ryant\StudioProjects\Renatus\tests
adb shell date %dateMM%%dateDD%%timeHH%%timeMM%%dateYYYY%.%timeSS%
adb shell am instrument -w -r -e debug false a75f.io.renatus.test/android.support.test.runner.AndroidJUnitRunner
#call SSEZoneSetBackTest.bat
PAUSE
adb pull /sdcard/simulation C:\Users\ryant\StudioProjects\Renatus\tests\TESTRESULTS\