import java.io.IOException;
import java.nio.file.Path;

public class Main {
    private static final String CONFIG_FILE = "config.ini";

    public static void main(String[] args) {
        Path configPath = args.length > 0 ? Path.of(args[0]) : Path.of(CONFIG_FILE);
        ConfigReader configReader = new ConfigReader();

        try {
            Config config = configReader.read(configPath);
            printConfig(config);
        } catch (IOException exception) {
            System.err.println("Configuration error:");
            System.err.println(exception.getMessage());
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
}
