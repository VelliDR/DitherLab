# DitherLab Ultra

DitherLab Ultra is a sophisticated Android application that ports several visual engines from the original DitherLab PWA, offering real-time retro and glitch effects with high performance.

## Features
- **SensorCorrupt Engine**: Reacts to device gyroscope to create data mosh and corruption effects.
- **Glitch Engine**: Emulates Spider-Verse aesthetic with full RGB/CMYK channel splitting, VHS style shards, and BenDay halftone dots.
- **Van Gogh Engine**: Painterly oil-paint simulation using flow fields.
- **Subject Segmentation**: Allows applying visual effects purely to the background or subject using ML Kit's Subject Segmentation.
- **Hardware & Software Canvas Fallbacks**: Ensures stable rendering across varying Android devices.

## Tech Stack
- Android SDK (Kotlin)
- Jetpack Compose (UI)
- ML Kit (Subject Segmentation)
- RenderScript / Canvas API (Image Processing)

## Setup
Clone this repository and open it in Android Studio. Ensure that you have the latest JDK and Android SDK installed. Sync the project with Gradle files and run on your device or emulator.
