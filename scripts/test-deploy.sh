#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source_root=$(pwd -P)
root="$source_root/deploy-test-$$"
mkdir -p "$root/bin" "$root/releases/v1/deploy" "$root/releases/v2/deploy" "$root/releases/v3/deploy"
printf 'COMPOSE_PROJECT_NAME=starter-test\n' > "$root/.env"
for v in v1 v2 v3; do cp deploy/compose.yml deploy/deploy.sh deploy/rollback.sh deploy/backup.sh "$root/releases/$v/deploy/"; done
cat > "$root/bin/docker" <<'SH'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "$MOCK_LOG"
if [[ "$*" == *' exec -T mysql '* ]]; then
  if [[ "${FAIL_BACKUP:-false}" == true ]]; then exit 1; fi
  printf 'CREATE TABLE test (id int);\n'; exit 0
fi
if [[ "$*" == *releases/v3/* && "$*" == *' up '* ]]; then exit 1; fi
exit 0
SH
# Git Bash has no flock on some Windows installations; mocking concurrency is explicitly out of scope.
if ! command -v flock >/dev/null; then printf '#!/usr/bin/env bash\nexit 0\n' > "$root/bin/flock"; fi
chmod +x "$root/bin/"*
export PATH="$root/bin:$PATH" MOCK_LOG="$root/docker.log"
(cd "$root/releases/v1"; bash deploy/deploy.sh "$root" v1 example/backend:v1 example/frontend:v1 all)
[[ $(cat "$root/.current-release") == v1 ]]
(cd "$root/releases/v2"; bash deploy/deploy.sh "$root" v2 example/backend:v2 example/frontend:v2 backend)
grep -q 'FRONTEND_IMAGE=example/frontend:v1' "$root/releases/v2/images.env"
[[ $(cat "$root/.previous-release") == v1 ]]
if (export FAIL_BACKUP=true; cd "$root/releases/v3"; bash deploy/deploy.sh "$root" v3 example/backend:v3 example/frontend:v3 all); then echo 'Expected backup failure' >&2; exit 1; fi
[[ $(cat "$root/.current-release") == v2 ]]
if (cd "$root/releases/v3"; bash deploy/deploy.sh "$root" v3 example/backend:v3 example/frontend:v3 all); then echo 'Expected health failure' >&2; exit 1; fi
[[ $(cat "$root/.current-release") == v2 ]]
bash deploy/rollback.sh "$root" v1
[[ $(cat "$root/.current-release") == v1 ]]
echo "PASS: first release, partial release, failed health recovery, explicit rollback. Test artifacts: $root"
