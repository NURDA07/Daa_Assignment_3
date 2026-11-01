import coonect.Kosaraju;
import coonect.TopoSort;
import dsu.DSU;
import experiments.GraphGeneration;
import experiments.Metrics;
import graph.Edge;
import graph.Graph;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import shortestpath.Dijkstra;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GraphTest {
    private static int checks;

    public static void main(String[] args) {
        testGraphAndDsu();
        testMstExamples();
        testDirectedExamples();
        testShortestPaths();
        testGenerators();
        testRandomGraphs();
        System.out.println("PASS: " + checks + " correctness checks");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void rejects(Runnable operation, String message) {
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            check(true, message);
            return;
        }
        check(false, message);
    }

    private static void testGraphAndDsu() {
        Graph graph = new Graph(3, false);
        graph.addEdge(0, 1, 4);
        check(graph.getEdges().size() == 1, "Undirected edge stored once globally");
        check(graph.getNeighbors(1).get(0).to == 0, "Reverse adjacency exists");
        check(graph.getNeighbors(2).isEmpty(), "Isolated vertex exists");
        try {
            graph.getEdges().clear();
            check(false, "Graph edges must be read-only");
        } catch (UnsupportedOperationException expected) {
            check(true, "Graph edges are read-only");
        }
        rejects(() -> graph.addEdge(-1, 0, 1), "Negative vertex rejected");
        rejects(() -> graph.addEdge(0, 3, 1), "Out-of-range vertex rejected");
        rejects(() -> new Graph(0, false), "Graph requires at least one vertex");
        Graph directed = new Graph(2, true);
        directed.addEdge(0, 1, 1);
        check(directed.getNeighbors(1).isEmpty(), "Directed edge has no reverse adjacency");

        DSU dsu = new DSU(6);
        check(dsu.union(0, 1), "First union");
        check(dsu.union(2, 3), "Second group");
        check(dsu.union(2, 4), "Larger group");
        check(dsu.union(0, 2), "Smaller group joins larger group");
        check(dsu.find(1) == dsu.find(4), "Transitive DSU membership");
        check(dsu.find(5) != dsu.find(0), "Separate DSU group");
        check(!dsu.union(1, 4), "Redundant union returns false");
        rejects(() -> dsu.find(6), "Invalid DSU vertex rejected");
        rejects(() -> new DSU(-1), "Negative DSU size rejected");
        Metrics metrics = new Metrics();
        DSU balanced = new DSU(8, metrics);
        balanced.union(0, 1);
        balanced.union(2, 3);
        balanced.union(0, 2);
        balanced.union(4, 5);
        balanced.union(6, 7);
        balanced.union(4, 6);
        balanced.union(0, 4);
        long before = metrics.parentTraversals;
        balanced.find(7);
        long first = metrics.parentTraversals - before;
        before = metrics.parentTraversals;
        balanced.find(7);
        check(metrics.parentTraversals - before < first, "Path compression reduces later traversal");
    }

    private static void testMstExamples() {
        Graph triangle = new Graph(3, false);
        triangle.addEdge(0, 1, 4);
        triangle.addEdge(1, 2, 2);
        triangle.addEdge(0, 2, 9);
        verifyMst(triangle, 6, true);
        Graph tied = new Graph(4, false);
        tied.addEdge(0, 1, 1);
        tied.addEdge(1, 2, 1);
        tied.addEdge(2, 3, 1);
        tied.addEdge(3, 0, 1);
        verifyMst(tied, 3, true);
        Graph disconnected = new Graph(4, false);
        disconnected.addEdge(0, 1, 2);
        disconnected.addEdge(2, 3, 3);
        verifyMst(disconnected, 5, false);
        verifyMst(new Graph(1, false), 0, true);
        verifyMst(new Graph(3, false), 0, false);
        Graph special = new Graph(3, false);
        special.addEdge(0, 0, -100);
        special.addEdge(0, 1, 8);
        special.addEdge(0, 1, -2);
        special.addEdge(1, 2, 0);
        verifyMst(special, -2, true);
        Graph largeWeights = new Graph(3, false);
        largeWeights.addEdge(0, 1, Integer.MAX_VALUE);
        largeWeights.addEdge(1, 2, Integer.MAX_VALUE);
        verifyMst(largeWeights, 2L * Integer.MAX_VALUE, true);
        rejects(() -> Kruskal.compute(new Graph(2, true)), "Kruskal rejects directed input");
        rejects(() -> Prim.compute(new Graph(2, true)), "Prim rejects directed input");
    }

    private static void verifyMst(Graph graph, long weight, boolean connected) {
        for (MSTResult result : List.of(Kruskal.compute(graph), Prim.compute(graph))) {
            check(result.totalWeight() == weight, "Expected MST/forest weight " + weight);
            check(result.connected() == connected, "Connectivity flag");
            DSU selected = new DSU(graph.getVertexCount());
            long sum = 0;
            for (Edge edge : result.edges()) {
                check(selected.union(edge.from, edge.to), "Selected forest has no cycle");
                sum += edge.weight;
            }
            check(sum == result.totalWeight(), "Selected weights sum correctly");
            for (Edge edge : graph.getEdges()) {
                check(selected.find(edge.from) == selected.find(edge.to), "Forest spans input components");
            }
            if (connected) {
                check(result.edges().size() == graph.getVertexCount() - 1, "MST has V-1 edges");
            }
        }
    }

    private static void testDirectedExamples() {
        Graph dag = new Graph(5, true);
        dag.addEdge(0, 1, 1);
        dag.addEdge(0, 2, 1);
        dag.addEdge(1, 3, 1);
        dag.addEdge(2, 3, 1);
        checkTopo(dag);
        checkScc(dag);
        Graph cycle = new Graph(5, true);
        cycle.addEdge(0, 1, 1);
        cycle.addEdge(1, 2, 1);
        cycle.addEdge(2, 0, 1);
        cycle.addEdge(2, 3, 1);
        cycle.addEdge(3, 4, 1);
        cycle.addEdge(4, 3, 1);
        check(TopoSort.compute(cycle).hasCycle(), "Directed cycle detected");
        Set<Set<Integer>> actual = new HashSet<>();
        for (List<Integer> group : Kosaraju.compute(cycle)) {
            actual.add(new HashSet<>(group));
        }
        check(actual.equals(Set.of(Set.of(0, 1, 2), Set.of(3, 4))), "Known SCC partition");
        checkScc(cycle);
        Graph self = new Graph(1, true);
        self.addEdge(0, 0, 0);
        check(TopoSort.compute(self).hasCycle(), "Self-loop is a cycle");
        checkScc(self);
        Graph single = new Graph(1, true);
        checkTopo(single);
        checkScc(single);
        rejects(() -> TopoSort.compute(new Graph(2, false)), "Topo rejects undirected input");
        rejects(() -> Kosaraju.compute(new Graph(2, false)), "SCC rejects undirected input");
    }

    private static void checkTopo(Graph graph) {
        TopoSort.Result result = TopoSort.compute(graph);
        check(!result.hasCycle(), "DAG accepted");
        check(result.order().size() == graph.getVertexCount(), "Topo includes all vertices");
        int[] position = new int[graph.getVertexCount()];
        Arrays.fill(position, -1);
        for (int i = 0; i < result.order().size(); i++) {
            int v = result.order().get(i);
            check(position[v] == -1, "Topo has no duplicates");
            position[v] = i;
        }
        for (Edge edge : graph.getEdges()) {
            check(position[edge.from] < position[edge.to], "Every edge respects topological order");
        }
    }

    private static void checkScc(Graph graph) {
        int n = graph.getVertexCount();
        boolean[][] reach = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            reach[i][i] = true;
        }
        for (Edge edge : graph.getEdges()) {
            reach[edge.from][edge.to] = true;
        }
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    reach[i][j] |= reach[i][k] && reach[k][j];
                }
            }
        }
        int[] component = new int[n];
        Arrays.fill(component, -1);
        List<List<Integer>> groups = Kosaraju.compute(graph);
        for (int c = 0; c < groups.size(); c++) {
            for (int v : groups.get(c)) {
                check(component[v] == -1, "Vertex belongs to only one SCC");
                component[v] = c;
            }
        }
        for (int i = 0; i < n; i++) {
            check(component[i] != -1, "Every vertex belongs to an SCC");
            for (int j = 0; j < n; j++) {
                check((component[i] == component[j]) == (reach[i][j] && reach[j][i]),
                        "SCC agrees with independent reachability");
            }
        }
    }

    private static void testShortestPaths() {
        Graph graph = new Graph(5, true);
        graph.addEdge(0, 1, 10);
        graph.addEdge(0, 2, 1);
        graph.addEdge(2, 1, 2);
        graph.addEdge(1, 3, 0);
        Dijkstra.Result result = Dijkstra.compute(graph, 0);
        check(result.distanceTo(3) == 3, "Shortest distance with stale queue entry");
        check(result.pathTo(3).equals(List.of(0, 2, 1, 3)), "Shortest path reconstruction");
        check(result.distanceTo(4) == Dijkstra.INF, "Unreachable distance");
        check(result.pathTo(4).isEmpty(), "Unreachable path is empty");
        check(result.pathTo(0).equals(List.of(0)), "Source path");
        check(Dijkstra.compute(new Graph(1, true), 0).distanceTo(0) == 0, "Singleton distance");
        Graph large = new Graph(3, true);
        large.addEdge(0, 1, Integer.MAX_VALUE);
        large.addEdge(1, 2, Integer.MAX_VALUE);
        check(Dijkstra.compute(large, 0).distanceTo(2) == 2L * Integer.MAX_VALUE, "Long path sum");
        Graph negative = new Graph(3, true);
        negative.addEdge(1, 2, -1);
        rejects(() -> Dijkstra.compute(negative, 0), "Even unreachable negative edge rejected");
        rejects(() -> Dijkstra.compute(graph, 5), "Invalid source rejected");
        rejects(() -> result.pathTo(-1), "Invalid path destination rejected");
    }

    private static void testGenerators() {
        int[][] sizes = {{10, 30}, {100, 500}, {1000, 5000}};
        for (int[] size : sizes) {
            Graph graph = GraphGeneration.connectedUndirected(size[0], size[1], 42);
            Graph same = GraphGeneration.connectedUndirected(size[0], size[1], 42);
            check(graph.getEdges().size() == size[1], "Exact generated edge count");
            for (int i = 0; i < size[1]; i++) {
                Edge a = graph.getEdges().get(i);
                Edge b = same.getEdges().get(i);
                check(a.from == b.from && a.to == b.to && a.weight == b.weight, "Seed reproducibility");
            }
            check(Kruskal.compute(graph).connected(), "Generated MST graph connected");
            check(Kruskal.compute(graph).totalWeight() == Prim.compute(graph).totalWeight(), "Generated MST agreement");
            Graph dag = GraphGeneration.dag(size[0], size[1], 42);
            Graph cyclic = GraphGeneration.cyclicDirected(size[0], size[1], 42);
            check(dag.getEdges().size() == size[1], "DAG edge count");
            check(cyclic.getEdges().size() == size[1], "Cyclic edge count");
            checkTopo(dag);
            check(TopoSort.compute(cyclic).hasCycle(), "Generator guarantees cycle");
            check(Kosaraju.compute(cyclic).size() == size[0] - Math.max(2, size[0] / 2) + 1,
                    "Generated SCC count");
            checkNoDuplicateEdges(graph);
            checkNoDuplicateEdges(dag);
            checkNoDuplicateEdges(cyclic);
        }
        rejects(() -> GraphGeneration.dag(3, 4, 42), "Impossible DAG size rejected");
        rejects(() -> GraphGeneration.connectedUndirected(4, 2, 42), "Too few connected edges rejected");
        rejects(() -> GraphGeneration.cyclicDirected(3, 5, 42), "Impossible cyclic model rejected");
    }

    private static void checkNoDuplicateEdges(Graph graph) {
        Set<String> used = new HashSet<>();
        for (Edge edge : graph.getEdges()) {
            int a = graph.isDirected() ? edge.from : Math.min(edge.from, edge.to);
            int b = graph.isDirected() ? edge.to : Math.max(edge.from, edge.to);
            check(a != b && used.add(a + ":" + b), "No generated loops or duplicate edges");
        }
    }

    private static void testRandomGraphs() {
        Random random = new Random(2026);
        for (int trial = 0; trial < 30; trial++) {
            int n = 1 + random.nextInt(5);
            Graph undirected = new Graph(n, false);
            Graph directed = new Graph(n, true);
            for (int a = 0; a < n; a++) {
                for (int b = a + 1; b < n; b++) {
                    if (random.nextBoolean()) {
                        undirected.addEdge(a, b, random.nextInt(7));
                    }
                }
                for (int b = 0; b < n; b++) {
                    if (a != b && random.nextBoolean()) {
                        directed.addEdge(a, b, random.nextInt(7));
                    }
                }
            }
            MSTResult result = Kruskal.compute(undirected);
            verifyMst(undirected, bruteForestWeight(undirected), result.connected());
            checkScc(directed);
            boolean hasCycle = Kosaraju.compute(directed).stream().anyMatch(group -> group.size() > 1);
            check(TopoSort.compute(directed).hasCycle() == hasCycle, "Random cycle detection");
            if (!hasCycle) {
                checkTopo(directed);
            }
            checkDistances(directed);
            checkDistances(undirected);
        }
    }

    private static long bruteForestWeight(Graph graph) {
        List<Edge> edges = graph.getEdges();
        int n = graph.getVertexCount();
        long best = Long.MAX_VALUE;
        for (int mask = 0; mask < (1 << edges.size()); mask++) {
            int[] labels = new int[n];
            for (int i = 0; i < n; i++) {
                labels[i] = i;
            }
            long weight = 0;
            boolean valid = true;
            for (int i = 0; i < edges.size(); i++) {
                if ((mask & (1 << i)) == 0) {
                    continue;
                }
                Edge edge = edges.get(i);
                int from = labels[edge.from];
                int to = labels[edge.to];
                if (from == to) {
                    valid = false;
                    break;
                }
                for (int v = 0; v < n; v++) {
                    if (labels[v] == to) {
                        labels[v] = from;
                    }
                }
                weight += edge.weight;
            }
            for (Edge edge : edges) {
                valid &= labels[edge.from] == labels[edge.to];
            }
            if (valid) {
                best = Math.min(best, weight);
            }
        }
        return best;
    }

    private static void checkDistances(Graph graph) {
        int n = graph.getVertexCount();
        long infinity = Long.MAX_VALUE / 4;
        long[][] reference = new long[n][n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(reference[i], infinity);
            reference[i][i] = 0;
            for (Edge edge : graph.getNeighbors(i)) {
                reference[i][edge.to] = Math.min(reference[i][edge.to], edge.weight);
            }
        }
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    reference[i][j] = Math.min(reference[i][j], reference[i][k] + reference[k][j]);
                }
            }
        }
        for (int source = 0; source < n; source++) {
            Dijkstra.Result result = Dijkstra.compute(graph, source);
            for (int target = 0; target < n; target++) {
                long expected = reference[source][target] == infinity ? Dijkstra.INF : reference[source][target];
                check(result.distanceTo(target) == expected, "Dijkstra agrees with Floyd-Warshall");
                List<Integer> path = result.pathTo(target);
                if (expected == Dijkstra.INF) {
                    check(path.isEmpty(), "Unreachable random path");
                    continue;
                }
                check(path.get(0) == source && path.get(path.size() - 1) == target, "Path endpoints");
                long sum = 0;
                for (int i = 1; i < path.size(); i++) {
                    int previous = path.get(i - 1);
                    int next = path.get(i);
                    int smallest = Integer.MAX_VALUE;
                    for (Edge edge : graph.getNeighbors(previous)) {
                        if (edge.to == next) {
                            smallest = Math.min(smallest, edge.weight);
                        }
                    }
                    check(smallest != Integer.MAX_VALUE, "Path uses actual edges");
                    sum += smallest;
                }
                check(sum == expected, "Path weight equals distance");
            }
        }
    }
}
