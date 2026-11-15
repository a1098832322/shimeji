#!/usr/bin/env bash
# Shimeji Studio · 皮肤及动作设计器 启动脚本
#
#   ./studio.sh                       # 当前目录为工作区，随机端口，自动开浏览器
#   ./studio.sh --port 7799           # 固定端口
#   ./studio.sh --workspace ~/shimeji --no-browser
#
# 说明：设计器与预览进程必须共用同一个工作目录（引擎按 cwd + ./img/<皮肤>/ 解析图片），
#      所以这里先在引擎根目录编译，再以该目录为 cwd 启动。
set -euo pipefail

cd "$(dirname "$0")"
ROOT="$(pwd)"
CP_FILE="$ROOT/target/studio-classpath.txt"

echo "[studio] 引擎根目录: $ROOT"

if [ ! -d "$ROOT/target/classes" ] || [ -n "$(find "$ROOT/src/main/java" -newer "$ROOT/target/classes" -name '*.java' -print -quit 2>/dev/null)" ]; then
  echo "[studio] 编译中…"
  mvn -q -DskipTests compile
fi

if [ ! -s "$CP_FILE" ]; then
  echo "[studio] 解析依赖 classpath…"
  mvn -q dependency:build-classpath -Dmdep.outputFile="$CP_FILE"
fi

# dev 布局（src/main/resources/conf）由 StudioMain 自动识别并补 -Ddev=true
exec java -cp "$ROOT/target/classes:$(cat "$CP_FILE")" \
     com.group_finity.mascot.studio.StudioMain "$@"
