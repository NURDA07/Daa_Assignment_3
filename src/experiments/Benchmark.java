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
        writeResults(rows, tables.resolve("results.md"));
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

    private static void writeResults(List<Row> rows, Path path) throws IOException {
        StringBuilder text = new StringBuilder("# Measured results\n\n");
        text.append("Times are means of five measured runs after ten warmups. Counters are per run.\n\n");
        text.append("| Algorithm | V | E | Mean (microseconds) | Edge examinations | Comparisons | Find calls | Union calls | Parent traversals | Queue adds | Queue polls | Relaxations | Vertex visits |\n");
        text.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (Row row : rows) {
            Metrics m = row.getMetrics();
            text.append(String.format(Locale.ROOT,
                    "| %s | %d | %d | %.3f | %d | %d | %d | %d | %d | %d | %d | %d | %d |%n",
                    row.getAlgorithm(), row.getVertices(), row.getEdges(), row.getAverageNs() / 1000,
                    m.edgeExaminations, m.comparisons, m.findCalls, m.unionCalls, m.parentTraversals,
                    m.queueAdds, m.queuePolls, m.relaxations, m.vertexVisits));
        }
        Files.writeString(path, text.toString());
        updateReadme(rows, text.toString());
    }

    private static void updateReadme(List<Row> rows, String table) throws IOException {
        Path readme = Path.of("README.md");
        if (!Files.exists(readme)) {
            return;
        }
        String original = Files.readString(readme);
        int start = original.indexOf("## Results\n");
        int end = original.indexOf("## Testing\n");
        if (start < 0 || end < start) {
            return;
        }
        StringBuilder replacement = new StringBuilder("## Results\n\n");
        replacement.append(table.substring(table.indexOf("\n\n") + 2));
        replacement.append("\nThe detailed data is in [raw.csv](results/tables/raw.csv) and [summary.csv](results/tables/summary.csv). Zero counters mean that type of operation was not recorded for the algorithm, not that no work was performed.\n\n");
        replacement.append("![Execution time vs vertices](results/plots/time-vs-vertices.svg)\n\n");
        replacement.append("![Execution time vs edges](results/plots/time-vs-edges.svg)\n\n");
        replacement.append("Both plots use the same three configurations, so V and E change together. They do not isolate the causal effect of one variable. All algorithms are shown on linear axes; very short runtimes appear near the baseline.\n\n");
        replacement.append("## Comparison and Discussion\n\n");
        for (int v : new int[]{10, 100, 1000}) {
            Row kruskal = findRow(rows, "Kruskal", v);
            Row prim = findRow(rows, "Prim", v);
            replacement.append(String.format(Locale.ROOT,
                    "- At V=%d, Kruskal averaged %.3f microseconds and Prim %.3f microseconds; %s was faster in this run. Kruskal recorded %d sort comparisons and %d union calls; Prim recorded %d heap comparisons and %d queue polls.%n",
                    v, kruskal.getAverageNs() / 1000, prim.getAverageNs() / 1000,
                    kruskal.getAverageNs() <= prim.getAverageNs() ? "Kruskal" : "Prim",
                    kruskal.getMetrics().comparisons, kruskal.getMetrics().unionCalls,
                    prim.getMetrics().comparisons, prim.getMetrics().queuePolls));
        }
        replacement.append("\nKruskal sorts all E edges but can stop its DSU scan early. Prim avoids global sorting but maintains and drains a lazy heap, including obsolete candidate edges. Comparator counts are not the entire cost: allocations, list copies, DSU work, and heap maintenance also matter. A winner on these inputs is not a universal winner. DSU path compression and union by size keep root searches short; the traversal counter records actual link-following work, but no unoptimized DSU timing experiment is claimed.\n\n");
        Row sccSmall = findRow(rows, "Kosaraju", 10);
        Row sccLarge = findRow(rows, "Kosaraju", 1000);
        replacement.append(String.format(Locale.ROOT,
                "Kosaraju averaged %.3f microseconds for the small input and %.3f for the large input. Its edge examinations are exactly 3E (first traversal, transpose construction, second traversal) and vertex visits are exactly 2V. These counts agree with O(V+E), even when elapsed-time ratios differ because of JVM and allocation effects.%n%n",
                sccSmall.getAverageNs() / 1000, sccLarge.getAverageNs() / 1000));
        replacement.append("Topological sorting on the DAG examines 2E edges and removes all V vertices. On the cyclic input it examines E edges for indegrees, but removes no vertices because the ring and forward chain give every vertex positive indegree. Its smaller measured cost is a property of this input, not a faster asymptotic bound for all cyclic graphs.\n\n");
        Row dijkstraSmall = findRow(rows, "Dijkstra", 10);
        Row dijkstraLarge = findRow(rows, "Dijkstra", 1000);
        replacement.append(String.format(Locale.ROOT,
                "Dijkstra averaged %.3f microseconds on the small graph and %.3f on the large graph. It examines E edges to validate weights plus 2E adjacency entries because every vertex in the connected undirected graph is reachable. Successful relaxations and stale heap records depend on the weights and graph topology, explaining changes in heap comparisons and polls.%n%n",
                dijkstraSmall.getAverageNs() / 1000, dijkstraLarge.getAverageNs() / 1000));
        replacement.append("The fixed seed makes the inputs and operation counts reproducible, not wall-clock durations. Five measured runs, ten warmups, shared-JVM execution, and counters enabled during timing limit statistical conclusions. Use raw runs to inspect variability. No exact proportional growth or statistically significant speed difference is claimed.\n\n");
        Files.writeString(readme, original.substring(0, start) + replacement + original.substring(end));
    }

    private static Row findRow(List<Row> rows, String algorithm, int vertices) {
        return rows.stream().filter(row -> row.getAlgorithm().equals(algorithm)
                && row.getVertices() == vertices).findFirst().orElseThrow();
    }
}
