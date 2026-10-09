@echo off
echo.
echo [��Ϣ] ʹ��Jar��������Web���̡�
echo.

cd %~dp0
cd ../xinke-admin/target

set JAVA_OPTS=-Xms256m -Xmx768m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=384m -XX:+HeapDumpOnOutOfMemoryError

java -jar %JAVA_OPTS% xinke-admin.jar

cd bin
pause
