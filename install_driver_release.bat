@echo off
REM Build and install ONLY the driver Release APK, then launch it on a connected device

cd /d "%~dp0"
echo ========================================
echo Build and Install: driver Release
echo ========================================

goto :MAIN

:CHECK_JAVA
set "FOUND_JAVA="
setlocal EnableExtensions EnableDelayedExpansion

echo Detecting compatible Java (JDK 17+)...
set "JAVA_PATHS[0]=%JAVA_HOME%"
set "JAVA_PATHS[1]=C:\Program Files\Android\Android Studio\jbr"
set "JAVA_PATHS[2]=C:\Program Files\Android\Android Studio1\jbr"
set "JAVA_PATHS[3]=%LocalAppData%\Programs\Android Studio\jbr"
set "JAVA_PATHS[4]=C:\Program Files\Microsoft\jdk-21"
set "JAVA_PATHS[5]=C:\Program Files\Microsoft\jdk-17.0.11.9-hotspot"
set "JAVA_PATHS[6]=C:\Program Files\Java\jdk-21"
set "JAVA_PATHS[7]=C:\Program Files\Java\jdk-17"
set "JAVA_PATHS[8]=C:\Program Files\Eclipse Adoptium\jdk-21"
set "JAVA_PATHS[9]=C:\Program Files\Eclipse Adoptium\jdk-17"

for /L %%i in (0,1,9) do (
    if defined JAVA_PATHS[%%i] (
        set "TEST_PATH=!JAVA_PATHS[%%i]!"
        if exist "!TEST_PATH!\bin\java.exe" (
            echo Checking: !TEST_PATH!
            "!TEST_PATH!\bin\java.exe" -version >nul 2>&1
            if !ERRORLEVEL! EQU 0 (
                "!TEST_PATH!\bin\java.exe" -version 2>&1 | findstr /i "version" > "%TEMP%\javaversion.tmp"
                for /f "tokens=3" %%v in ('type "%TEMP%\javaversion.tmp"') do (
                    set "VER=%%v"
                    set "VER=!VER:"=!"
                    echo   Version: !VER!
                    echo !VER! | findstr /r "^1[7-9]\." >nul && set "FOUND_JAVA=!TEST_PATH!" && del "%TEMP%\javaversion.tmp" 2>nul && goto :JAVA_FOUND
                    echo !VER! | findstr /r "^2[0-9]\." >nul && set "FOUND_JAVA=!TEST_PATH!" && del "%TEMP%\javaversion.tmp" 2>nul && goto :JAVA_FOUND
                )
                del "%TEMP%\javaversion.tmp" 2>nul
            )
        )
    )
)

echo.
echo [ERROR] No compatible JDK 17+ found!
echo See JAVA_SETUP_GUIDE.md for installation options.
echo.
pause >nul
endlocal
exit /b 1

:JAVA_FOUND
echo.
echo [SUCCESS] Using Java from: !FOUND_JAVA!
endlocal & set "FOUND_JAVA=%FOUND_JAVA%"
set "JAVA_HOME=%FOUND_JAVA%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
"%JAVA_HOME%\bin\java.exe" -version

echo.
exit /b 0

:MAIN
call :CHECK_JAVA || exit /b 1

if not exist "gradlew.bat" (
    echo ERROR: gradlew.bat not found in %CD%
    pause >nul
    exit /b 1
)

echo.
echo Building driver Release APK...
call gradlew.bat assembleDriverRelease --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo BUILD FAILED for driver Release. See errors above.
    pause >nul
    exit /b 1
)

set "APK_PATH=app\build\outputs\apk\driver\release\app-driver-release.apk"
echo Checking for APK: %APK_PATH%
if not exist "%APK_PATH%" (
    echo ERROR: APK not found at expected path.
    dir "app\build\outputs\apk\driver\release" 2>nul
    pause >nul
    exit /b 1
)

echo.
echo Build success! Preparing to install on device...

set "ADB=adb"
where adb >nul 2>&1
if errorlevel 1 (
    if defined ANDROID_HOME if exist "%ANDROID_HOME%\platform-tools\adb.exe" set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
)
if "%ADB%"=="adb" (
    where adb >nul 2>&1
)
if errorlevel 1 (
    if defined ANDROID_SDK_ROOT if exist "%ANDROID_SDK_ROOT%\platform-tools\adb.exe" set "ADB=%ANDROID_SDK_ROOT%\platform-tools\adb.exe"
)
if "%ADB%"=="adb" if exist "%LocalAppData%\Android\Sdk\platform-tools\adb.exe" set "ADB=%LocalAppData%\Android\Sdk\platform-tools\adb.exe"

if "%ADB%"=="adb" (
    where adb >nul 2>&1 || (
        echo ERROR: adb not found. Ensure Android SDK platform-tools are installed and in PATH.
        pause >nul
        exit /b 1
    )
)

echo Using ADB: %ADB%

set "DEVICE_SERIAL="
for /f "skip=1 tokens=1,2" %%a in ('"%ADB%" devices') do (
    if /I "%%b"=="device" if not defined DEVICE_SERIAL set "DEVICE_SERIAL=%%a"
)

if not defined DEVICE_SERIAL (
    echo ERROR: No connected device/emulator detected. Connect one and enable USB debugging.
    "%ADB%" devices
    pause >nul
    exit /b 1
)

echo Using device: %DEVICE_SERIAL%

echo Installing driver Release APK...
"%ADB%" -s "%DEVICE_SERIAL%" install -r "%APK_PATH%"
if %ERRORLEVEL% NEQ 0 (
    echo Install failed. Trying uninstall+install...
    "%ADB%" -s "%DEVICE_SERIAL%" uninstall com.example.toda.driver >nul 2>&1
    "%ADB%" -s "%DEVICE_SERIAL%" install "%APK_PATH%"
    if %ERRORLEVEL% NEQ 0 (
        echo Install failed again. Aborting.
        pause >nul
        exit /b 1
    )
)

echo Launching app main activity...
"%ADB%" -s "%DEVICE_SERIAL%" shell am start -n "com.example.toda.driver/com.example.toda.MainActivity"

if %ERRORLEVEL% EQU 0 (
    echo Success! driver Release installed and launched.
) else (
    echo Installed, but failed to auto-launch. You can open it manually.
)

echo.
echo Done.
pause >nul
exit /b 0
