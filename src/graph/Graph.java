package graph;

import java.util.ArrayList;
import java.util.List;

public class Graph {
    private final int vertexCount;
    private final boolean directed;
    private final List<Edge> edges;
    private final List<List<Edge>> adjacency;

    public Graph( int vertexCount, boolean directed) {
        if (vertexCount <= 0) {
            throw new IllegalArgumentException("Vertex count must be positive.");
        }

        this.vertexCount = vertexCount;
        this.directed = directed;
        this.edges = new ArrayList<>();
        this.adjacency = new ArrayList<>();

        for (int i = 0; i < vertexCount; i++) {
            adjacency.add(new ArrayList<>());
        }
    }
    private void validateVertex(int vertex) {
        if (vertex < 0 || vertex >= vertexCount) {
            throw new IllegalArgumentException("Vertex " + vertex + " is out of bounds.");
        }
    }

    public void addEdge( int from, int to, int weight) {
        validateVertex(from);
        validateVertex(to);
    
        Edge edge = new Edge(from, to, weight);
        edges.add(edge);
        adjacency.get(from).add(edge);

        if (!directed) {
            Edge reverse = new Edge(to, from, weight);
            adjacency.get(to).add(reverse);
        }
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public boolean isDirected() {
        return directed;
    }

    public List<Edge> getEdges() {
        return List.copyOf(edges);
    }

    public List<Edge> getNeighbors(int vertex) {
        validateVertex(vertex);
        return List.copyOf(adjacency.get(vertex));
    }
}
