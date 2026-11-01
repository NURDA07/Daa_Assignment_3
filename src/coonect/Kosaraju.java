package coonect;

import experiments.Metrics;
import graph.Edge;
import graph.Graph;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class Kosaraju {
    private static class Frame {
        final int vertex;
        int nextEdge;

        Frame(int vertex) {
            this.vertex = vertex;
        }
    }

    public static List<List<Integer>> compute(Graph graph) {
        return compute(graph, new Metrics());
    }

    public static List<List<Integer>> compute(Graph graph, Metrics metrics) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("SCC analysis requires a directed graph");
        }
        int n = graph.getVertexCount();
        boolean[] seen = new boolean[n];
        List<Integer> finish = new ArrayList<>();
        for (int start = 0; start < n; start++) {
            if (!seen[start]) {
                finishDfs(graph, start, seen, finish, metrics);
            }
        }
        Graph reversed = new Graph(n, true);
        for (Edge edge : graph.getEdges()) {
            metrics.edgeExaminations++;
            reversed.addEdge(edge.to, edge.from, edge.weight);
        }
        seen = new boolean[n];
        List<List<Integer>> components = new ArrayList<>();
        Collections.reverse(finish);
        for (int start : finish) {
            if (seen[start]) {
                continue;
            }
            List<Integer> component = new ArrayList<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(start);
            seen[start] = true;
            while (!stack.isEmpty()) {
                int vertex = stack.pop();
                metrics.vertexVisits++;
                component.add(vertex);
                for (Edge edge : reversed.getNeighbors(vertex)) {
                    metrics.edgeExaminations++;
                    if (!seen[edge.to]) {
                        seen[edge.to] = true;
                        stack.push(edge.to);
                    }
                }
            }
            components.add(List.copyOf(component));
        }
        return List.copyOf(components);
    }

    private static void finishDfs(Graph graph, int start, boolean[] seen,
                                   List<Integer> finish, Metrics metrics) {
        Deque<Frame> stack = new ArrayDeque<>();
        seen[start] = true;
        metrics.vertexVisits++;
        stack.push(new Frame(start));
        while (!stack.isEmpty()) {
            Frame frame = stack.peek();
            List<Edge> neighbors = graph.getNeighbors(frame.vertex);
            if (frame.nextEdge == neighbors.size()) {
                finish.add(frame.vertex);
                stack.pop();
            } else {
                Edge edge = neighbors.get(frame.nextEdge++);
                metrics.edgeExaminations++;
                if (!seen[edge.to]) {
                    seen[edge.to] = true;
                    metrics.vertexVisits++;
                    stack.push(new Frame(edge.to));
                }
            }
        }
    }
}
