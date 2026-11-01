package experiments;

import coonect.Kosaraju;
import coonect.TopoSort;
import graph.Graph;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import shortestpath.Dijkstra;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Benchmark {
    private static final int REPEATS = 5;
    private static final int WARMUPS = 10;
    private static final long SEED = 42;
    private static volatile long sink;

    @FunctionalInterface
    private interface Experiment {
        long run(Metrics metrics);
    }

    public static class Row {
        private final String algorithm;
        private final String graphType;
        private final int vertices;
        private final int edges;
        private final double averageNs;
        private final Metrics metrics;

        public Row(String algorithm, String graphType, int vertices, int edges,
                   double averageNs, Metrics metrics) {
            this.algorithm = algorithm;
            this.graphType = graphType;
            this.vertices = vertices;
            this.edges = edges;
            this.averageNs = averageNs;
            this.metrics = metrics;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public String getGraphType() {
            return graphType;
        }

        public int getVertices() {
            return vertices;
        }

        public int getEdges() {
            return edges;
        }

        public double getAverageNs() {
            return averageNs;
        }

        public Metrics getMetrics() {
            return metrics;
        }
    }

    public static void main(String[] args) throws IOException {
        Path tables = Path.of("results", "tables");
        Path plots = Path.of("results", "plots");
        Files.createDirectories(tables);
        Files.createDirectories(plots);
        List<String> raw = new ArrayList<>();
        raw.add("algorithm,graph_type,vertices,edges,repeat,time_ns," + Metrics.csvHeader());
        List<Row> rows = new ArrayList<>();
        int[][] sizes = {{10, 30}, {100, 500}, {1000, 5000}};
        for (int[] size : sizes) {
            int v = size[0];
            int e = size[1];
            Graph undirected = GraphGeneration.connectedUndirected(v, e, SEED);
            Graph dag = GraphGeneration.dag(v, e, SEED);
            Graph cyclic = GraphGeneration.cyclicDirected(v, e, SEED);
            long expectedMst = Kruskal.compute(undirected).getTotalWeight();
            if (Prim.compute(undirected).getTotalWeight() != expectedMst
                    || TopoSort.compute(dag).hasCycle() || !TopoSort.compute(cyclic).hasCycle()) {
                throw new IllegalStateException("Generated graph validation failed");
            }
            measure("Kruskal", "connected-undirected", v, e, metrics -> {
                MSTResult result = Kruskal.compute(undirected, metrics);
                if (!result.isConnected() || result.getTotalWeight() != expectedMst) {
                    throw new IllegalStateException("Incorrect Kruskal result");
                }
                return result.getTotalWeight();
            }, rows, raw);
            measure("Prim", "connected-undirected", v, e, metrics -> {
                MSTResult result = Prim.compute(undirected, metrics);
                if (!result.isConnected() || result.getTotalWeight() != expectedMst) {
                    throw new IllegalStateException("Incorrect Prim result");
                }
                return result.getTotalWeight();
            }, rows, raw);
            measure("Kosaraju", "cyclic-directed", v, e,
                    metrics -> Kosaraju.compute(cyclic, metrics).size(), rows, raw);
            measure("Topo-DAG", "dag", v, e,
                    metrics -> TopoSort.compute(dag, metrics).getOrder().size(), rows, raw);
            measure("Topo-cycle", "cyclic-directed", v, e,
                    metrics -> TopoSort.compute(cyclic, metrics).hasCycle() ? 1 : 0, rows, raw);
            measure("Dijkstra", "connected-undirected", v, e,
                    metrics -> Dijkstra.compute(undirected, 0, metrics).distanceTo(v - 1), rows, raw);
        }
        Files.write(tables.resolve("raw.csv"), raw);
        List<String> summary = new ArrayList<>();
        summary.add("algorithm,graph_type,vertices,edges,average_time_ns," + Metrics.csvHeader());
        for (Row row : rows) {
            summary.add(String.format(Locale.ROOT, "%s,%s,%d,%d,%.2f,%s", row.getAlgorithm(),
                    row.getGraphType(), row.getVertices(), row.getEdges(), row.getAverageNs(), row.getMetrics().csvValues()));
            System.out.printf(Locale.ROOT, "%-12s V=%4d E=%4d average=%10.2f ns%n",
                    row.getAlgorithm(), row.getVertices(), row.getEdges(), row.getAverageNs());
        }
        Files.write(tables.resolve("summary.csv"), summary);
        Files.writeString(tables.resolve("environment.txt"),
                "Timestamp UTC: " + Instant.now() + "\nJava: " + System.getProperty("java.version")
                + "\nVM: " + System.getProperty("java.vm.name")
                + "\nOS: " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                + "\nArchitecture: " + System.getProperty("os.arch")
                + "\nAvailable processors: " + Runtime.getRuntime().availableProcessors()
                + "\nSeed per graph: " + SEED + "\nWarmups: " + WARMUPS + "\nMeasured repeats: " + REPEATS
                + "\nOperation counters are enabled during timing.\n");
        PlotWriter.write(rows, plots.resolve("time-vs-vertices.svg"), true);
        PlotWriter.write(rows, plots.resolve("time-vs-edges.svg"), false);
        System.out.println("Saved results/tables and results/plots");
        System.out.println("Consumed result: " + sink);
    }

    private static void measure(String algorithm, String type, int v, int e, Experiment experiment,
                                List<Row> rows, List<String> raw) {
        for (int i = 0; i < WARMUPS; i++) {
            sink = experiment.run(new Metrics());
        }
        long total = 0;
        String expectedCounters = null;
        Metrics last = null;
        for (int i = 1; i <= REPEATS; i++) {
            Metrics metrics = new Metrics();
            long start = System.nanoTime();
            long answer = experiment.run(metrics);
            long elapsed = System.nanoTime() - start;
            sink = answer;
            total += elapsed;
            if (expectedCounters != null && !expectedCounters.equals(metrics.csvValues())) {
                throw new IllegalStateException("Operation counts changed across repeated input");
            }
            expectedCounters = metrics.csvValues();
            raw.add(algorithm + "," + type + "," + v + "," + e + "," + i + "," + elapsed
                    + "," + metrics.csvValues());
            last = metrics;
        }
        rows.add(new Row(algorithm, type, v, e, total / (double) REPEATS, last));
    }
}
