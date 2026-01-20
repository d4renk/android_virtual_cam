<div align="center">

# 📷 Android Virtual Cam

An Xposed-based virtual camera module for Android that supports replacing camera preview with video.

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-7.0%2B-green.svg)](https://developer.android.com/about/versions/nougat)
[![Version](https://img.shields.io/badge/Version-4.4-orange.svg)]()

[简体中文](./README.md) | [繁體中文](./README_tc.md) | [English](./README_en.md)

</div>

---

## ⚠️ Disclaimer

> **DO NOT USE FOR ANY ILLEGAL PURPOSE, YOU NEED TO TAKE ALL RESPONSIBILITY AND CONSEQUENCES!**
> This project is for learning and research purposes only.

---

## 🚀 Quick Start

### 1. Requirements
- Android 7.0 (API 24) or higher.
- Xposed framework installed (LSPosed recommended).

### 2. Installation & Activation
1. Download and install the module APK.
2. Enable the module in your Xposed manager.
   - **LSPosed Users**: Select the **Target App** (the app you want to hook), **NO** need to select System Framework.
3. Force stop and restart the target app.

### 3. Permissions & Storage
1. Ensure the target app has **Read External Storage** permission.
2. If the app does not request storage permission, the module will redirect to the app's private directory:
   - `/[Internal Storage]/Android/data/[Package Name]/files/Camera1/`
3. Otherwise, the default working directory is:
   - `/[Internal Storage]/Download/Camera1/` (Recommended)
   - or `/[Internal Storage]/DCIM/Camera1/` (Legacy support)

> 💡 **Tip**: You can use the "Select Directory" feature within the VCam app to automatically create the necessary folders.

## 🛠️ Usage Guide

### Prepare Video Source
1. **Get Resolution**: Open the camera preview in the target app. Look for a toast message showing the resolution (e.g., "Width: 1280 Height: 720").
2. **Prepare Video**:
   - Create or resize a video file to match the resolution shown.
   - Name it `virtual.mp4`.
   - Place it in the working directory (`Camera1` folder).
   - *Optional: Use the "Select Photo -> Generate Video" feature in the VCam app.*

### Photo Replacement (Optional)
If you see a "Picture Taken" toast message when snapping a photo:
1. Prepare an image with the resolution shown in the toast.
2. Name it `1000.bmp` (jpg/png renamed to bmp is also supported).
3. Place it in the working directory.

## ⚙️ Advanced Configuration

Create the following **files** (empty files are fine) in the `Camera1` directory to toggle features in real-time:

| Filename | Description |
| :--- | :--- |
| `no-silent.jpg` | 🔊 **Play Sound**: Play the original video sound during preview. |
| `disable.jpg` | ⏸️ **Disable Hook**: Temporarily disable the module and restore the real camera. |
| `no_toast.jpg` | 🔕 **Silent Mode**: Hide all toast messages from the module. |
| `force_show.jpg` | ℹ️ **Show Path**: Force show the directory redirection toast again. |
| `private_dir.jpg` | 🔒 **Force Private**: Force each app to use its own private directory. |

## ❓ FAQ

<details>
<summary><strong>Q: Front camera orientation is wrong?</strong></summary>
Usually, you need to **horizontally flip** the video and **rotate it 90° right**. The final resolution must match the toast message. Some apps may not require rotation, so please adjust based on actual results.
</details>

<details>
<summary><strong>Q: Black screen or camera fails to open?</strong></summary>
1. Check directory structure: It should be `Camera1/virtual.mp4`, NOT `Camera1/Camera1/virtual.mp4`.
2. The target app might not be compatible (e.g., some system cameras).
3. Check file permissions and paths.
</details>

<details>
<summary><strong>Q: Distorted or garbled screen?</strong></summary>
The video resolution does not match the camera preview resolution. Please strictly follow the resolution shown in the toast message.
</details>

<details>
<summary><strong>Q: `disable.jpg` not working?</strong></summary>
Try creating the file in both the private directory AND the public directory (`Download/Camera1` or `DCIM/Camera1`).
</details>

## 🐞 Feedback & Issues

Please report issues on [GitHub Issues](https://github.com/w2016561536/android_virtual_cam/issues).
> **Note**: When reporting a bug, please attach the **Xposed Module Log** for troubleshooting.

## 🔗 Credits

- Hook Method: [CameraHook](https://github.com/wangwei1237/CameraHook)
- H264 Hardware Decode: [Android-VideoToImages](https://github.com/zhantong/Android-VideoToImages)
- JPEG to YUV: [CSDN Blog](https://blog.csdn.net/jacke121/article/details/73888732)

---
<div align="center">
Made with ❤️ for Android
</div>