#!/bin/sh
exec java $BIOCACHE_OPTS -Dbiocache.config=./biocache-config.properties -Dfile.encoding=UTF8 -Dlog4j.configuration=file:///usr/lib/biocache/log4j.xml -Dactors.corePoolSize=8 -Dactors.maxPoolSize=16 -Dactors.minPoolSize=8 -Djava.util.Arrays.useLegacyMergeSort=true -Xmx4g -Xms4g  -jar $0 "$@"


