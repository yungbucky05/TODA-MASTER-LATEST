@echo off
setlocal enabledelayedexpansion

echo Testing Java detection...
echo.

set "TEST_PATH=C:\Program Files\Android\Android Studio\jbr"

if exist "%TEST_PATH%\bin\java.exe" (
    echo Found java.exe at: %TEST_PATH%\bin\java.exe
    echo.
    echo Running java -version:
    "%TEST_PATH%\bin\java.exe" -version
    echo.
    echo Detection successful!
) else (
    echo ERROR: java.exe not found at expected location
)

pause

