#!/usr/bin/env bash
set -Eeuo pipefail

root=/ruoyi
upstream_conf=/etc/nginx/conf.d/xinke-active-upstream.conf
lock=/run/lock/xinke-bluegreen.lock

[[ $EUID == 0 ]] || { echo 'Run through the restricted sudo helper' >&2; exit 1; }
exec 9>"$lock"
flock -n 9 || { echo 'Another release is in progress' >&2; exit 1; }

atomic_file() {
  local target=$1 value=$2 temporary="${1}.next.$$"
  printf '%s\n' "$value" > "$temporary"
  mv -f "$temporary" "$target"
}

atomic_link() {
  local target=$1 source=$2 temporary="${1}.next.$$"
  ln -s "$source" "$temporary"
  mv -Tf "$temporary" "$target"
}

set_backend() {
  local port=$1 temporary="${upstream_conf}.next.$$"
  printf 'upstream xinke_backend { server 127.0.0.1:%s; }\n' "$port" > "$temporary"
  mv -f "$temporary" "$upstream_conf"
  nginx -t >/dev/null
  systemctl reload nginx
}

healthy() {
  curl -fsS --max-time 3 "http://127.0.0.1:$1/" >/dev/null
}

wait_healthy() {
  local port=$1
  for _ in $(seq 1 45); do
    if healthy "$port"; then return 0; fi
    sleep 2
  done
  return 1
}

require_setup() {
  [[ -L $root/active-ui && -f $root/active-port && -f $root/active-service && -f $upstream_conf ]] || {
    echo 'Run bluegreen/setup.sh on the server first' >&2; exit 1;
  }
}

check_capacity() {
  local available
  available=$(awk '/MemAvailable:/ {print $2}' /proc/meminfo)
  (( available >= 850000 )) || {
    echo "Insufficient available RAM for two backends: ${available} kB (need 850000 kB)" >&2
    exit 1
  }
  (( $(df -Pk "$root" | awk 'NR==2 {print $4}') >= 1048576 )) || {
    echo 'Need at least 1 GiB free disk space' >&2; exit 1
  }
}

rollback_failed_deploy() {
  local status=$?
  trap - ERR
  if [[ ${switch_started:-0} == 1 ]]; then
    atomic_file "$root/active-port" "$old_port"
    atomic_link "$root/active-ui" "$old_ui"
    atomic_file "$root/active-service" "$old_service"
    if [[ -n $old_release ]]; then
      atomic_file "$root/active-release" "$old_release"
    else
      rm -f "$root/active-release"
    fi
    cp -a "$prepared/previous-upstream.conf" "$upstream_conf"
    nginx -t >/dev/null && systemctl reload nginx || true
  fi
  systemctl enable "$old_service" >/dev/null 2>&1 || true
  systemctl disable "$candidate" >/dev/null 2>&1 || true
  systemctl stop "$candidate" || true
  if [[ -n ${previous_slot:-} ]]; then
    atomic_link "$root/bg-slots/$slot" "$previous_slot"
  else
    rm -f "$root/bg-slots/$slot"
  fi
  echo "BLUEGREEN_DEPLOY_FAILED status=$status; previous service retained" >&2
  exit "$status"
}

rollback_failed_rollback() {
  local status=$?
  trap - ERR
  atomic_file "$root/active-port" "$current_port"
  atomic_link "$root/active-ui" "$current_ui"
  atomic_file "$root/active-service" "$current_service"
  atomic_file "$root/active-release" "$current_release"
  set_backend "$current_port" || true
  systemctl enable "$current_service" >/dev/null 2>&1 || true
  echo "BLUEGREEN_ROLLBACK_FAILED status=$status; current service retained" >&2
  exit "$status"
}

deploy() {
  local requested=$1 stamp=$2 release slot port candidate old_port old_service old_ui old_release
  local previous_slot='' prepared switch_started=0 connections
  [[ $stamp =~ ^[0-9]{8}-[0-9]{6}$ ]] || { echo 'Invalid release stamp' >&2; exit 2; }
  release=$(readlink -f -- "$requested")
  [[ $release == "/home/deploy/releases/release-$stamp" ]] || { echo 'Invalid release directory' >&2; exit 2; }
  [[ -s $release/xinke-admin.jar ]] || { echo 'Missing backend jar' >&2; exit 2; }
  require_setup
  check_capacity
  old_port=$(<"$root/active-port")
  old_service=$(<"$root/active-service")
  old_ui=$(readlink -f "$root/active-ui")
  old_release=$(cat "$root/active-release" 2>/dev/null || true)
  [[ $old_port == 8080 || $old_port == 8081 ]] || { echo 'Invalid active port' >&2; exit 1; }
  systemctl is-active --quiet "$old_service"
  healthy "$old_port"
  if [[ $old_port == 8080 ]]; then slot=green; port=8081; else slot=blue; port=8080; fi
  candidate="xinke-bluegreen@$slot.service"
  ! systemctl is-active --quiet "$candidate" || { echo 'Candidate slot is already running' >&2; exit 1; }
  previous_slot=$(readlink "$root/bg-slots/$slot" 2>/dev/null || true)
  prepared="$root/bg-releases/$stamp"
  [[ ! -e $prepared ]] || { echo 'Release stamp already exists' >&2; exit 1; }
  mkdir -p "$prepared/dist"
  install -m 0644 "$release/xinke-admin.jar" "$prepared/xinke-admin.jar"
  if [[ -s $release/xinke-ui.tar.gz ]]; then
    tar -xzf "$release/xinke-ui.tar.gz" -C "$prepared/dist"
  else
    cp -a "$old_ui/." "$prepared/dist/"
  fi
  [[ -s $prepared/dist/index.html ]] || { echo 'UI archive lacks index.html' >&2; exit 1; }
  for directory in static assets; do
    if [[ -d $old_ui/$directory ]]; then
      mkdir -p "$prepared/dist/$directory"
      cp -an "$old_ui/$directory/." "$prepared/dist/$directory/"
    fi
  done
  printf '%s\n' "$old_port" > "$prepared/previous-port"
  printf '%s\n' "$old_service" > "$prepared/previous-service"
  printf '%s\n' "$old_ui" > "$prepared/previous-ui"
  printf '%s\n' "$old_release" > "$prepared/previous-release"
  cp -a "$upstream_conf" "$prepared/previous-upstream.conf"
  atomic_link "$root/bg-slots/$slot" "$prepared"
  trap rollback_failed_deploy ERR
  systemctl start "$candidate"
  wait_healthy "$port"

  switch_started=1
  set_backend "$port"
  atomic_link "$root/active-ui" "$prepared/dist"
  atomic_file "$root/active-port" "$port"
  atomic_file "$root/active-service" "$candidate"
  atomic_file "$root/active-release" "$prepared"
  curl -fsS --max-time 5 http://127.0.0.1/ >/dev/null
  curl -fsS --max-time 5 -H 'Host: xinkeerp.cn' http://127.0.0.1/prod-api/ >/dev/null
  systemctl enable "$candidate" >/dev/null
  systemctl disable "$old_service" >/dev/null
  trap - ERR

  for _ in $(seq 1 120); do
    connections=$(ss -Htn state established | awk -v port=":$old_port" '$4 ~ port"$" {n++} END {print n+0}')
    if (( connections == 0 )); then break; fi
    sleep 1
  done
  if (( connections == 0 )); then
    systemctl stop "$old_service" || echo "Old service could not be stopped: $old_service" >&2
  else
    echo "Old service retained: $connections active connections after 120 seconds" >&2
  fi
  echo "BLUEGREEN_DEPLOY_OK active=$candidate port=$port release=$prepared previous=$old_service"
}

rollback() {
  require_setup
  local current_release old_port old_service old_ui old_release current_service current_port current_ui
  current_release=$(<"$root/active-release")
  [[ $current_release == "$root/bg-releases/"* && -d $current_release ]] || { echo 'No active blue-green release' >&2; exit 1; }
  old_port=$(<"$current_release/previous-port")
  old_service=$(<"$current_release/previous-service")
  old_ui=$(<"$current_release/previous-ui")
  old_release=$(<"$current_release/previous-release")
  current_service=$(<"$root/active-service")
  current_port=$(<"$root/active-port")
  current_ui=$(readlink -f "$root/active-ui")
  [[ $old_port == 8080 || $old_port == 8081 ]] || { echo 'Invalid previous port' >&2; exit 1; }
  [[ -d $old_ui ]] || { echo 'Previous UI is missing' >&2; exit 1; }
  trap rollback_failed_rollback ERR
  systemctl start "$old_service"
  wait_healthy "$old_port"
  set_backend "$old_port"
  atomic_link "$root/active-ui" "$old_ui"
  atomic_file "$root/active-port" "$old_port"
  atomic_file "$root/active-service" "$old_service"
  if [[ -n $old_release ]]; then atomic_file "$root/active-release" "$old_release"; else rm -f "$root/active-release"; fi
  curl -fsS --max-time 5 http://127.0.0.1/ >/dev/null
  curl -fsS --max-time 5 -H 'Host: xinkeerp.cn' http://127.0.0.1/prod-api/ >/dev/null
  systemctl enable "$old_service" >/dev/null
  systemctl disable "$current_service" >/dev/null
  systemctl stop "$current_service"
  trap - ERR
  echo "BLUEGREEN_ROLLBACK_OK active=$old_service port=$old_port previous=$current_service:$current_port"
}

case ${1:-} in
  status)
    require_setup
    echo "active_port=$(<"$root/active-port") active_service=$(<"$root/active-service")"
    echo "available_ram_kb=$(awk '/MemAvailable:/ {print $2}' /proc/meminfo)"
    ;;
  deploy)
    [[ $# == 3 ]] || { echo 'Usage: deploy RELEASE_DIR YYYYMMDD-HHMMSS' >&2; exit 2; }
    deploy "$2" "$3"
    ;;
  rollback)
    [[ $# == 1 ]] || { echo 'Usage: rollback' >&2; exit 2; }
    rollback
    ;;
  *)
    echo 'Usage: xinke-bluegreen-release status | deploy RELEASE_DIR YYYYMMDD-HHMMSS | rollback' >&2
    exit 2
    ;;
esac
