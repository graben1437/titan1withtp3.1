This build is based on tp3-ci-31 as of 11/10/2015.

It has not been extensively tested, but works on HBase 1.1.1, Hadoop 2.7.1 and Cassandra 2.1.5.
SparkGraphComputer was also tested successfully on both databases.

The build also runs on Hadoop 2.6.0, but has to be recompiled with that level of support, otherwise
read errors occur.

May packages used by Titan were updated to match newer versions already used in TinkerPop 3.1.

Support of HBase versions prior to HBase 1.0 were dropped, as was support for Hadoop 1.

I could not get GiraphGraphComputer to work on an actual cluster. This is due to
the fact that my cluster has Yarn, and TinkerPop 3.1 does not implement the GraphYarnClient
implementation in the GiraphGraphComputer.  GiraphGraphComputer runs fine on a single
node installation.

HBase has an intermittent problem shown below.
This problem has been reported both on the Titan forums as well as the HBase forums in the past
and is the result of a guava version mismatch.

```
    Caused by: java.lang.IllegalAccessError: tried to access method com.google.common.base.Stopwatch.<init>()V from class org.apache.hadoop.hbase.zookeeper.MetaTableLocator
    at org.apache.hadoop.hbase.zookeeper.MetaTableLocator.blockUntilAvailable(MetaTableLocator.java:596)
    at org.apache.hadoop.hbase.zookeeper.MetaTableLocator.blockUntilAvailable(MetaTableLocator.java:580)
    at org.apache.hadoop.hbase.zookeeper.MetaTableLocator.blockUntilAvailable(MetaTableLocator.java:559)
```

There are some minor code updates in addition to pom.xml file changes.
In addition, this build contains (slightly broken) kakfa publisher capability that was published (and worked)
in a separate build.  The kafka work will continue to be upgraded.
