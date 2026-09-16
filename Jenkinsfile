pipeline {

    agent any

    options {
        disableConcurrentBuilds()
        timeout(time: 1, unit: 'HOURS')
        skipDefaultCheckout(true)
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
                bat '''
                    echo ===== WORKSPACE DEBUG =====
                    cd

                    echo.
                    echo ===== DIRECTORY =====
                    dir

                    echo.
                    echo ===== POM FILES =====
                    dir /s /b pom.xml || echo No pom.xml found

                    echo.
                    echo ===== MAVEN WRAPPER =====
                    if exist mvnw (
                        echo Maven Wrapper found
                    ) else (
                        echo Maven Wrapper not found - using system Maven
                    )

                    echo.
                    echo ===== JAVA VERSION =====
                    java -version

                    echo.
                    echo ===== MAVEN VERSION =====
                    mvn -version

                    echo.
                    echo ===== DEBUG COMPLETE =====
                    exit /b 0
                '''
            }
        }

        stage('Build & Test') {
            steps {
                bat '''
                    echo ===== BUILD & TEST =====

                    mvn --version

                    mvn clean verify -Deureka.client.enabled=false -Dspring.cloud.discovery.enabled=false
                '''
            }
        }

        stage('Verify JaCoCo Report') {
            steps {
                bat '''
                    echo ===== VERIFYING JACOCO =====

                    if exist target\\site\\jacoco\\jacoco.xml (
                        echo JaCoCo report found.
                    ) else (
                        echo JaCoCo report missing!
                        dir /s /b target\\*.xml
                        exit /b 1
                    )
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
                        string(
                            credentialsId: 'sonartk',
                            variable: 'SONAR_TOKEN'
                        )
                    ]) {

                        bat '''
                            echo ===== SONARQUBE ANALYSIS =====

                            mvn org.sonarsource.scanner.maven:sonar-maven-plugin:5.2.0.4988:sonar -DskipTests -Dsonar.projectKey=%SONAR_PROJECT_KEY% -Dsonar.projectName=%SONAR_PROJECT_NAME% -Dsonar.host.url=%SONAR_HOST_URL% -Dsonar.token=%SONAR_TOKEN% -Dsonar.java.binaries=target/classes -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
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

                catchError(
                    buildResult: 'SUCCESS',
                    stageResult: 'UNSTABLE'
                ) {

                    withCredentials([
                        string(
                            credentialsId: 'nvd-api-key',
                            variable: 'NVD_KEY'
                        )
                    ]) {

                        bat '''
                            echo ===== OWASP DEPENDENCY CHECK =====
                            echo Running OWASP Dependency Check...
                        '''

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
        }

        stage('Publish OWASP Report') {
            steps {

                catchError(
                    buildResult: 'SUCCESS',
                    stageResult: 'UNSTABLE'
                ) {

                    dependencyCheckPublisher(
                        pattern: 'dependency-check-report.csv'
                    )
                }
            }
        }

        stage('Archive Reports') {
            steps {
                archiveArtifacts(
                    artifacts: 'dependency-check-report.csv,target/site/jacoco/**,target/surefire-reports/**',
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
            echo 'UNSTABLE: OWASP Dependency Check encountered an issue, but the build completed.'
        }

        failure {
            echo 'FAILED: Check Jenkins console output.'
        }

        always {
            cleanWs()
            echo 'Pipeline execution finished.'
        }
    }
}
