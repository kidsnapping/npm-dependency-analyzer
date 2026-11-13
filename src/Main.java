
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class Main {
    private static final String CONFIG_FILE = "config.ini";

    public static void main(String[] args) {
        Path configPath = args.length > 0
                ? Path.of(args[0])
                : Path.of(CONFIG_FILE);

        ConfigReader configReader = new ConfigReader();
        NpmRepository npmRepository = new NpmRepository();

        try {
            Config config = configReader.read(configPath);

            printConfig(config);

            Map<String, String> dependencies =
                    npmRepository.getDependencies(config);

            printDependencies(dependencies);
        } catch (IOException exception) {
            System.err.println("Error:");
            System.err.println(exception.getMessage());
            System.exit(1);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Request was interrupted");
            System.exit(1);
        }
    }

    private static void printConfig(Config config) {
        System.out.println("package_name = " + config.getPackageName());
        System.out.println("repository = " + config.getRepository());
        System.out.println("test_mode = " + config.isTestMode());
        System.out.println("version = " + config.getVersion());
        System.out.println("max_depth = " + config.getMaxDepth());
    }

    private static void printDependencies(Map<String, String> dependencies) {
        System.out.println();
        System.out.println("Direct dependencies:");

        if (dependencies.isEmpty()) {
            System.out.println("No dependencies found");
            return;
        }

        for (Map.Entry<String, String> dependency : dependencies.entrySet()) {
            System.out.println(
                    dependency.getKey() + " : " + dependency.getValue()
            );
        }
    }
}
