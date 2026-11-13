
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class GraphvizExporter {
    public void export(
            Map<String, Set<String>> graph,
            String fileName
    ) throws IOException, InterruptedException {
        String dotFile = fileName + ".dot";
        String pngFile = fileName + ".png";

        String dotContent = createDotContent(graph);
        Files.writeString(Path.of(dotFile), dotContent);

        Process process = new ProcessBuilder(
                "dot",
                "-Tpng",
                dotFile,
                "-o",
                pngFile
        ).inheritIO().start();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Graphviz could not create PNG image");
        }

        System.out.println();
        System.out.println("Graph files created:");
        System.out.println(dotFile);
        System.out.println(pngFile);
    }

    private String createDotContent(Map<String, Set<String>> graph) {
        StringBuilder dot = new StringBuilder();

        dot.append("digraph DependencyGraph {\n");
        dot.append("    rankdir=LR;\n");
        dot.append("    node [shape=box];\n");

        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            String packageName = escape(entry.getKey());

            if (entry.getValue().isEmpty()) {
                dot.append("    \"").append(packageName).append("\";\n");
            }

            for (String dependency : entry.getValue()) {
                dot.append("    \"")
                        .append(packageName)
                        .append("\" -> \"")
                        .append(escape(dependency))
                        .append("\";\n");
            }
        }

        dot.append("}\n");
        return dot.toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
