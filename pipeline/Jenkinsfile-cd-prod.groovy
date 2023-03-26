def datetime = new Date().format('yyyyMMddHHmmss')

pipeline {
    agent any

    stages {
        stage('Download') {
            steps {
                print "===== Nexusからリリース対象モジュールをダウンロードします ====="
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    env.URL = 'http://localhost:8081' +
                            '/service/rest/v1/search/assets/download' +
                            '?sort=version' +
                            '&repository=maven-releases' +
                            '&maven.groupId=' + pom.groupId +
                            '&maven.artifactId=' + pom.artifactId +
                            '&maven.baseVersion=' + pom.version
                    '&maven.extension=jar'
                    env.TMP = 'C:\\fundanalyzer\\bin\\tmp\\' + datetime + '\\'
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
                    sc.exe stop fundanalyzer
                    @echo off
                    :DoWhile
                      sc.exe query fundanalyzer | findstr STATE | findstr STOPPED
                      if %errorlevel% equ 0 goto DoWhileExit
                    goto DoWhile
                    :DoWhileExit
                    '''
                    echo "*********************"
                    echo "サービスを停止しました。"
                    echo "*********************"
                    bat "sc.exe query fundanalyzer"

                    env.DATETIME = datetime
                    env.JAR = jar
                    bat '''
                    cd C:\\fundanalyzer\\bin
                    ren fundanalyzer.jar fundanalyzer.jar.%DATETIME%
                    copy C:\\fundanalyzer\\bin\\tmp\\%DATETIME%\\%JAR% fundanalyzer.jar
                    '''
                    echo "*********************"
                    echo "JARファイルを差し替えました。"
                    echo "*********************"

                    bat '''
                    sc.exe start fundanalyzer
                    @echo off
                    :DoWhile
                      sc.exe query fundanalyzer | findstr STATE | findstr RUNNING
                      if %errorlevel% equ 0 goto DoWhileExit
                    goto DoWhile
                    :DoWhileExit
                    '''
                    echo "*********************"
                    echo "サービスを起動しました。"
                    echo "*********************"
                    bat "sc.exe query fundanalyzer"
                }
                print "===== デプロイを正常に終了しました ====="
            }
        }

        stage('Clean') {
            steps {
                script {
                    timeout(time: 60, unit: "MINUTES") {
                        RELEASE_SCOPE = input message: 'rollback or complete ?', ok: 'Submit!', parameters: [
                                choice(
                                        name: 'RELEASE_SCOPE',
                                        choices: 'rollback\ncomplete'
                                )
                        ]
                    }

                    switch (RELEASE_SCOPE) {
                        case "rollback":
                            print "===== ロールバックを開始します ====="
                            bat '''
                            sc.exe stop fundanalyzer
                            @echo off
                            :DoWhile
                              sc.exe query fundanalyzer | findstr STATE | findstr STOPPED
                              if %errorlevel% equ 0 goto DoWhileExit
                            goto DoWhile
                            :DoWhileExit
                            '''
                            echo "*********************"
                            echo "サービスを停止しました。"
                            echo "*********************"
                            bat "sc.exe query fundanalyzer"

                            env.DATETIME = datetime
                            bat '''
                            cd C:\\fundanalyzer\\bin
                            del fundanalyzer.jar
                            @echo off
                            :DoWhile
                              if not exist fundanalyzer.jar goto DoWhileExit
                            goto DoWhile
                            :DoWhileExit
                            @echo on
                            ren fundanalyzer.jar.%DATETIME% fundanalyzer.jar
                            '''
                            echo "*********************"
                            echo "JARファイルを元に戻しました。"
                            echo "*********************"

                            bat '''
                            sc.exe start fundanalyzer
                            @echo off
                            :DoWhile
                              sc.exe query fundanalyzer | findstr STATE | findstr RUNNING
                              if %errorlevel% equ 0 goto DoWhileExit
                            goto DoWhile
                            :DoWhileExit
                            '''
                            echo "*********************"
                            echo "サービスを起動しました。"
                            echo "*********************"
                            bat "sc.exe query fundanalyzer"

                            print "===== ロールバックを正常に終了しました ====="
                            break

                        case "complete":
                            print "===== コンプリートを開始します ====="

                            env.DATETIME = datetime
                            bat '''
                            cd C:\\fundanalyzer\\bin
                            del fundanalyzer.jar.%DATETIME%
                            '''

                            print "===== コンプリートを正常に終了しました ====="
                            break
                    }
                }
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
