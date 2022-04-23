:: 設定ファイルを読み込む
for /f "usebackq tokens=1,* delims==" %%a in ("env") do (
    set %%a=%%b
)

rem ELASTIC_APM_AGENT_PATH=
rem ELASTIC_APM_AGENT_VERSION=
rem FUNDANALYZER_VERSION=

java^
    -javaagent:%ELASTIC_APM_AGENT_PATH%\elastic-apm-agent-%ELASTIC_APM_AGENT_VERSION%.jar  ^
    -Delastic.apm.service_name=fundanalyzer  ^
    -Delastic.apm.server_urls=http://localhost:8200  ^
    -Delastic.apm.secret_token= ^
    -Delastic.apm.environment=production  ^
    -Delastic.apm.application_packages=com.github.ioridazo  ^
    -Xms2G  ^
    -Xmx2G  ^
    -Duser.timezone=Asia/Tokyo  ^
    -jar fundanalyzer-%FUNDANALYZER_VERSION%.jar  ^
    --spring.profiles.active=prod
