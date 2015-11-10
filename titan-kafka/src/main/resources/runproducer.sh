#!/bin/bash

export TITAN_HOME=../../../../../titan1.0.0.kafka
export TITAN_KAFKA_HOME=$TITAN_HOME/titan-kafka/target
export KAFKA_LIBS=$TITAN_HOME/titan-kafka/lib

java -cp /home/graphie/titankafka/kafkalibs/*:$KAFKA_LIBS/*:$TITAN_HOME/lib/*:$TITAN_KAFKA_HOME/titan-kafka-1.0.1-SNAPSHOT.jar  org.apache.titan.kafka.TitanKafkaProducer producer.properties titan
