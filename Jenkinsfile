
pipeline {
    agent any

    tools {
        maven 'Maven'
    }

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
            options {
                timeout(time: 7, unit: 'MINUTES')
            }
            steps {
                script {
                    def branchName = env.BRANCH_NAME ?:
                        (scm.branches?.getAt(0)?.name
                            ?.replaceFirst(/^\*\//, '') ?: 'main')

                    echo "Checkout started: ${new Date()}"
                    echo "Target branch: ${branchName}"

                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${branchName}"]],
                        doGenerateSubmoduleConfigurations: false,
                        extensions: [
                            [$class: 'CloneOption',
                             shallow: true,
                             depth: 1,
                             noTags: true,
                             honorRefspec: true,
                             timeout: 5]
                        ],
                        userRemoteConfigs: [[
                            credentialsId: 'githubtoken',
                            url: 'https://github.com/KodemiDevTeam/Assessment_Service.git',
                            refspec: "+refs/heads/${branchName}:refs/remotes/origin/${branchName}"
                        ]]
                    ])

                    echo "Checkout completed: ${new Date()}"
                }
            }
        }

        stage('Debug Workspace') {
            steps {
                sh '''
                    set -eu

                    echo "===== WORKSPACE DEBUG ====="
                    pwd
                    ls -la

                    echo "===== POM FILES ====="
                    find . -name "pom.xml" -print

                    echo "===== JAVA VERSION ====="
                    java -version

                    echo "===== MAVEN VERSION ====="
                    mvn -version

                    echo "===== GIT COMMIT ====="
                    git log -1 --oneline

                    echo "===== ROOT POM CHECK ====="
                    test -f pom.xml || {
                        echo "ERROR: Root pom.xml not found"
                        exit 1
                    }
                '''
            }
        }

        stage('Build and Test') {
            options {
                timeout(time: 25, unit: 'MINUTES')
            }
            steps {
                sh '''
                    set -eu

                    echo "===== BUILD AND TEST ====="
                    date

                    mvn -B -ntp clean verify \
                        -Deureka.client.enabled=false \
                        -Dspring.cloud.discovery.enabled=false

                    echo "===== BUILD COMPLETED ====="
                    date
                '''
            }
        }

        stage('Verify JaCoCo Report') {
            steps {
                sh '''
                    set -eu

                    echo "===== VERIFYING JACOCO ====="

                    if [ -f target/site/jacoco/jacoco.xml ]; then
                        echo "JaCoCo report found."
                        ls -lh target/site/jacoco/jacoco.xml
                    else
                        echo "JaCoCo report missing!"
                        find target -name "*.xml" -print 2>/dev/null || true
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
                            odcInstallation: 'OWASP',
                            additionalArguments: """
                                --nvdApiKey ${NVD_KEY}
                                --format ALL
                                --out .
                                --disableOssIndex
                            """.stripIndent().trim()
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
                    script {
                        if (fileExists('dependency-check-report.xml')) {
                            dependencyCheckPublisher(
                                pattern: 'dependency-check-report.xml'
                            )
                        } else {
                            error('OWASP XML report not found.')
                        }
                    }
                }
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
                        sh '''
                            set -eu

                            echo "===== SONARQUBE ANALYSIS ====="

                            mvn -B -ntp \
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
            echo 'UNSTABLE: Check OWASP Dependency Check and report publishing.'
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
