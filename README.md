This readme is current as of 2/4/2016.
This build is still under active development.

This build looks the closest to what I suggest Titan 1.1.0 should have been.

It eliminates back level HBase/Hadoop, upgrades many of depedency versions,
includes numerous compatiility fixes, had many document improvements, and
works with Graph Computing - a key feature in TinkerPop 3.X.

Pull requests also included:
1195
1197
1250 - for the HBase 1.1.1 support only currently (HBase 1.0 classes not yet changed)
1238 - (doc change)
1255 - (doc change)
1246


Many of the changes in this build are submitted to the main titan 3.1 stream as pull requests
but are pending. Many changes are marked with "DAVID" in the code and poms to identify place where this
build has been changed.

This Titan build works for:

- TinkerPop 3.1.0-incubating
- HBase 1.1.1, 
- Hadoop 2.7.1
- Cassandra 2.2.3

This build also was tested on HDFS 2.6.0, but has to be recompiled at that version level before it can be used.
TinkerPop 3.1.1-SNAPSHOT (on its way to 3.1.1-incubating as this is typed) does not currently work.


*Both SparkGraphComputer and GiraphGraphComputer work in this build.*

SparkGraphComputer:
Make sure HADOOP_GREMLIN_LIBS is exported and points to the Titan lib directory.

GiraphGraphComputer
Make sure HADOOP_CONF is in your classpath.
Make sure the ext/giraph-gremlin/lib directory is in the HADOOP_GREMLIN_LIBS path in addition to the main Titan lib directory.
Make sure to edit the gremlin.sh file and at the very bottom uncomment and edit the JAVA_OPTS line to match your hadoop version information.


** NOTE: because of changes in Cassandra between 2.1 and 2.2, where they apparently made
no effort to be backwards compatible, the astynax driver no longer works.  The latest
build tree for astynax shows it supports Cassandra 2.0.x.  If you try to use
astynax with Cassandra, with this build, it will not work.  You must use thrift.
I am trying to figure out exactly what we could do on this topic.


Support of HBase versions prior to HBase 1.0 were dropped, as was support for Hadoop 1.
TinkerPop 3.x only supports Hadoop 2 for GraphComputing and dragging extra dependencies
makes the build that much more complicated with no benefit.

The Kafka publisher code was upgraded to work with this build, although on deletes,
the publisher may generate exceptions.  That will be fixed and pushed to this build.
Exactly how to package the Kafka work without forcing Kafka jar files into the Titan
lib is being figured out.

