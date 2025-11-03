@echo off
echo Checking SHA-1 fingerprints for Firebase configuration...
echo.
echo === DEBUG KEYSTORE SHA-1 ===
keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
echo.
echo === RELEASE KEYSTORE SHA-1 (if exists) ===
if exist keystore.jks (
    keytool -list -v -keystore keystore.jks -alias release
) else (
    echo No release keystore found
)
echo.
echo Copy the SHA-1 fingerprint(s) above and add them to your Firebase project console
echo Go to: https://console.firebase.google.com/project/toda-contribution-system/settings/general
pause


