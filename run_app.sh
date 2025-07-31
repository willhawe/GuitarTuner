#!/bin/bash

# Set up environment
source ~/.zshrc

echo "🎸 Building and installing Guitar Tuner app..."

# Build and install
./gradlew installDebug

if [ $? -eq 0 ]; then
    echo "✅ App installed successfully!"
    echo "🚀 Launching app..."
    
    # Launch the app
    /Users/willhawe/Library/Android/sdk/platform-tools/adb shell am start -n com.whawe.guitartuner/.MainActivity
    
    echo ""
    echo "🎉 Your Guitar Tuner is now running!"
    echo "📱 Features:"
    echo "   • Tap 'Start Tuning' to begin demo"
    echo "   • Watch the frequency and note detection"
    echo "   • See accuracy indicators (Perfect/Good/Adjust/Way off)"
    echo "   • Demo cycles through all 6 guitar strings"
    echo ""
    echo "🎸 The app will cycle through E2, A2, D3, G3, B3, E4 every 2 seconds"
    echo "   in demo mode to show how the tuner works!"
else
    echo "❌ Build failed. Check the error messages above."
fi 