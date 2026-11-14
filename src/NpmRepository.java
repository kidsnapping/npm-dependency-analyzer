
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NpmRepository {
    private static final int HTTP_OK_STATUS = 200;
    private static final int DEPENDENCIES_CONTENT_GROUP = 1;
    private static final int DEPENDENCY_NAME_GROUP = 1;
    private static final int DEPENDENCY_VERSION_GROUP = 2;

    public Map<String, String> getDependencies(Config config)
            throws IOException, InterruptedException {
        if (config.isTestMode()) {
            String json = Files.readString(Path.of(config.getRepository()));
            return parseDependencies(json);
        }

        return getDependencies(
                config.getPackageName(),
                config.getVersion(),
                config.getRepository()
        );
    }

    public Map<String, String> getDependencies(
            String packageName,
            String version,
            String repository
    ) throws IOException, InterruptedException {
        String exactVersion = normalizeVersion(version);
        String json = downloadPackageInfo(packageName, exactVersion, repository);
        return parseDependencies(json);
    }

    private String downloadPackageInfo(
            String packageName,
            String version,
            String repository
    ) throws IOException, InterruptedException {
        while (repository.endsWith("/")) {
            repository = repository.substring(0, repository.length() - 1);
        }

        String encodedName = packageName.replace("/", "%2F");
        String url = repository + "/" + encodedName + "/" + version;

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.npm.install-v1+json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != HTTP_OK_STATUS) {
            throw new IOException(
                    "Cannot get " + packageName + "@" + version
                            + ". HTTP status: " + response.statusCode()
            );
        }

        return response.body();
    }

    private String normalizeVersion(String version) throws IOException {
        String normalized = version.trim();

        if (normalized.startsWith("^") || normalized.startsWith("~")) {
            normalized = normalized.substring(1);
        }

        if (!normalized.matches("\\d+\\.\\d+\\.\\d+")) {
            throw new IOException("Unsupported dependency version: " + version);
        }

        return normalized;
    }

    private Map<String, String> parseDependencies(String json) throws IOException {
        Map<String, String> dependencies = new HashMap<>();

        Pattern dependenciesPattern = Pattern.compile(
                "\"dependencies\"\\s*:\\s*\\{([^}]*)\\}",
                Pattern.DOTALL
        );

        Matcher dependenciesMatcher = dependenciesPattern.matcher(json);

        if (!dependenciesMatcher.find()) {
            return dependencies;
        }

        String dependenciesText = dependenciesMatcher.group(DEPENDENCIES_CONTENT_GROUP);

        Pattern dependencyPattern = Pattern.compile(
                "\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\""
        );

        Matcher dependencyMatcher = dependencyPattern.matcher(dependenciesText);

        while (dependencyMatcher.find()) {
            dependencies.put(
                    dependencyMatcher.group(DEPENDENCY_NAME_GROUP),
                    dependencyMatcher.group(DEPENDENCY_VERSION_GROUP)
            );
        }

        if (!dependenciesText.isBlank() && dependencies.isEmpty()) {
            throw new IOException("Could not parse package dependencies");
        }

        return dependencies;
    }
}
