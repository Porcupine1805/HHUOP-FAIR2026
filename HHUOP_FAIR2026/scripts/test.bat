@echo off
call "%~dp0build.bat" || exit /b 1
java -cp "%~dp0..\build\hhuop-fair2026.jar" org.hhuop.experiment.SelfTestMain "%~dp0.." || exit /b 1
java -cp "%~dp0..\build\hhuop-fair2026.jar" org.hhuop.experiment.OraclePropertyTestMain 200 || exit /b 1
