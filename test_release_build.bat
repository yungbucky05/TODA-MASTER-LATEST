@echo off
REM ============================================
REM  TODA APK Release Build Script (Enhanced)
REM ============================================

cd /d "%~dp0"

echo.
echo ============================================
echo  TODA Release APK Build (Passenger / Driver / Admin)
echo ============================================
echo Current directory: %CD%
echo.

REM ----------------------------------------------------------------
REM  Force Gradle to use Microsoft JDK 17
REM ----------------------------------------------------------------
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Using Java from: %JAVA_HOME%
java -version
echo.

REM ----------------------------------------------------------------
REM  Check gradlew existence
REM ----------------------------------------------------------------
if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat not found in current directory!
    echo Make sure this script is in your Android project root.
    echo Current directory: %CD%
    pause
    exit /b 1
)

REM ----------------------------------------------------------------
REM  Unlock build folder (kill gradle/java if needed)
REM ----------------------------------------------------------------
echo Attempting to unlock build folder...
taskkill /f /im java.exe 2>nul
taskkill /f /im gradle.exe 2>nul
timeout /t 2 /nobreak >nul

if exist "build" (
    echo Removing existing root build folder...
    rmdir /s /q "build"
)
if exist "app\build" (
    echo Removing existing app build folder...
    rmdir /s /q "app\build"
)
timeout /t 2 /nobreak >nul

echo Starting Gradle daemon cleanup...
call gradlew.bat --stop
timeout /t 2 /nobreak >nul

REM ----------------------------------------------------------------
REM  Do initial clean
REM ----------------------------------------------------------------
echo Performing initial clean...
call gradlew.bat clean --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Initial clean failed. Check the Gradle output above.
    pause
    exit /b 1
)

REM ----------------------------------------------------------------
REM  Build Passenger Release
REM ----------------------------------------------------------------
echo.
echo Building Passenger release APK...
call gradlew.bat assemblePassengerRelease --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] BUILD FAILED for Passenger. Check the Gradle output above.
    pause
    exit /b 1
)
set "APK_PASSENGER=app\build\outputs\apk\passenger\release\app-passenger-release.apk"
if exist "%APK_PASSENGER%" (
    echo ✓ SUCCESS: %APK_PASSENGER% created!
) else (
    echo ✗ WARNING: Expected APK not found at %APK_PASSENGER%
)

REM ----------------------------------------------------------------
REM  Build Driver Release
REM ----------------------------------------------------------------
echo.
echo Building Driver release APK...
call gradlew.bat assembleDriverRelease --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] BUILD FAILED for Driver. Check the Gradle output above.
    pause
    exit /b 1
)
set "APK_DRIVER=app\build\outputs\apk\driver\release\app-driver-release.apk"
if exist "%APK_DRIVER%" (
    echo ✓ SUCCESS: %APK_DRIVER% created!
) else (
    echo ✗ WARNING: Expected APK not found at %APK_DRIVER%
)

REM ----------------------------------------------------------------
REM  Build Admin Release
REM ----------------------------------------------------------------
echo.
echo Building Admin release APK...
call gradlew.bat assembleAdminRelease --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] BUILD FAILED for Admin. Check the Gradle output above.
    pause
    exit /b 1
)
set "APK_ADMIN=app\build\outputs\apk\admin\release\app-admin-release.apk"
if exist "%APK_ADMIN%" (
    echo ✓ SUCCESS: %APK_ADMIN% created!
) else (
    echo ✗ WARNING: Expected APK not found at %APK_ADMIN%
)

echo.
echo ============================================
echo ALL REQUESTED BUILDS COMPLETED
echo ============================================
echo.
echo Press any key to close...
pause >nul
