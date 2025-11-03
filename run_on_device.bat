@echo off
echo Running TODA app on device...
cd /d "%~dp0"

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
pause
exit /b 1

:JAVA_OK
set "GRADLE_JAVA_ARG=-Dorg.gradle.java.home=%JAVA_HOME%"

REM ---------------------------------------------------------------
REM  Determine flavor to run (default: passenger)
REM ---------------------------------------------------------------
set "FLAVOR=%~1"
if "%FLAVOR%"=="" set "FLAVOR=passenger"

set "FLAVOR_TASK="
if /I "%FLAVOR%"=="passenger" set "FLAVOR_TASK=Passenger"
if /I "%FLAVOR%"=="driver" set "FLAVOR_TASK=Driver"
if /I "%FLAVOR%"=="admin" set "FLAVOR_TASK=Admin"
if /I "%FLAVOR%"=="barker" set "FLAVOR_TASK=Barker"

if "%FLAVOR_TASK%"=="" (
  echo Unknown flavor "%FLAVOR%". Use one of: passenger, driver, admin, barker.
  pause
  exit /b 1
)

set "INSTALL_TASK=install%FLAVOR_TASK%Debug"
set "APP_ID_BASE=com.example.toda"
set "APP_ID=%APP_ID_BASE%.%FLAVOR%.debug"
set "MAIN_ACTIVITY=com.example.toda.MainActivity"

echo Cleaning previous builds...
call gradlew.bat %GRADLE_JAVA_ARG% clean
if %ERRORLEVEL% NEQ 0 (
  echo Clean failed. Aborting.
  pause
  exit /b 1
)

echo Building and installing %FLAVOR% debug version...
call gradlew.bat %GRADLE_JAVA_ARG% %INSTALL_TASK%

if %ERRORLEVEL% EQU 0 (
    echo Build successful! Starting the app on device...
    adb shell am start -n "%APP_ID%/%MAIN_ACTIVITY%"
    echo Done! The app should be running on your device or emulator.
) else (
    echo Build failed! Please check the error messages above.
)

pause
