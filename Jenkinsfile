pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                echo 'Building the application...'
                // Example: sh 'mvn clean package'
            }
        }
        
        stage('Test') {
            steps {
                echo 'Running tests...'
                // Example: sh 'mvn test'
            }
        }
        
        stage('Deploy') {
            steps {
                echo 'Deploying to production...'
                // Example: sh 'docker build -t my-app .'
            }
        }
    }
    
    post {
        always {
            echo 'Cleaning up workspace...'
            cleanWs()
        }
    }
}