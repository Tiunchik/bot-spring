pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'bot-spring'
        GIT_REPO = 'https://github.com/Tiunchik/bot-spring.git'
        DOCKER_TAG = "${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
    }

    stages {
        stage('Клонирование репозитория') {
            steps {
                git branch: 'master',
                    url: "${GIT_REPO}"
            }
        }

        stage('Установка зависимостей и сборка') {
            steps {
                // Установка необходимых инструментов
                sh 'apt-get update'
                sh 'apt-get install -y openjdk-11-jdk'

                // Сборка проекта с помощью Gradle
                sh 'gradle build'
            }
        }

        stage('Тестирование') {
            steps {
                // Запуск тестов
                sh 'gradle test'
            }
        }

        stage('Создание Docker образа') {
            steps {
                // Проверка установки Docker
                sh 'which docker || (sudo apt-get update && sudo apt-get install -y docker.io)'

                // Создание и сборка Docker образа
                sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
            }
        }

        stage('Сохранение образа в локальном Docker') {
            steps {
                sh "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} localhost:5000/${DOCKER_IMAGE}:${DOCKER_TAG}"
                sh "docker push localhost:5000/${DOCKER_IMAGE}:${DOCKER_TAG}"
            }
        }
    }

    post {
        always {
            // Очистка рабочих директорий
            deleteDir()
        }

        failure {
            // Отправка уведомления при неудаче
            echo 'Всё фигово'
        }
    }
}