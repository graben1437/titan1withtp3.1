package org.apache.titan.kafka;

import com.thinkaurelius.titan.core.*;
import com.thinkaurelius.titan.core.log.*;
import com.thinkaurelius.titan.graphdb.relations.SimpleTitanProperty;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.util.EntityUtils;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

/**
 * Created by robinson on 10/09/15.
 */
public class TitanHttpPostProducer
{
    private static Logger LOGGER = LoggerFactory.getLogger(TitanHttpPostProducer.class);
    private static int state = -1;

    private CloseableHttpClient client = null;
    private String apiURL = null;
    private String userId = null;
    private String password = null;

    private String titanConfigLocation = null;
    private boolean finishRunning = false;

    private TitanGraph graph = null;


    /**
     * Construtor
     *
     * @cfgProps configuration file containing ips, urls, etc.
     */
    public TitanHttpPostProducer(final String cfgProps)
    {
        // create the producer
        Properties props = new Properties();
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(cfgProps));
            props.load(br);
            titanConfigLocation = props.getProperty("titan.config.location");
            assert (titanConfigLocation != null);



            // set up some hard coded links for the URL we publish to
            client = HttpClients.createDefault();
            // curl -u 93df7adc-5dd9-446c-83f5-8d6d18c3b3bb:320f5ee2-90c9-430b-a2b4-312b25a8e6f7 "https://graphrestify.stage1.ng.bluemix.net/graphs/a15ccb5d-ce94-45dd-8638-02c82295bad7/g61089256"

            // replace the url shown here with the URL specfic to your graph instance from your JSON - labeled api URL
            apiURL = "http://ssa-nodered.sl.cloud9.ibm.com:1880";
            // replace the user id shown here with the user id provided for your graph instance
            // userId = "5d19cdab-561d-4220-9576-a9dea983f545";
            // replace the password shown here with the password provided for your graph instance
            // password = "3553eab2-023b-4bc4-8282-2d73dbe2bf55";

            state = 1;
        }
        catch (IOException ie)
        {
            LOGGER.error(ie.getMessage(), ie);
            System.out.println("failed to create producer");
            // failed
            state = 0;
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
    }



    /**
     * registers the listener method with Titan transaction logging
     * system
     */
    private void registerListenPublish()
    {

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

                                     System.out.println("checkpoint 1");
                                     for (Change kind : Change.values())
                                     {
                                         System.out.println("checkpoint 2");
                                         if ((kind.compareTo(Change.ADDED) == 0) || (kind.compareTo(Change.REMOVED) == 0))
                                         {
                                             System.out.println("checkpoint 3");
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
                                                     // edge.properties puts out an interable
                                                     anAddedEdge.put("edgeid", edge.id());
                                                     anAddedEdge.put("edgelabel", edge.edgeLabel());

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

                                     System.out.println("checkpoint 4");
                                     // convert the hashmap(s) to the JSON string
                                     // we push to Kafka topic
                                     // catching all exceptions for now
                                     try
                                     {
                                         String transmissionString = JSONObject.toJSONString(transactionMap);
                                         System.out.println("published: " + transmissionString);
                                         publish(transmissionString);
                                     }
                                     catch (Exception e)
                                     {
                                         e.printStackTrace();
                                     }
                                 }
                             }
                ).build();
    }


    /**
     * Posts JSON to a URL
     * @param transmissionString
     */
    private void publish(String transmissionString)
    {
        try
        {
            String iotype = "json";

            String fieldName = "json";
            String contentType = "application/json";
            // String fileName = "filename.graphson";

            // upload a GraphSON file
            HttpPost httpPost = new HttpPost(apiURL + "/titan");
            // byte[] userpass = (userId + ":" + password).getBytes();
            // byte[] encoding = Base64.encodeBase64(userpass);
            /// httpPost.setHeader("Authorization", "Basic " + new String(encoding));

            MultipartEntityBuilder meb = MultipartEntityBuilder.create();
            meb.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            meb.addPart("fieldName", new StringBody(fieldName));
            meb.addPart("contentType", new StringBody(contentType));
            // meb.addPart("name", new StringBody(fileName));
            meb.addTextBody("transaction", transmissionString);
            // FileBody fileBody = new FileBody(new File("./data/" + fileName));
            // meb.addPart(fieldName, fileBody);

            httpPost.setEntity(meb.build());

            HttpResponse httpResponse = client.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();

            // EntityUtils.consume(httpEntity);
            String responseString = EntityUtils.toString(httpEntity, "UTF-8");
            System.out.println(responseString);

        } catch (Exception e)
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
        if (graph != null)
            graph.close();
    }



    /**
     *
     * @param args  program takes two arguments: full path to properties file
     *              and name of the kafka topic
     */
    public static void main(String[] args)
    {
        long startTime = System.currentTimeMillis();
        System.out.println("Start time: " + startTime);

        if (args != null && args.length < 1)
        {
            System.err.println("Usage: TitanHttpPostProducer producer.properties");
            System.exit(1);
        }

        TitanHttpPostProducer thpp = new TitanHttpPostProducer(args[0]);
        if (state == 1)
        {
            // load is where the listener on Titan is registered and where the
            // publishing to the httpURL takes place
            thpp.registerListenPublish();

            try
            {
                // basically, wait forever.
                thpp.waitForever();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                thpp.closeGraph();
            }
        }
        else
        {
            System.out.println("Failed to create producer. TitanHttpPostProducer ending.");
        }

        long endTime = System.currentTimeMillis();
        System.out.println("End time: " + endTime);
    }
}
