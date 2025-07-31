#!/bin/bash

# Source the shell profile to get Java environment
source ~/.zshrc

echo "Java version:"
java -version

echo ""
echo "Building Guitar Tuner app..."
./gradlew build

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful! Your app is ready to run."
    echo ""
    echo "To install and run on a connected device:"
    echo "  ./gradlew installDebug"
    echo ""
    echo "To run on an emulator:"
    echo "  ./gradlew installDebug"
    echo ""
    echo "To clean and rebuild:"
    echo "  ./gradlew clean build"
else
    echo ""
    echo "❌ Build failed. Check the error messages above."
fi 