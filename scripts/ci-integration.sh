#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd -P)
suffix="$(date +%s)-$$"
mysql="starter-it-mysql-$suffix"; redis="starter-it-redis-$suffix"; network="starter-it-$suffix"
export MYSQL_ROOT_PASSWORD MYSQL_PASSWORD DB_PASSWORD REDIS_PASSWORD
MYSQL_ROOT_PASSWORD=$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')
MYSQL_PASSWORD=$MYSQL_ROOT_PASSWORD; DB_PASSWORD=$MYSQL_ROOT_PASSWORD; REDIS_PASSWORD=$MYSQL_ROOT_PASSWORD
cleanup() { docker rm -fv "$mysql" "$redis" >/dev/null 2>&1 || true; docker network rm "$network" >/dev/null 2>&1 || true; }
trap cleanup EXIT
docker network create "$network" >/dev/null
docker run -d --name "$mysql" --network "$network" -e MYSQL_ROOT_PASSWORD -e MYSQL_PASSWORD -e MYSQL_USER=starter_it -e MYSQL_DATABASE=starter_it mysql:8.0.44 --performance-schema=OFF --innodb-buffer-pool-size=32M >/dev/null
docker run -d --name "$redis" --network "$network" -e REDIS_PASSWORD redis:7.4.2-alpine sh -c 'exec redis-server --requirepass "$REDIS_PASSWORD"' >/dev/null
ready=false
for ((i=0;i<90;i++)); do
  if docker exec "$mysql" sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -e "SELECT 1"' >/dev/null 2>&1; then ready=true;break;fi
  sleep 2
done
[[ "$ready" == true ]] || exit 1
docker run --rm --network "$network" --user "$(id -u):$(id -g)" -v "$root:/repo" -w /repo/backend \
  -e MAVEN_CONFIG=/tmp/maven \
  -e 'JAVA_TOOL_OPTIONS=-Xms64m -Xmx384m -Dfile.encoding=UTF-8' \
  -e STARTER_INTEGRATION_TESTS=true -e DB_HOST="$mysql" -e DB_PORT=3306 -e DB_NAME=starter_it -e DB_USER=starter_it -e DB_PASSWORD \
  -e REDIS_HOST="$redis" -e REDIS_PORT=6379 -e REDIS_PASSWORD -e STARTER_BASELINE_EXISTING=false \
  -e ADMIN_INITIAL_PASSWORD=VerifyStarter123 -e APP_PUBLIC_URL=http://localhost:8080 \
  maven:3.9.9-eclipse-temurin-17 mvn -Duser.home=/tmp/maven -Dmaven.repo.local=/tmp/maven/repository -s ../deploy/maven-settings.xml -B -ntp verify
