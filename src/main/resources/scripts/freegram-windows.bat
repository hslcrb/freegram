@echo off
REM FreeGram Launcher for Windows
REM Requires Java 17+ and JavaFX 17+
setlocal
set SCRIPT_DIR=%~dp0
set JAR=%SCRIPT_DIR%freegram-1.0.0.jar

if defined JAVAFX_PATH (
    java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.web,javafx.swing -jar "%JAR%" %*
) else if exist "C:\Program Files\Java\javafx-sdk-17\lib" (
    java --module-path "C:\Program Files\Java\javafx-sdk-17\lib" --add-modules javafx.controls,javafx.web,javafx.swing -jar "%JAR%" %*
) else (
    echo Warning: JavaFX path not found. Set JAVAFX_PATH or install JavaFX SDK.
    java -jar "%JAR%" %*
)
pause
