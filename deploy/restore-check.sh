#!/usr/bin/env bash
set -euo pipefail
# Restores ONLY into a disposable, network-isolated MySQL container. No production DB writes.
archive=${1:?absolute .sql.gz backup}; image=${MYSQL_DRILL_IMAGE:-mysql:8.0.44}
[[ "$archive" == /* && "$archive" == *.sql.gz && -f "$archive" && -f "$archive.sha256" ]] || exit 2
(cd "$(dirname "$archive")"; sha256sum -c "$(basename "$archive").sha256")
gzip -t "$archive"
name="starter-restore-check-$(date +%s)-$$"
export MYSQL_ROOT_PASSWORD
MYSQL_ROOT_PASSWORD=$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')
cleanup() { docker rm -fv "$name" >/dev/null 2>&1 || true; }
trap cleanup EXIT
docker run -d --name "$name" --network none -e MYSQL_ROOT_PASSWORD -e MYSQL_DATABASE=restore_check "$image" --performance-schema=OFF --innodb-buffer-pool-size=32M >/dev/null
ready=false
for ((i=0;i<90;i++)); do
  if docker exec "$name" sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -e "SELECT 1"' >/dev/null 2>&1; then ready=true;break;fi
  sleep 2
done
[[ "$ready" == true ]] || { echo 'Restore MySQL did not become ready' >&2; exit 1; }
gzip -dc "$archive" | docker exec -i "$name" sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot restore_check'
admin_count=$(docker exec "$name" sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N restore_check -e "SELECT COUNT(*) FROM system_users WHERE id=1 AND tenant_id=1;"')
[[ "$admin_count" == 1 ]] || { echo 'Restored admin record is missing' >&2; exit 1; }
docker exec "$name" sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N restore_check -e "SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema=DATABASE(); SELECT CAST(completed AS UNSIGNED) AS initialized FROM starter_installation WHERE id=1;"'
echo 'Restore drill passed: SQL imported into an isolated MySQL database; source database unchanged.'
