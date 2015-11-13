# Titan-Kafka Publisher

The Titan-Kafka listener/publisher publishes each Titan transaction to a configurable Kafka topic in
a compact JSON format.

Changes were made to core Titan in addition to adding a new module, so a full Titan package is included.

## Technology Preview

This proof-of-concept code provides a working example of a Titan/Kafka listener/publisher.
 
Please provide feedback via github issues on architectural or design considerations you would like
considered as this function matures. 

Eventually, this code will be refactored into a formal pull request to the Titan project after consideration
of input from the community.

Because this is experimental, some of the packaging and test cases are not yet present.

This readme provides a fairly extensive discussion of different design considerations.

In addition to the Titan-Kafka listener/publisher, a sample Titan-HTTP listener/publisher is also provided, although
it is not the focus of this effort.

## Getting Started
This preview is based on Titan 1.0.1-SNAPSHOT

### Prerequisites and Notes
Titan 1.0.1-Snapshot requires Java 8.  It must be in the path for compiling and executing these examples.

You may need to install maven if it is not already installed.

A four node Cassandra 2.1 cluster was used to test this code. You can use a single Cassandra node or a cluster.
There is nothing Cassandra specific to the Titan/Kafka support so HBase can also be used.

A four node Kafka 0.8.2 cluster was used to test this code.  You can choose to use any size Kafka cluster.

**1)** Build This Titan Package

Download this source from github and build Titan exactly as you would with Titan trunk:
[Titan Build Instructions](http://s3.thinkaurelius.com/docs/titan/1.0.0/building.html)

`mvn clean install -DskipTests=true` issued from $TITAN_HOME is typically sufficient.


**2)** Install a Kafka cluster (and its prerequisites).
[Kafka Quickstart Instructions](http://kafka.apache.org/documentation.html#quickstart)

Test the Kafka cluster per the instructions prior to trying the Titan/Kafka integration.

This code was tested with Kafka 0.8.2

**3)** Update the **cass.properties** file.
 
Navigate to the $TITAN_HOME/titan-kafka/src/main/resources subdirectory.

Edit the cass.properties example file and configure the file for your environment. 
 
There are two new properties in the cass.properties file.
```
tx.report-all-transactions=true  

Indicates all transactions, even auto-transactions, should be reported to the logging system.
See the discussion below for more details.
```

```
tx.logidentifier.name='kafkaproducer' 
 
Defines the user log name used by the Titan logging system when recording all transaction activity.
This value is also used by the Kafka Listener/Producer to register with the appropriate change log in Titan.
```

```
tx.log-tx=true

This parameter is not new.  But don't forget to set it to enable logging.  It must be set to true
when the data store is initially created
```

**4)** Update the **producer.properties** file.

Under your Titan build, navigate to the ../titan-kafka/src/main/resources subdirectory.

Edit the producer.properties sample file and configure to match your environment. In particular, update the following
values:

```
metadata.broker.list=XX.XX.71.199:9092

Should be set to the IP address and port where a Kafka broker is running.
```

```
hostport=XX.XX.71.77:2181

Should be set to the IP address and port of your Zookeeper server used by Kafka.
```

```
titan.config.location=<path>

Verify the specified path points to the correct subdirectory in your installation where the cass.properties file is
located.
```

*Note:* Kafka does not 'like' non-Kafka properties in the producer.properties file.  The result is when the Titan-Kafka
listener/producer starts, it will issue warnings about invalid properties.  Ideally, non-Kafka properties should be in 
a separate properties file, but for now, they are piggy backed in the current file.  The warnings can be ignored.


**5)** Copy the **cass.properties** file from the $TITAN_HOME/titan-kafka/src/main/resources directory
to the $TITAN_HOME/bin directory.

This step is optional.  I find it easier to have the cass.properties file in the bin directory (or the config directory)
to use when opening a graph with TitanFactory in a gremlin.sh session.

**6)** Start Kafka Brokers.

Follow the instructions in the Kafka documentation to start all of your brokers that you want to use for testing.

Typically, you can issue the following command from the $KAFKA_HOME/bin directory once the server.properties
file is appropriately configured for your environment:

`./kafka-server-start.sh ../config/server.properties`

**7)** Create the Titan-Kafka topic.

Follow the instructions in the Kafka documentation to create a topic with the desired name and the desired number
of partitions.  The topic name must be consistent with what is used in all of the other steps.

Creating a Kafka topic may look like this:
`./kafka-topics.sh --create --zookeeper XX.XX.75.120:2181 --replication-factor 1 --partitions 2 --topic titan`

Verify the topic looks like you would expect:
`./kafka-topics.sh --describe --zookeeper XX.XX.75.120:2181 --topic titan`

**8)** Start the Kafka console listeners.

This step is optional, but provides a simple way to verify that messages are being published to Kafka and can be read 
by a consumer.

Follow the instructions in the Kafka documentation to start the console listeners.

Typically, you can issue the following command from the $KAFKA_HOME/bin directory:
`./kafka-console-consumer.sh --zookeeper <zookeeper IP and port>  --topic <must match topic in producer.properties>
                            --from-beginning`

Specifying --from-beginning will display all messages in the topic, regardless of when received or if
already read.

**9)** Put Kafka jar files in the path of the producer.

If running the Titan-Kafka listener/producer on the same machine as the Kafka broker, edit the `runproducer.sh`
script and make sure the `KAFKA_LIBS` export points to the correct location.

If running the Titan-Kafka listener/producer on a separate machine from a Kafka broker (which was the
configuration that was tested) the Kafka jar files must be copied to the machine where the listener/producer
is running and placed in the classpath.  Note that the `runproducer.sh` script contains a reference to the
Kafka libraries that may need to be edited depending on where you copy them:
`export KAFKA_LIBS=$TITAN_HOME/titan-kafka/lib`

**10)** Start the titan-kafka listener/producer.

Under your Titan build, navigate to the $TITAN_HOME/titan-kafka/src/main/resources subdirectory.

Enter: `./runproducer.sh` 
to start the listener/publisher.  Monitor the console messages for any errors registering with Titan and/or 
Kafka.

**11)** Start a gremlin shell and generate transactions.

Under your Titan build, navigate to the $TITAN_HOME/bin subdirectory.

Enter: `./gremlin.sh` 
to start the gremlin shell.

Generate both auto and named transactions:

An auto transaction add session example:

```
gremlin> graph=TitanFactory.open('./cass.properties')
==>standardtitangraph[cassandrathrift:[XX.XX.71.230, XX.XX.71.231, XX.XX.71.232]]
gremlin> g=graph.traversal()
==>graphtraversalsource[standardtitangraph[cassandrathrift:[XX.XX.71.230, XX.XX.71.231, XX.XX.71.232]], standard]
gremlin> v1=graph.addVertex('sky', 'blue')
==>v[4280]
gremlin> v2=graph.addVertex('grass', 'green')
==>v[4200]
gremlin> v1.addEdge('above', v2)
==>e[17b-3aw-4get-38o][4280-above->4200]
gremlin> graph.tx().commit()
==>null
```

At this point, switch to the console where the titan-kafka listener/producer is running. You should see activity, after a
small delay, as the listener is notified by Titan of the transaction and the listener/producer publishes the transaction
to Kafka.  It should look something like this:

```
published: {"txtime":2015-10-14T01:08:01.414Z,"removed":[],"added":[{"sky":"blue","removedEdges":[],"id":4320,
"label":"vertex","addededges":[{"edgeid":17g-3c0-8sut-oe0ag,"edgelabel":"above"}]},
{"removedEdges":[],"grass":"green","id":40964344,"label":"vertex","addededges":[]}],
"txinstance":"0a5b47eb26629-xyz-88-machine-com1","txinfo":1}
```

Switch to the Kafka console consumers and verify, after a small delay, that you see the JSON message from the producer
on the console.

An auto transaction remove session example (relies on the add session above):
```
gremlin> v2.remove()
==>null
gremlin> graph.tx().commit()
```
At this point, repeat the verification steps by checking the various consoles as mentioned
previously.

```
published: {"txtime":2015-10-14T01:12:57.034Z,"removed":[{"removedEdges":[],"id":40964344,"label":"vertex",
"addededges":[]}],"added":[],"txinstance":"0a5b47eb26629-xyz-88-machine-com1","txinfo":2}
```

A named transaction session add example:
```
gremlin> graph=TitanFactory.open('./cass.properties')
==>standardtitangraph[cassandrathrift:[XX.XX.71.219, XX.XX.71.223, XX.XX.71.229]]
gremlin> g=graph.traversal()
==>graphtraversalsource[standardtitangraph[cassandrathrift:[XX.XX.71.219, XX.XX.71.223, XX.XX.71.229]], standard]
gremlin> tx=graph.buildTransaction().logIdentifier('kafkaproducer').start()
==>standardtitantx[0x5d8ab698]
gremlin> v1=tx.addVertex(label, 'human')
==>v[4288]
gremlin> v2=tx.addVertex(label, 'dog')
==>v[4272]
gremlin> v1.property('name', 'billybob')
==>vp[name->billybob]
gremlin> v2=tx.addVertex(label, 'dog')
==>v[8384]
gremlin> v2.property('name', 'sounder')
==>vp[name->sounder]
gremlin> e1=v1.addEdge('owns', v2, 'hunts', 'racoons')
==>e[2s8-3b4-6o7p-6gw][4288-owns->8384]
gremlin> tx.commit()
==>null
```

At this point, repeat the verification steps by checking the various consoles as mentioned previously.  An important 
detail:  The logIdentifier string specified must match the log identifier value in your cass.properties file specified 
by the tx.logidentifier.name property value.

```
published: {"txtime":2015-10-14T01:16:24.601Z,"removed":[],"added":[{"removedEdges":[],"name":"billybob","id":4288,
"label":"human","addededges":[{"hunts":"racoons","edgeid":2s8-3b4-6o7p-6gw,"edgelabel":"owns"}]},
{"removedEdges":[],"id":4272,"label":"dog","addededges":[]},{"removedEdges":[],"name":"sounder","id":8384,
"label":"dog","addededges":[]}],"txinstance":"0a5b47eb26629-xyz-88-machine-com2","txinfo":1}
```

There is no such thing in Titan today representing a remove on a named transaction (see discussion below).
Here is an example of a remove of items committed with a named transaction, using auto transaction:

```
gremlin> vX1=g.V(4288).next()
==>v[4288]
gremlin> vX1.remove()
==>null
gremlin> graph.tx().commit()
==>null
```

With the results:
```
published: {"txtime":2015-10-14T01:18:38.899Z,"removed":
[{"removedEdges":[{"hunts":"racoons","edgeid":2s8-3b4-6o7p-6gw,"edgelabel":owns}],"id":4288,"label":"vertex",
"addededges":[]}],"added":[],"txinstance":"0a5b47eb26629-xyz-88-machine-com2","txinfo":2}
```


## Existing Problems and Tips

**1)** Symptom: The Titan-Kafka listener/publisher is not notified of a transaction.

This was consistently reproduceable.  Here is one scenario:  Day 1, successfully run the Titan-Kafka integration. Stop
the Kafka brokers, Kafka consumers, the Titan-Kafka listener/provider and the gremlin test shell.
Day 2, start everything again - brokers, consumers, listener/provider, gremlin shell.  Create transactions.  Nothing 
is reported to the listener/provider.

This is believed to be an existing problem in Titan's logging system, although no investigation of the problem
has been done yet.

To resolve the problem, drop the test Titan keyspace and let it be recreated by the listener/producer
or the gremlin shell activities.  Clearly, this is not an acceptable production solution, but it works to
test the listener/producer code.

Once the Titan-Kafka listener/publisher receives a "first" transaction message from the Titan change log system,
it it appears to work continuously until stopped and restarted after a period of time.

**2)** Tip: Resetting the Kafka topic

You may want to delete a Kafka topic and start new.  Reasons for doing this are to clean out
old messages, or to change the partitioning of the topic.  Kafka provides instructions on how
to delete a topic using the kafka-topics.sh command.

`./kafka-topics.sh --delete --topic titan --zookeeper XX.XX.75.120:2181`

**3)** Tip: Kafka and SolrCloud collisions in Zookeeper

SolrCloud and Kafka have namespace collisions in Zookeeper.  Beware if sharing 
the same Zookeeper between a SolrCloud and a Kafka installation.  This problem
can manifest itself in 'wild' looking replication or partition settings for a Kafka
topic if SolrCloud data is already in Zookeeper prior to starting your Kafka cluster.

**4** Using the HTTP Producer Example

Edit the source code, hard code the URL endpoint where the JSON payload should be sent.
The line to change in the source is as follows:

`apiURL = "http://your.http.endpoint:port";`

Rebuild just the titan-kafka module:
`mvn clean install` 
from the titan-kafka directory should work.

## Discussion
### Running Across A Cluster
Based on empirical evidence, Titan's logging system is node specific.  In other words,
the node where the transaction occurs is where the logging of the transaction takes place, and where
the call back to a listener occurs (makes sense given the way Java works).

The implication is that if running Titan on a multi-node cluster, a Titan-Kafka listener/producer process
will need to be started on each of the nodes in order to collect all transactions occurring across the
Titan cluster.  The listener/publisher on each node can be configured to publish to the same Kafka topic,
providing a centralized list of transactions in one place for consumers.

### Transactions
Titan supports two basic types of transactions: named and auto transactions.

Named transactions are explicitly opened by the user, are directed to a specific user log for recording,
and only support certain types of actions according to comments in the original code - which are primarily
the create/update activities.  Here is how to open a named transaction:
`tx=graph.buildTransaction().logIdentifier('kafkaproducer').start()`

Here are comments from the TitanGraphTransaction.java interface about what is currently supported with named 
transactions. "Remove" can happen in the middle of a transaction that is adding items, but there is no way
to do strictly a named remove action.
```
A graph transaction supports:
 Creating vertices, properties and edges
 Creating types
 Index-based retrieval of vertices
 Querying edges and vertices
 Aborting and committing transaction
```

You can experiment with the behavior of named transactions and the Titan-Kafka listener/publisher by turning
the `tx.report-all-transactions` value in the cass.properties file to false and specifying the correct
string in the logIdentifier in your commands (see examples above).  The listener/publisher will still
publish transactions performed on the named transactions, but auto transactions will no longer be reported.

Auto transactions occur every time some change is made to the graph outside of an explicitly declared transaction.
Transactions are started on the "graph" object and are committed by issuing the `graph.tx().commit()` command.
By default, these auto transaction are unnamed and therefore not logged in Titan code.

Changes were made in the base Titan code to associate both named and auto transactions with the same user log name
when the `tx.report-all-transactions` value is set to true.  This is how the experimental build is able to capture
all transactions - named and auto to the transaction log.  Remove actions are also captured.

The intended behavior is for the Kafka listener/publisher to publish the entire Titan transaction stream to Kafka.

### Standalone or Embedded Provider
The experimental build uses the existing change log infrastructure of Titan and simply registers the listener into
the Titan change log infrastructure.  The listener itself is a separate process (JVM).

One reason the code is a separate process, is that after looking at the existing Titan code, it turns out
that the  KCVSLog subsystem isn't entirely pluggable.  For example, StandardTitanGraph.java refers specifically
to KCVSLog rather than an interface:  
`final KCVSLog txLog = logTransaction.backend.getSystemTxLog()`

A second reason the listener is a separate process, is this was the fastest way to get the function running.

An alternative to running the Titan-Kafka listener/publisher as a separate process, is to alter the "add" logic
in the KCVSLog code so that each time a new transaction is added to the log, a message is published to Kafka.  This 
would require Kafka dependencies inside Titan, which might be undesirable.  

Another alternative to running a separate process, is to write a replacement logging implementation that is specific
to Kafka and build it as a stand alone module that can be "plugged in" via configuration.
 
The module could be implemented two different ways.  It could publish messages directly to Kafka in lieu of writing to
disk.  The advantage is a lighter weight approach to recording transactions without writes to disk by Titan.
The disadvantage is if Kafka brokers are unreachable, transactions messages would be lost.  The other
obvious alternative is to combine Kafka and the current KCVSLog so that if the Kafka broker becomes unavilable,
messages can be retried during their retention period in the log (default = 2 days).

### JSON reporting Format
The Titan change log code delivers changes in a vertex centric fashion where edge updates are linked to
the vertices.  The JSON format published by the Kafka listener/publisher is heavily influenced by way it receives
messages.  Edge information is linked to the outgoing vertex that owns the edge.

Each message represents a single Titan transaction.  Based on the contents of a transaction, the message
will contain removed vertices, added vertices and within each vertex data, edge information for removed
and added vertices.

A JSON message published that added two vertices with an edge between the vertices:

```
{"txtime":2015-10-13T18:34:25.701Z,"removed":[],"added":[{"removedEdges":[],"name":"billybob","id":4336,
"label":"human","addededges":[{"hunts":"racoons","edgeid":1lq-3cg-6o7p-oe3ag,"edgelabel":"owns"}]},
{"removedEdges":[],"id":40964136,"label":"dog","addededges":[]},{"removedEdges":[],"name":"sounder","id":40968232,
"label":"dog","addededges":[]}],"txinstance":"0a5b47eb26629-xyz-88-machine-com1","txinfo":1}
```

### Serialization of the JSON payload
Currently, the JSON payload is not seralized.  Kafka producer/consumers frequently use serialization for efficiency.
Various options are available for serialization, including Avro and Kryo.  It isn't clear if serialization provides
significant benefit in this case.

I prefer not to get into pluggable serialization, or switches to turn serialization on and off.

### Attributes reported on removal
The experimental Titan code base contains changes to the ElementLifeCycle.java class in Titan to add a new lifecycle
to represent "ChangeLog". 

The current Titan change log logic "reconstitutes" vertices and edges based on information serialized to disk in
the user transaction log.  The logic reuses existing objects in Titan, such as the StandardVertex and CacheVertex 
(and/or similar such classes). These classes are intended to represent objects in the active graph, not reconstituted 
objects from a change log.  This caused problems in the listener when trying to retrieve properties of removed objects,
because the existing logic checks for a "removed" state and throws exceptions, preventing properties from being 
retrieved.  It turns out the user transaction log doesn't record all of the properties for objects being deleted anyway,
so at the moment, there is no benefit provided by the ChangeLog state that is in the current experiment.

Another option, and perhaps a better one, is to create a set of ChangeLogVertex... set of classes and include just
the functions that can be applied to change log objects to these new classes.  It would be these objects that
would be passed to listeners in the future.

A third option, is to leave the change log code the way it is and modify the listener to check for the state of a
vertex, and if it is marked as "removed", then to avoid trying to retrieve properties on the vertex.

What provides the best long term value ?
