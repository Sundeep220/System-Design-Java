package CreationPatterns.AbstractFactory.prob2;

public enum CloudProvider {
    AWS("Amazon Web Services"),
    AZURE("Microsoft Azure"),
    GCP("Google Cloud Platform");

    private final String displayName; // 1️⃣ store the custom name

    CloudProvider(String displayName) {
        this.displayName = displayName; // 2️⃣ assign it
    }

    public String getDisplayName() {   // 3️⃣ getter
        return displayName;
    }
}
