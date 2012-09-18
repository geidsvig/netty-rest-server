#!/bin/sh

AKKA_HOME="target/rest-dist"
AKKA_CLASSPATH="$AKKA_HOME/config:$AKKA_HOME/lib/*"
JAVA_OPTS="-Djava.library.path=/usr/local/lib -Xms512M -Xmx1024M -Xss1M -XX:+UseParallelGC -XX:GCTimeRatio=19"

java $JAVA_OPTS -cp "$AKKA_CLASSPATH" -Dakka.home="$AKKA_HOME" akka.kernel.Main ca.figmint.RestServerBoot
