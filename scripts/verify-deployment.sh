#!/usr/bin/env bash
set -euo pipefail
# Linux + Docker CLI + bash/flock/gzip/coreutils. Uses already verified JAR and frontend dist.
source_root=$(cd "$(dirname "$0")/.." && pwd -P)
root="$source_root/verification-deploy-$(date +%s)-$$"
version="verify-$(date +%s)-$$"; project="starter-$version"
[[ -f "$source_root/backend/yudao-server/target/yudao-server.jar" && -f "$source_root/frontend/dist/index.html" ]] || exit 2
mkdir -p "$root/backend" "$root/frontend" "$root/releases/v1/deploy" "$root/releases/v2/deploy"
cp "$source_root/backend/yudao-server/target/yudao-server.jar" "$root/backend/app.jar"
cp -R "$source_root/frontend/dist/." "$root/frontend/"
cp "$source_root/deploy/nginx.conf" "$root/frontend/default.conf"
cat > "$root/backend/Dockerfile" <<'DOCKER'
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY app.jar /app/app.jar
ENTRYPOINT ["java","-Xms64m","-Xmx384m","-jar","/app/app.jar"]
DOCKER
cat > "$root/frontend/Dockerfile" <<'DOCKER'
FROM nginx:1.28-alpine
COPY . /usr/share/nginx/html/
COPY default.conf /etc/nginx/conf.d/default.conf
DOCKER
docker build -t "starter-verify/backend:$version" "$root/backend"
docker build -t "starter-verify/frontend:$version" "$root/frontend"
password=$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')
umask 077
cat > "$root/.env" <<ENV
COMPOSE_PROJECT_NAME=$project
DB_NAME=starter_verify
DB_USER=yudao
DB_PASSWORD=$password
MYSQL_ROOT_PASSWORD=$password
REDIS_PASSWORD=$password
ADMIN_INITIAL_PASSWORD=VerifyStarter123
HTTP_BIND=127.0.0.1
HTTP_PORT=0
APP_PUBLIC_URL=http://localhost:8080
ENV
for v in v1 v2; do
  cp "$source_root/deploy/"* "$root/releases/$v/deploy/"
  # Test-only footprint: production compose keeps its normal MySQL settings.
  sed -i '/image: mysql:8.0.44/a\    command: ["--performance-schema=OFF", "--innodb-buffer-pool-size=32M"]' "$root/releases/$v/deploy/compose.yml"
done
dc() { docker compose --env-file "$root/.env" --env-file "$root/releases/v1/images.env" -p "$project" -f "$root/releases/v1/deploy/compose.yml" "$@"; }
cleanup() {
  dc logs --no-color > "$root/containers.log" 2>&1 || true
  dc down -v >/dev/null 2>&1 || true
}
trap cleanup EXIT
export DEPLOY_PULL_POLICY=missing
(cd "$root/releases/v1"; bash deploy/deploy.sh "$root" v1 "starter-verify/backend:$version" "starter-verify/frontend:$version" all)
dc exec -T frontend wget -q -O- 'http://127.0.0.1/admin-api/system/tenant/get-id-by-name?name=%E9%BB%98%E8%AE%A4%E7%A7%9F%E6%88%B7' | grep '"data":1' >/dev/null
dc exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot "$MYSQL_DATABASE" -e "INSERT INTO example_task(tenant_id,owner_id,code,name,status) VALUES(1,1,\"VERIFY\",\"deployment verification\",0);"'
docker tag "starter-verify/backend:$version" "starter-verify/backend:$version-v2"
(cd "$root/releases/v2"; bash deploy/deploy.sh "$root" v2 "starter-verify/backend:$version-v2" "starter-verify/frontend:$version" backend)
bash "$root/releases/v2/deploy/rollback.sh" "$root" v1
count=$(dc exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N "$MYSQL_DATABASE" -e "SELECT COUNT(*) FROM example_task WHERE code=\"VERIFY\";"')
[[ "$count" == 1 && $(cat "$root/.current-release") == v1 ]] || exit 1
archive=$(ls -t "$root/backups/"*.sql.gz | head -1)
dc stop frontend backend redis
bash "$source_root/deploy/restore-check.sh" "$archive"
echo "PASS: real container first install, backend upgrade, pre-upgrade backup, application rollback, persisted data and isolated restore. Evidence: $root"
