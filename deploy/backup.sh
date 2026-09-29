#!/usr/bin/env bash
set -euo pipefail
umask 077
root=${1:?deployment root}; release=${2:-}; retain=${BACKUP_RETAIN_DAYS:-14}
[[ "$root" =~ ^/[A-Za-z0-9_/-]+$ && "$root" != / && "$root" != *..* ]] || exit 2
[[ "$retain" =~ ^[0-9]+$ && "$retain" -ge 1 ]] || exit 2
if [[ -z "$release" ]]; then release="$root/releases/$(cat "$root/.current-release")"; fi
[[ "$release" == "$root" || "$release" == "$root"/releases/* ]] || exit 2
[[ "$release" != *..* && -f "$release/deploy/compose.yml" && -f "$root/.env" ]] || exit 2
project=$(sed -n 's/^COMPOSE_PROJECT_NAME=//p' "$root/.env" | tr -d '\r')
[[ "$project" =~ ^[a-z0-9][a-z0-9_-]+$ ]] || exit 2
mkdir -p "$root/backups"
exec 8>"$root/backups/.backup.lock"
flock -n 8 || { echo 'Another backup is running' >&2; exit 1; }
name="mysql-$(date -u +%Y%m%dT%H%M%SZ)-$$.sql.gz"
partial="$root/backups/$name.partial"
trap 'rm -f -- "$partial"' EXIT
docker compose --env-file "$root/.env" -p "$project" -f "$release/deploy/compose.yml" exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --quick --hex-blob --routines --events --triggers --set-gtid-purged=OFF --no-tablespaces "$MYSQL_DATABASE"' | gzip > "$partial"
gzip -t "$partial"
gzip -dc "$partial" | grep 'CREATE TABLE' > /dev/null
mv "$partial" "$root/backups/$name"
(cd "$root/backups"; sha256sum "$name" > "$name.sha256")
find "$root/backups" -maxdepth 1 -type f -name 'mysql-*.sql.gz*' -mtime +"$retain" -delete
echo "Backup ready: $root/backups/$name"
