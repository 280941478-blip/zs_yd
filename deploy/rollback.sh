#!/usr/bin/env bash
set -euo pipefail
root=${1:?deployment directory}; version=${2:-}
[[ "$root" =~ ^/[A-Za-z0-9_/-]+$ && "$root" != / && "$root" != *..* ]] || exit 2
exec 9>"$root/.deploy.lock"; flock -n 9 || exit 1
if [[ -z "$version" ]]; then version=$(cat "$root/.previous-release"); fi
[[ "$version" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]{0,80}$ && "$version" != *..* ]] || exit 2
release="$root/releases/$version"
[[ -f "$release/images.env" ]] || { echo 'Unknown release' >&2; exit 2; }
project=$(sed -n 's/^COMPOSE_PROJECT_NAME=//p' "$root/.env" | tr -d '\r')
[[ "$project" =~ ^[a-z0-9][a-z0-9_-]+$ ]] || exit 2
dc() { docker compose --env-file "$root/.env" --env-file "$release/images.env" -p "$project" -f "$release/deploy/compose.yml" "$@"; }
pull_policy=${DEPLOY_PULL_POLICY:-always}
[[ "$pull_policy" =~ ^(always|missing)$ ]] || exit 2
dc pull --policy "$pull_policy" backend frontend
dc up -d --no-build --no-deps --wait --wait-timeout 420 backend frontend
dc up -d --no-build --no-deps --force-recreate --wait --wait-timeout 60 frontend
if [[ -f "$root/.current-release" ]]; then cp "$root/.current-release" "$root/.previous-release"; fi
printf '%s' "$version" > "$root/.current-release"
echo "Application rolled back to $version. Database unchanged."
