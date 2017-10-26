cd C:\Users\ryant\StudioProjects\Renatus\tests
call SSEZoneSetBackTest.bat
call SSEDeadbandTest.bat
PAUSE
adb pull /sdcard/simulation C:\Users\ryant\StudioProjects\Renatus\tests\TESTRESULTS\