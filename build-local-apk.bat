@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo === PocketCode Local ZIP-to-APK Builder (Windows) ===

if not exist "debug.keystore" (
  if exist "debug.keystore.base64" (
    echo Decoding debug.keystore from debug.keystore.base64...
    powershell -NoProfile -Command "[IO.File]::WriteAllBytes('debug.keystore', [Convert]::FromBase64String((Get-Content -Raw 'debug.keystore.base64').Trim()))"
    if errorlevel 1 (
      echo Failed to decode debug.keystore.base64.
      pause
      exit /b 1
    )
  ) else (
    echo Generating fresh debug.keystore with keytool...
    keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
  )
)

set "GRADLE_CMD="
if exist "gradlew.bat" (
  set "GRADLE_CMD=gradlew.bat"
) else (
  where gradle >nul 2>nul
  if not errorlevel 1 (
    set "GRADLE_CMD=gradle"
  )
)

if "%GRADLE_CMD%"=="" (
  for /d %%D in ("%USERPROFILE%\.gradle\wrapper\dists\gradle-9.3.1-bin\*") do (
    for /d %%E in ("%%D\gradle-*") do (
      if exist "%%E\bin\gradle.bat" set "GRADLE_CMD=%%E\bin\gradle.bat"
    )
  )
)

if "%GRADLE_CMD%"=="" (
  echo.
  echo Gradle was not found in PATH.
  echo Easiest fix:
  echo   1. Open this unzipped folder in Android Studio.
  echo   2. Click Build ^> Build Bundle(s^) / APK(s^) ^> Build APK(s^).
  echo   ^(debug.keystore has already been prepared for you!^)
  echo.
  pause
  exit /b 1
)

echo Building debug APK with "%GRADLE_CMD%"...
call "%GRADLE_CMD%" :app:assembleDebug
if errorlevel 1 (
  echo.
  echo Build failed. Make sure Android SDK is installed ^(ANDROID_HOME / ANDROID_SDK_ROOT^).
  pause
  exit /b 1
)

echo.
echo SUCCESS! Your APK is ready at:
echo   %cd%\app\build\outputs\apk\debug\app-debug.apk
echo Copy this file to your Android phone and tap it to install.
echo.
pause
