import dsu.DSU;
import graph.Edge;
import graph.Graph;

public class Main {
    public static void main(String[] args) {
        Graph city = new Graph(3, false);

        city.addEdge(0, 1, 4);
        city.addEdge(1, 2, 2);
        city.addEdge(0, 2, 9);

        System.out.println("Number of districts: " + city.getVertexCount());

        for (Edge edge : city.getEdges()) {
            System.out.println(
                edge.from + "-" + edge.to + ",weight:" +edge.weight
            );
        }
        DSU groups = new DSU(3);

        System.out.println(groups.union(0, 1));
        System.out.println(groups.union(1, 2));
        System.out.println(groups.union(0, 2));

        System.out.println(groups.find(0));
        System.out.println(groups.find(1));
        System.out.println(groups.find(2));
    }
}
