I open more windows in a separate section and start some default consumers to see my data
Use this to start the kafka consumer on one of the kafka nodes to listen to my topic
./kafka-console-consumer.sh --zookeeper 10.91.71.119  --topic titan  --from-beginning


Use this command to start up a kafka servers
I open 3-4 windows on kg3 -> kg6 and start the kafka servers.
./kafka-server-start.sh  ../config/server.properties  &

Kafka is installed on kg6, 5, 4, and 3 I believe


In another window, where the kafka producer for Titan runs, I have to
source Java 8:
[graphie@kg2-48 titankafka]$ . ./setenv.sh


Then I start the producer using this script:


Then I go into gremlin and generate some transaction events.



Have seen this error at least once when the transaction appeared to
commit ok in gremlin, but failed in the publisher:
java.nio.channels.ClosedByInterruptException
	at java.nio.channels.spi.AbstractInterruptibleChannel.end(AbstractInterruptibleChannel.java:202)
	at sun.nio.ch.FileChannelImpl.write(FileChannelImpl.java:215)
	at java.nio.channels.Channels.writeFullyImpl(Channels.java:78)
	at java.nio.channels.Channels.writeFully(Channels.java:101)
	at java.nio.channels.Channels.access$000(Channels.java:61)
	at java.nio.channels.Channels$1.write(Channels.java:174)
	at sun.nio.cs.StreamEncoder.writeBytes(StreamEncoder.java:221)
	at sun.nio.cs.StreamEncoder.implClose(StreamEncoder.java:316)
	at sun.nio.cs.StreamEncoder.close(StreamEncoder.java:149)
	at java.io.OutputStreamWriter.close(OutputStreamWriter.java:233)
	at java.io.BufferedWriter.close(BufferedWriter.java:266)
	at com.thinkaurelius.titan.diskstorage.log.kcvs.KCVSLog.openTx(KCVSLog.java:331)
	at com.thinkaurelius.titan.diskstorage.util.BackendOperation.execute(BackendOperation.java:131)
	at com.thinkaurelius.titan.diskstorage.util.BackendOperation$1.call(BackendOperation.java:147)
	at com.thinkaurelius.titan.diskstorage.util.BackendOperation.executeDirect(BackendOperation.java:56)
	at com.thinkaurelius.titan.diskstorage.util.BackendOperation.execute(BackendOperation.java:42)
	at com.thinkaurelius.titan.diskstorage.util.BackendOperation.execute(BackendOperation.java:144)
	at com.thinkaurelius.titan.diskstorage.log.kcvs.KCVSLog.writeSetting(KCVSLog.java:980)
	at com.thinkaurelius.titan.diskstorage.log.kcvs.KCVSLog.close(KCVSLog.java:312)
	at com.thinkaurelius.titan.diskstorage.log.kcvs.KCVSLogManager.close(KCVSLogManager.java:234)
	at com.thinkaurelius.titan.diskstorage.Backend.close(Backend.java:546)
	at com.thinkaurelius.titan.util.system.IOUtils.closeQuietly(IOUtils.java:49)
	at com.thinkaurelius.titan.graphdb.database.StandardTitanGraph.closeInternal(StandardTitanGraph.java:221)
