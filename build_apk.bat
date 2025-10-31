@echo off
echo Building TODA APK for Distribution...
echo.
echo Current directory: %CD%
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

REM Clean the project first
echo Cleaning project...
call gradlew.bat clean
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR during clean phase. Error code: %ERRORLEVEL%
    echo This might be due to:
    echo - Java not properly installed
    echo - Android SDK not configured
    echo - Network connectivity issues
    echo.
    echo Press any key to continue and see more details...
    pause >nul
    exit /b 1
)

echo.
echo Clean completed successfully!
echo.
echo Building RELEASE APK (works on all Android devices)...
call gradlew.bat assembleRelease
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR building release APK. Error code: %ERRORLEVEL%
    echo This might be due to:
    echo - Missing dependencies
    echo - Build configuration issues
    echo - Java/Android SDK problems
    echo.
    echo Press any key to continue and try debug build...
    pause >nul
) else (
    echo.
    echo Release APK built successfully!
)

echo.
echo Building debug APK (for development only)...
call gradlew.bat assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR building debug APK. Error code: %ERRORLEVEL%
    echo.
) else (
    echo.
    echo Debug APK built successfully!
)

echo.
echo ========================================
echo BUILD PROCESS COMPLETED!
echo ========================================
echo.
echo Checking for APK files...

if exist "app\build\outputs\apk\release\app-release.apk" (
    echo ✓ FOUND: app\build\outputs\apk\release\app-release.apk
    echo   FOR DISTRIBUTION - Use this APK for other phones!
) else (
    echo ✗ NOT FOUND: Release APK was not created
)

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo ✓ FOUND: app\build\outputs\apk\debug\app-debug.apk
    echo   FOR DEVELOPMENT ONLY - Only works on debug devices
) else (
    echo ✗ NOT FOUND: Debug APK was not created
)

echo.
echo If no APK files were found, check the error messages above.
echo Common solutions:
echo 1. Make sure Java is properly installed and JAVA_HOME is set
echo 2. Ensure Android SDK is properly configured
echo 3. Check your internet connection for downloading dependencies
echo 4. Try running: setup_diagnostic.bat for detailed diagnostics
echo.
echo Press any key to exit...
pause >nul
