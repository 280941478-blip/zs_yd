pipeline {
  agent { label 'docker' }
  options {
    timestamps()
    disableConcurrentBuilds()
    timeout(time: 60, unit: 'MINUTES')
    buildDiscarder(logRotator(numToKeepStr: '20'))
    skipDefaultCheckout(true)
  }
  parameters {
    string(name: 'GIT_REF', defaultValue: '', description: '留空使用任务配置分支，或填写分支/标签；回滚不依赖此参数')
    choice(name: 'ACTION', choices: ['BUILD_ONLY', 'DEPLOY', 'ROLLBACK'], description: '默认只构建验证')
    choice(name: 'TARGET_ENV', choices: ['test', 'prod'], description: '目标环境')
    choice(name: 'SCOPE', choices: ['all', 'backend', 'frontend'], description: '首次发布选all；回滚总是恢复前后端组合')
    string(name: 'ROLLBACK_VERSION', defaultValue: '', description: '回滚版本号，留空使用服务器记录的上一版本')
  }
  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script {
          if (params.GIT_REF?.trim()) {
            def ref=params.GIT_REF.trim()
            if (!(ref ==~ /[A-Za-z0-9][A-Za-z0-9_.\/-]*/) || ref.contains('..')) error('分支/标签格式不正确')
            checkout([$class: 'GitSCM', branches: [[name: ref]], userRemoteConfigs: scm.userRemoteConfigs])
          }
          def config=readJSON(file: 'deploy/targets.json')
          def target=config[params.TARGET_ENV]
          env.REGISTRY=config.registry
          env.REGISTRY_HOST=config.registryHost
          env.REGISTRY_CREDENTIAL=config.registryCredentialId
          env.PROJECT=config.project
          env.REMOTE_HOST=target.host; env.REMOTE_USER=target.user; env.REMOTE_PORT=target.port
          env.REMOTE_DIR=target.directory; env.SSH_CREDENTIAL=target.sshCredentialId; env.KNOWN_HOSTS_CREDENTIAL=target.knownHostsCredentialId
          env.RELEASE_VERSION="${env.BUILD_NUMBER}-"+sh(script:'git rev-parse --short=12 HEAD',returnStdout:true).trim()
          env.BACKEND_IMAGE="${env.REGISTRY}/${env.PROJECT}/backend:${env.RELEASE_VERSION}"
          env.FRONTEND_IMAGE="${env.REGISTRY}/${env.PROJECT}/frontend:${env.RELEASE_VERSION}"
          if (!(env.REGISTRY ==~ /[A-Za-z0-9][A-Za-z0-9._:\/-]*/) || !(env.PROJECT ==~ /[a-z0-9][a-z0-9-]*/)) error('镜像配置格式错误')
          if (params.ACTION!='BUILD_ONLY') {
            if (env.REMOTE_HOST=='CHANGE_ME' || env.REGISTRY_HOST=='registry.example.com') error('请先配置deploy/targets.json和Jenkins凭据')
            if (!(env.REMOTE_HOST ==~ /[A-Za-z0-9][A-Za-z0-9.-]*/) || !(env.REMOTE_USER ==~ /[a-z_][a-z0-9_-]*/) || !(env.REMOTE_PORT ==~ /[0-9]{1,5}/)) error('SSH目标格式错误')
            if (!(env.REMOTE_DIR ==~ /\/[A-Za-z0-9_\/-]+/) || env.REMOTE_DIR.contains('..')) error('部署目录格式错误')
          }
          if (params.ROLLBACK_VERSION && (!(params.ROLLBACK_VERSION ==~ /[A-Za-z0-9][A-Za-z0-9_.-]{0,80}/) || params.ROLLBACK_VERSION.contains('..'))) error('回滚版本格式错误')
        }
      }
    }
    stage('Build and test') {
      when { expression { params.ACTION!='ROLLBACK' } }
      steps {
        sh '''
          set -eu
          bash -n deploy/deploy.sh deploy/rollback.sh deploy/backup.sh deploy/restore-check.sh deploy/restore.sh scripts/dev.sh scripts/ci-integration.sh scripts/verify-deployment.sh scripts/verify-restore.sh
          bash scripts/test-deploy.sh
          if [ "$SCOPE" = all ] || [ "$SCOPE" = backend ]; then
            DOCKER_BUILDKIT=1 docker build -f deploy/Dockerfile.backend -t "$BACKEND_IMAGE" .
          fi
          if [ "$SCOPE" = all ] || [ "$SCOPE" = frontend ]; then
            DOCKER_BUILDKIT=1 docker build -f deploy/Dockerfile.frontend -t "$FRONTEND_IMAGE" .
          fi
        '''
      }
    }
    stage('Database and permissions integration') {
      when { expression { params.ACTION!='ROLLBACK' && params.SCOPE!='frontend' } }
      steps { sh 'bash scripts/ci-integration.sh' }
    }
    stage('Push images') {
      when { expression { params.ACTION=='DEPLOY' } }
      steps {
        withCredentials([usernamePassword(credentialsId: env.REGISTRY_CREDENTIAL, usernameVariable:'REG_USER',passwordVariable:'REG_PASS')]) {
          sh '''
            set -eu
            export DOCKER_CONFIG="$(mktemp -d)"
            trap 'rm -rf "$DOCKER_CONFIG"' EXIT
            printf '%s' "$REG_PASS" | docker login "$REGISTRY_HOST" --username "$REG_USER" --password-stdin
            if [ "$SCOPE" = all ] || [ "$SCOPE" = backend ]; then docker push "$BACKEND_IMAGE"; fi
            if [ "$SCOPE" = all ] || [ "$SCOPE" = frontend ]; then docker push "$FRONTEND_IMAGE"; fi
          '''
        }
      }
    }
    stage('Deploy or rollback') {
      when { expression { params.ACTION!='BUILD_ONLY' } }
      steps {
        sshagent(credentials: [env.SSH_CREDENTIAL]) {
          withCredentials([file(credentialsId:env.KNOWN_HOSTS_CREDENTIAL,variable:'KNOWN_HOSTS')]) {
            sh '''
              set -eu
              if [ "$ACTION" = DEPLOY ]; then
                tar -czf release.tar.gz deploy database UPSTREAM.json
                ssh -p "$REMOTE_PORT" -o StrictHostKeyChecking=yes -o UserKnownHostsFile="$KNOWN_HOSTS" "$REMOTE_USER@$REMOTE_HOST" "mkdir -p '$REMOTE_DIR/releases/$RELEASE_VERSION'"
                scp -P "$REMOTE_PORT" -o StrictHostKeyChecking=yes -o UserKnownHostsFile="$KNOWN_HOSTS" release.tar.gz "$REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR/releases/$RELEASE_VERSION/release.tar.gz"
                ssh -p "$REMOTE_PORT" -o StrictHostKeyChecking=yes -o UserKnownHostsFile="$KNOWN_HOSTS" "$REMOTE_USER@$REMOTE_HOST" "cd '$REMOTE_DIR/releases/$RELEASE_VERSION' && tar -xzf release.tar.gz && bash deploy/deploy.sh '$REMOTE_DIR' '$RELEASE_VERSION' '$BACKEND_IMAGE' '$FRONTEND_IMAGE' '$SCOPE'"
              else
                scp -P "$REMOTE_PORT" -o StrictHostKeyChecking=yes -o UserKnownHostsFile="$KNOWN_HOSTS" deploy/rollback.sh "$REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR/rollback.sh"
                ssh -p "$REMOTE_PORT" -o StrictHostKeyChecking=yes -o UserKnownHostsFile="$KNOWN_HOSTS" "$REMOTE_USER@$REMOTE_HOST" "bash '$REMOTE_DIR/rollback.sh' '$REMOTE_DIR' '$ROLLBACK_VERSION'"
              fi
            '''
          }
        }
      }
    }
  }
  post {
    always {
      archiveArtifacts artifacts: 'UPSTREAM.json', fingerprint: true
      junit testResults: 'backend/**/target/surefire-reports/TEST-*.xml', allowEmptyResults: true
    }
    success { echo "任务完成：${params.ACTION}，构建版本 ${env.RELEASE_VERSION}" }
  }
}
