package org.apache.titan.kafka;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;

/**
 * Created by robinson on 10/5/15.
 */
public class Driver
{
    public static void main(String args[])
    {
        Driver dr = new Driver();
        dr.doWork();
    }

    private void doWork()
    {
        TitanGraph graph = TitanFactory.build().set("storage.backend", "inmemory").open();

        // do "auto" transaction work
        graph.addVertex("type", "human");
        graph.tx().commit();

        // do "named" transaction work
//        TitanTransaction tx = graph.buildTransaction().logIdentifier("david1").start();
//        Vertex v = tx.addVertex("type", "dog");
//        Long id = (Long)v.id();
//        tx.commit();

//        GraphTraversalSource g = graph.traversal();
//        Vertex v1 = g.V(id.longValue()).next();
//        v1.remove();

        graph.close();
    }
}
