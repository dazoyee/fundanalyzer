@echo off

rem �x�����ϐ�
setlocal EnableDelayedExpansion

set LATEST_JENKINS_DIR="C:\ProgramData\Jenkins\.jenkins\workspace\fundanalyzer - production\target"
set LATEST_JENKINS_JAR="%LATEST_JENKINS_DIR%\fundanalyzer-*.jar"

rem �ŐV�o�[�W�������擾
if exist "%LATEST_JENKINS_JAR%" (
  for %%f in ("%LATEST_JENKINS_JAR%") do (
  set JAR_NAME=%%~nf
  set VERSION=!JAR_NAME:~-5!
  )
) else (
  echo Jenkins��JAR�t�@�C�����擾�ł��܂���ł����B
  exit /b
)

set FUNDANALYZER_DIR=C:\fundanalyzer\bin
set LATEST_FUNDANALYZER_JAR="%FUNDANALYZER_DIR%\fundanalyzer-%VERSION%.jar"

rem JAR�t�@�C���̍����ւ�
if not exist "%LATEST_FUNDANALYZER_JAR%" (
  copy "%LATEST_JENKINS_DIR%\fundanalyzer-%VERSION%.jar" %FUNDANALYZER_DIR%\
  echo �ŐV��JAR�t�@�C����%FUNDANALYZER_DIR%�ɔz�u���܂����B
) else (
  echo �ŐV��JAR�t�@�C�������łɑ��݂��Ă��邽�߁A�����𒆒f���܂����B
  exit /b
)

set ENV=%FUNDANALYZER_DIR%\env

rem �o�[�W�����̍����ւ�
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

rem �T�[�r�X�ċN��
sc.exe stop %SERVICE_NAME%
:DoWhile
  sc.exe query %SERVICE_NAME% | findstr STATE | findstr STOPPED
  if %errorlevel% equ 0 goto DoWhileExit
goto DoWhile
:DoWhileExit
sc.exe start %SERVICE_NAME%
