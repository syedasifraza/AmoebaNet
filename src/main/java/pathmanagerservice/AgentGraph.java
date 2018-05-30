package pathmanagerservice;

import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AgentGraph {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Map<String, Vertex> graph; // mapping of vertex names to Vertex objects, built from a set of Edges
    public static Set<DeviceId> tempMap = new LinkedHashSet<>();
    public static double cost;
    //public Map<Collection<String>, Double> dvcWithCost = new HashMap<>();

    /* One edge of the graph (only used by Graph constructor) */
    public static class Edge {
        private final Logger log = LoggerFactory.getLogger(getClass());
        public final String v1, v2;
        public final long dist;
        public Edge(String v1, String v2, int dist) {
            this.v1 = v1;
            this.v2 = v2;
            this.dist = dist;
        }
    }

    /* One vertex of the graph, complete with mappings to neighbouring vertices */
    public static class Vertex implements Comparable<Vertex> {
        private final Logger log = LoggerFactory.getLogger(getClass());
        public final String name;
        public int dist = Integer.MIN_VALUE; // MAX_VALUE assumed to be infinity
        public Vertex previous = null;
        public final Map<Vertex, Integer> neighbours = new HashMap();

        public Vertex(String name) {
            this.name = name;
        }

        private void printPath() {
            if (this == this.previous) {
                log.info("devices {}", this.name);
                tempMap.add(DeviceId.deviceId(this.name));
            } else if (this.previous == null) {
                log.info("not reached {}(unreached)", this.name);
            } else {
                this.previous.printPath();
                log.info("from to -> {}({})", this.name, this.dist);
                tempMap.add(DeviceId.deviceId(this.name));
                cost = this.dist;
            }
            log.info("Collection {}", tempMap);
        }

        public int compareTo(Vertex other) {
            if (dist == other.dist) {
                return name.compareTo(other.name);
            }

            return Integer.compare(dist, other.dist);
        }

        @Override public String toString() {
            return "(" + name + ", " + dist + ")";
        }
    }

    /* Builds a graph from a set of edges */
    public AgentGraph(Edge[] edges) {
        graph = new HashMap(edges.length);
        // graph hashmap size willbe 9

        //one pass to find all vertices
        for (Edge e : edges) {
            if (!graph.containsKey(e.v1)) {
                graph.put(e.v1, new Vertex(e.v1)); // map vertex key with vertex obj
            }
            if (!graph.containsKey(e.v2)) {
                graph.put(e.v2, new Vertex(e.v2));
            }
        }

        //another pass to set neighbouring vertices
        for (Edge e : edges) {
            graph.get(e.v1).neighbours.put(graph.get(e.v2), (int) e.dist);
            //graph.get(e.v2).neighbours.put(graph.get(e.v1), e.dist); // also do this for an undirected graph
            //System.out.println("Q :" + graph.get(e.v1).neighbours);
        }
    }

    /* Runs dijkstra using a specified source vertex */
    public void dijkstra(String startName) {
        if (!graph.containsKey(startName)) {
            log.info("Graph doesn't contain start vertex \"{}\"\n", startName);
            return;
        }
        final Vertex source = graph.get(startName);
        NavigableSet<Vertex> q = new TreeSet();


        // set-up vertices
        for (Vertex v : graph.values()) {
            v.previous = v == source ? source : null;
            // if v == source then v.previous  = source else v.previous = null
            v.dist = v == source ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            q.add(v);
        }

        //System.out.println("Q :" + q);
        dijkstra(q);
    }

    /* Implementation of dijkstra's algorithm using a binary heap. */
    private void dijkstra(final NavigableSet<Vertex> q) {
        Vertex u, v;
        while (!q.isEmpty()) {

            u = q.pollLast();
            // /u = q.pollFirst(); // vertex with shortest distance (first iteration will return source)

            if (u.dist == Integer.MIN_VALUE) {
                break; // we can ignore u (and any other remaining vertices) since they are unreachable
            }
            //System.out.println("U Vertex :" + u.neighbours.entrySet());
            //System.out.println("Q :" + q);

            //look at distances to each neighbour
            for (Map.Entry<Vertex, Integer> a : u.neighbours.entrySet()) {
                v = a.getKey(); //the neighbour in this iteration
                //System.out.println("a Value :" + a.getValue());
                //System.out.println("V dist :" + v);
                //System.out.println("Q :" + q);
                //System.out.println("U :" + u);

                final int alternateDist = Math.min(u.dist, a.getValue());


                if (alternateDist > v.dist) { // shorter path to neighbour found
                    q.remove(v);
                    v.dist = alternateDist;
                    v.previous = u;
                    q.add(v);
                }

                //System.out.println("distance :" + a.getKey().dist);
                //System.out.println("V previous :" + v.previous);
            }
        }
    }

    /* Prints a path from the source to the specified vertex */
    public void printPath(String endName) {
        if (!graph.containsKey(endName)) {
            log.info("Graph doesn't contain end vertex \"{}\"\n", endName);
            return;
        }

        graph.get(endName).printPath();

        //System.out.println();
    }
    /* Prints the path from the source to every vertex (output order is not guaranteed) */
    public Map<Set<DeviceId>, Double> getDvcInPath() {
        Map<Set<DeviceId>, Double> dvcWithCost = new HashMap<>();
        tempMap.iterator().forEachRemaining(n -> {
            log.info("print collection {}", n);
        });
        dvcWithCost.put(tempMap, cost);
        //return tempMap;
        log.info("tempMap {}", tempMap);
        return dvcWithCost;
    }

    public void cleanPath() {
        tempMap.clear();
    }


    public void printAllPaths() {
        for (Vertex v : graph.values()) {
            v.printPath();
            System.out.println();
        }
    }
}
