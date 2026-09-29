#!/usr/bin/env bash
set -euo pipefail
# Called from a versioned release directory containing deploy/ and database/.
root=${1:?deployment directory}; version=${2:?release version}
backend=${3:?backend image}; frontend=${4:?frontend image}; scope=${5:-all}
[[ "$root" =~ ^/[A-Za-z0-9_/-]+$ && "$root" != / && "$root" != *..* ]] || exit 2
[[ "$version" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]{0,80}$ && "$version" != *..* ]] || exit 2
[[ "$scope" =~ ^(all|backend|frontend)$ ]] || exit 2
for image in "$backend" "$frontend"; do [[ "$image" =~ ^[A-Za-z0-9][A-Za-z0-9._:/-]+$ && "$image" != *latest ]] || exit 2; done
release=$(pwd -P)
[[ "$release" == "$root/releases/$version" ]] || { echo 'Run inside the expected release directory' >&2; exit 2; }
[[ -f "$root/.env" ]] || { echo 'First configure the server .env using .env.example' >&2; exit 2; }
project=$(sed -n 's/^COMPOSE_PROJECT_NAME=//p' "$root/.env" | tr -d '\r')
[[ "$project" =~ ^[a-z0-9][a-z0-9_-]+$ ]] || exit 2
exec 9>"$root/.deploy.lock"
flock -n 9 || { echo 'Another deployment is running' >&2; exit 1; }
previous=''
if [[ -f "$root/.current-release" ]]; then previous=$(cat "$root/.current-release"); fi
if [[ -n "$previous" ]]; then
  [[ "$previous" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]{0,80}$ && "$previous" != *..* ]] || exit 2
  old="$root/releases/$previous"
  [[ -f "$old/images.env" ]] || exit 2
  if [[ "$scope" == frontend ]]; then backend=$(sed -n 's/^BACKEND_IMAGE=//p' "$old/images.env"); fi
  if [[ "$scope" == backend ]]; then frontend=$(sed -n 's/^FRONTEND_IMAGE=//p' "$old/images.env"); fi
elif [[ "$scope" != all ]]; then
  echo 'The first deployment must include both frontend and backend' >&2; exit 2
fi
printf 'BACKEND_IMAGE=%s\nFRONTEND_IMAGE=%s\n' "$backend" "$frontend" > images.env
dc() { docker compose --env-file "$root/.env" --env-file "$1/images.env" -p "$project" -f "$1/deploy/compose.yml" "${@:2}"; }
dc "$release" config --quiet
pull_policy=${DEPLOY_PULL_POLICY:-always}
[[ "$pull_policy" =~ ^(always|missing)$ ]] || exit 2
dc "$release" pull --policy "$pull_policy" mysql redis backend frontend
if [[ -n "$previous" && "$scope" != frontend ]]; then
  # Refuse an application upgrade if its pre-migration backup fails.
  bash "$release/deploy/backup.sh" "$root" "$old"
fi
if dc "$release" up -d --no-build --wait --wait-timeout 420; then
  # Nginx resolves the backend container address at startup; refresh after a backend replacement.
  if dc "$release" up -d --no-build --no-deps --force-recreate --wait --wait-timeout 60 frontend; then
    if [[ -n "$previous" ]]; then printf '%s' "$previous" > "$root/.previous-release"; fi
    printf '%s' "$version" > "$root/.current-release"
    echo "Deployment healthy: $version"; exit 0
  fi
fi
echo 'Deployment failed. Inspect docker compose logs on the server.' >&2
if [[ -n "$previous" ]]; then
  dc "$old" up -d --no-build --no-deps --wait --wait-timeout 420 backend frontend
  dc "$old" up -d --no-build --no-deps --force-recreate --wait --wait-timeout 60 frontend
  echo "Application restored to $previous; database was not rolled back." >&2
fi
exit 1
