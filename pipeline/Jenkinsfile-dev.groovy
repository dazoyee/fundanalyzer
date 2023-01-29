def datetime = new Date().format('yyyyMMddHHmmss')

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
                // git url: 'https://github.com/ioridazo/fundanalyzer.git', branch: 'develop'
                checkout scm
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
                        },
                        clover: {
                            clover(
                                    cloverReportDir: 'target/site',
                                    cloverReportFileName: 'clover.xml',
                                    // optional, default is: method=70, conditional=80, statement=80
                                    healthyTarget: [methodCoverage: 70, conditionalCoverage: 80, statementCoverage: 80],
                                    // optional, default is none
                                    unhealthyTarget: [methodCoverage: 50, conditionalCoverage: 50, statementCoverage: 50],
                                    // optional, default is none
                                    failingTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0]
                            )
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
                            repository: 'maven-snapshots',
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

        stage('Download') {
            steps {
                print "===== Nexusからリリース対象モジュールをダウンロードします ====="
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    env.URL = 'http://localhost:8081' +
                            '/service/rest/v1/search/assets/download' +
                            '?sort=version' +
                            '&repository=maven-snapshots' +
                            '&maven.groupId=' + pom.groupId +
                            '&maven.artifactId=' + pom.artifactId +
                            '&maven.baseVersion=' + pom.version
                    '&maven.extension=jar'
                    env.TMP = 'C:\\fundanalyzer-develop\\bin\\tmp\\' + datetime + '\\'
                    env.JAR = 'fundanalyzer-' + pom.version + '.jar'
                    bat '''
                    mkdir %TMP%
                    bitsadmin /transfer "nexus" "%URL%" %TMP%%JAR%
                    '''
                }
                print "===== リリース対象モジュールを正常にダウンロードしました ====="
            }
        }

        stage('Deploy') {
            steps {
                print "===== デプロイを開始します ====="
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    def jar = 'fundanalyzer-' + pom.version + '.jar'

                    bat '''
                    sc.exe stop fundanalyzer-develop
                    @echo off
                    :DoWhile
                      sc.exe query fundanalyzer-develop | findstr STATE | findstr STOPPED
                      if %errorlevel% equ 0 goto DoWhileExit
                    goto DoWhile
                    :DoWhileExit
                    '''
                    echo "*********************"
                    echo "サービスを停止しました。"
                    echo "*********************"
                    bat "sc.exe query fundanalyzer-develop"

                    env.DATETIME = datetime
                    env.JAR = jar
                    bat '''
                    cd C:\\fundanalyzer-develop\\bin
                    ren fundanalyzer.jar fundanalyzer.jar.%DATETIME%
                    copy C:\\fundanalyzer-develop\\bin\\tmp\\%DATETIME%\\%JAR% fundanalyzer.jar
                    '''
                    echo "*********************"
                    echo "JARファイルを差し替えました。"
                    echo "*********************"

                    bat '''
                    sc.exe start fundanalyzer-develop
                    @echo off
                    :DoWhile
                      sc.exe query fundanalyzer-develop | findstr STATE | findstr RUNNING
                      if %errorlevel% equ 0 goto DoWhileExit
                    goto DoWhile
                    :DoWhileExit
                    '''
                    echo "*********************"
                    echo "サービスを起動しました。"
                    echo "*********************"
                    bat "sc.exe query fundanalyzer-develop"
                }
                print "===== デプロイを正常に終了しました ====="
            }
        }

        stage('Clean') {
            steps {
                print "===== コンプリートを開始します ====="
                script {
                    env.DATETIME = datetime
                    bat '''
                    cd C:\\fundanalyzer-develop\\bin
                    del fundanalyzer.jar.%DATETIME%
                    '''
                }
                print "===== コンプリートを正常に終了しました ====="
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
