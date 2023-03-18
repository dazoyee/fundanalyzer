pipeline {
    agent any

    tools {
        jdk 'openjdk17'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))
    }

//    triggers {
//        pollSCM('H * * * *')
//    }

    stages {
        stage('Checkout') {
            steps {
                print "===== チェックアウトを開始します ====="
                bat 'git config --system core.longpaths true'
                git url: 'https://github.com/ioridazo/fundanalyzer.git', branch: 'main'
                // checkout scm
                print "===== チェックアウトを正常に終了しました ====="
            }
        }

        stage('Test') {
            steps {
                print "===== テストを開始します ====="
                bat "./mvnw clean"
//                bat "./mvnw clean clover:setup"
                bat "./mvnw test surefire-report:report pmd:pmd pmd:cpd jacoco:report spotbugs:spotbugs"
                print "===== テストを正常に終了しました ====="
            }
        }

        stage('Analytics') {
            steps {
                parallel(
                        junit: {
                            junit '**/target/surefire-reports/TEST-*.xml'
                        },
                        jacoco: {
                            jacoco(
                                    execPattern: 'target/*.exec',
                                    classPattern: 'target/classes',
                                    sourcePattern: 'src/main/java',
                                    exclusionPattern: 'src/test*'
                            )
                        },
                        pmd: {
                            recordIssues(tools: [pmdParser(pattern: 'target/pmd.xml')])
                        },
                        cpd: {
                            recordIssues(tools: [cpd(pattern: 'target/cpd.xml')])
                        },
                        spotBugs: {
                            recordIssues(tools: [spotBugs(pattern: 'target/spotbugsXml.xml', useRankAsPriority: true)])
//                        },
//                        clover: {
//                            clover(
//                                    cloverReportDir: 'target/site',
//                                    cloverReportFileName: 'clover.xml',
//                                    // optional, default is: method=70, conditional=80, statement=80
//                                    healthyTarget: [methodCoverage: 70, conditionalCoverage: 80, statementCoverage: 80],
//                                    // optional, default is none
//                                    unhealthyTarget: [methodCoverage: 50, conditionalCoverage: 50, statementCoverage: 50],
//                                    // optional, default is none
//                                    failingTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0]
//                            )
                        }
                )
            }
        }

        stage('Build') {
            steps {
                print "===== ビルドを開始します ====="
                bat "./mvnw package -DskipTests=true"
                archiveArtifacts artifacts: 'target/*.jar,target/*.zip', followSymlinks: false
                print "===== ビルドを正常に終了しました ====="
            }
        }

        stage('Upload') {
            steps {
                print "===== ビルド成果物をNexusにアップロードします ====="
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    nexusArtifactUploader(
                            nexusVersion: 'nexus3',
                            credentialsId: 'nexus3',
                            protocol: 'http',
                            nexusUrl: 'localhost:8081',
                            repository: 'maven-releases',
                            groupId: pom.groupId,
                            version: pom.version,
                            artifacts: [
                                    [
                                            artifactId: pom.artifactId,
                                            file      : 'target/fundanalyzer-' + pom.version + '.jar',
                                            type      : 'jar'
                                    ]
                            ]
                    )
                }
                print "===== ビルド成果物を正常にアップロードしました ====="
            }
        }
    }

    post {
        success {
            slackSend(
                    baseUrl: 'https://pj-share-knowledges.slack.com/services/hooks/jenkins/',
                    channel: 'UKNK4SZPX',
                    color: 'good',
                    message: "${env.JOB_NAME} - ${env.BUILD_NUMBER} SUCCESS\n${env.BUILD_URL}",
                    notifyCommitters: true,
                    tokenCredentialId: 'Slack Jenkins Bot'
            )
        }

        failure {
            slackSend(
                    baseUrl: 'https://pj-share-knowledges.slack.com/services/hooks/jenkins/',
                    channel: 'UKNK4SZPX',
                    color: 'danger',
                    message: "${env.JOB_NAME} - ${env.BUILD_NUMBER} FAILED\n${env.BUILD_URL}",
                    notifyCommitters: true,
                    tokenCredentialId: 'Slack Jenkins Bot'
            )
        }
    }
}
