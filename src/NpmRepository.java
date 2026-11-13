
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
    public Map<String, String> getDependencies(Config config)
            throws IOException, InterruptedException {
        String json;

        if (config.isTestMode()) {
            json = Files.readString(Path.of(config.getRepository()));
        } else {
            json = downloadPackageInfo(config);
        }

        return parseDependencies(json);
    }

    private String downloadPackageInfo(Config config)
            throws IOException, InterruptedException {
        String repository = config.getRepository();

        while (repository.endsWith("/")) {
            repository = repository.substring(0, repository.length() - 1);
        }

        String packageName = config.getPackageName().replace("/", "%2F");
        String url = repository + "/" + packageName + "/" + config.getVersion();

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

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Repository returned HTTP status: " + response.statusCode()
            );
        }

        return response.body();
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

        String dependenciesText = dependenciesMatcher.group(1);

        Pattern dependencyPattern = Pattern.compile(
                "\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\""
        );

        Matcher dependencyMatcher = dependencyPattern.matcher(dependenciesText);

        while (dependencyMatcher.find()) {
            String name = dependencyMatcher.group(1);
            String version = dependencyMatcher.group(2);
            dependencies.put(name, version);
        }

        if (!dependenciesText.isBlank() && dependencies.isEmpty()) {
            throw new IOException("Could not parse package dependencies");
        }

        return dependencies;
    }
}
