package mst;

import experiments.Metrics;
import graph.Edge;
import graph.Graph;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class Prim {
    public static MSTResult compute(Graph graph) {
        return compute(graph, new Metrics());
    }

    public static MSTResult compute(Graph graph, Metrics metrics) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("MST requires an undirected graph");
        }
        boolean[] visited = new boolean[graph.getVertexCount()];
        PriorityQueue<Edge> queue = new PriorityQueue<>((a, b) -> {
            metrics.comparisons++;
            return Integer.compare(a.weight, b.weight);
        });
        List<Edge> chosen = new ArrayList<>();
        long total = 0;
        int components = 0;
        for (int start = 0; start < visited.length; start++) {
            if (visited[start]) {
                continue;
            }
            components++;
            visit(graph, start, visited, queue, metrics);
            while (!queue.isEmpty()) {
                Edge edge = queue.poll();
                metrics.queuePolls++;
                if (visited[edge.to]) {
                    continue;
                }
                chosen.add(edge);
                total += edge.weight;
                visit(graph, edge.to, visited, queue, metrics);
            }
        }
        return new MSTResult(chosen, total, components == 1);
    }

    private static void visit(Graph graph, int vertex, boolean[] visited,
                              PriorityQueue<Edge> queue, Metrics metrics) {
        visited[vertex] = true;
        metrics.vertexVisits++;
        for (Edge edge : graph.getNeighbors(vertex)) {
            metrics.edgeExaminations++;
            if (!visited[edge.to]) {
                queue.add(edge);
                metrics.queueAdds++;
            }
        }
    }
}
