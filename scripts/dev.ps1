param([ValidateSet('init','infra','backend','frontend','all','stop')][string]$Action='init')
$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
function Check { if ($LASTEXITCODE -ne 0) { throw "命令执行失败，退出码 $LASTEXITCODE" } }
switch ($Action) {
  init { node scripts/init-env.cjs; Check }
  infra { docker compose --env-file .env -f deploy/compose.yml -f deploy/compose.dev.yml up -d --wait mysql redis; Check }
  backend { Set-Location backend; mvn -s ../deploy/maven-settings.xml -B -ntp -DskipTests package; Check; java -jar cdzs-server/target/cdzs-server.jar; Check }
  frontend { Set-Location frontend; pnpm install --frozen-lockfile; Check; pnpm dev; Check }
  all { node scripts/init-env.cjs; Check; docker compose --env-file .env -f deploy/compose.yml up -d --build --wait --wait-timeout 600; Check }
  stop { docker compose --env-file .env -f deploy/compose.yml -f deploy/compose.dev.yml stop; Check }
}
