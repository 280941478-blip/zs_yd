#!/usr/bin/env bash
set -euo pipefail
# Reuse ONLY an isolated verify-deployment fixture after its containers have been removed.
root=${1:?verification-deploy fixture}; archive=${2:?fixture backup}
[[ "$root" == /*/verification-deploy-* && "$root" != *..* ]] || exit 2
project=$(sed -n 's/^COMPOSE_PROJECT_NAME=//p' "$root/.env")
[[ "$project" == starter-verify-* ]] || exit 2
release="$root/releases/v1"
dc() { docker compose --env-file "$root/.env" --env-file "$release/images.env" -p "$project" -f "$release/deploy/compose.yml" "$@"; }
[[ -z $(dc ps -q) ]] || { echo 'Fixture is still running; refuse to replace it' >&2; exit 2; }
cleanup() { dc down -v >/dev/null 2>&1 || true; }
trap cleanup EXIT
dc up -d --wait --wait-timeout 120 mysql
gzip -dc "$archive" | dc exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot "$MYSQL_DATABASE"'
dc exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot "$MYSQL_DATABASE" -e "INSERT INTO example_task(tenant_id,owner_id,code,name,status) VALUES(1,1,\"AFTER_BACKUP\",\"restore verification\",0);"'
bash "$release/deploy/restore.sh" "$root" "$archive" "--replace-database=$project"
counts=$(dc exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N "$MYSQL_DATABASE" -e "SELECT COUNT(*) FROM example_task WHERE code=\"VERIFY\"; SELECT COUNT(*) FROM example_task WHERE code=\"AFTER_BACKUP\";"')
[[ "$counts" == $'1\n0' ]] || { echo 'Restored data does not match the backup' >&2; exit 1; }
echo 'PASS: explicit restore stopped applications, created a safety backup and replaced only the disposable database with the selected snapshot.'
