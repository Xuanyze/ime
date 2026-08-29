FROM mcr.microsoft.com/devcontainers/base:ubuntu-24.04

# 安卓 SDK 的根目录；具体组件由 post-create.sh 安装（这样重建容器更快，且可利用 Codespace 缓存）
ENV ANDROID_HOME=/opt/android-sdk
RUN sudo mkdir -p ${ANDROID_HOME} && sudo chmod 777 ${ANDROID_HOME}
