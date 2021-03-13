REM start.bat  code
REM bin配下にある任意の.jarを設定すること
java -Xms2G -Xmx4G -Duser.timezone=Asia/Tokyo -jar ..\fundanalyzer.jar^
    --spring.profiles.active=prod
