This readme is current as of 2/11/2016.
This build is still under active development.

This build looks the closest to what I suggest Titan 1.1.0 should have been.

It eliminates back level HBase/Hadoop, upgrades many of depedency versions,
includes numerous compatiility fixes, has many document improvements, and
works with Graph Computing - a key feature in TinkerPop 3.X.

*Both SparkGraphComputer and GiraphGraphComputer work in this build!*

I am in the process of updating this build to be as "turn key" as possible.
This means in some cases extra jars might be included, which makes for a bigger
Titan foot print, but it saves the user from fumbling around with classpath-olgy.
Unfortunately, classpath-ology is still in play even with this build.

Pull requests also included:

* 1195
* 1197
* 1250 - for the HBase 1.1.1 support only currently (HBase 1.0 classes not yet changed)
* 1238 - (doc change)
* 1255 - (doc change)
* 1246
* 1258 - LOCAL_ONE support
* 1263 - technically was already fixed in this build, but adding the pull request # as reference
         there are actually several missing/incorrect properties in the sample props files.
* 1248 - Unicode support, bug fix


Many of the changes in this build are submitted to the main titan 3.1 stream as pull requests
but are pending. Many changes are marked with "DAVID" to identify where code or poms have
been changed for this build. Many of the example property files, key to success in getting
graph computing working, differ from the main line Titan 1.x files.

This Titan build works for:

- TinkerPop 3.1.0-incubating
- HBase 1.1.1, 
- Hadoop 2.7.1
- Cassandra 2.2.3

This build also was tested on HDFS 2.6.0, but has to be recompiled at that version level before it can be used.
TinkerPop 3.1.1-SNAPSHOT (on its way to 3.1.1-incubating as this is typed) also works, as do very early
versions of TP 3.2.0, although the main pom must be updated. 


SparkGraphComputer:
Make sure HADOOP_GREMLIN_LIBS is exported and points to the Titan lib directory prior to starting
gremlin shell sessions, if you are using those.

GiraphGraphComputer
Make sure HADOOP_CONF is in your classpath.
Make sure the ext/giraph-gremlin/lib directory is in the HADOOP_GREMLIN_LIBS path in addition to the main Titan lib directory.
Make sure to edit the gremlin.sh file and at the very bottom uncomment and edit the JAVA_OPTS line to match your hadoop version information.


** NOTE: because of changes in Cassandra between 2.1 and 2.2, where no effort was apparently made to be backwards compatible,
the astynax driver no longer works.  The latest build tree for astynax shows it supports Cassandra 2.0.x.  If you try to use
astynax with Cassandra, with this build, it will not work.  You must use thrift.  I am trying to figure out exactly what we could do on this topic.


Support of HBase versions prior to HBase 1.0 were dropped, as was support for Hadoop 1.
TinkerPop 3.x only supports Hadoop 2 for GraphComputing and dragging extra dependencies
makes the build that much more complicated with no benefit.

The Kafka publisher code was upgraded to work with this build, although on deletes,
the publisher may generate exceptions.  That will be fixed and pushed to this build.
Exactly how to package the Kafka work without forcing Kafka jar files into the Titan
lib is being figured out. This is still experimental, needs test cases and some formal
packaging, but has been tested and works.

