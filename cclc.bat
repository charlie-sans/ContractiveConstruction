@echo off
REM CCL Compiler Launcher Batch File
REM This batch file sets up the environment and launches the CCL compiler.
REM Usage: cclc [--lang <language>] [--debug] <source file>
REM launch the jar file with the appropriate Java command.
setlocal
set JAVA_CMD=java
set CCL_JAR_PATH=D:\ContractiveConstruction\ccl\target\ccl-1.0-SNAPSHOT-jar-with-dependencies.jar
%JAVA_CMD% -jar "%CCL_JAR_PATH%" %*
endlocal