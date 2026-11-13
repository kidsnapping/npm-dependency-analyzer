
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DependencyGraph {
    private final Map<String, Set<String>> graph = new LinkedHashMap<>();

    public void addPackage(String packageName) {
        graph.putIfAbsent(packageName, new LinkedHashSet<>());
    }

    public void addDependency(String packageName, String dependency) {
        addPackage(packageName);
        addPackage(dependency);
        graph.get(packageName).add(dependency);
    }

    public void build(
            Map<String, Set<String>> sourceGraph,
            String rootPackage,
            int maxDepth
    ) {
        if (!sourceGraph.containsKey(rootPackage)) {
            throw new IllegalArgumentException(
                    "Package not found in test graph: " + rootPackage
            );
        }

        graph.clear();

        List<QueueItem> queue = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();

        queue.add(new QueueItem(rootPackage, "", 0));
        visited.add(rootPackage);

        processTestQueue(sourceGraph, queue, visited, 0, maxDepth);
    }

    public void buildFromNpm(Config config, NpmRepository repository)
            throws IOException, InterruptedException {
        graph.clear();

        List<QueueItem> queue = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();

        queue.add(new QueueItem(
                config.getPackageName(),
                config.getVersion(),
                0
        ));
        visited.add(config.getPackageName());

        processNpmQueue(
                config,
                repository,
                queue,
                visited,
                0
        );
    }

    private void processTestQueue(
            Map<String, Set<String>> sourceGraph,
            List<QueueItem> queue,
            Set<String> visited,
            int index,
            int maxDepth
    ) {
        if (index >= queue.size()) {
            return;
        }

        QueueItem current = queue.get(index);
        addPackage(current.packageName);

        if (current.depth < maxDepth) {
            Set<String> dependencies = sourceGraph.getOrDefault(
                    current.packageName,
                    new LinkedHashSet<>()
            );

            for (String dependency : dependencies) {
                addDependency(current.packageName, dependency);

                if (visited.add(dependency)) {
                    queue.add(new QueueItem(
                            dependency,
                            "",
                            current.depth + 1
                    ));
                }
            }
        }

        processTestQueue(
                sourceGraph,
                queue,
                visited,
                index + 1,
                maxDepth
        );
    }

    private void processNpmQueue(
            Config config,
            NpmRepository repository,
            List<QueueItem> queue,
            Set<String> visited,
            int index
    ) throws IOException, InterruptedException {
        if (index >= queue.size()) {
            return;
        }

        QueueItem current = queue.get(index);

        addPackage(current.packageName);

        if (current.depth < config.getMaxDepth()) {
            Map<String, String> dependencies = repository.getDependencies(
                    current.packageName,
                    current.version,
                    config.getRepository()
            );

            for (Map.Entry<String, String> dependency : dependencies.entrySet()) {
                String name = dependency.getKey();
                String version = dependency.getValue();

                addDependency(current.packageName, name);

                if (visited.add(name)) {
                    queue.add(new QueueItem(
                            name,
                            version,
                            current.depth + 1
                    ));
                }
            }
        }

        processNpmQueue(
                config,
                repository,
                queue,
                visited,
                index + 1
        );
    }

    public void print() {
        System.out.println();
        System.out.println("Dependency graph:");

        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            System.out.println(
                    entry.getKey() + " -> " + entry.getValue()
            );
        }
    }

    private static class QueueItem {
        private final String packageName;
        private final String version;
        private final int depth;

        private QueueItem(String packageName, String version, int depth) {
            this.packageName = packageName;
            this.version = version;
            this.depth = depth;
        }
    }
}
