#!/usr/bin/env bash
# 单服务镜像部署：bash backend/startup.sh /absolute/path/service.jar service-name 8081
set -euo pipefail
JAR=${1:?请提供已构建的 jar 路径}
NAME=${2:?请提供容器名称}
PORT=${3:?请提供服务端口}
ROOT=$(cd "$(dirname "$0")" && pwd)
CONTEXT=$(mktemp -d)
trap 'rm -rf "$CONTEXT"' EXIT
cp "$JAR" "$CONTEXT/app.jar"
cp "$ROOT/Dockerfile" "$CONTEXT/Dockerfile"
docker build -t "$NAME:local" "$CONTEXT"
docker run -d --name "$NAME" -p "127.0.0.1:$PORT:$PORT" -e JAVA_OPTS="${JAVA_OPTS:--Xms128m -Xmx320m}" "$NAME:local"
