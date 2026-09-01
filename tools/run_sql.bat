@echo off
rem ============================================================
rem DM8 SQL Runner - Windows batch launcher
rem
rem Usage:
rem   1) run a SQL file on local db (default):
rem        run_sql.bat <sql-file-path>
rem   2) run a SQL file on 51 db:
rem        run_sql.bat <sql-file-path> -db=51
rem   3) no argument: enter interactive mode (type SQL line by line)
rem ============================================================

set "DM_DRIVER=C:\Users\8823\.m2\repository\com\dameng\DmJdbcDriver18\8.1.3.140\DmJdbcDriver18-8.1.3.140.jar"

if not exist "%DM_DRIVER%" (
    echo [ERROR] DM driver not found: %DM_DRIVER%
    echo         Please edit DM_DRIVER at the top of this file.
    goto :eof
)

rem switch to the folder containing this script (tools folder)
cd /d "%~dp0"

rem compile (ignore errors if already compiled)
javac -encoding UTF-8 -cp "%DM_DRIVER%" DmSqlRunner.java 2>nul

rem run
java -cp ".;%DM_DRIVER%" DmSqlRunner %*

pause
