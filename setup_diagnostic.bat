@echo off
echo ====================================
echo TODA Project Setup Diagnostic
echo ====================================
echo.

echo [1/6] Checking Java Installation (needs JDK 17+)...
echo JAVA_HOME = %JAVA_HOME%
java -version 2>nul
if %errorlevel% neq 0 (
    echo ❌ Java NOT found - Install JDK 17 or use Android Studio's embedded JBR
) else (
    echo ✅ Java found
)
echo.

echo [2/6] Checking Android SDK...
if exist "%USERPROFILE%\AppData\Local\Android\Sdk" (
    echo ✅ Android SDK found at %USERPROFILE%\AppData\Local\Android\Sdk
) else (
    echo ❌ Android SDK NOT found - Install Android Studio
)
echo.

echo [3/6] Checking local.properties...
if exist "local.properties" (
    echo ✅ local.properties exists
    type local.properties
) else (
    echo ❌ local.properties missing - Will be created on first Android Studio sync
)
echo.

echo [4/6] Checking google-services.json...
if exist "app\google-services.json" (
    echo ✅ google-services.json exists
    echo 📋 Current project_id:
    findstr "project_id" app\google-services.json
) else (
    echo ❌ google-services.json missing
)
echo.

echo [5/6] Checking debug keystore...
if exist "%USERPROFILE%\.android\debug.keystore" (
    echo ✅ Debug keystore exists
    echo 📋 SHA-1 fingerprint:
    keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android 2>nul | findstr SHA1
) else (
    echo ❌ Debug keystore NOT found - Will be created on first build
)
echo.

echo [6/6] Checking Gradle Wrapper...
if exist "gradlew.bat" (
    echo ✅ Gradle wrapper exists
) else (
    echo ❌ Gradle wrapper missing
)
echo.

echo ====================================
echo Setup Summary:
echo ====================================
echo If you see any ❌ items above, follow the NEW_LAPTOP_SETUP.md guide
echo.
echo MOST IMPORTANT: Update Firebase SHA-1 fingerprint!
echo Run check_sha1.bat after Android Studio setup
echo.
pause
