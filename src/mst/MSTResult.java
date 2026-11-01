package mst;

import graph.Edge;
import java.util.List;

public class MSTResult {
    private final List<Edge> edges;
    private final long totalWeight;
    private final boolean connected;

    public MSTResult(List<Edge> edges, long totalWeight, boolean connected) {
        this.edges = List.copyOf(edges);
        this.totalWeight = totalWeight;
        this.connected = connected;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public long getTotalWeight() {
        return totalWeight;
    }

    public boolean isConnected() {
        return connected;
    }
}
