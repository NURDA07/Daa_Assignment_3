package dsu;

import experiments.Metrics;

public class DSU {
    private final int[] parent;
    private final int[] size;
    private final Metrics metrics;

    public DSU(int vertexCount) {
        this(vertexCount, new Metrics());
    }

    public DSU(int vertexCount, Metrics metrics) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException(
                "Vertex count cannot be negative"
            );
        }

        parent = new int[vertexCount];
        size = new int[vertexCount];
        this.metrics = metrics;

        for (int i = 0; i < vertexCount; i++) {
            parent[i] = i;
            size[i] = 1;
        }
    }
    private void validateVertex(int vertex) {
        if (vertex < 0 || vertex >= parent.length) {
            throw new IllegalArgumentException(
                "Invalid vertex: " + vertex
            );
        }
    }

    public int find(int x) {
        validateVertex(x);
        metrics.findCalls++;
        int root = x;
        while (parent[root] != root) {
            metrics.parentTraversals++;
            root = parent[root];
        }
        while (parent[x] != x) {
            metrics.parentTraversals++;
            int next = parent[x];
            parent[x] = root;
            x = next;
        }
        return root;
    }

    public boolean union(int a, int b) {
        metrics.unionCalls++;
        int rootA = find(a);
        int rootB = find(b);

        if (rootA == rootB) {
            return false;
        }

        if (size[rootA] < size[rootB]) {
            int temp = rootA;
            rootA = rootB;
            rootB = temp;
        }

        parent[rootB] = rootA;
        size[rootA] += size[rootB];

        return true;
    }
}
