#!/usr/bin/env bash
set -euo pipefail

[[ $EUID == 0 ]] || { echo 'Run setup.sh as root' >&2; exit 1; }
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
root=/ruoyi
nginx_conf=/etc/nginx/conf.d/ruoyi.conf
upstream_conf=/etc/nginx/conf.d/xinke-active-upstream.conf
unit=/etc/systemd/system/xinke-bluegreen@.service
helper=/usr/local/sbin/xinke-bluegreen-release

[[ -f $nginx_conf && -d $root/dist && -f $root/xinke-admin.jar ]] || {
  echo 'Expected the existing /ruoyi deployment and Nginx configuration' >&2; exit 1;
}
[[ ! -e $root/active-port && ! -e $upstream_conf && ! -e $root/active-ui ]] || {
  echo 'Blue-green setup already exists; inspect it before rerunning' >&2; exit 1;
}
systemctl is-active --quiet ruoyi
curl -fsS --max-time 5 http://127.0.0.1:8080/ >/dev/null
grep -Fq 'proxy_pass http://127.0.0.1:8080/;' "$nginx_conf"
available=$(awk '/MemAvailable:/ {print $2}' /proc/meminfo)
(( available >= 850000 )) || {
  echo "Insufficient available RAM for blue-green setup: ${available} kB" >&2; exit 1
}
bash -n "$script_dir/deploy.sh"

backup="$nginx_conf.before-bluegreen-$(date +%Y%m%d-%H%M%S)"
cp -a "$nginx_conf" "$backup"
mkdir -p "$root/bg-slots" "$root/bg-releases" /etc/xinke-bluegreen
printf 'XINKE_PORT=8080\n' > /etc/xinke-bluegreen/blue.env
printf 'XINKE_PORT=8081\n' > /etc/xinke-bluegreen/green.env
printf '8080\n' > "$root/active-port"
printf 'ruoyi\n' > "$root/active-service"
ln -s "$root/dist" "$root/active-ui"
install -m 0644 "$script_dir/xinke-bluegreen@.service" "$unit"
install -m 0755 "$script_dir/deploy.sh" "$helper"
printf 'upstream xinke_backend { server 127.0.0.1:8080; }\n' > "$upstream_conf"
install -m 0644 "$script_dir/ruoyi.conf" "$nginx_conf"

if ! nginx -t || ! systemctl reload nginx || ! curl -fsS --max-time 5 http://127.0.0.1/ >/dev/null; then
  cp -a "$backup" "$nginx_conf"
  rm -f "$upstream_conf"
  nginx -t && systemctl reload nginx
  rm -f "$root/active-port" "$root/active-service" "$root/active-ui" "$unit" "$helper"
  echo 'Nginx setup failed; original configuration restored' >&2
  exit 1
fi

systemctl daemon-reload
sudoers_tmp=/etc/sudoers.d/xinke-bluegreen.tmp
printf 'deploy ALL=(root) NOPASSWD: %s *\n' "$helper" > "$sudoers_tmp"
chmod 0440 "$sudoers_tmp"
visudo -cf "$sudoers_tmp" >/dev/null
mv -f "$sudoers_tmp" /etc/sudoers.d/xinke-bluegreen
echo "BLUEGREEN_SETUP_OK nginx_backup=$backup"
