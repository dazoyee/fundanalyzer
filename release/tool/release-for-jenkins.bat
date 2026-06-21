@echo off

rem 遅延環境変数
setlocal EnableDelayedExpansion

set LATEST_JENKINS_DIR="C:\ProgramData\Jenkins\.jenkins\workspace\fundanalyzer - production\target"
set LATEST_JENKINS_JAR="%LATEST_JENKINS_DIR%\fundanalyzer-*.jar"

rem 最新バージョンを取得
if exist "%LATEST_JENKINS_JAR%" (
  for %%f in ("%LATEST_JENKINS_JAR%") do (
  set JAR_NAME=%%~nf
  set VERSION=!JAR_NAME:~-5!
  )
) else (
  echo JenkinsのJARファイルを取得できませんでした。
  exit /b
)

set FUNDANALYZER_DIR=C:\fundanalyzer\bin
set LATEST_FUNDANALYZER_JAR="%FUNDANALYZER_DIR%\fundanalyzer-%VERSION%.jar"

rem JARファイルの差し替え
if not exist "%LATEST_FUNDANALYZER_JAR%" (
  copy "%LATEST_JENKINS_DIR%\fundanalyzer-%VERSION%.jar" %FUNDANALYZER_DIR%\
  echo 最新のJARファイルを%FUNDANALYZER_DIR%に配置しました。
) else (
  echo 最新のJARファイルがすでに存在しているため、処理を中断しました。
  exit /b
)

set ENV=%FUNDANALYZER_DIR%\env

rem バージョンの差し替え
ren %ENV% env_old
for /f "delims=" %%e in (%ENV%_old) do (
  set line=%%e
  set target=!line:~0,20!
  if !target! == FUNDANALYZER_VERSION (
    echo FUNDANALYZER_VERSION=%VERSION%>>%ENV%
  ) else (
    echo !line!>>%ENV%
  )
)
del %ENV%_old

set SERVICE_NAME=fundanalyzer

rem サービス再起動
sc.exe stop %SERVICE_NAME%
:DoWhile
  sc.exe query %SERVICE_NAME% | findstr STATE | findstr STOPPED
  if %errorlevel% equ 0 goto DoWhileExit
goto DoWhile
:DoWhileExit
sc.exe start %SERVICE_NAME%
