#!/bin/bash

# Run the Alfresco transformation service JAR file with the specified Java options
exec java $JAVA_OPTS -jar /usr/bin/${1}.jar "$@"
