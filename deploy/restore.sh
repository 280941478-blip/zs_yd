#!/usr/bin/env bash
set -euo pipefail
umask 077
root=${1:?deployment root}; archive=${2:?absolute backup path}; confirm=${3:-}
[[ "$root" =~ ^/[A-Za-z0-9_/-]+$ && "$root" != / && "$root" != *..* ]] || exit 2
project=$(sed -n 's/^COMPOSE_PROJECT_NAME=//p' "$root/.env" | tr -d '\r')
[[ "$project" =~ ^[a-z0-9][a-z0-9_-]+$ && "$confirm" == "--replace-database=$project" ]] || {
  echo 'This replaces the entire project database. Pass --replace-database=<COMPOSE_PROJECT_NAME> only during an approved maintenance window.' >&2; exit 2;
}
[[ "$archive" == /* && "$archive" == *.sql.gz && -f "$archive" && -f "$archive.sha256" ]] || exit 2
(cd "$(dirname "$archive")"; sha256sum -c "$(basename "$archive").sha256")
gzip -t "$archive"
version=$(cat "$root/.current-release")
[[ "$version" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]{0,80}$ && "$version" != *..* ]] || exit 2
release="$root/releases/$version"
exec 9>"$root/.deploy.lock"; flock -n 9 || exit 1
dc() { docker compose --env-file "$root/.env" --env-file "$release/images.env" -p "$project" -f "$release/deploy/compose.yml" "$@"; }
dc stop frontend backend
# Snapshot after stopping writes. Do not expire any backups while preparing a restore.
BACKUP_RETAIN_DAYS=365000 bash "$release/deploy/backup.sh" "$root" "$release"
dc exec -T mysql sh -c '
  case "$MYSQL_DATABASE" in ""|*[!a-zA-Z0-9_]*) exit 2;; esac
  MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -e "DROP DATABASE \`$MYSQL_DATABASE\`; CREATE DATABASE \`$MYSQL_DATABASE\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
'
gzip -dc "$archive" | dc exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot "$MYSQL_DATABASE"'
echo 'Database restored. Applications remain STOPPED. Start a schema-compatible application release after checking data and clearing stale application caches.'
