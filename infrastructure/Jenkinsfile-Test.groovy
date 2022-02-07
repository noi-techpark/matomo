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
