package org.apache.titan.kafka;

import com.thinkaurelius.titan.core.*;
import com.thinkaurelius.titan.core.log.*;
import com.thinkaurelius.titan.graphdb.relations.SimpleTitanProperty;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.*;



import kafka.common.QueueFullException;
import kafka.javaapi.producer.Producer;
import org.json.simple.JSONObject;

/**
 *
 */
public class TitanKafkaProducer
{
    private static Logger LOGGER = LoggerFactory.getLogger(TitanKafkaProducer.class);
    private final String className = "TitanKafkaProducer";
    private static int state = -1;


    private Producer<byte[], byte[]> producer;
    private String topic;

    private String titanConfigLocation = null;
    private String logNameIdentifier = "kafkaproducer";

    private boolean finishRunning = false;

    private TitanGraph graph = null;

    /**
     * Construtor
     *
     * @cfgProps configuration file containing Kafka and zookeeper IPs and ports
     * @topic the Kafka topic name to publish to
     */
    public TitanKafkaProducer(final String cfgProps, final String topic)
    {
        final String methodName = "TitanKafkaProducer.constructor";
        this.topic = topic;
        assert (topic != null);

        // create the producer
        Properties props = new Properties();
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(cfgProps));
            props.load(br);
            titanConfigLocation = props.getProperty("titan.config.location");
            assert (titanConfigLocation != null);
            logNameIdentifier = props.getProperty("titan.log.name");
        }
        catch (IOException ie)
        {
            LOGGER.error(ie.getMessage(), ie);
        }
        finally
        {
            if (br != null) {
                try {
                    br.close();
                }
                catch (IOException ioe) {
                    LOGGER.error(ioe.getMessage(), ioe);
                }
                finally {
                    br = null;
                }
            }
        }


        try
        {
            ProducerConfig config = new ProducerConfig(props);
            producer = new Producer<byte[], byte[]>(config);
            state = 1;
        }
        catch (Exception e)
        {
            System.out.println("failed to create producer");
            // failed
            state = 0;
        }
    }


    /**
     * registers the listener method with Titan transaction logging
     * system
     */
    private void registerListenPublish()
    {
        final String methodName = "registerListenPublish";

        System.out.println("Connecting to Titan");
        graph = TitanFactory.open(titanConfigLocation);
        assert(graph != null);

        System.out.println("registering with the Titan Log Processor framework");
        LogProcessorFramework logProcessor = TitanFactory.openTransactionLog(graph);

        logProcessor.addLogProcessor("kafkaproducer").
                setProcessorIdentifier("kafkaproducerlistener").
                setStartTime(Instant.now()).
                addProcessor(new ChangeProcessor()
                             {
                                 public void process(TitanTransaction tx, TransactionId txId, ChangeState changeState)
                                 {
                                     HashMap transactionMap = new HashMap<String, Object>();

                                     // System.out.println("checkpoint 1");
                                     for (Change kind : Change.values())
                                     {
                                         // System.out.println("checkpoint 2");
                                         if ((kind.compareTo(Change.ADDED) == 0) || (kind.compareTo(Change.REMOVED) == 0))
                                         {
                                             // System.out.println("checkpoint 3");
                                             String txInstance = txId.getInstanceId();
                                             long txInfo = txId.getTransactionId();
                                             Instant txTime = txId.getTransactionTime();

                                             transactionMap.put("txinstance", txInstance);
                                             transactionMap.put("txinfo", txInfo);
                                             transactionMap.put("txtime", txTime);

                                             ArrayList addedVertexList = new ArrayList();
                                             // handle adds and removes for vertices
                                             // edges handled in context of vertices
                                             Set<TitanVertex> vertexSet = changeState.getVertices(kind);
                                             for (TitanVertex v : vertexSet)
                                             {
                                                 HashMap aVertexMap = new HashMap<String, Object>();

                                                 aVertexMap.put("id", v.id());
                                                 aVertexMap.put("label", v.label());

                                                 Iterator vertProps = v.properties();
                                                 while (vertProps.hasNext())
                                                 {
                                                     VertexProperty vp = (VertexProperty) vertProps.next();
                                                     aVertexMap.put(vp.key(), vp.value());
                                                 }

                                                 ArrayList addedEdgeList = new ArrayList();
                                                 Iterable<TitanEdge> aEdgeItr = changeState.getEdges(v, Change.ADDED, Direction.OUT);
                                                 for (Iterator<TitanEdge> anEdge = aEdgeItr.iterator(); anEdge.hasNext(); )
                                                 {
                                                     HashMap anAddedEdge = new HashMap<String, Object>();

                                                     TitanEdge edge = anEdge.next();
                                                     // edge.properties puts out an iterable
                                                     anAddedEdge.put("edgeid", edge.id());
                                                     EdgeLabel el = edge.edgeLabel();
                                                     anAddedEdge.put("edgelabel", el.toString());

                                                     Iterator propsItr = edge.properties();
                                                     while (propsItr.hasNext())
                                                     {
                                                         SimpleTitanProperty ep = (SimpleTitanProperty) propsItr.next();
                                                         anAddedEdge.put(ep.propertyKey(), ep.value());
                                                     }
                                                     addedEdgeList.add(anAddedEdge);
                                                 }
                                                 aVertexMap.put("addededges", addedEdgeList);

                                                 ArrayList removedEdgeList = new ArrayList();
                                                 Iterable<TitanEdge> rEdgeItr = changeState.getEdges(v, Change.REMOVED, Direction.OUT);
                                                 for (Iterator<TitanEdge> anEdge = rEdgeItr.iterator(); anEdge.hasNext(); )
                                                 {
                                                     HashMap aRemovedEdge = new HashMap<String, Object>();
                                                     TitanEdge edge = anEdge.next();

                                                     // edge.properties puts out an interable
                                                     aRemovedEdge.put("edgeid", edge.id());
                                                     aRemovedEdge.put("edgelabel", edge.edgeLabel());

                                                     Iterator propsItr = edge.properties();
                                                     while (propsItr.hasNext())
                                                     {
                                                         SimpleTitanProperty ep = (SimpleTitanProperty) propsItr.next();
                                                         aRemovedEdge.put(ep.propertyKey(), ep.value());
                                                     }
                                                     removedEdgeList.add(aRemovedEdge);
                                                 }
                                                 aVertexMap.put("removedEdges", removedEdgeList);
                                                 addedVertexList.add(aVertexMap);
                                             }

                                             if (kind.compareTo(Change.ADDED) == 0)
                                                 transactionMap.put("added", addedVertexList);

                                             if (kind.compareTo(Change.REMOVED) == 0)
                                                 transactionMap.put("removed", addedVertexList);
                                         }
                                     }

                                     // System.out.println("checkpoint 4");
                                     // convert the hashmap(s) to the JSON string
                                     // we push to Kafka topic
                                     // catching all exceptions for now
                                     try
                                     {
                                         String transmissionString = JSONObject.toJSONString(transactionMap);
                                         System.out.println("published: " + transmissionString);
                                         publish(transmissionString);
                                     } catch (Exception e)
                                     {
                                         e.printStackTrace();
                                     }
                                 }
                             }
                ).build();
    }

    /**
     * Currently, no serialization.  Publishes clear text
     * JSON string
     *
     * @param transmissionString String of JSON to publish
     */
    public void publish ( final String transmissionString)
    {
        final String methodName = "publish";
            try
            {
                if (transmissionString.trim().length() > 0)
                {
                    String pKey = "default";
                    byte[] partitionKey = pKey.getBytes();
                    byte[] msg = transmissionString.getBytes();
                    KeyedMessage<byte[], byte[]> data = new KeyedMessage<byte[], byte[]>(topic, partitionKey, msg);

                    try
                    {
                        producer.send(data);
                    }
                    catch (QueueFullException qfe)
                    {
                        try
                        {
                            // let consumers do their thing
                            // no fancy backoff right now
                            Thread.sleep(10L);
                        }
                        catch (Exception nocare)
                        {
                            nocare.printStackTrace();
                        }
                        // retry this message
                        producer.send(data);
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
    }


    /**
     * Nothing sets finishRunning to true.
     * This is how we listen and publish
     * forever on the call back thread,
     * while main thread here waits.
     */
    public synchronized void waitForever()
    {
        final String methodName = "waitForever";

        while (!finishRunning)
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                // don't care
            }
        }
    }

    public void closeGraph()
    {
        final String methodName = "closeGraph";

        if (graph != null)
            graph.close();
    }

    /**
     * Called whenever the application is stopping, to allow handlers to clean up.
     */
    public void destroy()
    {
        final String methodName = "destroy";

        if (producer != null)
        {
            producer.close();
        }
        producer = null;
    }


    /**
     *
     * @param args  program takes two arguments: full path to properties file
     *              and name of the kafka topic
     */
    public static void main(String[] args)
    {
        final String methodName = "main";

        // long startTime = System.currentTimeMillis();
        // System.out.println("Start time: " + startTime);

        if (args != null && args.length < 2)
        {
            System.err.println("Usage: TitanKafkaProducer producer.properties topic");
            System.exit(1);
        }

        TitanKafkaProducer prod = new TitanKafkaProducer(args[0], args[1]);
        if (state == 1)
        {
            // load is where the listener on Titan is registered and where the
            // publishing to the Kafka broker takes place
            prod.registerListenPublish();

            try
            {
                // basically, wait forever.
                prod.waitForever();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                prod.closeGraph();
                prod.destroy();
            }
        }
        else
        {
            System.out.println("Failed to create producer.  TitanKafkaProducer ending.");
        }

        // long endTime = System.currentTimeMillis();
        // System.out.println("End time: " + endTime);
    }
}
