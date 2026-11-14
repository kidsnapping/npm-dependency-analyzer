
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphTestReader {
    private static final int MAX_SPLIT_PARTS = 2;
    public Map<String, Set<String>> read(Path filePath) throws IOException {
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new IOException("Test graph file does not exist: " + filePath);
        }

        Map<String, Set<String>> graph = new LinkedHashMap<>();
        Set<String> definedPackages = new HashSet<>();
        List<String> lines = Files.readAllLines(filePath);

        for (int i = 0; i < lines.size(); i++) {
            parseLine(lines.get(i), i + 1, graph, definedPackages);
        }

        return graph;
    }

    private void parseLine(
            String line,
            int lineNumber,
            Map<String, Set<String>> graph,
            Set<String> definedPackages
    ) throws IOException {
        if (line.isBlank()) {
            return;
        }

        String[] parts = line.split(":", MAX_SPLIT_PARTS);

        if (parts.length != 2) {
            throw new IOException("Invalid graph format at line " + lineNumber);
        }

        String packageName = parts[0].trim();

        if (!isValidPackageName(packageName)) {
            throw new IOException("Invalid package name at line " + lineNumber);
        }

        if (!definedPackages.add(packageName)) {
            throw new IOException("Duplicate package at line " + lineNumber);
        }

        Set<String> dependencies = parseDependencies(parts[1], lineNumber);
        graph.put(packageName, dependencies);

        for (String dependency : dependencies) {
            graph.putIfAbsent(dependency, new LinkedHashSet<>());
        }
    }

    private Set<String> parseDependencies(String value, int lineNumber)
            throws IOException {
        Set<String> dependencies = new LinkedHashSet<>();

        if (value.isBlank()) {
            return dependencies;
        }

        String[] names = value.split(",");

        for (String name : names) {
            String dependency = name.trim();

            if (!isValidPackageName(dependency)) {
                throw new IOException(
                        "Invalid dependency at line " + lineNumber
                );
            }

            dependencies.add(dependency);
        }

        return dependencies;
    }

    private boolean isValidPackageName(String name) {
        return name.matches("[A-Z]");
    }
}
