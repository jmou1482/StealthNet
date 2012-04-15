#!/bin/sh

# Java configuration
JRE=java
JRE_FLAGS="-enableassertions -enablesystemassertions"

# ANT configuration
ANT="$(which ant) -quiet"
[ "$?" -eq 0 ] && HAS_ANT=1 || HAS_ANT=0

# Get classpath
if [ $HAS_ANT -eq 1 ]; then
	# Get classpath from ant
	CLASSPATH=`$ANT classpath | sed -n "s/.*Classpath: '\(.*\)'/\1/p"`
else
	# Use default classpath
	CLASSPATH=""
fi

# Get JAR file (specified relative to script)
JAR_DIR=`dirname $0`
if [ $HAS_ANT -eq 1 ]; then
	# Get JAR path from ant
	JAR_DIR="$CLIENT_DIR`$ANT jar_dir | sed -n "s/.*JAR directory: '\(.*\)'/\1/p"`"
else
	# Use default JAR path
	JAR_DIR="$CLIENT_DIR/build/classes"
fi

# Debug options
DEBUG=" \
	-Ddebug.StealthNetChat=true \
	-Ddebug.StealthNetClient=true \
	-Ddebug.StealthNetComms=true \
	-Ddebug.StealthNetEncryption=true \
	-Ddebug.StealthNetFileTranser=true \
	-Ddebug.StealthNetKeyExchange=true \
	-Ddebug.StealthNetPacket=true \
	-Ddebug.StealthNetPRNG=true \
	-Ddebug.StealthNetServer=true \
	-Ddebug.StealthNetServerThread=true \
	"
DEBUG_ARG=

# Get program command line options
PN=`basename $0`
ARGS=`getopt --name "$PN" --long debug,client,server --options d -- "$@"`
if [ $? -ne 0 ]; then
    echo "getopt failed!" >&2
    exit 1
fi
eval set -- $ARGS
while [ $# -gt 0 ]; do
    case $1 in
        -d | --debug)
            DEBUG_ARG=$DEBUG
            ;;
            
        --client)
        	JAR_FILE=StealthNet_client.jar
        	;;
        	
        --server)
        	JAR_FILE=StealthNet_server.jar
        	;;
    esac
    shift
done

# Make sure client or server mode was specified
if [ -z "$JAR_FILE" ]; then
	echo "You must specify client (\`--client') or server (\`--server') mode." >&2
	exit 1
fi

# Execute
if [ -n "$CLASSPATH" ]; then
	$JRE $DEBUG_ARG -classpath "$CLASSPATH" $JRE_FLAGS -jar $JAR_DIR/$JAR_FILE $@
else
	$JRE $DEBUG_ARG $JRE_FLAGS -jar $JAR_DIR/$JAR_FILE $@
fi