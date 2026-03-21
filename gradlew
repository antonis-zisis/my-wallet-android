#!/bin/sh

#
# Copyright © 2015-2021 the original authors.
# Licensed under the Apache License, Version 2.0
#

##############################################################################
# Gradle startup script for POSIX
##############################################################################

# Attempt to set APP_HOME
app_path=$0
while
    APP_HOME=${app_path%"${app_path##*/}"}
    [ "${APP_HOME}" ] && [ -L "$app_path" ]
do
    app_path=$(ls -ld "$app_path" | awk 'NF>1{print $NF}')
done
APP_HOME=$(cd "${APP_HOME:-./}" > /dev/null && pwd -P) || exit

APP_NAME="Gradle"
APP_BASE_NAME=${0##*/}

# Add default JVM options
DEFAULT_JVM_OPTS='-Xmx64m -Xms64m'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD=maximum

warn() {
    echo "$*"
}

die() {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "$(uname)" in
    CYGWIN*)  cygwin=true  ;;
    Darwin*)  darwin=true  ;;
    MSYS* | MINGW*) msys=true ;;
    NONSTOP*) nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the Java command
if [ -n "$JAVA_HOME" ]; then
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then
        JAVACMD=$JAVA_HOME/jre/sh/java
    else
        JAVACMD=$JAVA_HOME/bin/java
    fi
    if [ ! -x "$JAVACMD" ]; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    JAVACMD=java
    if ! command -v java > /dev/null 2>&1; then
        die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
    fi
fi

# Increase max file descriptors if we can
if ! "$cygwin" && ! "$darwin" && ! "$nonstop"; then
    case $MAX_FD in
        max*) MAX_FD=$(ulimit -H -n) || warn "Could not query max file descriptor limit" ;;
    esac
    case $MAX_FD in
        '' | soft) ;;
        *) ulimit -n "$MAX_FD" || warn "Could not set max file descriptor limit to $MAX_FD" ;;
    esac
fi

# Cygwin/MSYS path conversion
if "$cygwin" || "$msys"; then
    APP_HOME=$(cygpath --path --mixed "$APP_HOME")
    CLASSPATH=$(cygpath --path --mixed "$CLASSPATH")
    JAVACMD=$(cygpath --unix "$JAVACMD")
fi

exec "$JAVACMD" \
    $DEFAULT_JVM_OPTS \
    $JAVA_OPTS \
    $GRADLE_OPTS \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
