@echo off
set ROOT=%~dp0\..
cd /d "%ROOT%"
call scripts\build.bat
java -cp build\hhuop-fair2026.jar org.hhuop.experiment.SelfTestMain "%ROOT%"
java -cp build\hhuop-fair2026.jar org.hhuop.experiment.ExperimentMain --db datasets/example/database.txt --profits datasets/example/profits.txt --alpha 0.4 --beta 0.78 --sensitiveFile datasets/example/sensitive.txt --epsilon 0.001 --borderEta 0.05 --out results/example_results.csv --actionDir results/example_actions
