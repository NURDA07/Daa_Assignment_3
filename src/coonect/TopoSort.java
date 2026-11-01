package coonect;

import experiments.Metrics;
import graph.Edge;
import graph.Graph;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class TopoSort {
    public record Result(List<Integer> order, boolean hasCycle) {
        public Result {
            order = List.copyOf(order);
        }
    }

    public static Result compute(Graph graph) {
        return compute(graph, new Metrics());
    }

    public static Result compute(Graph graph, Metrics metrics) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("Topological ordering requires a directed graph");
        }
        int[] indegree = new int[graph.getVertexCount()];
        for (Edge edge : graph.getEdges()) {
            metrics.edgeExaminations++;
            indegree[edge.to]++;
        }
        Queue<Integer> queue = new ArrayDeque<>();
        for (int v = 0; v < indegree.length; v++) {
            if (indegree[v] == 0) {
                queue.add(v);
                metrics.queueAdds++;
            }
        }
        List<Integer> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            int vertex = queue.remove();
            metrics.queuePolls++;
            metrics.vertexVisits++;
            order.add(vertex);
            for (Edge edge : graph.getNeighbors(vertex)) {
                metrics.edgeExaminations++;
                if (--indegree[edge.to] == 0) {
                    queue.add(edge.to);
                    metrics.queueAdds++;
                }
            }
        }
        return new Result(order, order.size() != graph.getVertexCount());
    }
}
