# kinesis-three
This is a sample Kinesis Consumer Application, with basic implementation of [amazon-kinesis-client](https://github.com/awslabs/amazon-kinesis-client) library.
- It demonstrates the ingestion part of incoming stream.
- The output should be properly formatted for your own use case.

## Run
- To run, first you need a valid AWS auth; with that you should be able to read/write DynamoDB and read from Kinesis stream. 
- Sample AWS policy is shown [here](https://github.com/az3/kinesis-three/blob/master/src/main/resources/policy.json). You can test it on AWS [Policy Simulator](https://policysim.aws.amazon.com/home/index.jsp).
- Then, you need to prepare a "properties" file to connect to AWS. Sample file is [here](https://github.com/az3/kinesis-three/blob/master/src/main/resources/test.properties).
```
$ cat <<EOF >/tmp/app.prop
appName = kinesis-three-app1
kinesisInputStream = mystream
regionName = us-east-1
kinesisEndpoint = https://kinesis.us-east-1.amazonaws.com
workerId = myworker1a
idleTimeBetweenReadsInMillis = 1000
prometheusPort = 9751

EOF
```

- Finally, you can run application with the following command.
```
$ git clone --depth 1 https://github.com/az3/kinesis-three.git
$ cd kinesis-three
$ mvn clean install
$ java -DCONFIG_FILE=/tmp/app.prop -jar target/kinesis-three.jar
```

- It will parse incoming stream data to console (stdout) and persist position in DynamoDB.
