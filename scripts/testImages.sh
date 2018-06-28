#!/bin/bash
# For each transform project, check the live probe in each docker image works.
set -e

docker images
echo

transformers=`ls | grep alfresco-docker- | sed 's/alfresco-docker-\(.*\)/\1/'`
for transformer in $transformers
do
  echo
  echo === $transformer ===
  repo=`docker images | awk '{print $1}' | grep $transformer | sort -u`
  echo docker run --rm -d -p 8090:8090 --name $transformer $repo:$tag
       docker run --rm -d -p 8090:8090 --name $transformer $repo:$tag >/dev/null

  WAIT_INTERVAL=1
  COUNTER=0
  TIMEOUT=30
  t0=`date +%s`
  echo -n "Waiting for $transformer to start "
  until $(curl --output /dev/null --silent --fail http://localhost:8090/live) || [ "$COUNTER" -eq "$TIMEOUT" ]; do
     printf '.'
     sleep $WAIT_INTERVAL
     COUNTER=$(($COUNTER+$WAIT_INTERVAL))
  done
  t1=`date +%s`
  delta=$(($t1 - $t0))

  docker stop $transformer > /dev/null

  if (("$COUNTER" < "$TIMEOUT")) ; then
     echo " started in $delta seconds"
  else
     echo " did not start after $delta seconds"
     exit 1
  fi
done
echo
