package experiments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;

public class PlotWriter {
    public static void write(List<Benchmark.Row> rows, Path output, boolean vertices) throws IOException {
        Map<String, List<Benchmark.Row>> series = new LinkedHashMap<>();
        double maxTime = 0;
        int maxX = 0;
        for (Benchmark.Row row : rows) {
            series.computeIfAbsent(row.algorithm(), key -> new ArrayList<>()).add(row);
            maxTime = Math.max(maxTime, row.averageNs() / 1000);
            maxX = Math.max(maxX, vertices ? row.vertices() : row.edges());
        }
        maxTime = Math.max(1, maxTime * 1.12);
        maxX = (int) (maxX * 1.05);
        String[] colors = {"#2563eb", "#dc2626", "#059669", "#9333ea", "#d97706", "#0891b2"};
        String axis = vertices ? "Number of vertices (V)" : "Number of edges (E)";
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1100\" height=\"650\" viewBox=\"0 0 1100 650\">");
        svg.append("<rect width=\"1100\" height=\"650\" fill=\"white\"/>");
        svg.append("<g font-family=\"Arial,sans-serif\" fill=\"#1f2937\" font-size=\"15\">");
        text(svg, 95, 38, "Execution time vs. " + (vertices ? "vertices" : "edges"), "font-size=\"25\" font-weight=\"bold\"");
        text(svg, 95, 65, "Mean of 5 runs; 10 warmups; seed 42; counters enabled", "font-size=\"14\"");
        for (int i = 0; i <= 5; i++) {
            double x = 95 + 700.0 * i / 5;
            double y = 535 - 420.0 * i / 5;
            line(svg, 95, y, 795, y, "#e5e7eb");
            text(svg, 80, y + 5, String.format(Locale.ROOT, "%.1f", maxTime * i / 5), "text-anchor=\"end\"");
            text(svg, x, 565, Integer.toString(maxX * i / 5), "text-anchor=\"middle\"");
        }
        line(svg, 95, 115, 95, 535, "#374151");
        line(svg, 95, 535, 795, 535, "#374151");
        text(svg, 445, 605, axis, "text-anchor=\"middle\"");
        svg.append("<text x=\"24\" y=\"325\" transform=\"rotate(-90 24 325)\" text-anchor=\"middle\">Mean execution time (microseconds)</text>");
        int index = 0;
        for (Map.Entry<String, List<Benchmark.Row>> entry : series.entrySet()) {
            String color = colors[index];
            StringBuilder points = new StringBuilder();
            for (Benchmark.Row row : entry.getValue()) {
                double x = 95 + 700.0 * (vertices ? row.vertices() : row.edges()) / maxX;
                double y = 535 - 420 * (row.averageNs() / 1000) / maxTime;
                points.append(String.format(Locale.ROOT, "%.2f,%.2f ", x, y));
            }
            svg.append("<polyline fill=\"none\" stroke=\"").append(color)
                    .append("\" stroke-width=\"2.5\" points=\"").append(points).append("\"/>");
            for (Benchmark.Row row : entry.getValue()) {
                double x = 95 + 700.0 * (vertices ? row.vertices() : row.edges()) / maxX;
                double y = 535 - 420 * (row.averageNs() / 1000) / maxTime;
                svg.append(String.format(Locale.ROOT,
                        "<circle cx=\"%.2f\" cy=\"%.2f\" r=\"4\" fill=\"%s\"/>", x, y, color));
            }
            int legendY = 145 + index * 36;
            line(svg, 835, legendY, 865, legendY, color);
            text(svg, 878, legendY + 5, entry.getKey(), "");
            index++;
        }
        text(svg, 95, 635, "V and E grow together in the three assignment configurations.", "font-size=\"13\"");
        svg.append("</g></svg>");
        Files.writeString(output, svg.toString());
    }

    private static void line(StringBuilder svg, double x1, double y1, double x2, double y2, String color) {
        svg.append(String.format(Locale.ROOT,
                "<line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" stroke=\"%s\"/>", x1, y1, x2, y2, color));
    }

    private static void text(StringBuilder svg, double x, double y, String content, String attributes) {
        svg.append(String.format(Locale.ROOT, "<text x=\"%.2f\" y=\"%.2f\" %s>%s</text>", x, y, attributes, content));
    }
}
