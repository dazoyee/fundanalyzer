@echo off
setlocal

:: 設定ファイルを読み込む
for /f "usebackq tokens=1,* delims==" %%a in ("env") do (
    set %%a=%%b
)

rem ELASTIC_APM_AGENT_PATH=
rem ELASTIC_APM_AGENT_VERSION=

set time_tmp=%time: =0%
set now=%date:/=%%time_tmp:~0,2%%time_tmp:~3,2%%time_tmp:~6,2%

set APP_NAME=fundanalyzer
set APP_LOG_PATH=C:\fundanalyzer\logs
set GC_LOG=%APP_LOG_PATH%\gc_%now%.log

java^
    -javaagent:%ELASTIC_APM_AGENT_PATH%\elastic-apm-agent-%ELASTIC_APM_AGENT_VERSION%.jar  ^
    -Delastic.apm.service_name=%APP_NAME%  ^
    -Delastic.apm.server_urls=http://localhost:8200  ^
    -Delastic.apm.secret_token= ^
    -Delastic.apm.environment=production  ^
    -Delastic.apm.application_packages=com.github.ioridazo  ^
    -Xms256m  ^
    -Xmx256m  ^
    -Xlog:gc*=info:file=%GC_LOG% ^
    -XX:+HeapDumpOnOutOfMemoryError ^
    -Duser.timezone=Asia/Tokyo  ^
    -jar %APP_NAME%.jar  ^
    --spring.profiles.active=prod  ^
    --logging.path=%APP_LOG_PATH%

endlocal