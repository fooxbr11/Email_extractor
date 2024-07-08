@echo off
cd /d %~dp0
echo Running EmailAttachmentDaemon...

REM Verifica se os arquivos de log e o arquivo de data existem
if not exist logs\script\log.txt (
    echo Criando diretório e arquivo de log...
    mkdir logs\script
    echo [%date% %time%] Log iniciado. >> logs\script\log.txt
)

if not exist logs\script\last_clean.txt (
    echo Criando arquivo de última limpeza...
    echo %date% > logs\script\last_clean.txt
)

REM Lê a última data de limpeza
set /p last_clean=<logs\script\last_clean.txt

REM Calcula a diferença de dias
for /f "tokens=1-3 delims=/" %%a in ("%date%") do set current_date=%%a/%%b/%%c
for /f "tokens=1-3 delims=/" %%a in ("%last_clean%") do set clean_date=%%a/%%b/%%c

REM Converte as datas para dias julianos
call :dateToJulian %current_date% current_julian
call :dateToJulian %clean_date% clean_julian

set /a diff=current_julian-clean_julian

REM Limpa os arquivos de log se passaram mais de 10 dias
if %diff% gtr 10 (
    echo [%date% %time%] Limpando arquivos de log... >> logs\script\log.txt
    del logs\script\log.txt
    echo [%date% %time%] Log iniciado. >> logs\script\log.txt
    echo %date% > logs\script\last_clean.txt
    
    if exist logs\EmailAttachmentSaver.txt (
        del logs\EmailAttachmentSaver.txt
        echo [%date% %time%] Log EmailAttachmentSaver.txt limpo. >> logs\script\log.txt
    )
    
    if exist logs\EmailAttachmentDownloader.txt (
        del logs\EmailAttachmentDownloader.txt
        echo [%date% %time%] Log EmailAttachmentDownloader.txt limpo. >> logs\script\log.txt
    )
)

:loop

REM Verifica se o script foi chamado pelo serviço ou manualmente
if "%SERVICE_MODE%"=="true" (
    echo [%date% %time%] Rodando em modo de serviço... >> logs\script\log.txt
    REM Executa o arquivo JAR com o argumento 'service'
    java -jar Java\EmailAttachmentSaverConfig-1.0-jar-with-dependencies.jar service
) else (
    echo [%date% %time%] Rodando em modo manual... >> logs\script\log.txt
    REM Executa o arquivo JAR sem argumento
    java -jar Java\EmailAttachmentSaverConfig-1.0-jar-with-dependencies.jar
)

if %ERRORLEVEL% neq 0 (
    echo [%date% %time%] Error: Falha ao executar arquivo JAR. >> logs\script\log.txt
    exit /b %ERRORLEVEL%
)

echo [%date% %time%] Aguardando 1 minuto para rodar novamente... >> logs\script\log.txt
timeout /t 60

goto loop

pause

:dateToJulian
REM Converte uma data no formato dd/mm/yyyy para dias julianos
setlocal
set day=%1
set month=%2
set year=%3
set /a month=(month-14)/12
set /a julian=day-32075+1461*(year+4800+month)/4+367*(month-2-(month*12))/12-3*((year+4900+month)/100)/4
endlocal & set %4=%julian%
goto :eof
