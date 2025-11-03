@echo off

REM Change to the directory where this batch file is located
cd /d "%~dp0"

echo Testing TODA Release APK Build (passenger/driver/admin/barker)...
echo.
echo Current directory: %CD%
echo.

REM ============================================
REM  JAVA DETECTION AND SETUP
REM ============================================
echo Detecting compatible Java (JDK 17+)...
setlocal enabledelayedexpansion

set "FOUND_JAVA="

REM Check common Android Studio JBR locations
set "JAVA_PATHS[0]=C:\Program Files\Android\Android Studio\jbr"
set "JAVA_PATHS[1]=C:\Program Files\Android\Android Studio1\jbr"
set "JAVA_PATHS[2]=%LocalAppData%\Programs\Android Studio\jbr"
set "JAVA_PATHS[3]=C:\Program Files\Microsoft\jdk-17.0.11.9-hotspot"
set "JAVA_PATHS[4]=C:\Program Files\Microsoft\jdk-21"
set "JAVA_PATHS[5]=C:\Program Files\Java\jdk-17"
set "JAVA_PATHS[6]=C:\Program Files\Java\jdk-21"
set "JAVA_PATHS[7]=C:\Program Files\Eclipse Adoptium\jdk-17"
set "JAVA_PATHS[8]=C:\Program Files\Eclipse Adoptium\jdk-21"

for /L %%i in (0,1,8) do (
    if defined JAVA_PATHS[%%i] (
        set "TEST_PATH=!JAVA_PATHS[%%i]!"
        if exist "!TEST_PATH!\bin\java.exe" (
            echo Checking: !TEST_PATH!
            REM Test if java.exe runs successfully
            "!TEST_PATH!\bin\java.exe" -version >nul 2>&1
            if !ERRORLEVEL! EQU 0 (
                REM Get version - redirect to temp file to avoid quoting issues
                "!TEST_PATH!\bin\java.exe" -version 2>&1 | findstr /i "version" > "%TEMP%\javaversion.tmp"
                for /f "tokens=3" %%v in ('type "%TEMP%\javaversion.tmp"') do (
                    set "VER=%%v"
                    set "VER=!VER:"=!"
                    echo   Version: !VER!
                    REM Check if version is 17 or higher (simple check)
                    echo !VER! | findstr /r "^1[7-9]\." >nul
                    if !ERRORLEVEL! EQU 0 (
                        set "FOUND_JAVA=!TEST_PATH!"
                        del "%TEMP%\javaversion.tmp" 2>nul
                        goto :JAVA_FOUND
                    )
                    echo !VER! | findstr /r "^2[0-9]\." >nul
                    if !ERRORLEVEL! EQU 0 (
                        set "FOUND_JAVA=!TEST_PATH!"
                        del "%TEMP%\javaversion.tmp" 2>nul
                        goto :JAVA_FOUND
                    )
                )
                del "%TEMP%\javaversion.tmp" 2>nul
            )
        )
    )
)

echo.
echo [ERROR] No compatible JDK 17+ found!
echo.
echo The build requires JDK 17 or newer, but only Java 8 was found in PATH.
echo.
echo Please install one of the following:
echo   1. Android Studio (comes with embedded JBR)
echo   2. Microsoft OpenJDK 17 or 21
echo   3. Eclipse Temurin (Adoptium) JDK 17 or 21
echo.
echo See JAVA_SETUP_GUIDE.md for detailed instructions.
echo.
echo Press any key to exit...
pause >nul
exit /b 1

:JAVA_FOUND
echo.
echo [SUCCESS] Found compatible Java at: !FOUND_JAVA!
set "JAVA_HOME=!FOUND_JAVA!"
set "PATH=!JAVA_HOME!\bin;%PATH%"
echo.
echo Using this Java version:
"!JAVA_HOME!\bin\java.exe" -version
echo.

REM Check if gradlew exists
if not exist "gradlew.bat" (
    echo ERROR: gradlew.bat not found in current directory!
    echo Please make sure you're running this from the project root directory.
    echo Current directory: %CD%
    echo.
    echo Press any key to exit...
    pause >nul
    exit /b 1
)

echo Attempting to unlock build folder...
REM Kill any running gradle/java processes to avoid file locks
taskkill /f /im java.exe 2>nul
taskkill /f /im gradle.exe 2>nul
timeout /t 2 /nobreak >nul

REM Try to manually delete the build folder if it exists
if exist "build" (
    echo Removing existing build folder...
    rmdir /s /q "build" 2>nul
)
if exist "app\build" (
    echo Removing existing app build folder...
    rmdir /s /q "app\build" 2>nul
)

echo Waiting for file handles to release...
timeout /t 3 /nobreak >nul

echo Starting Gradle daemon cleanup...
call gradlew.bat --stop
timeout /t 2 /nobreak >nul

REM Do a single clean before all builds
echo Performing initial clean...
call gradlew.bat clean --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo Initial clean failed. Check output above.
    echo.
    echo Press any key to exit...
    pause >nul
    exit /b 1
)

REM Build Passenger release
echo.
echo Building Passenger release APK...
call gradlew.bat assemblePassengerRelease --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo BUILD FAILED for passenger. Check the Gradle output above.
    echo.
    echo Press any key to exit...
    pause >nul
    exit /b 1
)
set APK_PASSENGER=app\build\outputs\apk\passenger\release\app-passenger-release.apk
echo Checking for APK at %APK_PASSENGER%
if exist "%APK_PASSENGER%" (
    echo ✓ SUCCESS: %APK_PASSENGER% created!
) else (
    echo ✗ WARNING: Expected APK not found at %APK_PASSENGER%
    dir "app\build\outputs\apk\passenger\release\" || dir "app\build\outputs\apk\passenger\"
)

REM Build Driver release
echo.
echo Building Driver release APK...
call gradlew.bat assembleDriverRelease --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo BUILD FAILED for driver. Check the Gradle output above.
    echo.
    echo Press any key to exit...
    pause >nul
    exit /b 1
)
set APK_DRIVER=app\build\outputs\apk\driver\release\app-driver-release.apk
echo Checking for APK at %APK_DRIVER%
if exist "%APK_DRIVER%" (
    echo ✓ SUCCESS: %APK_DRIVER% created!
) else (
    echo ✗ WARNING: Expected APK not found at %APK_DRIVER%
    dir "app\build\outputs\apk\driver\release\" || dir "app\build\outputs\apk\driver\"
)

REM Build Admin release
echo.
echo Building Admin release APK...
call gradlew.bat assembleAdminRelease --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo BUILD FAILED for admin. Check the Gradle output above.
    echo.
    echo Press any key to exit...
    pause >nul
    exit /b 1
)
set APK_ADMIN=app\build\outputs\apk\admin\release\app-admin-release.apk
echo Checking for APK at %APK_ADMIN%
if exist "%APK_ADMIN%" (
    echo ✓ SUCCESS: %APK_ADMIN% created!
) else (
    echo ✗ WARNING: Expected APK not found at %APK_ADMIN%
    dir "app\build\outputs\apk\admin\release\" || dir "app\build\outputs\apk\admin\"
)

REM Build Barker release
echo.
echo Building Barker release APK...
call gradlew.bat assembleBarkerRelease --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo BUILD FAILED for barker. Check the Gradle output above.
    echo.
    echo Press any key to exit...
    pause >nul
    exit /b 1
)
set APK_BARKER=app\build\outputs\apk\barker\release\app-barker-release.apk
echo Checking for APK at %APK_BARKER%
if exist "%APK_BARKER%" (
    echo ✓ SUCCESS: %APK_BARKER% created!
) else (
    echo ✗ WARNING: Expected APK not found at %APK_BARKER%
    dir "app\build\outputs\apk\barker\release\" || dir "app\build\outputs\apk\barker\"
)

echo.
echo ========================================
echo ALL REQUESTED BUILDS COMPLETED
echo ========================================
echo.
echo Press any key to exit...
pause >nul
endlocal
