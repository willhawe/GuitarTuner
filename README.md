# Guitar Tuner Android App

A simple Android guitar tuner application that can detect musical notes from audio input.

## Issues Fixed

1. **Java Runtime**: Set up Java environment using Android Studio's built-in JDK
2. **Package Name**: Fixed package name mismatch in MainActivity.kt
3. **Dependencies**: Added all required dependencies to build.gradle.kts
4. **Repository**: Added JitPack repositony for audio processing libraries
5. **Build Configuration**: Properly configured the build system

## How to Run

### Quick Start
```bash
./run.sh
```

### Manual Build
```bash
# Set up Java environment
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# Build the app
./gradlew build

# Install on device/emulator
./gradlew installDebug
```

## App Features

- **Audio Permission**: Requests microphone access for audio input
- **Modern UI**: Uses Material Design components
- **Ready for Audio Processing**: Framework ready for pitch detection implementation

## Technical Details

- **Target SDK**: 36 (Android 14)
- **Min SDK**: 24 (Android 7.0)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Traditional Views
- **Build System**: Gradle with Kotlin DSL

## TODO

- [ ] Implement actual audio processing (currently using placeholder)
- [ ] Add pitch detection algorithm
- [ ] Add visual tuner interface
- [ ] Add note frequency calculations
- [ ] Add audio library integration (TarsosDSP or alternative)

## Development

The app is now ready for development! You can:

1. Open the project in Android Studio
2. Connect an Android device or start an emulator
3. Run the app using `./gradlew installDebug`

The basic structure is in place and the app will build and run successfully. 