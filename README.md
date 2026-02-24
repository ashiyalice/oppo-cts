# OppoCTS - Circle to Search Enabler for ColorOS

[English](README.md) | [한국어](README_ko.md) | [简体中文](README_zh.md) | [日本語](README_ja.md)

An enabler application that provides a **99% native-like user experience** for devices where Google's "Circle to Search (CTS)" is restricted or has inconvenient trigger methods, such as those running Chinese domestic ROMs (ColorOS).

## 📱 Tested Devices
- **Optimized for**: OPPO Find X8 Ultra (CN ROM / ColorOS)
- May work similarly on other Chinese domestic ROMs based on Android 14+ (MIUI, HyperOS, OriginOS, etc.), but not guaranteed.

## ✨ Key Features

### 1. 🌟 Perfect Invisible Bottom Trigger (Highly Recommended)
This is the core feature of the app. Unlike traditional Shizuku or Accessibility Service-based workarounds, this works **even when Wi-Fi is off or the Shizuku background process is dead**.
- Creates an **invisible transparent overlay** precisely at the location of the bottom gesture bar to detect touches.
- Just like the native experience, **long-pressing the bottom bar for 0.4 seconds instantly triggers CTS**.
- Perfectly distinguishes from normal swipe gestures (like swipe-to-home or recent apps), ensuring no interference with daily use.
- **Fine-tuning support**: Adjust the overlay's Y-offset (-100 to +100) and thickness pixel-by-pixel to match different devices' bottom bezels or resolutions. Includes a **Debug Mode** that paints the area red for easy setup.
- **Auto-hide**: When the navigation bar hides (e.g., watching full-screen videos or gaming), the transparent trigger area dynamically synchronizes with the system and hides itself instantly.

### 2. Fundamental Resolution & Permission Injection via Shizuku
Due to the nature of Chinese ROMs, the Google App does not activate CTS natively. This app uses Shizuku's powerful ADB shell permissions during initial setup to trick the Google App into force-enabling CTS.
- Automatically handles setting the default Digital Assistant (GmsFlagSetter, AssistantSetter) without needing to dig into system settings.
- If background services are killed by RAM management, it quietly restores the environment upon reboot.

### 3. Backup Trigger Methods
Includes Accessibility Service triggers for users who prefer physical buttons.
- Volume + / - Double Click or Long Press
- Camera Shutter Button

## ⚙️ Technical Details

This project operates in two main layers: the **Initial Setup Layer** and the **CTS Hook/Trigger Layer**.

1. **GMS Flag Injection**:
   - Hidden flags inside Google Play Services and the Google App dictate whether CTS is enabled.
   - Using Shizuku permissions during setup, the app modifies the internal package manager's components to make `com.google.android.googlequicksearchbox` recognize the current device as a supported device.

2. **Native Service Intent Hooker (Shizuku-Free Trigger)**:
   - Natively, Android triggers CTS by calling `VoiceInteractionManagerService.showSessionFromSession` or `ContextualSearchManagerService.startContextualSearch` with system privileges.
   - This app utilizes the **Android HiddenApiBypass** library to directly access these internal APIs via Java Reflection.
   - It **spoofs** the caller as a package that natively supports CTS (e.g., `hyperOS_home` or System UI) to bypass system security blocks.
   - As a result, once the initial GMS injection is complete, the actual popup action becomes completely independent (Shizuku-free) and does not rely on the Shizuku process at all.

3. **WindowInsets Overlay Matching**:
   - Instead of using a hardcoded size for the transparent overlay, it monitors Android's official `WindowInsetsCompat.Type.navigationBars()` API in real-time.
   - It dynamically synchronizes the overlay (`WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`) with the system gesture bar by tracking changes (e.g., landscape orientation shifting the bar to the left/right, or hiding completely in full-screen mode).

## 🤝 Credits

The core idea of native service triggering via package spoofing and the bypass parameters (flags=7, omni.entry_point=1, etc.) were inspired by, ported, and reconstructed from the [MiCTS](https://github.com/mizhiyong/MiCTS) project.

## ⚠️ Disclaimer
This application uses reflection to call hidden system APIs and modifies flags in third-party apps (Google).
- OS updates (especially Google App updates) may break functionality or block the bypass methods.
- The user assumes all responsibility for any software issues, such as device malfunctions or boot loops, resulting from the use of this app.
