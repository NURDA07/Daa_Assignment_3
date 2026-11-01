package dsu;

public class DSU {
    private final int[] parent;
    private final int[] size;

    public DSU(int vertexCount) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException(
                "Vertex count cannot be negative"
            );
        }

        parent = new int[vertexCount];
        size = new int[vertexCount];

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

        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }

        return parent[x];
    }

    public boolean union(int a, int b) {
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
