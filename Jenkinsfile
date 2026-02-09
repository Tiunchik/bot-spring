pipeline {
    agent any

    environment {
        DEPLOY_USER = 'BOT_USER'
        DEPLOY_HOST = 'BOT_SERVER'
        CREDENTIALS_ID = 'BOT_SERVER_CREDS'
        PROD_TOKEN = 'BOT_TOKEN'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'release', url: 'https://github.com/Tiunchik/bot-spring.git'
            }
        }
        stage('Make gradlew executable') {
            steps {
                sh 'chmod +x gradlew'
            }
        }
        stage('Build with Gradle') {
            steps {
                sh './gradlew clean build -x=test'
            }
        }
        stage('Deploy via SFTP') {
            steps {
                script {
                    def remote = [
                        name       : 'target-server',
                        host       : getSecretText(env.DEPLOY_HOST),
                        user       : getSecretText(env.DEPLOY_USER),
                        password   : getPassword(env.CREDENTIALS_ID),
                        allowAnyHosts: true
                    ]

                    sshCommand remote: remote, command: '''
                        mkdir -p /root/build/libs
                        mkdir -p /root/docker
                    '''
                    // Копируем Dockerfile и docker-compose.yml
                    sshPut remote: remote, from: 'Dockerfile', into: '/root'
                    sshPut remote: remote, from: 'docker/docker-compose.yml', into: '/root/docker'

                    // Копируем jar
                    def jarFile = sh(
                        script: 'find build/libs -name "*.jar" -type f | head -n 1',
                        returnStdout: true
                    ).trim()

                    if (jarFile.isEmpty()) {
                        error "Не найдено ни одного .jar файла в build/libs/"
                    }

                    sshPut remote: remote, from: jarFile, into: '/root/build/libs'
                }
            }
        }
        stage('Create .env file on remote server') {
            steps {
                script {
                    def remote = [
                        name       : 'target-server',
                        host       : getSecretText(env.DEPLOY_HOST),
                        user       : getSecretText(env.DEPLOY_USER),
                        password   : getPassword(env.CREDENTIALS_ID),
                        allowAnyHosts: true
                    ]

                    sshCommand remote: remote, command: """
rm -f /root/docker/.env
cat > /root/docker/.env <<EOF
SPRING_APPLICATION_PROFILE=prod
BOT_TOKEN=${getSecretText(env.PROD_TOKEN)}
EOF
                    """
                }
            }
        }
        stage('Run Docker Compose on Remote Server') {
            steps {
                script {
                    def remote = [
                        name       : 'target-server',
                        host       : getSecretText(env.DEPLOY_HOST),
                        user       : getSecretText(env.DEPLOY_USER),
                        password   : getPassword(env.CREDENTIALS_ID),
                        allowAnyHosts: true
                    ]

                    sshCommand remote: remote, command: """
                        cd /root/docker &&
                        docker-compose up --build -d
                    """
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}

// Функция для получения пароля из Credentials
def getPassword(String credentialsId) {
    withCredentials([usernamePassword(credentialsId: credentialsId, usernameVariable: 'USER', passwordVariable: 'PASS')]) {
        return env.PASS
    }
}

def getSecretText(String credentialsId) {
    withCredentials([string(credentialsId: credentialsId, variable: 'VALUE')]) {
        return env.VALUE
    }
}