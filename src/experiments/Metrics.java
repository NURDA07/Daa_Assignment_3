package experiments;

public class Metrics {
    public long edgeExaminations;
    public long comparisons;
    public long findCalls;
    public long unionCalls;
    public long parentTraversals;
    public long queueAdds;
    public long queuePolls;
    public long relaxations;
    public long vertexVisits;

    public static String csvHeader() {
        return "edge_examinations,comparisons,find_calls,union_calls,parent_traversals,"
                + "queue_adds,queue_polls,relaxations,vertex_visits";
    }

    public String csvValues() {
        return edgeExaminations + "," + comparisons + "," + findCalls + "," + unionCalls
                + "," + parentTraversals + "," + queueAdds + "," + queuePolls
                + "," + relaxations + "," + vertexVisits;
    }
}
