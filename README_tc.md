<div align="center">

# 📷 Android Virtual Cam

基於 Xposed 的安卓虛擬攝影機模組，支援影片替換相機預覽。

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-7.0%2B-green.svg)](https://developer.android.com/about/versions/nougat)
[![Version](https://img.shields.io/badge/Version-4.4-orange.svg)]()

[简体中文](./README.md) | [繁體中文](./README_tc.md) | [English](./README_en.md)

</div>

---

## ⚠️ 免責聲明

> **請勿用於任何非法用途，所有後果由使用者自行承擔。**
> 本專案僅供學習和研究使用。

---

## 🚀 快速開始

### 1. 環境要求
- Android 7.0 (API 24) 及以上。
- 已安裝 Xposed 框架 (推薦 LSPosed)。

### 2. 安裝與啟用
1. 下載並安裝本模組 APK。
2. 在 Xposed 框架中啟用模組。
   - **LSPosed 使用者**：請勾選**目標應用**（需要替換相機的 App），**無需**勾選系統框架。
3. 重啟目標應用。

### 3. 權限配置
1. 確保目標應用擁有**讀取本機儲存**權限。
2. 如果應用未申請儲存權限，模組會自動重新導向到私有目錄：
   - `/[內部儲存]/Android/data/[應用包名]/files/Camera1/`
3. 否則，預設工作目錄為：
   - `/[內部儲存]/Download/Camera1/` (推薦)
   - 或 `/[內部儲存]/DCIM/Camera1/` (舊版相容)

> 💡 **提示**：應用內提供了「選擇目錄」功能，可自動建立所需的資料夾。

## 🛠️ 使用指南

### 準備素材
1. **獲取解析度**：打開目標應用的相機預覽，觀察螢幕下方的氣泡提示（如「寬：1280 高：720」）。
2. **製作影片**：
   - 準備一個影片檔案，修改解析度以匹配提示。
   - 命名為 `virtual.mp4`。
   - 放入工作目錄（`Camera1` 資料夾）。
   - *可選：在 App 內使用「選擇照片→生成影片」功能自動處理。*

### 拍照替換 (可選)
如果拍照時收到「發現拍照」的氣泡提示：
1. 準備一張同解析度的圖片。
2. 命名為 `1000.bmp` (支援 jpg/png 改名為 bmp)。
3. 放入工作目錄。

## ⚙️ 進階配置

在 `Camera1` 目錄下建立以下**檔案**（0位元組即可）作為開關，即時生效：

| 檔名 | 功能描述 |
| :--- | :--- |
| `no-silent.jpg` | 🔊 **播放聲音**：替換影片時播放原聲。 |
| `disable.jpg` | ⏸️ **暫停替換**：臨時停用模組功能，恢復真實相機。 |
| `no_toast.jpg` | 🔕 **靜默模式**：隱藏所有 Toast 氣泡提示。 |
| `force_show.jpg` | ℹ️ **顯示路徑**：強制重新顯示目錄重定向提示。 |
| `private_dir.jpg` | 🔒 **強制私有**：強制每個應用使用獨立的私有目錄。 |

## ❓ 常見問題 (FAQ)

<details>
<summary><strong>Q: 前置鏡頭畫面方向不對？</strong></summary>
通常需要將影片**水準翻轉**並**右旋 90°**。處理後的解析度需與氣泡提示一致。部分應用可能無需旋轉，請根據實際效果調整。
</details>

<details>
<summary><strong>Q: 黑屏或相機啟動失敗？</strong></summary>
1. 檢查目錄結構：確保是 `Camera1/virtual.mp4`，而不是 `Camera1/Camera1/virtual.mp4`。
2. 目標應用可能不相容（如部分系統相機）。
3. 檢查檔案權限和路徑是否正確。
</details>

<details>
<summary><strong>Q: 畫面花屏或扭曲？</strong></summary>
影片解析度與相機預覽解析度不匹配。請嚴格按照氣泡提示的解析度調整影片。
</details>

<details>
<summary><strong>Q: `disable.jpg` 無效？</strong></summary>
請嘗試在私有目錄和公共目錄（`Download/Camera1` 或 `DCIM/Camera1`）都建立該檔案。
</details>

## 🐞 反饋與交流

如遇問題，請在 [Issues](https://github.com/w2016561536/android_virtual_cam/issues) 中反饋。
> **注意**：提交 Bug 時請務必附帶 **Xposed 模組日誌**，以便排查問題。

## 🔗 致謝

- Hook 思路：[CameraHook](https://github.com/wangwei1237/CameraHook)
- H264 硬解碼：[Android-VideoToImages](https://github.com/zhantong/Android-VideoToImages)
- JPEG 轉 YUV：[CSDN Blog](https://blog.csdn.net/jacke121/article/details/73888732)

---
<div align="center">
Made with ❤️ for Android
</div>