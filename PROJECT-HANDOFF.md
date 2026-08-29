# 安卓应用开发项目 — 交接文档

> 用途：粘贴/发给新对话的 ZCode，作为项目上下文。生成于 2026-08-29；**2026-08-29 下午已更新**（需求确定为班牌输入法，见下文「六、班牌输入法项目」）。

## 一、目标与背景

用户想开发一个安卓应用，但**不想在本地部署安卓开发环境**（Android Studio / SDK / 模拟器都不要装本地），希望结合云 IDE 和 ZCode 开发。

经过多轮方案讨论（Android Studio Cloud 已随 Firebase Studio 关闭（2027-03-22 大限）；Antigravity 是本地 IDE 不符合需求；AI Studio 安卓模式无终端装不了 zcode CLI），最终选定方案：

**本地 ZCode Desktop 写码（零安卓环境）→ push 到 GitHub → GitHub Actions 云端构建 APK → 从 Actions Artifacts 下载 APK 装真机。**

- 曾短暂尝试过 GitHub Codespaces 方案（devcontainer 装全 SDK），因 SDK 组件安装卡住（疑似网络/环境问题）放弃，**Codespace 已不再需要，应删除**（用户可能还没删）。
- 本地开发模式：ZCode 只写代码，不执行 gradle（本地无 SDK）；编译验证全部走云端 CI，「改码 → push → CI 构建 → 看日志」是唯一验证循环，单次约 3–6 分钟。

## 二、当前环境状态（全部就绪 ✅）

- **GitHub 仓库**：`github.com/Xuanyze/ime`（公开仓库），main 分支。
- **本地项目路径**：`C:\Users\16891\.zcode\workspace\default\android-codespace-starter`（git 仓库，remote origin = git@github.com:Xuanyze/ime.git）。
- **SSH 认证**：本机已生成 ed25519 密钥（`~/.ssh/id_ed25519`，注释 `Xuanyze@users.noreply.github.com`），用户已把公钥添加到 GitHub，push 链路验证畅通。
- **Git 身份**：仅设了仓库级 `user.name=Xuanyze`、`user.email=Xuanyze@users.noreply.github.com`（不是全局配置）。机器上没有 gh CLI，git 全局身份未配置。
- **CI 验证结果**：GitHub Actions「Build APK」已跑通，2026-08-29 的运行 conclusion = success。
- **环境怪癖**：本机 curl 走 schannel，访问 HTTPS 需加 `--ssl-no-revoke`，否则报 `CRYPT_E_NO_REVOCATION_CHECK`；WebFetch 对部分域名有证书校验问题。

## 三、项目技术栈与结构

最小可构建的 Kotlin + Jetpack Compose 项目：

- **工具链版本**：AGP 8.7.3 / Kotlin 2.1.0 / Compose BOM 2024.12.01 / compileSdk 35 / minSdk 24 / targetSdk 35 / JDK 17（CI 用 Temurin）/ Gradle 8.10.2（CI 由 `gradle/actions/setup-gradle@v4` 提供，项目无 wrapper jar，本地也无 gradle）。
- **包名/应用名**：`com.example.myapp` / MyApp —— **还是占位名，待用户确定真实应用名后修改**（涉及 settings.gradle.kts 的 rootProject.name、app/build.gradle.kts 的 namespace/applicationId、manifest 的 label、MainActivity 的 package）。
- **目录结构**：
  - `settings.gradle.kts`、`build.gradle.kts`（根）、`gradle.properties`（`-Xmx2g` + AndroidX 开关）
  - `app/build.gradle.kts`（Compose 开启，依赖 material3/ui/activity-compose）
  - `app/src/main/AndroidManifest.xml`（单 Activity，主题用 `Theme.Material.Light.NoActionBar` 占位）
  - `app/src/main/java/com/example/myapp/MainActivity.kt`（Compose 的 "Hello from Codespaces!"）
  - `.github/workflows/build-apk.yml`（push 到 main 触发 `gradle assembleDebug`，上传 artifact `app-debug-apk`；也支持 workflow_dispatch 手动触发）
  - `.devcontainer/`（Codespace 用的 devcontainer + post-create.sh，**现方案已不需要，可留可删**）
  - `README.md`（面向 Codespace 方案写的，内容已过时，待更新）

## 四、已知注意事项

1. Codespace 容器跑不了模拟器（无嵌套虚拟化），本方案预览一律靠真机装 APK；如需远程调试可考虑腾讯 WeTest / Firebase Test Lab 云真机。
2. CI 首次构建无缓存，约 3–6 分钟；后续可考虑加 gradle 缓存加速。
3. Firebase Studio 若用户有旧项目，需在 2027-03-22 前迁出，否则数据被删（用户应该没有，仅备忘）。
4. CI 构建失败时，用户会贴 Actions 日志的红色段落过来，据此修复后重新 push。

## 五、下一步（新对话的起点）

1. **确认应用需求**：用户还没说具体要做什么应用。先问清核心功能，然后修改包名/应用名（占位名 `com.example.myapp` 需要替换）。
2. 在此骨架上开发第一个真实功能，push 验证 CI。
3. 待办小项：更新 README 反映「本地 ZCode + Actions 云构建」的工作流；删除或归档 `.devcontainer/`；视情况加 gradle 缓存。

## 六、班牌输入法项目（2026-08-29 下午起，当前主线）

**需求**：基于 [gurecn/YuyanIme](https://github.com/gurecn/YuyanIme) 二次开发，做电子智能班牌专用横屏输入法（公共设备、Android 8~13 重点兼容 8/13、性能有限）。完整需求规格由用户给出（二十余节，含 UI 布局/Bar/HOME/验收标准），核心原则：能用>稳定>体验>美观；复用 Rime/现有代码，不过度重构。

### 已完成

- **仓库重组**：`android-codespace-starter` 已替换为 YuyanIme 完整源码（app 壳 + vendor 进来的 yuyansdk 模块，不再用 submodule）。旧骨架项目已删除。
- **CI 修复**：上游 `app/build.gradle` 的 `signingConfigs.release` 在无 keystore 时评估崩溃（`rootProject.file(null)`），已加存在性守卫；debug 无 keystore 时回退默认调试签名。CI 任务改为 `./gradlew assembleOfflineDebug`（项目有 offline flavor）。
- **CI 日志通道**：日志 API 需认证，故失败时 workflow 用 `gh api` 把日志关键行发到 commit comment（公开可读）+ 上传 `build-log` artifact。
- **P0 功能**（已编译通过，commit f26ad89 起）：
  - `SkbMenuMode.Bar` / `SkbMenuMode.Home` 两个常驻工具栏按钮（DB 种子 + menuSkbFunsPreset + 自绘 vector 图标 ic_menu_bar/ic_menu_home）。
  - `ImeService.barEscape()`：R+ 用 `WindowInsetsController.show(navigationBars)`，旧版本清 immersive 标志，然后 `requestHideSelf`；全程 try-catch 不崩溃。
  - `ImeService.homeEscape()`：`ACTION_MAIN/CATEGORY_HOME/NEW_TASK` 回桌面，失败仅隐藏。
  - `RimeUserdataGuard.limitUserdata()`：每日一次在 Rime 启动前删 `*.userdb` 与 rime 日志（Launcher.onInitDataChildThread 调用）。
- **P1 三栏布局**（commit 70a73b1，待 CI/真机验证）：
  - `keyboard/container/BoardPanels.kt`：`SchemeRail`（左栏 26键全拼/双拼/9键拼音，复用 `switchModeForSetting`，当前方案高亮）+ `FunctionColumn`（数字小键盘 7-9/4-6/1-3/.0⌫ + 常用中文符号 + ↑↓←→/Home/End/Del/⌫/Ctrl+A/C/X/V，全部复用现有编辑键链路）。
  - `InputView.updateBoardPanels()`：横屏（非悬浮）且 `leftMarginWidth>=150dp` 时把两栏挂到 `mInputKeyboardContainer` 两侧既有空白边距（YuyanIme 横屏键盘 skbWidth=70% 屏宽，两侧各 15% 空白），竖屏/悬浮/窄屏自动回退。

### YuyanIme 关键代码地图（调研结论）

- Service：`yuyansdk/.../service/ImeService.kt`（InputConnection 封装都在此类）；主视图 `keyboard/InputView.kt`（混合式：`sdk_skb_container.xml` + 代码自绘）。
- 键盘=数据驱动自绘：`KeyboardLoaderUtil` + `keyboard/KeyboardData.kt`（布局码表）+ `BaseKeyboardView/TextKeyboard` 自绘；容器在 `keyboard/container/`（Qwerty/T9Text/Symbol/ClipBoard/Settings/Candidates）。
- 候选条：`view/CandidatesBar.kt`（RecyclerView + 菜单区）；工具栏按钮数据 `data/SkbFunData.kt`，菜单点击入口 `keyboard/SettingsMenuClick.kt`，可配置项存 Room DB `SkbFun`（`DataBaseKT.kt` 种子，isKeep=1 为常驻）。
- 方案切换：`manager/InputModeSwitcher.kt`（位掩码 + Rime schema；`switchModeForSetting(Pair(mask, schema))`）；双拼 schema = `"double_pinyin_" + doublePYSchemaMode`。
- Rime：预编译 `libyuyanime.so`（yuyansdk/libs，四 ABI，无 native 源码），JNI 封装 `com.yuyan.inputmethod.core.Rime`，门面 `Kernel`；userdb 在 `Android/data/<pkg>/files/rime/`（shared 与 user 同目录）。
- 编辑键：`InputView.processUserDefKey`（USER_KEYCODE_* 用户码），`ImeService.sendCombinationKeyEvents`（Ctrl/Alt/Shift 组合已支持）。

### 下一步

1. 等 commit 70a73b1 的 CI 结果（三栏布局代码第一次编译）。
2. 用户真机验证：装 APK → 26键/9键/双拼输入、候选词、Bar/HOME 实际效果、两侧面板布局是否美观、数字键 Ctrl 组合在目标消息 App 是否生效。
3. 待办：按真机反馈微调面板比例/配色；Android 8 真机回归；评估是否调大横屏键盘占比（EnvironmentSingleton 的 0.7 系数）；更新应用名（仍是语燕默认名/包名，未改）。
