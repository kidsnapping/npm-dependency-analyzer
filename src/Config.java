public class Config {
    private final String packageName;
    private final String repository;
    private final boolean testMode;
    private final String version;
    private final int maxDepth;

    public Config(String packageName, String repository, boolean testMode,
                  String version, int maxDepth) {
        this.packageName = packageName;
        this.repository = repository;
        this.testMode = testMode;
        this.version = version;
        this.maxDepth = maxDepth;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getRepository() {
        return repository;
    }

    public boolean isTestMode() {
        return testMode;
    }

    public String getVersion() {
        return version;
    }

    public int getMaxDepth() {
        return maxDepth;
    }
}
