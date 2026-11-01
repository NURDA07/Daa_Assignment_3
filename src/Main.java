import coonect.Kosaraju;
import coonect.TopoSort;
import graph.Edge;
import graph.Graph;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import shortestpath.Dijkstra;

public class Main {
    public static void main(String[] args) {
        Graph city = new Graph(5, false);
        city.addEdge(0, 1, 4);
        city.addEdge(1, 2, 2);
        city.addEdge(0, 2, 9);
        city.addEdge(2, 3, 3);
        city.addEdge(3, 4, 1);
        city.addEdge(1, 4, 8);

        System.out.println("SMART CITY: " + city.getVertexCount() + " districts");
        printMst("Kruskal", Kruskal.compute(city));
        printMst("Prim", Prim.compute(city));

        System.out.println("\nShortest paths from district 0:");
        Dijkstra.Result routes = Dijkstra.compute(city, 0);
        for (int v = 0; v < city.getVertexCount(); v++) {
            System.out.println("To " + v + ": distance=" + routes.distanceTo(v)
                    + ", path=" + routes.pathTo(v));
        }

        Graph dag = new Graph(5, true);
        dag.addEdge(0, 1, 1);
        dag.addEdge(0, 2, 1);
        dag.addEdge(1, 3, 1);
        dag.addEdge(2, 3, 1);
        dag.addEdge(3, 4, 1);
        System.out.println("\nDAG order: " + TopoSort.compute(dag).order());

        Graph directed = new Graph(5, true);
        directed.addEdge(0, 1, 1);
        directed.addEdge(1, 2, 1);
        directed.addEdge(2, 0, 1);
        directed.addEdge(2, 3, 1);
        directed.addEdge(3, 4, 1);
        directed.addEdge(4, 3, 1);
        System.out.println("Cyclic graph detected: " + TopoSort.compute(directed).hasCycle());
        System.out.println("SCCs: " + Kosaraju.compute(directed));

        Graph disconnected = new Graph(3, false);
        disconnected.addEdge(0, 1, 7);
        printMst("Disconnected example", Kruskal.compute(disconnected));
        if (Dijkstra.compute(disconnected, 0).distanceTo(2) == Dijkstra.INF) {
            System.out.println("District 2 is unreachable from district 0");
        }
    }

    private static void printMst(String name, MSTResult result) {
        System.out.println("\n" + name + ": "
                + (result.connected() ? "MST" : "no MST; minimum spanning forest"));
        for (Edge edge : result.edges()) {
            System.out.println(edge.from + " -- " + edge.to + ", weight=" + edge.weight);
        }
        System.out.println("Total weight: " + result.totalWeight());
    }
}
