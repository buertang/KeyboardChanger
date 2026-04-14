# KeyboardChanger

一个轻量的 Android 输入法切换工具。项目通过调用系统输入法选择器和输入法设置页，为用户提供更快的切换入口，并在此基础上补充通知、悬浮按钮、快捷设置磁贴和桌面小组件等常用触发方式。

## 项目定位

`KeyboardChanger` 不实现输入法本身，也不替代系统输入法管理逻辑；它的目标是把“切换键盘”这件事做得更直接，减少用户反复进入系统设置的成本。

适合的使用场景包括：

- 经常在多个输入法之间切换
- 希望通过悬浮按钮或通知快速调起系统键盘选择器
- 想保留桌面小组件或快捷设置磁贴作为常驻入口
- 希望在重启后自动恢复常用入口

## 主要功能

- 打开系统输入法设置页
- 调起系统输入法选择器
- 前台通知常驻入口
- 可拖拽、吸边、半隐藏的悬浮按钮
- 快捷设置磁贴入口
- 桌面小组件入口
- 开机后恢复通知或悬浮按钮
- 应用主题色、明暗模式、应用语言设置
- 简体中文、繁体中文、英文多语言支持

## 技术栈

- Kotlin 2.0.21
- Android Gradle Plugin 9.0.1
- Jetpack Compose
- Material 3
- AndroidX Navigation Compose
- SharedPreferences 本地配置存储

## 运行环境

- `minSdk = 24`
- `targetSdk = 36`
- `compileSdk = 36`
- Java 11

推荐使用最新版 Android Studio 直接打开工程进行运行和调试。

## 权限与能力说明

项目当前依赖以下系统能力：

- `SYSTEM_ALERT_WINDOW`：用于显示悬浮按钮
- `POST_NOTIFICATIONS`：用于显示前台通知入口
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE`：用于维持通知与悬浮按钮服务
- `RECEIVE_BOOT_COMPLETED`：用于设备重启后恢复入口

应用的核心切换流程仍然是调用系统输入法选择器，因此行为边界相对清晰：它负责“更快地打开系统切换入口”，而不是直接替换系统输入法逻辑。

## 目录结构

```text
.
├─ app
│  ├─ src/main/java/com/buertang/keyboardchanger
│  │  ├─ boot                  # 开机恢复逻辑
│  │  ├─ core                  # 系统调用与通用工具
│  │  ├─ data                  # 本地偏好配置
│  │  ├─ feature               # 设置页、关于页
│  │  ├─ keyboard              # 输入法切换桥接与启动入口
│  │  ├─ service               # 前台服务与悬浮按钮
│  │  ├─ tile                  # 快捷设置磁贴
│  │  ├─ ui                    # 主题、语言、导航
│  │  └─ widget                # 桌面小组件
│  └─ src/main/res             # 资源文件
├─ gradle/libs.versions.toml   # 依赖与插件版本
├─ build.gradle.kts            # 顶层构建配置
└─ settings.gradle.kts         # 工程模块声明
```

## 构建方式

### Debug

在 Windows 下：

```powershell
.\gradlew.bat assembleDebug
```

在 macOS / Linux 下：

```bash
./gradlew assembleDebug
```

### Release

`app/build.gradle.kts` 会读取根目录下的 `keystore.properties`。当签名信息存在时，`release` 构建会自动使用对应签名配置。

Windows：

```powershell
.\gradlew.bat assembleRelease
```

macOS / Linux：

```bash
./gradlew assembleRelease
```

## 使用说明

1. 安装并启动应用。
2. 通过“打开键盘设置”进入系统输入法设置，确保目标输入法已经启用。
3. 点击“选择键盘”验证系统输入法选择器可以正常弹出。
4. 根据需要开启通知入口、悬浮按钮、重启后恢复等功能。
5. 若启用悬浮按钮，首次需要授予悬浮窗权限。
6. 可将快捷设置磁贴或桌面小组件加入系统界面，作为常驻切换入口。

## 当前项目特性概览

- 主界面使用 Compose 构建
- 输入法切换桥接页使用传统 View 布局，保证系统弹窗调起更稳定
- 配置项默认保存在 `SharedPreferences`
- 语言切换会同步刷新通知文案

## 开源协议

本项目使用 `GNU General Public License v3.0`（`GPL-3.0`）开源发布。

这意味着基于本项目进行再分发或发布修改版本时，需要继续遵守 GPL-3.0 的条款。完整协议内容见仓库根目录的 `LICENSE` 文件。

Copyright (C) 2026 buertang
