# OppoCTS - Circle to Search Enabler for ColorOS

[English](README.md) | [한국어](README_ko.md) | [简体中文](README_zh.md) | [日本語](README_ja.md)

专为国行设备（如ColorOS）等默认限制谷歌“画圈搜索 (Circle to Search, CTS)”功能或触发方式不便的设备设计，提供**与原生体验99%一致**的使能应用。

## 📱 测试设备
- **优化设备**: OPPO Find X8 Ultra (国行 ROM / ColorOS)
- 其他基于Android 14+的国行ROM（如MIUI, HyperOS, OriginOS等）可能也可以工作，但不保证。

## ✨ 核心功能与特性

### 1. 🌟 完美的底部手势条触发（强烈推荐）
这是本应用最核心的功能。与传统的基于Shizuku或无障碍服务的绕过方式不同，此方法**即使在断开Wi-Fi或Shizuku后台进程被杀的情况下也能正常工作**。
- 在屏幕底部的系统手势条位置创建一个**不可见的透明覆盖层**来捕获触摸事件。
- 与原生体验一致，**长按底部指示条 0.4 秒即可立即呼出 CTS**。
- 完美区分正常的滑动（如上滑返回桌面、查看最近应用）与长按，毫不干扰日常使用。
- **支持位置微调**: 支持逐像素调节覆盖层的上下偏移（-100 到 +100）和厚度，以适应不同设备的底部边框或分辨率。配置时提供**调试模式**，将触控区域显示为红色以便于对齐。
- **自动隐藏**: 当导航条隐藏时（如全屏观看视频或玩游戏），透明触发区域会与系统同步，动态自动隐藏。

### 2. 通过 Shizuku 的彻底解决与权限注入
受限于国行 ROM 的特性，谷歌App默认不会激活 CTS。本应用在初始设置时，利用 Shizuku 强大的 ADB 权限来欺骗谷歌App，强制其启用 CTS。
- 自动处理默认数字助手的设置 (GmsFlagSetter, AssistantSetter)，无需用户深入系统设置手动更改。
- 如果后台服务被内存清理机制杀掉，应用会在重启后在后台静默恢复环境。

### 3. 多种备用触发方式
针对偏好物理按键的用户，提供了基于无障碍服务的触发方式。
- 音量 + / - 双击或长按
- 相机快门键触发

## ⚙️ 工作原理 (Technical Details)

该项目主要分为两层：**环境初始化层** 和 **CTS Hook/触发层**。

1. **GMS 标志注入 (GMS Flag Injection)**:
   - Google Play 服务和 Google App 内部存在隐藏特征标志（Flags），决定是否启用 CTS。
   - 在设置阶段，应用使用 Shizuku 权限修改内部包管理器的功能配置，使 `com.google.android.googlequicksearchbox` 将当前设备识别为受支持的设备。

2. **原生服务 Intent Hooker (免 Shizuku 触发)**:
   - 在原生Android中，长按底部时系统会以系统权限调用 `VoiceInteractionManagerService.showSessionFromSession` 或 `ContextualSearchManagerService.startContextualSearch` 来唤出 CTS。
   - 本应用利用 **Android HiddenApiBypass** 库，通过 Java 反射 (Reflection) 直接调用这些内部 API。
   - 此时，应用会将调用者 (Caller) **伪装 (Spoofing)** 为原生支持 CTS 的系统包（例如 `hyperOS_home` 或 System UI），从而绕过系统安全拦截。
   - 因此，只要完成最初的 GMS 权限注入，实际唤出弹窗的动作就完全独立（免 Shizuku），不再依赖 Shizuku 进程。

3. **WindowInsets 覆盖层匹配**:
   - 透明覆盖层没有使用硬编码尺寸，而是实时监听 Android 官方的 `WindowInsetsCompat.Type.navigationBars()` API。
   - 它能感知导航条的状态变化（例如：横屏时导航条移至左侧/右侧，或在全屏模式下完全消失），从而使覆盖层 (`WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`) 精准同步覆盖在底部的系统手势偏置栏上方。

## 🤝 鸣谢 (Credits)

本应用通过包名伪装触发原生系统的核心思路及其绕过参数（如 flags=7, omni.entry_point=1 等）均受到了 [MiCTS](https://github.com/mizhiyong/MiCTS) 项目逻辑分析的启发，并在此基础上进行了移植与重构。

## ⚠️ 免责声明
本应用使用了反射调用隐藏的系统 API，并修改了第三方应用（Google）的特征标志。
- 系统更新（尤其是谷歌App自身的更新）可能会导致相关方法被封堵或应用无法正常工作。
- 使用本应用而导致的设备故障、无限重启等任何软件问题，所有的风险与责任均由用户自行承担。
