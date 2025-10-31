@echo off
echo Running TODA app on virtual device...
cd /d "C:\Users\kenai\AndroidStudioProjects\TODA-MASTER-LATEST"

echo Cleaning previous builds...
call gradlew clean

echo Building and installing debug version...
call gradlew installDebug

if %ERRORLEVEL% EQU 0 (
    echo Build successful! Starting the app on device...
    adb shell am start -n "com.example.toda.debug/com.example.toda.MainActivity"
    echo Done! The app should be running on your virtual device.
) else (
    echo Build failed! Please check the error messages above.
)

pause
