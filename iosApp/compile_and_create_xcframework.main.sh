#!/bin/bash

PROJECT=$1

if [$PROJECT -eq ""]; then
  ls -al ~/Library/Developer/Xcode/DerivedData/
  echo "Please supply project id with syntax \n"
  echo "compile_and_create_xcframework.main.sh <Projectid>"
  exit
fi

xcodebuild -scheme ARAudioKit -configuration Debug -sdk iphoneos build
xcodebuild -scheme ARAudioKit -configuration Debug -sdk iphonesimulator build

xcodebuild -create-xcframework -framework ~/Library/Developer/Xcode/DerivedData/$PROJECT/Build/Products/Debug-iphoneos/ARAudioKit.framework -framework ~/Library/Developer/Xcode/DerivedData/$PROJECT/Build/Products/Debug-iphonesimulator/ARAudioKit.framework -output ARAudioKit.xcframework