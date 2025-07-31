# Guitar Tuner Debug Guide

## 🚨 If Your App Keeps Crashing

### 1. **Test the Updated App**
The app has been updated with better error handling and a test button. Try running it now - it should be more stable.

### 2. **What to Look For**
- **App opens**: Should show "Guitar Tuner Ready!" 
- **Test button**: Tap it to verify the app is working
- **Permission dialog**: Should appear asking for microphone access
- **Status updates**: The status text should update when you tap the button

### 3. **Common Crash Causes & Solutions**

#### **Theme Issues**
- **Symptom**: App crashes immediately on startup
- **Solution**: The app now has explicit theme setting and fallback UI

#### **Permission Issues**
- **Symptom**: App crashes when requesting microphone permission
- **Solution**: The app now has better permission handling

#### **Layout Issues**
- **Symptom**: App crashes when loading the layout
- **Solution**: Added fallback UI that creates a simple TextView if layout fails

### 4. **How to Get Logs (if needed)**

If the app still crashes, you can get logs by:

```bash
# Install Android Studio and use Logcat
# Or use adb if you have it installed:
adb logcat | grep GuitarTuner
```

### 5. **Quick Test Commands**

```bash
# Rebuild and install
source ~/.zshrc && ./gradlew clean installDebug

# Run the app
# Open the app on your emulator/device
```

### 6. **Expected Behavior**

✅ **App should:**
- Open without crashing
- Show "Guitar Tuner Ready!" text
- Display a "Test App" button
- Show "Status: Ready" text
- Request microphone permission when you tap the button
- Show a toast message when you tap the test button

❌ **If it still crashes:**
- Check if the emulator/device is working properly
- Try restarting the emulator
- Make sure you have enough storage space on the device

### 7. **Next Steps**

Once the app is stable, we can:
- Add the actual audio processing functionality
- Implement pitch detection
- Add a proper tuner interface

The current version is a stable foundation that should run without crashes! 