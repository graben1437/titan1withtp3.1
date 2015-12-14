This readme is current as of 12/14/2015.

This build is based on tp3-ci-31 as of 11/10/2015.
Many of the changes in this build are submitted to the main titan 3.1 stream as pull requests,
but at this point in time, they have not been pulled in. You can look in the poms and code
and should see markers with pull request numbers for most of the changes.

This build also includes Titan pull requests 1195 and 1197.

Build TinkerPop 3.1-incubating (master branch) first in order to have those
jars available in your maven repository for this Titan build.

This Titan build works for:

- TinkerPop 3.1
- HBase 1.1.1, 
- Hadoop 2.7.1
- Cassandra 2.2.3

This build also was tested on HDFS 2.6.0, but has to be recompiled at that version
level before it can be used.

Work is underway to get SparkGraphComputer, Giraph, Faunus working also and 
that work will be pushed to this repo when ready.


There are code updates in addition to pom.xml file changes, that include updates to code to support
HBase 1.1.1 and to pre-install the gremlin-spark plug-in into the Gremlin shell.

Many packages used by Titan were updated to match newer versions already used in TinkerPop 3.1.

Support of HBase versions prior to HBase 1.0 were dropped, as was support for Hadoop 1.

I could not get GiraphGraphComputer to work on an actual cluster. This is due to
the fact that my cluster has Yarn, and TinkerPop 3.1 does not implement the GraphYarnClient
implementation in the GiraphGraphComputer.  GiraphGraphComputer runs fine on a single
node installation.

The Kafka publisher code was upgraded to work with this build, although on deletes,
the publisher may generate exceptions.  That will be fixed and pushed to this build.
Exactly how to package the Kafka work without forcing Kafka jar files into the Titan
lib is being figured out.

The following problem, caused by HBase using a very old (version 12) of guava, should
be solved.  I backed off the Titan guava level to version 16 (from the latest version, which is 18).
This problem seems to have gone away with 16...although I'll keep looking for it:

```
    Caused by: java.lang.IllegalAccessError: tried to access method com.google.common.base.Stopwatch.<init>()V from class org.apache.hadoop.hbase.zookeeper.MetaTableLocator
    at org.apache.hadoop.hbase.zookeeper.MetaTableLocator.blockUntilAvailable(MetaTableLocator.java:596)
    at org.apache.hadoop.hbase.zookeeper.MetaTableLocator.blockUntilAvailable(MetaTableLocator.java:580)
    at org.apache.hadoop.hbase.zookeeper.MetaTableLocator.blockUntilAvailable(MetaTableLocator.java:559)
```
