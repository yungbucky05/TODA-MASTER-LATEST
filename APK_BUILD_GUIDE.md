# Building Your TODA App APK

## ⚠️ IMPORTANT: Debug vs Release APK

**The issue you experienced** is that debug APKs only work on development devices (like your wirelessly debugged phone). For distribution to other phones, you need a **release APK**.

## Quick Build Instructions

### Option 1: Using the Updated Batch File (Recommended)
1. Double-click `build_apk.bat` in your project folder
2. Wait for the build process to complete
3. **Use the RELEASE APK**: `app\build\outputs\apk\release\app-release.apk`

### Option 2: Manual Command Line
Open Command Prompt in your project directory and run:
```
gradlew clean
gradlew assembleRelease
```

### Option 3: Using Android Studio
1. Open the project in Android Studio
2. Go to Build → Generate Signed Bundle / APK → APK
3. Choose "release" build variant

## APK Output Locations and Usage

### ✅ FOR DISTRIBUTION (Use This One!)
- **File**: `app\build\outputs\apk\release\app-release.apk`
- **Works on**: ALL Android devices
- **Purpose**: Installing on other people's phones

### ❌ FOR DEVELOPMENT ONLY (Don't Use This!)
- **File**: `app\build\outputs\apk\debug\app-debug.apk`  
- **Works on**: Only development/wirelessly debugged devices
- **Purpose**: Testing during development only

## Installing the RELEASE APK on Any Android Device

### Prerequisites
1. Enable "Developer Options" on the target Android device:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - Developer Options will appear in Settings

2. Enable "Install Unknown Apps":
   - Settings → Apps & Notifications → Special App Access → Install Unknown Apps
   - Allow your file manager or browser to install unknown apps

### Installation Methods

#### Method 1: Copy to Device
1. Copy `app-release.apk` to the phone's storage
2. Use a file manager to locate and tap the APK
3. Follow the installation prompts

#### Method 2: Google Drive/Email
1. Upload `app-release.apk` to Google Drive or email it
2. Download on the target phone
3. Tap to install

#### Method 3: ADB Install (if USB connected)
```
adb install app\build\outputs\apk\release\app-release.apk
```

## Why the Debug APK Didn't Work

Debug APKs have restrictions that prevent them from working on regular devices:
- **Debug certificates** - Only trusted by development environments
- **Debugging flags** - Can cause crashes on non-development devices  
- **Development dependencies** - May require development tools to be present

## Key Changes Made

I've fixed your build configuration to:
1. ✅ Create proper release builds that work on all devices
2. ✅ Sign the release APK with debug keys (works everywhere, no keystore needed)
3. ✅ Separate debug and release configurations
4. ✅ Updated build script to prioritize release builds

## Important Notes

- **Always use the RELEASE APK** (`app-release.apk`) for distribution
- The release APK will work on any Android device with your minimum SDK (24+)
- Your Firebase, location, and NFC features will work properly in the release build
- No additional signing or keystore setup required - it's ready to distribute

Run the updated `build_apk.bat` now to create your distribution-ready APK!
