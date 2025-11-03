@echo off
echo ============================================
echo  JDK 17 Download and Installation Guide
echo ============================================
echo.

REM Check if Android Studio is already installed
echo Checking for existing Android Studio installation...
if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    echo.
    echo [FOUND] Android Studio JBR detected at:
    echo C:\Program Files\Android\Android Studio\jbr
    echo.
    "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" -version
    echo.
    echo You already have a compatible JDK! Try running test_release_build.bat again.
    echo.
    pause
    exit /b 0
)

echo Not found at default location.
echo.

echo ============================================
echo  OPTION 1: Microsoft OpenJDK 17 (Recommended)
echo ============================================
echo.
echo This will open your browser to download Microsoft OpenJDK 17.
echo.
echo Instructions:
echo   1. Download the Windows x64 MSI installer
echo   2. Run the installer
echo   3. Accept default installation options
echo   4. After installation, run test_release_build.bat again
echo.
echo Download URL: https://learn.microsoft.com/en-us/java/openjdk/download#openjdk-17
echo.
set /p "choice=Press Y to open browser, or N to see other options: "
if /i "%choice%"=="Y" (
    start https://learn.microsoft.com/en-us/java/openjdk/download#openjdk-17
    echo.
    echo Browser opened. After installing JDK, run test_release_build.bat
    pause
    exit /b 0
)

echo.
echo ============================================
echo  OPTION 2: Eclipse Temurin (Adoptium)
echo ============================================
echo.
echo Another popular free JDK distribution.
echo.
echo Instructions:
echo   1. Download JDK 17 or 21 for Windows x64
echo   2. Run the MSI installer
echo   3. Make sure to check "Set JAVA_HOME" during installation
echo   4. After installation, run test_release_build.bat again
echo.
echo Download URL: https://adoptium.net/temurin/releases/?version=17
echo.
set /p "choice=Press Y to open browser: "
if /i "%choice%"=="Y" (
    start https://adoptium.net/temurin/releases/?version=17
    echo.
    echo Browser opened. After installing JDK, run test_release_build.bat
    pause
    exit /b 0
)

echo.
echo ============================================
echo  OPTION 3: Use Android Studio's JBR
echo ============================================
echo.
echo If you already have Android Studio installed elsewhere,
echo you can manually set JAVA_HOME to point to its JBR.
echo.
echo Common Android Studio JBR locations:
echo   C:\Program Files\Android\Android Studio\jbr
echo   C:\Program Files\Android\Android Studio1\jbr
echo   %LocalAppData%\Programs\Android Studio\jbr
echo.
echo To set JAVA_HOME permanently:
echo   1. Press Win + R, type: sysdm.cpl
echo   2. Click "Environment Variables"
echo   3. Under System Variables, click "New"
echo   4. Variable name: JAVA_HOME
echo   5. Variable value: (path to JBR folder)
echo   6. Click OK, then add %%JAVA_HOME%%\bin to PATH
echo.
echo See JAVA_SETUP_GUIDE.md for detailed instructions.
echo.
pause

