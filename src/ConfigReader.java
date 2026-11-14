
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigReader {
    private static final int MIN_DEPTH = 1;
    private static final int MAX_DEPTH = 1000;

    private static final Set<String> ALLOWED_KEYS = Set.of(
            "package_name",
            "repository",
            "test_mode",
            "version",
            "max_depth"
    );

    public static Config read(Path filePath) throws IOException {
        List<String> errors = new ArrayList<>();
        Map<String, String> values = parseFile(filePath, errors);

        String packageName = values.get("package_name");
        String repository = values.get("repository");
        String testModeValue = values.get("test_mode");
        String version = values.get("version");
        String maxDepthValue = values.get("max_depth");

        validatePackageName(packageName, errors);
        validateRepository(repository, errors);

        boolean testMode = parseTestMode(testModeValue, errors);
        validateVersion(version, errors);

        int maxDepth = parseMaxDepth(maxDepthValue, errors);

        if (repository != null && !repository.isBlank()) {
            validateRepositoryMode(repository, testMode, errors);
        }

        if (!errors.isEmpty()) {
            throw new IOException(String.join("\n", errors));
        }

        return new Config(packageName, repository, testMode, version, maxDepth);
    }

    private static final int SECTION_NAME_START = 1;
    private static final int SECTION_NAME_END_OFFSET = 1;

    private static Map<String, String> parseFile(
            Path filePath,
            List<String> errors
    ) throws IOException {
        Map<String, String> values = new HashMap<>();

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            errors.add("Configuration file does not exist: " + filePath);
            return values;
        }

        List<String> lines = Files.readAllLines(filePath);
        SectionState state = new SectionState();

        parseLines(lines, values, errors, state);

        if (!state.settingsSectionFound) {
            errors.add("Missing [settings] section");
        }

        return values;
    }

    private static void parseLines(
            List<String> lines,
            Map<String, String> values,
            List<String> errors,
            SectionState state
    ) {
        for (int i = 0; i < lines.size(); i++) {
            processLine(lines.get(i), i + 1, values, errors, state);
        }
    }

    private static void processLine(
            String sourceLine,
            int lineNumber,
            Map<String, String> values,
            List<String> errors,
            SectionState state
    ) {
        String line = sourceLine.trim();

        if (line.isEmpty() ||  line.startsWith(";") || line.startsWith("#")) {
            return;
        }

        if (isSectionHeader(line)) {
            state.insideSettings = processSection(
                    line, lineNumber, errors, state
            );
            return;
        }

        if (!state.insideSettings) {
            errors.add("Line " + lineNumber + ": parameter must be inside [settings]");
            return;
        }

        parseLine(line, lineNumber, values, errors);
    }

    private static boolean isSectionHeader(String line) {
        return line.startsWith("[") && line.endsWith("]");
    }

    private static boolean processSection(
            String line,
            int lineNumber,
            List<String> errors,
            SectionState state
    ) {
        String sectionName = line.substring(
                SECTION_NAME_START,
                line.length() - SECTION_NAME_END_OFFSET
        ).trim();

        if (!sectionName.equals("settings")) {
            errors.add("Line " + lineNumber + ": unknown section [" + sectionName + "]");
            return false;
        }

        if (state.settingsSectionFound) {
            errors.add("Line " + lineNumber + ": duplicate [settings] section");
        }

        state.settingsSectionFound = true;
        return true;
    }

    private static class SectionState {
        private boolean settingsSectionFound;
        private boolean insideSettings;
    }

    private static void parseLine(
            String line,
            int lineNumber,
            Map<String, String> values,
            List<String> errors
    ) {
        int separatorIndex = line.indexOf("=");

        if (separatorIndex == -1) {
            errors.add("Line " + lineNumber + ": expected key=value");
            return;
        }

        String key = line.substring(0, separatorIndex).trim();
        String value = line.substring(separatorIndex + 1).trim();

        if (!ALLOWED_KEYS.contains(key)) {
            errors.add("Line " + lineNumber + ": unknown parameter " + key);
            return;
        }

        if (values.containsKey(key)) {
            errors.add("Line " + lineNumber + ": duplicate parameter " + key);
            return;
        }

        values.put(key, value);
    }

    private static void validatePackageName(
            String packageName,
            List<String> errors
    ) {
        if (packageName == null || packageName.isBlank()) {
            errors.add("package_name is required");
            return;
        }

        if (!packageName.matches("(@[a-zA-Z0-9._-]+/)?[a-zA-Z0-9._-]+")) {
            errors.add("Invalid package_name: " + packageName);
        }
    }

    private static void validateRepository(
            String repository,
            List<String> errors
    ) {
        if (repository == null || repository.isBlank()) {
            errors.add("repository is required");
        }
    }

    private static boolean parseTestMode(
            String value,
            List<String> errors
    ) {
        if (value == null) {
            errors.add("test_mode is required");
            return false;
        }

        if (value.equalsIgnoreCase("true")) {
            return true;
        }

        if (value.equalsIgnoreCase("false")) {
            return false;
        }

        errors.add("test_mode must be true or false");
        return false;
    }

    private static void validateVersion(
            String version,
            List<String> errors
    ) {
        if (version == null || version.isBlank()) {
            errors.add("version is required");
            return;
        }

        String versionPattern = "\\d+\\.\\d+\\.\\d+(-[0-9A-Za-z.-]+)?(\\+[0-9A-Za-z.-]+)?";

        if (!version.matches(versionPattern)) {
            errors.add("Invalid version: " + version);
        }
    }

    private static int parseMaxDepth(
            String value,
            List<String> errors
    ) {
        if (value == null) {
            errors.add("max_depth is required");
            return MIN_DEPTH;
        }

        try {
            int depth = Integer.parseInt(value);

            if (depth < MIN_DEPTH || depth > MAX_DEPTH) {
                errors.add("max_depth must be between " + MIN_DEPTH + " and " + MAX_DEPTH);
                return MIN_DEPTH;
            }

            return depth;
        } catch (NumberFormatException exception) {
            errors.add("max_depth must be an integer");
            return MIN_DEPTH;
        }
    }

    private static void validateRepositoryMode(
            String repository,
            boolean testMode,
            List<String> errors
    ) {
        if (testMode) {
            try {
                Path repositoryPath = Path.of(repository);

                if (!Files.exists(repositoryPath) || !Files.isRegularFile(repositoryPath)) {
                    errors.add("Test repository file does not exist: " + repository);
                }
            } catch (RuntimeException exception) {
                errors.add("Invalid test repository path: " + repository);
            }

            return;
        }

        try {
            URI uri = URI.create(repository);

            if (!uri.isAbsolute()
                    || !(uri.getScheme().equalsIgnoreCase("http")
                    || uri.getScheme().equalsIgnoreCase("https"))) {
                errors.add("Repository must be an absolute HTTP or HTTPS URL");
            }
        } catch (IllegalArgumentException exception) {
            errors.add("Invalid repository URL: " + repository);
        }
    }
}
