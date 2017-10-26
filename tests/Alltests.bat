

DATE 10/26/17
TIME 8:00:00

set dateYYYY=%date:~10,4%
set dateMM=%date:~4,2%
set dateDD=%date:~7,2%
set timeHH=%time:~0,2%
set timeHH=%timeHH: =0%
set timeMM=%time:~3,2%
set timeSS=%time:~6,2%
adb shell date %dateMM%%dateDD%%timeHH%%timeMM%%dateYYYY%.%timeSS%
adb shell am instrument -w -r -e debug false a75f.io.renatus.test/android.support.test.runner.AndroidJUnitRunner
PAUSE
adb pull /sdcard/simulation C:\Users\ryant\StudioProjects\Renatus\tests\TESTRESULTS\