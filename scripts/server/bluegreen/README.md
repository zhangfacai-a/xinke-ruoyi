# Blue-green publishing (prepared, not installed)

The current production server has about 1.7 GiB RAM and only about 254 MiB available. Do not install or run this flow until the server has enough spare memory for two Java instances. The deploy helper refuses to stage a candidate below 850000 KiB available RAM or 1 GiB free disk. A 4 GiB server is the recommended starting point; verify actual headroom after upgrading.

The current service `ruoyi` stays on port 8080 for the first switch. The candidate starts on 8081. Later releases alternate between 8080 and 8081 using `xinke-bluegreen@blue.service` and `xinke-bluegreen@green.service`. Nginx reloads after the candidate responds on its own port. The old service remains until established connections drain, up to 120 seconds. Frontend assets from the prior release are copied into the new directory so open pages can still request their hashed files. Old releases are retained for rollback and require periodic disk review.

## One-time activation after capacity upgrade

1. Back up `/etc/nginx/conf.d/ruoyi.conf`, `/etc/systemd/system/ruoyi.service`, and the current release. Confirm the server configuration still matches the checked-in templates.
2. Copy this directory to the server and run `sudo bash setup.sh` from it. This installs the service template, Nginx upstream switch, and the restricted `/usr/local/sbin/xinke-bluegreen-release` helper. The existing `ruoyi` service remains live.
3. Run `sudo /usr/local/sbin/xinke-bluegreen-release status` and verify the site still works through Nginx before the first release.

The first activation needs root access to install the Nginx and systemd configuration. Later releases use the existing `deploy` SSH key through `deploy_with_key.py`; no root password is needed for each release.

## Release

Prepare a reviewed release directory containing `xinke-admin.jar`. Add `xinke-ui.tar.gz` only when the frontend changes; omitting it reuses the current frontend. The archive must contain `index.html` at its root. Apply backward-compatible database migrations before switching traffic. The script does not run migrations.

```sh
python scripts/server/bluegreen/deploy_with_key.py \
  --release PATH_TO_REVIEWED_RELEASE --stamp YYYYMMDD-HHMMSS
```

The helper checks memory, disk, old-service health, and candidate health before switching. It automatically restores Nginx, the active UI, and the old service when a pre-switch or switch check fails. For a later manual rollback, run `sudo /usr/local/sbin/xinke-bluegreen-release rollback` on the server. The `status` command reports the active port and available memory.

All four application scheduled task classes check `/ruoyi/active-port` when started through the blue-green service template. The old single-instance deployment has no marker setting and keeps its current behavior. During the first transition, stop the legacy `ruoyi` service after traffic drains; avoid switching while a long-running scheduled task is active. Database migrations must allow both application versions to run during the overlap. WebSocket connections and requests lasting longer than the drain period can still be interrupted; this is near-zero-downtime publishing, not an absolute guarantee of uninterrupted sessions.
