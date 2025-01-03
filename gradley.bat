@echo off
REM Set JAVA_HOME to JDK 8 installation path
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_202
set PATH=%JAVA_HOME%\bin;%PATH%

REM Set default version
set VERSION=
set ARG=%1

REM Check if a version argument is provided
if "%1"=="build" (
    if not "%2"=="" (
        set VERSION=%2
        shift
        shift
    ) else (
        shift
    )
)

REM Update mcmod.info with the new version if provided
if not "%VERSION%"=="" (
    powershell -Command "(Get-Content src/main/resources/mcmod.info) -replace '\"version\": \".*?\"', '\"version\": \"%VERSION%\"' | Set-Content src/main/resources/mcmod.info"
powershell -Command Set-Content -Path gradle.properties -Value 'mod_version=%VERSION%'

)

REM Run the gradlew command with the appropriate version
powershell -Command "Write-Output" '%ARG%'"
.\gradlew %ARG%

REM Reset build.gradle and mcmod.info to their original state (optional)
REM powershell -Command "(Get-Content build.gradle.bak) | Set-Content build.gradle"
REM powershell -Command "(Get-Content mcmod.info.bak) | Set-Content src/main/resources/mcmod.info"
