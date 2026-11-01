# Assignment 3: Smart City Infrastructure and Route Planning

## Overview

The program models a city as a weighted graph. Vertices are districts, edges are roads, and weights represent construction costs or travel distances. It finds minimum spanning trees, checks directed connections, orders dependencies, and finds shortest paths.

### Running the project

JDK 17 or newer is required. No external libraries are needed.

In VS Code, install the Java extensions, open the whole project folder, and use Run above the `main` method in:

- `src/Main.java` for the demonstration.
- `tests/GraphTest.java` for correctness tests.
- `src/experiments/Benchmark.java` for experiments and result files.

Use the same JDK for the project's Java compilation and execution. Run benchmarks with the project root as the working directory, so the files are saved in `results/`. VS Code manages its own compiled files; these are not submitted.

Alternatively, compile and run from a macOS/Linux terminal in the project root:

```sh
mkdir -p out
find src tests -name '*.java' -print > out/sources.txt
javac --release 17 -encoding UTF-8 -d out @out/sources.txt
java -cp out Main
java -cp out GraphTest
java -cp out experiments.Benchmark
```

Terminal compilation uses `out` to keep it separate from VS Code's build files. Both `bin` and `out` are ignored by Git.

The README is edited manually. After a new benchmark run, update the tables and discussion from `results/tables/summary.csv`.

### Structure

```text
src/
  Main.java
  graph/          Edge, Graph
  dsu/            DSU
  mst/            Kruskal, Prim, MSTResult
  coonect/        Kosaraju, TopoSort
  shortestpath/   Dijkstra
  experiments/    GraphGeneration, Benchmark, Metrics, PlotWriter
tests/GraphTest.java
results/tables/
results/plots/
README.md
```

## Implementation

### Graph

The graph uses adjacency lists and a separate list of all edges. Vertices are numbered from 0 to V-1. An undirected road is stored once in the edge list and in both directions in the adjacency lists. Directed roads are stored only in their given direction. Invalid vertex numbers are rejected.

`Edge` stores `from`, `to`, and `weight`. Weights are `int`, while total costs and distances are `long` to avoid overflowing an integer sum. The returned lists are read-only views, and algorithms do not change the graph.

### DSU and MST

DSU uses `parent` and `size` arrays. `find` locates the representative and compresses the path. `union` joins the smaller group to the larger one and returns false when the vertices already belong to the same group.

Kruskal sorts a copy of the edge list by weight. It accepts an edge when DSU joins two different groups. Prim starts from a vertex and uses `PriorityQueue` to select the cheapest road to an unvisited vertex. Old candidates leading to visited vertices are skipped.

Both algorithms return the selected edges, total weight, and connectivity status. A connected graph has V-1 selected edges. For a disconnected graph they return a minimum spanning forest and report that a complete MST does not exist. Prim restarts in each unvisited component. A graph with one vertex has MST weight 0. MST algorithms require undirected input and allow negative weights.

### Topological ordering and SCC

Topological sorting uses Kahn's algorithm. It counts incoming edges and puts vertices with indegree 0 into a queue. Removing a vertex decreases its neighbors' indegrees. If fewer than V vertices are processed, the graph has a cycle and no complete topological order exists.

Kosaraju finds strongly connected components. It records DFS finishing order, reverses all edges, and visits the reversed graph in decreasing finishing order. Each new traversal gives one SCC. Explicit stacks are used instead of recursive DFS. Every vertex, including an isolated vertex, belongs to one component. Both algorithms require directed input.

### Dijkstra

Dijkstra stores distances, predecessors, and a priority queue of vertex-distance entries. When a shorter route is found, it updates the distance and predecessor and adds a new queue entry. Outdated entries are skipped. Paths are reconstructed by following predecessors back to the source.

Unreachable vertices have distance `Long.MAX_VALUE` and an empty path. Negative weights are rejected because they break the greedy assumption that the smallest selected distance is final. For example, edges 0->1 with weight 2, 0->2 with weight 5, and 2->1 with weight -10 can improve vertex 1 after it has been selected. Bellman-Ford is an alternative for negative edges.

## Complexity Analysis

V means vertices and E means logical edges. Undirected graphs contain 2E adjacency entries. The implementation uses adjacency lists and Java binary heaps. Space below is additional to the input graph, except for the graph storage row.

| Algorithm / operation | Time | Space |
|---|---|---|
| Graph storage | O(V + E) construction, amortized | O(V + E) |
| DSU initialization | O(V) | O(V) |
| DSU find / union | O(alpha(V)) amortized per operation | O(1) per operation after initialization |
| Kruskal | O(V + E log(E+1) + E alpha(V)) | O(V + E) |
| Prim | O(V + E log(E+1)) | O(V + E) |
| Kosaraju | O(V + E) | O(V + E) |
| Topological ordering | O(V + E) | O(V) |
| Dijkstra | O(V + E + E log(E+1)) | O(V + E) |
| Path reconstruction | O(V) worst case | O(V) |

Kruskal is usually described as O(E log E) for connected graphs because sorting dominates. More edges increase sorting work. Prim's queue stores edges, and Dijkstra's queue can contain repeated entries, so both queues can use O(E) space. For simple graphs log E is O(log V), giving the usual O(E log V) form for connected Prim and O((V+E) log V) for Dijkstra.

More vertices increase array sizes; more edges increase neighbor scans. Kosaraju and topological sorting do a constant number of scans, giving linear O(V+E) time. Kosaraju also stores the reversed graph.

Union by size limits tree depth to O(log V): a node's depth increases only when its group joins a group at least as large. Path compression shortens later searches. Together they give O(alpha(V)) amortized operations, which is almost constant in practice. Initialization still takes O(V).

## Experimental Setup

| Graph | V | E |
|---|---:|---:|
| Small | 10 | 30 |
| Medium | 100 | 500 |
| Large | 1000 | 5000 |

Each graph uses a fresh `Random(42)`, weights from 1 to 100, and no repeated edges or self-loops.

- **Connected undirected:** connect each vertex after 0 to a random earlier vertex, forming a tree. Add random unique roads until there are E edges. Kruskal, Prim, and Dijkstra use this graph; Dijkstra starts from 0.
- **DAG:** add edges only from a smaller vertex number to a larger one, which prevents cycles. Used for topological ordering.
- **Cyclic directed:** make a ring from the first half of the vertices and a forward chain through the remaining vertices. Extra edges go forward or stay inside the ring's core. Later vertices cannot reach back into the core. Used for Kosaraju and cyclic topological tests.

Each algorithm/input combination runs at all three sizes, with 10 warmups followed by 5 measured repetitions. There are 90 measured runs. Each run creates fresh algorithm state. Timing uses `System.nanoTime()`. Generation, printing, and saving files are outside the timer. Sorting, reversed-graph construction, counters, and small MST result checks are inside it. Returned values are consumed after timing.

The same seed reproduces graphs and operation counts, but timings vary with JVM compilation, memory allocation, garbage collection, and other computer activity. These are simple benchmarks in one JVM, without separate process forks. Machine and Java information is saved in [environment.txt](results/tables/environment.txt).

### Counted operations

| Counter | What is counted |
|---|---|
| Edge examinations | Edges processed in loops, including weight validation, indegree calculation, and reversing edges |
| Comparisons | Comparator calls in sorting or a priority queue |
| Find / union calls | Calls to DSU methods |
| Parent traversals | Links followed during root search and path compression |
| Queue adds / polls | Insertions and removals in Prim, topological sorting, and Dijkstra |
| Relaxations | Successful distance improvements in Dijkstra |
| Vertex visits | Visits in Prim, topological sorting, Dijkstra, or both Kosaraju passes |

Counters measure different actions and should be compared separately. The five repetitions are checked to have identical counts.

## Results

Times are means of five measured runs after ten warmups. Counters are per run.

| Algorithm | V | E | Mean (microseconds) | Edge examinations | Comparisons | Find calls | Union calls | Parent traversals | Queue adds | Queue polls | Relaxations | Vertex visits |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Kruskal | 10 | 30 | 58.625 | 14 | 107 | 28 | 14 | 30 | 0 | 0 | 0 | 0 |
| Prim | 10 | 30 | 28.975 | 60 | 203 | 0 | 0 | 0 | 30 | 30 | 0 | 10 |
| Kosaraju | 10 | 30 | 39.117 | 90 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 20 |
| Topo-DAG | 10 | 30 | 9.675 | 60 | 0 | 0 | 0 | 0 | 10 | 10 | 0 | 10 |
| Topo-cycle | 10 | 30 | 2.050 | 30 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| Dijkstra | 10 | 30 | 27.150 | 90 | 82 | 0 | 0 | 0 | 18 | 18 | 17 | 10 |
| Kruskal | 100 | 500 | 179.967 | 164 | 3827 | 328 | 164 | 496 | 0 | 0 | 0 | 0 |
| Prim | 100 | 500 | 162.592 | 1000 | 7697 | 0 | 0 | 0 | 500 | 500 | 0 | 100 |
| Kosaraju | 100 | 500 | 301.017 | 1500 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 200 |
| Topo-DAG | 100 | 500 | 72.883 | 1000 | 0 | 0 | 0 | 0 | 100 | 100 | 0 | 100 |
| Topo-cycle | 100 | 500 | 19.242 | 500 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| Dijkstra | 100 | 500 | 203.508 | 1500 | 2403 | 0 | 0 | 0 | 200 | 200 | 199 | 100 |
| Kruskal | 1000 | 5000 | 835.942 | 3356 | 50369 | 6712 | 3356 | 12356 | 0 | 0 | 0 | 0 |
| Prim | 1000 | 5000 | 921.942 | 10000 | 110958 | 0 | 0 | 0 | 5000 | 5000 | 0 | 1000 |
| Kosaraju | 1000 | 5000 | 595.867 | 15000 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 2000 |
| Topo-DAG | 1000 | 5000 | 150.625 | 10000 | 0 | 0 | 0 | 0 | 1000 | 1000 | 0 | 1000 |
| Topo-cycle | 1000 | 5000 | 51.525 | 5000 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| Dijkstra | 1000 | 5000 | 523.483 | 15000 | 36698 | 0 | 0 | 0 | 1987 | 1987 | 1986 | 1000 |

The detailed data is in [raw.csv](results/tables/raw.csv) and [summary.csv](results/tables/summary.csv). Zero counters mean that type of operation was not recorded for the algorithm, not that no work was performed.

![Execution time vs vertices](results/plots/time-vs-vertices.svg)

![Execution time vs edges](results/plots/time-vs-edges.svg)

Both plots use the same three configurations, so V and E change together. They do not isolate the causal effect of one variable. All algorithms are shown on linear axes; very short runtimes appear near the baseline.

## Comparison and Discussion

Prim was faster in this recorded run at V=10 and V=100: 28.975 vs 58.625 microseconds and 162.592 vs 179.967 microseconds. At V=1000, Kruskal was faster: 835.942 vs 921.942 microseconds. These results do not mean either algorithm is always faster.

At V=1000, Kruskal made 50,369 sorting comparisons and 3,356 union calls. Prim made 110,958 heap comparisons and 5,000 queue removals. Kruskal sorts every edge but can stop its DSU scan early; Prim processes candidates in a heap, including candidates that become outdated. Operation counts alone do not include every execution cost.

DSU path compression and union by size keep representative searches short. The counters record actual parent-link traversals; no separate timing comparison with an unoptimized DSU was performed.

Kosaraju increased from 39.117 to 595.867 microseconds between the small and large graphs. It examines exactly 3E edges and visits vertices 2V times, matching O(V+E). The three scans cover the first DFS, reversed-graph construction, and the second DFS.

Topological sorting examines 2E edges on a DAG and processes all V vertices. On the generated cyclic graphs, every vertex has positive indegree, so the queue starts empty. It detects the cycle after the E-edge indegree scan. This explains the lower times for these particular cyclic inputs.

Dijkstra increased from 27.150 to 523.483 microseconds. All vertices are reachable in these connected undirected graphs. It examines E edges when checking weights and 2E adjacency entries during traversal. The weights affect the number of successful relaxations and queue operations.

The operation growth agrees with the theoretical bounds, while time ratios are affected by JVM behavior and overhead. Five repetitions and three graph sizes are not enough for universal speed conclusions. V and E grow together here, so the plots do not isolate the effect of either variable.

## Testing

Run `GraphTest` in VS Code or use `java -cp out GraphTest` after terminal compilation. The latest checked version passes 32,599 individual assertions. This is the number of checks, not the number of separate test cases. The tests throw `AssertionError` on failure and do not need the `-ea` flag.

Required cases are small graphs, disconnected graphs, cycles, multiple valid MSTs, unreachable vertices, repeated weights, and a single vertex. Extra checks cover invalid vertices, negative and zero weights, parallel edges, self-loops, old queue entries, and sums larger than `Integer.MAX_VALUE`.

- **MST:** known examples and exhaustive edge-subset enumeration on random graphs of at most five vertices. Check weight, no cycles, spanning of components, and agreement between Kruskal and Prim.
- **Topological ordering:** each vertex occurs once and every edge goes from an earlier position to a later one. Cycles and self-loops must be detected.
- **SCC:** compare the partition with independent all-pairs reachability. Two vertices share an SCC exactly when they can reach each other. Every vertex occurs once.
- **Dijkstra:** compare every source with Floyd-Warshall on random graphs. Check path endpoints, existing edges, path weight, and unreachable vertices.
- **DSU and generation:** check unions, membership, path compression, graph sizes, seed reproducibility, connectivity, and generated cycles.

There are 30 fixed-seed random small-graph trials. The generated assignment-size graphs are also checked for exact edge counts, no duplicates, MST agreement, valid DAG ordering, and the expected SCC count.

## Conclusion

Kruskal and Prim solve minimum-cost network construction and give the same MST cost. Kruskal works from sorted edges and DSU; Prim grows a tree through adjacency lists and a priority queue. Their measured performance depends on graph structure and implementation.

Topological sorting is useful for dependencies without cycles. SCC finds mutually reachable groups in directed graphs. Dijkstra finds shortest routes from one source with non-negative weights. An MST minimizes the total cost of the network and does not generally preserve shortest paths.
