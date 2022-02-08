pipeline {
  agent any

  environment {
    AWS_ACCESS_KEY_ID = credentials('AWS_ACCESS_KEY_ID')
    AWS_SECRET_ACCESS_KEY = credentials('AWS_SECRET_ACCESS_KEY')
    DOCKER_PROJECT_NAME = "matomo"
    SERVER_PORT = "1042"
    
  }

  stages {
    stage('Configure') {
      steps {
        sh """
          rm -f .env
          echo 'COMPOSE_PROJECT_NAME=${DOCKER_PROJECT_NAME}' >> .env
          echo 'SERVER_PORT=${SERVER_PORT}' >> .env
          echo 'MYSQL_PASSWORD=' >> .env
          echo 'MYSQL_DATABASE=matomo' >> .env
          echo 'MYSQL_USER=matomo' >> .env
          echo 'MATOMO_DATABASE_ADAPTER=mysql' >> .env
          echo 'MATOMO_DATABASE_TABLES_PREFIX=matomo_' >> .env
          echo 'MATOMO_DATABASE_USERNAME=matomo' >> .env
          echo 'MATOMO_DATABASE_PASSWORD=' >> .env
          echo 'MATOMO_DATABASE_DBNAME=matomo' >> .env
        """
      }
    }
    stage('Deploy') {
      steps {
        sshagent(['jenkins-ssh-key']) {
          sh """
            (cd infrastructure/ansible && ansible-galaxy install -f -r requirements.yml)
            (cd infrastructure/ansible && ansible-playbook --limit=test deploy.yml --extra-vars "release_name=${BUILD_NUMBER}")
          """
        }
      }
    }
  }
}
