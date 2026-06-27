@echo off
setlocal

:: ê›íËÉtÉ@ÉCÉãÇì«Ç›çûÇﬁ
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
    -Xms1g  ^
    -Xmx1g  ^
    -XX:+UseG1GC  ^
    -XX:+UseStringDeduplication  ^
    -XX:MaxMetaspaceSize=256m  ^
    -XX:+HeapDumpOnOutOfMemoryError  ^
    -XX:HeapDumpPath=%APP_LOG_PATH%  ^
    -XX:+ExitOnOutOfMemoryError  ^
    -Xlog:gc*=info:file=%GC_LOG%:uptime,level,tags:filecount=5,filesize=10M ^
    -Duser.timezone=Asia/Tokyo  ^
    -jar %APP_NAME%.jar  ^
    --spring.profiles.active=prod  ^
    --logging.path=%APP_LOG_PATH%

endlocal