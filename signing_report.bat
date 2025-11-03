@echo off
REM Auto-detect JDK 17+ and run Gradle :app:signingReport so dependencies that require Java 11+ won't fail.
REM This avoids the IDE/path defaulting to Java 8.

cd /d "%~dp0"

echo ============================================
echo  TODA - signingReport (JDK 17+ bootstrap)
echo ============================================
echo.

echo Detecting compatible Java (JDK 17+)...
setlocal enabledelayedexpansion
set "FOUND_JAVA="

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
            "!TEST_PATH!\bin\java.exe" -version >nul 2>&1
            if !ERRORLEVEL! EQU 0 (
                "!TEST_PATH!\bin\java.exe" -version 2>&1 | findstr /i "version" > "%TEMP%\javaversion.tmp"
                for /f "tokens=3" %%v in ('type "%TEMP%\javaversion.tmp"') do (
                    set "VER=%%v"
                    set "VER=!VER:"=!"
                    echo   Version: !VER!
                    echo !VER! | findstr /r "^1[7-9]\." >nul && set "FOUND_JAVA=!TEST_PATH!"
                    echo !VER! | findstr /r "^2[0-9]\." >nul && set "FOUND_JAVA=!TEST_PATH!"
                )
                del "%TEMP%\javaversion.tmp" 2>nul
                if defined FOUND_JAVA goto :JAVA_FOUND
            )
        )
    )
)

echo.
echo [ERROR] No compatible JDK 17+ found.
echo.
echo Fix:
echo  - Install JDK 17/21 (MS OpenJDK or Temurin), or
echo  - Use Android Studio's embedded JBR (point JAVA_HOME to its jbr folder).
echo  - See JAVA_SETUP_GUIDE.md.
echo.
pause
exit /b 1

:JAVA_FOUND
echo.
echo [OK] Using Java at: !FOUND_JAVA!
set "JAVA_HOME=!FOUND_JAVA!"
set "PATH=!JAVA_HOME!\bin;%PATH%"
"!JAVA_HOME!\bin\java.exe" -version

echo.
if not exist "gradlew.bat" (
  echo gradlew.bat not found. Run this from the project root.
  pause
  exit /b 1
)

echo Running: gradlew :app:signingReport ...
call gradlew.bat :app:signingReport --no-daemon
set ERR=%ERRORLEVEL%

echo.
echo ============================================
echo  Done. Look above for the SHA-1/SHA-256 per variant
echo ============================================

endlocal
exit /b %ERR%
