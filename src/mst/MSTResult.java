package mst;

import graph.Edge;
import java.util.List;

public record MSTResult(List<Edge> edges, long totalWeight, boolean connected) {
    public MSTResult {
        edges = List.copyOf(edges);
    }
}
