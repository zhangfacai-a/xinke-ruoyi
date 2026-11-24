#!/bin/sh
set -eu

ACTION="${1:-run}"
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
JAR_PATH="$PROJECT_ROOT/xinke-admin/target/xinke-admin.jar"

cd "$PROJECT_ROOT"

case "$ACTION" in
  build)
    mvn -pl xinke-admin -am clean package -DskipTests
    ;;
  clean)
    mvn clean
    ;;
  test)
    mvn -pl xinke-admin -am test
    ;;
  run)
    if [ ! -f "$JAR_PATH" ]; then
      echo "Missing $JAR_PATH. Run: scripts/server/manage.sh build" >&2
      exit 1
    fi
    exec java -Xms256m -Xmx768m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=384m \
      -XX:+HeapDumpOnOutOfMemoryError -jar "$JAR_PATH"
    ;;
  *)
    echo "Usage: $0 {build|clean|test|run}" >&2
    exit 2
    ;;
esac
