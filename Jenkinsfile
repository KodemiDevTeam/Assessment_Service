pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timeout(time: 1, unit: 'HOURS')
        skipDefaultCheckout(true)
    }

    environment {
        SONAR_PROJECT_KEY  = 'Assessment_Service_Dev'
        SONAR_PROJECT_NAME = 'Assessment_Service_Dev'
    }

    stages {

        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: scm.branches,
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [
                        [$class: 'CloneOption',
                         shallow: true,
                         depth: 1,
                         noTags: true,
                         honorRefspec: true,
                         timeout: 20]
                    ],
                    userRemoteConfigs: [[
                        credentialsId: 'githubtoken',
                        url: 'https://github.com/KodemiDevTeam/Assessment_Service.git',
                        refspec: '+refs/heads/*:refs/remotes/origin/*'
                    ]]
                ])
            }
        }

        stage('Debug Workspace') {
            steps {
                sh '''
                    echo "===== WORKSPACE DEBUG ====="
                    pwd

                    echo
                    echo "===== DIRECTORY ====="
                    ls -la

                    echo
                    echo "===== POM FILES ====="
                    find . -name "pom.xml"

                    echo
                    echo "===== JAVA VERSION ====="
                    java -version

                    echo
                    echo "===== MAVEN VERSION ====="
                    mvn -version
                '''
            }
        }

        stage('Build and Test') {
            steps {
                sh '''
                    echo "===== BUILD AND TEST ====="

                    mvn clean verify \
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
                        echo "JaCoCo report found."
                        ls -lh target/site/jacoco/jacoco.xml
                    else
                        echo "JaCoCo report missing!"
                        find target -name "*.xml" 2>/dev/null || true
                        exit 1
                    fi
                '''
            }
        }

        stage('Publish Test Results') {
            steps {
                junit(
                    allowEmptyResults: true,
                    testResults: '**/target/surefire-reports/*.xml'
                )
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
                        echo '===== RUNNING OWASP DEPENDENCY CHECK ====='

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
                        pattern: 'dependency-check-report.xml'
                    )
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube2') {
                    withCredentials([
                        string(
                            credentialsId: 'sonar-token',
                            variable: 'SONAR_TOKEN'
                        )
                    ]) {
                        sh '''
                            echo "===== SONARQUBE ANALYSIS ====="

                            mvn \
                                org.sonarsource.scanner.maven:sonar-maven-plugin:5.2.0.4988:sonar \
                                -DskipTests \
                                -Dsonar.projectKey="${SONAR_PROJECT_KEY}" \
                                -Dsonar.projectName="${SONAR_PROJECT_NAME}" \
                                -Dsonar.host.url="${SONAR_HOST_URL}" \
                                -Dsonar.token="${SONAR_TOKEN}" \
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

        stage('Archive Reports') {
            steps {
                archiveArtifacts(
                    artifacts: 'dependency-check-report.csv,dependency-check-report.xml,target/site/jacoco/**,target/surefire-reports/**',
                    fingerprint: true,
                    allowEmptyArchive: true
                )
            }
        }
    }

    post {
        success {
            echo 'SUCCESS: Assessment Service Build + Tests + OWASP + SonarQube completed.'
        }

        unstable {
            echo 'UNSTABLE: Check OWASP Dependency Check and test results.'
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
