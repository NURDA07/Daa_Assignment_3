package experiments;

import graph.Graph;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class GraphGeneration {
    public static Graph connectedUndirected(int vertices, int edges, long seed) {
        validate(vertices, edges, false);
        if (edges < vertices - 1) {
            throw new IllegalArgumentException("A connected graph needs at least V - 1 edges");
        }
        Graph graph = new Graph(vertices, false);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        for (int v = 1; v < vertices; v++) {
            add(graph, v, random.nextInt(v), random, used);
        }
        fill(graph, edges, random, used, false);
        return graph;
    }

    public static Graph dag(int vertices, int edges, long seed) {
        validate(vertices, edges, false);
        Graph graph = new Graph(vertices, true);
        fill(graph, edges, new Random(seed), new HashSet<>(), true);
        return graph;
    }

    public static Graph cyclicDirected(int vertices, int edges, long seed) {
        validate(vertices, edges, true);
        if (vertices < 2 || edges < vertices) {
            throw new IllegalArgumentException("This generator needs V >= 2 and E >= V");
        }
        Graph graph = new Graph(vertices, true);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        int core = Math.max(2, vertices / 2);
        long maximum = (long) vertices * (vertices - 1) / 2 + (long) core * (core - 1) / 2;
        if (edges > maximum) {
            throw new IllegalArgumentException("Too many edges for this cyclic graph model");
        }
        for (int v = 0; v < core; v++) {
            add(graph, v, (v + 1) % core, random, used);
        }
        for (int v = core; v < vertices; v++) {
            add(graph, v - 1, v, random, used);
        }
        while (graph.getEdges().size() < edges) {
            int a = random.nextInt(vertices);
            int b = random.nextInt(vertices);
            if (a != b && (a < b || (a < core && b < core))) {
                add(graph, a, b, random, used);
            }
        }
        return graph;
    }

    private static void fill(Graph graph, int target, Random random, Set<Long> used, boolean dag) {
        while (graph.getEdges().size() < target) {
            int a = random.nextInt(graph.getVertexCount());
            int b = random.nextInt(graph.getVertexCount());
            if (a == b) {
                continue;
            }
            if (!graph.isDirected() || dag) {
                int min = Math.min(a, b);
                b = Math.max(a, b);
                a = min;
            }
            add(graph, a, b, random, used);
        }
    }

    private static void add(Graph graph, int a, int b, Random random, Set<Long> used) {
        int keyA = graph.isDirected() ? a : Math.min(a, b);
        int keyB = graph.isDirected() ? b : Math.max(a, b);
        long key = ((long) keyA << 32) | (keyB & 0xffffffffL);
        if (used.add(key)) {
            graph.addEdge(a, b, 1 + random.nextInt(100));
        }
    }

    private static void validate(int vertices, int edges, boolean directed) {
        long maximum = (long) vertices * (vertices - 1) / (directed ? 1 : 2);
        if (vertices < 1 || edges < 0 || edges > maximum) {
            throw new IllegalArgumentException("Invalid graph size");
        }
    }
}
