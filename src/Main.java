
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class Main {
    private static final String CONFIG_FILE = "config.ini";

    public static void main(String[] args) {
        Path configPath = args.length > 0
                ? Path.of(args[0])
                : Path.of(CONFIG_FILE);

        ConfigReader configReader = new ConfigReader();

        try {
            Config config = configReader.read(configPath);
            String selectedPackage = args.length > 1
                    ? args[1]
                    : config.getPackageName();

            if (config.isTestMode()) {
                runTestMode(config, selectedPackage);
            } else {
                runNpmMode(config, selectedPackage);
            }
        } catch (IOException exception) {
            System.err.println("Error:");
            System.err.println(exception.getMessage());
            System.exit(1);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Request was interrupted");
            System.exit(1);
        } catch (IllegalArgumentException exception) {
            System.err.println("Error:");
            System.err.println(exception.getMessage());
            System.exit(1);
        }
    }

    private static void runTestMode(
            Config config,
            String selectedPackage
    ) throws IOException {
        GraphTestReader reader = new GraphTestReader();
        DependencyGraph dependencyGraph = new DependencyGraph();

        Map<String, Set<String>> sourceGraph =
                reader.read(Path.of(config.getRepository()));

        dependencyGraph.build(
                sourceGraph,
                config.getPackageName(),
                config.getMaxDepth()
        );

        dependencyGraph.print();
        dependencyGraph.printReverseDependencies(selectedPackage);
    }

    private static void runNpmMode(
            Config config,
            String selectedPackage
    ) throws IOException, InterruptedException {
        NpmRepository repository = new NpmRepository();
        DependencyGraph dependencyGraph = new DependencyGraph();

        dependencyGraph.buildFromNpm(config, repository);
        dependencyGraph.print();
        dependencyGraph.printReverseDependencies(selectedPackage);
    }
}
