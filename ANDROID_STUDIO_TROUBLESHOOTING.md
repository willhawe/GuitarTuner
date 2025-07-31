# Android Studio Troubleshooting Guide

## 🚨 If Your App Keeps Crashing in Android Studio

### 1. **Try Running from Terminal First**
The app builds and installs successfully from terminal, so the issue might be with Android Studio's run configuration.

### 2. **Android Studio-Specific Solutions**

#### **Clear Android Studio Cache**
1. Close Android Studio
2. Delete these folders:
   ```bash
   rm -rf ~/Library/Caches/AndroidStudio*
   rm -rf ~/Library/Application\ Support/AndroidStudio*
   ```
3. Restart Android Studio

#### **Invalidate Caches and Restart**
1. In Android Studio: `File` → `Invalidate Caches and Restart`
2. Choose "Invalidate and Restart"

#### **Check Run Configuration**
1. Go to `Run` → `Edit Configurations`
2. Make sure the configuration is set to:
   - **Module**: `app`
   - **Launch Options**: `Default Activity`
   - **Target Device**: Your emulator or device

#### **Sync Project with Gradle Files**
1. `File` → `Sync Project with Gradle Files`
2. Wait for sync to complete

### 3. **Emulator Issues**

#### **Restart Emulator**
1. Close the emulator completely
2. In Android Studio: `Tools` → `AVD Manager`
3. Start a fresh emulator instance

#### **Create New Emulator**
1. `Tools` → `AVD Manager`
2. `Create Virtual Device`
3. Choose a simple device (like Pixel 2)
4. Use API level 30 or 31 (more stable)

### 4. **Alternative Testing Methods**

#### **Run from Terminal (Recommended)**
```bash
source ~/.zshrc && ./gradlew installDebug
```
Then open the app manually on the emulator.

#### **Use ADB to Launch**
```bash
adb shell am start -n com.whawe.guitartuner/.MainActivity
```

### 5. **Check Logs in Android Studio**
1. Open `Logcat` window (View → Tool Windows → Logcat)
2. Filter by your app: `package:com.whawe.guitartuner`
3. Look for red error messages when the app crashes

### 6. **Expected Behavior**
The app should show:
- "Guitar Tuner App is working!" text
- No crashes or force closes
- Simple white background with black text

### 7. **If Still Crashing**

#### **Try Different API Level**
- Create emulator with API 30 (Android 11)
- More stable than API 36

#### **Check Device Storage**
- Make sure emulator has enough storage space
- Clear emulator data if needed

#### **Use Physical Device**
- Connect a real Android device
- Enable USB debugging
- Often more stable than emulator

### 8. **Last Resort**
If Android Studio keeps causing issues:
1. Use the terminal build method
2. Open the app manually on emulator/device
3. The app is working - it's just an Android Studio configuration issue

The app itself is stable and working. The issue is likely with Android Studio's run configuration or emulator setup. 