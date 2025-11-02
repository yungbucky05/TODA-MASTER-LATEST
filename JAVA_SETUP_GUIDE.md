# TODA Project Java Setup Guide

Android Gradle Plugin 8.x requires JDK 17 or newer. Java 8 will NOT work for builds.

## Issue: JAVA_HOME is not set or points to an invalid JDK
Gradle can't find a compatible Java runtime. Fix it with one of the options below.

## Quick Fix (current terminal session only)

Option A: Use Android Studio's embedded JBR (recommended)

On cmd.exe:

```
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"
gradlew.bat -Dorg.gradle.java.home="%JAVA_HOME%" -v
```

If you have a different install path, try:

```
set "JAVA_HOME=C:\Program Files\Android\Android Studio1\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"
gradlew.bat -Dorg.gradle.java.home="%JAVA_HOME%" -v
```

Option B: Use a standalone JDK 17/21 (Microsoft, Temurin, Zulu, etc.)

Replace the path with your installed JDK:

```
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.11.9-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
java -version
gradlew.bat -Dorg.gradle.java.home="%JAVA_HOME%" -v
```

## Permanent Fix (system-wide)

1. Open System Environment Variables
   - Press Win + R, type `sysdm.cpl`, press Enter
   - Click "Environment Variables..."
2. Add JAVA_HOME
   - System variables -> New...
   - Name: `JAVA_HOME`
   - Value: `C:\Program Files\Android\Android Studio\jbr` (or your JDK 17 path)
3. Update PATH
   - Edit the `Path` system variable -> New -> `%JAVA_HOME%\bin`
4. Open a new cmd terminal and verify:

```
java -version
javac -version
```

## Alternative: Tell Gradle which JDK to use
You can explicitly point Gradle at a JDK without changing global env vars:

```
gradlew.bat -Dorg.gradle.java.home="C:\Program Files\Android\Android Studio\jbr" build
```

If builds still fail due to Java issues, run `setup_diagnostic.bat` and follow its hints.
