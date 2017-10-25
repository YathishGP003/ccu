Powershell.exe -executionpolicy remotesigned -File  C:\Users\Yinten\repos\75F\renatus\tests\SSEZoneSetBackTest.ps1
set dateYYYY=%date:~10,4%
set dateMM=%date:~4,2%
set dateDD=%date:~7,2%
set timeHH=%time:~0,2%
set timeMM=%time:~3,2%
set timeSS=%time:~6,2%
adb shell date %dateMM%%dateDD%%timeHH%%timeMM%%dateYYYY%.%timeSS%
adb shell am instrument -w -r -e debug true -e class a75f.io.renatus.SSEZoneSetBackTest#runTest a75f.io.renatus.test/android.support.test.runner.AndroidJUnitRunner
adb pull /sdcard/simulation C:\Users\ryant\StudioProjects\Renatus\tests\TESTRESULTS\SSEZoneSetBackTest\
pause

