#!/usr/bin/env bash
set -euo pipefail

# 兜底：容器由旧配置创建时可能没有这个环境变量
export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
sudo mkdir -p "$ANDROID_HOME" && sudo chown "$(whoami)" "$ANDROID_HOME"

# 1) 安装 Android SDK 命令行工具（已装过则跳过，重建容器时省时间）
if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "==> Installing Android cmdline-tools..."
  sudo apt-get update -qq && sudo apt-get install -y -qq unzip zip >/dev/null
  curl -sSL -o /tmp/cmdline-tools.zip \
    "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools
  mv /tmp/cmdline-tools/cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
fi

# 2) 接受许可并安装 platform-tools / platform / build-tools（与 app/build.gradle.kts 的 compileSdk=35 匹配）
yes | sdkmanager --licenses >/dev/null
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0" >/dev/null
echo "==> Android SDK ready: $(sdkmanager --version)"

# 3) 安装 ZCode CLI（首次运行 zcode 按提示登录智谱账号即可）
if ! command -v zcode >/dev/null 2>&1; then
  npm install -g @zcode/cli
fi
zcode --version || true

echo "==> Done. Try: gradle assembleDebug"
