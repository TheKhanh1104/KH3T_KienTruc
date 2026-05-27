pipeline {
    agent any

    tools {
        maven 'Maven3'
        jdk 'JDK21'
        nodejs 'NodeJS20'
    }

    environment {
        DOCKER_REGISTRY = 'your-registry.com'
        APP_NAME = 'kh3t-shop'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Backend') {
            steps {
                dir('kh3tshop-be') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Build Frontend') {
            steps {
                dir('kh3tshop-be/src/main/resources/static/kh3tshop-fe') {
                    sh 'npm install'
                    sh 'npm run build'
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                sh 'docker-compose build'
                // sh 'docker login -u $DOCKER_USER -p $DOCKER_PASS'
                // sh 'docker-compose push'
            }
        }

        stage('Deploy') {
            steps {
                sh 'docker-compose up -d'
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
