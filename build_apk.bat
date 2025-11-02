@echo off
echo Building TODA APK for Distribution...
echo.
echo Current directory: %CD%
echo.

REM ----------------------------------------------------------------
REM  Detect a compatible JDK (17 or higher) and set JAVA_HOME/PATH
REM ----------------------------------------------------------------
setlocal EnableExtensions EnableDelayedExpansion
echo Detecting a compatible JDK (17+)...

REM Skip subroutine definitions on startup
goto :MAIN

:try_java
set "CAND=%~1"
if "%CAND%"=="" goto :eof
if not exist "%CAND%\bin\java.exe" goto :eof
set "_JV_CMD=\"%CAND%\bin\java\" -version"
for /f "usebackq tokens=*" %%v in (`%_JV_CMD% 2^>^&1`) do (
    echo %%v | findstr /R /C:"17\." /C:"18\." /C:"19\." /C:"20\." /C:"21\." /C:"22\." /C:"23\." >nul
    if not errorlevel 1 (
        set "JAVA_HOME=%CAND%"
        set "PATH=%JAVA_HOME%\bin;%PATH%"
        echo Using Java from: %JAVA_HOME%
        java -version
        echo.
        goto :JAVA_OK
    )
)
exit /b 0

:MAIN

call :try_java "%JAVA_HOME%"
call :try_java "%ProgramFiles%\Android\Android Studio\jbr"
call :try_java "%ProgramFiles%\Android\Android Studio1\jbr"
for /d %%d in ("%ProgramFiles%\Java\jdk-*") do call :try_java "%%~fd"
for /d %%d in ("%ProgramFiles%\Microsoft\jdk-*") do call :try_java "%%~fd"
for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\jdk-*") do call :try_java "%%~fd"
for /d %%d in ("%ProgramFiles%\Zulu\zulu-*-jdk") do call :try_java "%%~fd"
if defined ProgramFiles(x86) (
  for /d %%d in ("%ProgramFiles(x86)%\Java\jdk-*") do call :try_java "%%~fd"
  for /d %%d in ("%ProgramFiles(x86)%\Microsoft\jdk-*") do call :try_java "%%~fd"
  for /d %%d in ("%ProgramFiles(x86)%\Eclipse Adoptium\jdk-*") do call :try_java "%%~fd"
  for /d %%d in ("%ProgramFiles(x86)%\Zulu\zulu-*-jdk") do call :try_java "%%~fd"
)

echo.
echo ERROR: No compatible JDK 17+ found on this system.
echo        Install JDK 17 or use Android Studio's embedded JBR.
echo        See JAVA_SETUP_GUIDE.md for details.
pause >nul
exit /b 1

:JAVA_OK
set "GRADLE_JAVA_ARG=-Dorg.gradle.java.home=%JAVA_HOME%"

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
call gradlew.bat %GRADLE_JAVA_ARG% clean
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
call gradlew.bat %GRADLE_JAVA_ARG% assembleRelease
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
call gradlew.bat %GRADLE_JAVA_ARG% assembleDebug
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
    echo FOUND: app\build\outputs\apk\release\app-release.apk
    echo   FOR DISTRIBUTION - Use this APK for other phones!
) else (
    echo NOT FOUND: Release APK was not created
)

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo FOUND: app\build\outputs\apk\debug\app-debug.apk
    echo   FOR DEVELOPMENT ONLY - Only works on debug devices
) else (
    echo NOT FOUND: Debug APK was not created
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
