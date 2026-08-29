# Android 开发 Codespace 模板（Kotlin + Compose + ZCode）

本地零安装：安卓 SDK、JDK、Gradle 全部在 Codespace 云端容器里，构建产物通过 GitHub Actions 下载。

## 使用步骤

1. 在 GitHub 新建一个仓库，把本目录内容全部推上去（含 `.devcontainer/` 和 `.github/`）。
2. 仓库页面 → Code → Codespaces → **Create codespace on main**。
   首次启动会自动执行 `post-create.sh`：装 Android SDK（platform 35 + build-tools）和 ZCode CLI，约 5–10 分钟。
3. 终端里运行 `zcode`，按提示登录智谱账号（浏览器打开授权链接即可）。之后就可以让 ZCode 写码、改码、跑构建。
4. 预览 App（三选一）：
   - 推送代码，等 GitHub Actions 构建完，在 Actions 页面下载 `app-debug-apk` 装到手机；
   - 或在 Codespace 里跑 `gradle assembleDebug`，从文件树下载 `app/build/outputs/apk/debug/app-debug.apk`；
   - 或配合 Google AI Studio 的云端模拟器做快速预览（见之前讨论）。

## 常用命令

```bash
gradle assembleDebug      # 构建 debug APK（产物在 app/build/outputs/apk/debug/）
gradle assembleRelease    # release 构建（需签名配置）
zcode                     # 启动 ZCode agent
sdkmanager --list         # 查看可安装的 SDK 组件
```

## 注意事项

- Codespace 不用时记得停掉（仓库 Codespaces 页面或 VS Code 左下角），免费额度是每月 120 核心小时 + 15GB 存储，2 核机器挂一天就烧掉 48 小时。
- 模拟器在 Codespace 容器里跑不起来（无嵌套虚拟化），预览一律走真机 APK 或云真机服务。
- 升级 SDK 版本时同步改两处：`post-create.sh` 里的 `platforms;android-XX` 和 `app/build.gradle.kts` 里的 `compileSdk`。
