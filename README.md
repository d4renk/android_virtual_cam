<div align="center">

# 📷 Android Virtual Cam

基于 Xposed 的安卓虚拟摄像头模块，支持视频替换相机预览。

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-7.0%2B-green.svg)](https://developer.android.com/about/versions/nougat)
[![Version](https://img.shields.io/badge/Version-4.4-orange.svg)]()

[简体中文](./README.md) | [繁體中文](./README_tc.md) | [English](./README_en.md)

> **源码镜像**：[Gitee (中国大陆加速)](https://gitee.com/w2016561536/android_virtual_cam)

</div>

---

## ⚠️ 免责声明

> **请勿用于任何非法用途，所有后果由使用者自行承担。**
> 本项目仅供学习和研究使用。

---

## 🚀 快速开始

### 1. 环境要求
- Android 7.0 (API 24) 及以上。
- 已安装 Xposed 框架 (推荐 LSPosed)。

### 2. 安装与激活
1. 下载并安装本模块 APK。
2. 在 Xposed 框架中启用模块。
   - **LSPosed 用户**：请勾选**目标应用**（需要替换相机的 App），**无需**勾选系统框架。
3. 重启目标应用。

### 3. 权限配置
1. 确保目标应用拥有**读取本地存储**权限。
2. 如果应用未申请存储权限，模块会自动重定向到私有目录：
   - `/[内部存储]/Android/data/[应用包名]/files/Camera1/`
3. 否则，默认工作目录为：
   - `/[内部存储]/Download/Camera1/` (推荐)
   - 或 `/[内部存储]/DCIM/Camera1/` (旧版兼容)

> 💡 **提示**：应用内提供了“选择目录”功能，可自动创建所需的文件夹。

## 🛠️ 使用指南

### 准备素材
1. **获取分辨率**：打开目标应用的相机预览，观察屏幕下方的气泡提示（如“宽：1280 高：720”）。
2. **制作视频**：
   - 准备一个视频文件，修改分辨率以匹配提示。
   - 命名为 `virtual.mp4`。
   - 放入工作目录（`Camera1` 文件夹）。
   - *可选：在 App 内使用“选择照片→生成视频”功能自动处理。*

### 拍照替换 (可选)
如果拍照时收到“发现拍照”的气泡提示：
1. 准备一张同分辨率的图片。
2. 命名为 `1000.bmp` (支持 jpg/png 改名为 bmp)。
3. 放入工作目录。

## ⚙️ 高级配置

在 `Camera1` 目录下创建以下**文件**（0字节即可）作为开关，实时生效：

| 文件名 | 功能描述 |
| :--- | :--- |
| `no-silent.jpg` | 🔊 **播放声音**：替换视频时播放原声。 |
| `disable.jpg` | ⏸️ **暂停替换**：临时停用模块功能，恢复真实相机。 |
| `no_toast.jpg` | 🔕 **静默模式**：隐藏所有 Toast 气泡提示。 |
| `force_show.jpg` | ℹ️ **显示路径**：强制重新显示目录重定向提示。 |
| `private_dir.jpg` | 🔒 **强制私有**：强制每个应用使用独立的私有目录。 |

## ❓ 常见问题 (FAQ)

<details>
<summary><strong>Q: 前置摄像头画面方向不对？</strong></summary>
通常需要将视频**水平翻转**并**右旋 90°**。处理后的分辨率需与气泡提示一致。部分应用可能无需旋转，请根据实际效果调整。
</details>

<details>
<summary><strong>Q: 黑屏或相机启动失败？</strong></summary>
1. 检查目录结构：确保是 `Camera1/virtual.mp4`，而不是 `Camera1/Camera1/virtual.mp4`。
2. 目标应用可能不兼容（如部分系统相机）。
3. 检查文件权限和路径是否正确。
</details>

<details>
<summary><strong>Q: 画面花屏或扭曲？</strong></summary>
视频分辨率与相机预览分辨率不匹配。请严格按照气泡提示的分辨率调整视频。
</details>

<details>
<summary><strong>Q: `disable.jpg` 无效？</strong></summary>
请尝试在私有目录和公共目录（`Download/Camera1` 或 `DCIM/Camera1`）都创建该文件。
</details>

## 🐞 反馈与交流

如遇问题，请在 [Issues](https://github.com/w2016561536/android_virtual_cam/issues) 中反馈。
> **注意**：提交 Bug 时请务必附带 **Xposed 模块日志**，以便排查问题。

## 🔗 致谢

- Hook 思路：[CameraHook](https://github.com/wangwei1237/CameraHook)
- H264 硬解码：[Android-VideoToImages](https://github.com/zhantong/Android-VideoToImages)
- JPEG 转 YUV：[CSDN Blog](https://blog.csdn.net/jacke121/article/details/73888732)

---
<div align="center">
Made with ❤️ for Android
</div>