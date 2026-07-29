pipeline {

    agent any

    options {
        disableConcurrentBuilds()
        timeout(time: 1, unit: 'HOURS')
    }

    environment {
        SONAR_PROJECT_KEY  = "Assessment_Service_Dev"
        SONAR_PROJECT_NAME = "Assessment_Service_Dev"
    }

    stages {

        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Debug Workspace') {
            steps {
                sh '''
                echo "===== WORKSPACE DEBUG ====="
                pwd
                ls -la
                find . -name pom.xml
                '''
            }
        }

        stage('Build & Test') {
            steps {
                sh '''
                chmod +x mvnw

                ./mvnw clean verify \
                -Deureka.client.enabled=false \
                -Dspring.cloud.discovery.enabled=false
                '''
            }
        }

        stage('Verify JaCoCo Report') {
            steps {
                sh '''
                echo "===== VERIFYING JACOCO ====="

                if [ -f target/site/jacoco/jacoco.xml ]; then
                    echo "JaCoCo report found"
                else
                    echo "JaCoCo report missing"
                    exit 1
                fi
                '''
            }
        }

        stage('Publish Test Results') {
            steps {
                junit allowEmptyResults: true,
                      testResults: '**/target/surefire-reports/*.xml'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarscanner') {

                    withCredentials([
                        string(credentialsId: 'sonartk', variable: 'SONAR_TOKEN')
                    ]) {

                        sh '''
                        echo "===== SONARQUBE ANALYSIS ====="

                        chmod +x mvnw

                        ./mvnw \
                        org.sonarsource.scanner.maven:sonar-maven-plugin:5.2.0.4988:sonar \
                        -DskipTests \
                        -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                        -Dsonar.projectName=${SONAR_PROJECT_NAME} \
                        -Dsonar.host.url=$SONAR_HOST_URL \
                        -Dsonar.token=$SONAR_TOKEN \
                        -Dsonar.java.binaries=target/classes \
                        -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
                }
            }
        }

        stage('OWASP Dependency Check') {
            steps {
                withCredentials([
                    string(credentialsId: 'nvd-api-key', variable: 'NVD_KEY')
                ]) {

                    dependencyCheck(
                        odcInstallation: 'Default',
                        additionalArguments: """
                        --nvdApiKey ${NVD_KEY}
                        --format CSV
                        --out .
                        --disableOssIndex
                        """
                    )
                }
            }
        }

        stage('Publish OWASP Report') {
            steps {
                dependencyCheckPublisher(
                    pattern: 'dependency-check-report.csv'
                )
            }
        }

        stage('Archive Reports') {
            steps {
                archiveArtifacts(
                    artifacts: '''
                    dependency-check-report.csv,
                    target/site/jacoco/**,
                    target/surefire-reports/**
                    ''',
                    fingerprint: true,
                    allowEmptyArchive: true
                )
            }
        }
    }

    post {

        success {
            echo 'SUCCESS: Assessment Service Build + Sonar + OWASP completed successfully.'
        }

        unstable {
            echo 'UNSTABLE: Quality Gate failed.'
        }

        failure {
            echo 'FAILED: Check pipeline logs.'
        }

        always {
            cleanWs()
            echo 'Pipeline execution finished.'
        }
    }
}
