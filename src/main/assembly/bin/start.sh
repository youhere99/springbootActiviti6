#!/bin/sh
if [ "$1" = "centertest" ]; then
	MEM_SIZE=" -Xmx512m -Xms512m "
elif [ "$1" = "test" ]; then
	MEM_SIZE=" -Xmx2g -Xms2g "
elif [ "$1" = "internettest" ]; then
	MEM_SIZE=" -Xmx512m -Xms512m "
elif [ "$1" = "dev" ]; then
	MEM_SIZE=" -Xmx512m -Xms512m "
elif [ "$1" = "thirdtest" ]; then
	MEM_SIZE=" -Xmx512m -Xms512m "
elif [ "$1" = "network51" ]; then
	MEM_SIZE=" -Xmx2g -Xms2g "
elif [ "$1" = "internet1" ]; then
	MEM_SIZE=" -Xmx2g -Xms2g "
elif [ "$1" = "release" ]; then
	MEM_SIZE=" -Xmx8g -Xms8g "
else
	echo "Please make sure the positon variable is centertest or test or dev or thirdtest or network51 or internet1 or release."
	exit 0
fi
PROFILES_ACTIVE='-Dspring.profiles.active='$1
echo "$PROFILES_ACTIVE"

source /etc/profile

cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
SERVER_NAME=$DEPLOY_DIR
JRE_HOME=$JAVA_HOME/jre
PATH=$JAVA_HOME/bin:$PATH
export JAVA_HOME
export PATH

APP_PID=`ps -f | grep java | grep "$DEPLOY_DIR" | awk '{print $2}'`
if [ -n "$APP_PID" ]; then
	echo "The $SERVER_NAME already started! "
	echo "PID: $APP_PID"
	exit 1
fi

LOGS_DIR=$DEPLOY_DIR/logs
if [ ! -d $LOGS_DIR ]; then
  mkdir $LOGS_DIR
fi

JAVA_DEBUG_OPTS=""
if [ "$2" = "debug" ]; then
  JAVA_DEBUG_OPTS=" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n "
fi
JAVA_JMX_OPTS=""
if [ "$2" = "jmx" ]; then
  JAVA_JMX_OPTS=" -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false "
fi
JAVA_MEM_OPTS=""
BITS=`java -version 2>&1 | grep -i 64-bit`
if [ -n "$BITS" ]; then
  JAVA_MEM_OPTS=" -server "$MEM_SIZE" -Xss256k -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=256m "
else
  JAVA_MEM_OPTS=" -server "$MEM_SIZE" -Xss128k -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=128m "
fi
echo -e "Starting the $SERVER_NAME ..."
nohup java $JAVA_MEM_OPTS $JAVA_DEBUG_OPTS $JAVA_JMX_OPTS $PROFILES_ACTIVE -jar $DEPLOY_DIR/lib/*.jar > /dev/null 2>&1 &
COUNT=0
while [ $COUNT -lt 1 ]; do
  echo -e ".\c"
  sleep 1
  if [ -n "$SERVER_PORT" ]; then
    COUNT=`netstat -an | grep '\<$SERVER_PORT\>'  | wc -l`
  else
    COUNT=`ps -f | grep java | grep "$DEPLOY_DIR" | awk '{print $2}' | wc -l`
  fi
  if [ $COUNT -gt 0 ]; then
    break
  fi
done
echo "OK!"
APP_PID=`ps -f | grep java | grep "$DEPLOY_DIR" | awk '{print $2}'`
echo "PID: $APP_PID"
