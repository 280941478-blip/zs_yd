#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
case "${1:-init}" in
  init) node scripts/init-env.cjs ;;
  infra) docker compose --env-file .env -f deploy/compose.yml -f deploy/compose.dev.yml up -d --wait mysql redis ;;
  backend) cd backend; mvn -s ../deploy/maven-settings.xml -B -ntp -DskipTests package; exec java -jar cdzs-server/target/cdzs-server.jar ;;
  frontend) cd frontend; pnpm install --frozen-lockfile; exec pnpm dev ;;
  all) node scripts/init-env.cjs; docker compose --env-file .env -f deploy/compose.yml up -d --build --wait --wait-timeout 600 ;;
  stop) docker compose --env-file .env -f deploy/compose.yml -f deploy/compose.dev.yml stop ;;
  *) echo 'Usage: bash scripts/dev.sh init|infra|backend|frontend|all|stop' >&2; exit 2 ;;
esac
