#/usr/bin/env bash
nohup java -server -XX:+UseG1GC -DCONFIG_FILE=/data/kinesis/prod.properties -jar /data/kinesis/kinesis-three.jar >>/data/kinesis/out.log 2>&1 &
