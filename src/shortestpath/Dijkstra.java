package shortestpath;

import experiments.Metrics;
import graph.Edge;
import graph.Graph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class Dijkstra {
    public static final long INF = Long.MAX_VALUE;

    private record Entry(int vertex, long distance) { }

    public static class Result {
        private final int source;
        private final long[] distances;
        private final int[] previous;

        private Result(int source, long[] distances, int[] previous) {
            this.source = source;
            this.distances = distances;
            this.previous = previous;
        }

        public long distanceTo(int vertex) {
            validate(vertex);
            return distances[vertex];
        }

        public List<Integer> pathTo(int vertex) {
            validate(vertex);
            if (distances[vertex] == INF) {
                return List.of();
            }
            List<Integer> path = new ArrayList<>();
            for (int current = vertex; current != -1; current = previous[current]) {
                path.add(current);
                if (current == source) {
                    break;
                }
            }
            Collections.reverse(path);
            return List.copyOf(path);
        }

        private void validate(int vertex) {
            if (vertex < 0 || vertex >= distances.length) {
                throw new IllegalArgumentException("Invalid destination vertex: " + vertex);
            }
        }
    }

    public static Result compute(Graph graph, int source) {
        return compute(graph, source, new Metrics());
    }

    public static Result compute(Graph graph, int source, Metrics metrics) {
        int n = graph.getVertexCount();
        if (source < 0 || source >= n) {
            throw new IllegalArgumentException("Invalid source vertex: " + source);
        }
        for (Edge edge : graph.getEdges()) {
            metrics.edgeExaminations++;
            if (edge.weight < 0) {
                throw new IllegalArgumentException("Dijkstra requires non-negative edge weights");
            }
        }
        long[] distances = new long[n];
        int[] previous = new int[n];
        Arrays.fill(distances, INF);
        Arrays.fill(previous, -1);
        distances[source] = 0;
        PriorityQueue<Entry> queue = new PriorityQueue<>((a, b) -> {
            metrics.comparisons++;
            return Long.compare(a.distance(), b.distance());
        });
        queue.add(new Entry(source, 0));
        metrics.queueAdds++;
        while (!queue.isEmpty()) {
            Entry entry = queue.poll();
            metrics.queuePolls++;
            if (entry.distance() != distances[entry.vertex()]) {
                continue;
            }
            metrics.vertexVisits++;
            for (Edge edge : graph.getNeighbors(entry.vertex())) {
                metrics.edgeExaminations++;
                long candidate = entry.distance() + edge.weight;
                if (candidate < distances[edge.to]) {
                    distances[edge.to] = candidate;
                    previous[edge.to] = edge.from;
                    metrics.relaxations++;
                    queue.add(new Entry(edge.to, candidate));
                    metrics.queueAdds++;
                }
            }
        }
        return new Result(source, distances, previous);
    }
}
