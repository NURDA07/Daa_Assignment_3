package mst;

import dsu.DSU;
import experiments.Metrics;
import graph.Edge;
import graph.Graph;
import java.util.ArrayList;
import java.util.List;

public class Kruskal {
    public static MSTResult compute(Graph graph) {
        return compute(graph, new Metrics());
    }

    public static MSTResult compute(Graph graph, Metrics metrics) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("MST requires an undirected graph");
        }
        List<Edge> sorted = new ArrayList<>(graph.getEdges());
        sorted.sort((a, b) -> {
            metrics.comparisons++;
            return Integer.compare(a.weight, b.weight);
        });
        DSU groups = new DSU(graph.getVertexCount(), metrics);
        List<Edge> chosen = new ArrayList<>();
        long total = 0;
        for (Edge edge : sorted) {
            metrics.edgeExaminations++;
            if (groups.union(edge.from, edge.to)) {
                chosen.add(edge);
                total += edge.weight;
                if (chosen.size() == graph.getVertexCount() - 1) {
                    break;
                }
            }
        }
        return new MSTResult(chosen, total, chosen.size() == graph.getVertexCount() - 1);
    }
}
