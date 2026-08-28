$ErrorActionPreference = "Stop"

$javaHome = "C:\Users\30056891\.copilot\session-state\486b37de-1952-46cf-9099-c821e144cf89\files\tools\jdk-17\jdk-17.0.20.1+1"

if (-not (Test-Path "$javaHome\bin\java.exe")) {
    throw "Portable JDK not found at '$javaHome'. Install JDK 17 and set JAVA_HOME, then run .\gradlew.bat assembleDebug."
}

$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;$env:PATH"

.\gradlew.bat assembleDebug
