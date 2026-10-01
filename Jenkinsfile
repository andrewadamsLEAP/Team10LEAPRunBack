pipeline {
    agent any

    environment {
        MAVEN_HOME = tool 'Maven3'
        JAVA_HOME = tool 'JDK17'
        PATH = "${MAVEN_HOME}/bin:${JAVA_HOME}/bin:${PATH}"
        PROJECT_VERSION = '0.0.3'
        DOCKER_REGISTRY = 'your-docker-registry'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                echo "✓ Building branch: ${env.BRANCH_NAME}"
                echo "✓ Build number: ${env.BUILD_NUMBER}"
                echo "✓ Commit: ${env.GIT_COMMIT}"
            }
        }

        stage('Setup') {
            steps {
                echo '📋 Verifying Java and Maven...'
                sh 'java -version'
                sh 'mvn --version'
            }
        }

        stage('Clean & Compile') {
            steps {
                echo '🧹 Cleaning and compiling project...'
                sh 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
                echo '🧪 Running unit and integration tests...'
                
                // Set test database credentials
                withEnv(['DB_USERNAME=sa', 'DB_PASSWORD=', 'SPRING_PROFILES_ACTIVE=test']) {
                    sh 'mvn test -Dtest.db.skip=false'
                }
            }

            post {
                always {
                    echo '📊 Generating test reports...'
                    
                    // Always publish test results, even if some fail
                    junit testResults: 'target/surefire-reports/*.xml', 
                          allowEmptyResults: false,
                          keepLongSTDIN: true
                    
                    // Archive test reports for debugging
                    archiveArtifacts artifacts: 'target/surefire-reports/**', 
                                     allowEmptyArchive: true
                }
                
                success {
                    echo '✅ All tests passed!'
                }
                
                failure {
                    echo '❌ Tests failed! Check archived test reports'
                    echo '📁 View test results: target/surefire-reports/'
                }
            }
        }

        stage('Build Package') {
            steps {
                echo '📦 Building JAR package...'
                sh 'mvn package -DskipTests'
            }
            
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: false
                    echo '✓ JAR archived successfully'
                }
            }
        }

        stage('Code Quality (Optional)') {
            when {
                branch 'main'
            }
            steps {
                echo '🔍 Analyzing code quality with SonarQube...'
                // Uncomment below when SonarQube is configured
                // sh 'mvn sonar:sonar -Dsonar.projectKey=trading-app -Dsonar.host.url=http://sonarqube:9000'
                echo '⚠️ SonarQube analysis skipped (not configured)'
            }
        }

        stage('Docker Build') {
            when {
                branch 'main'
            }
            steps {
                echo '🐳 Building Docker image...'
                sh 'docker build -t ${DOCKER_REGISTRY}/trading-app:${PROJECT_VERSION} .'
                sh 'docker build -t ${DOCKER_REGISTRY}/trading-app:latest .'
                echo '✓ Docker image built'
            }
        }

        stage('Docker Push') {
            when {
                branch 'main'
            }
            steps {
                echo '📤 Pushing Docker image to registry...'
                // Uncomment when Docker credentials are configured
                // sh 'docker login -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD} ${DOCKER_REGISTRY}'
                // sh 'docker push ${DOCKER_REGISTRY}/trading-app:${PROJECT_VERSION}'
                // sh 'docker push ${DOCKER_REGISTRY}/trading-app:latest'
                echo '⚠️ Docker push skipped (registry not configured)'
            }
        }

        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                echo '🚀 Deploying to production...'
                // Add your deployment steps here
                // Example: docker-compose up, kubectl apply, etc.
                echo '⚠️ Deployment skipped (environment not configured)'
            }
        }
    }

    post {
        always {
            echo '🧹 Cleaning up...'
            // Optional: Clean workspace if needed
            // cleanWs()
        }

        success {
            echo "✅ Build ${env.BUILD_NUMBER} PASSED on branch ${env.BRANCH_NAME}"
            // Optional: Send Slack notification
            // slackSend(color: 'good', message: "Build ${env.BUILD_NUMBER} passed!")
        }

        failure {
            echo "❌ Build ${env.BUILD_NUMBER} FAILED on branch ${env.BRANCH_NAME}"
            // Optional: Send Slack notification
            // slackSend(color: 'danger', message: "Build ${env.BUILD_NUMBER} failed!")
        }

        unstable {
            echo "⚠️ Build ${env.BUILD_NUMBER} UNSTABLE on branch ${env.BRANCH_NAME}"
        }
    }
}
