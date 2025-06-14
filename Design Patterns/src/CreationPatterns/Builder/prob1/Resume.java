package CreationPatterns.Builder.prob1;

import java.util.List;

public class Resume {
    private final String name;
    private final String email;

    // Optional fields
    private final String phone;
    private final String linkedin;
    private final String github;
    private final String summary;
    private final List<String> skills;
    private final List<String> experiences;

    private Resume(ResumeBuilder builder) {
        this.name = builder.name;
        this.email = builder.email;
        this.phone = builder.phone;
        this.linkedin = builder.linkedin;
        this.github = builder.github;
        this.summary = builder.summary;
        this.skills = builder.skills;
        this.experiences = builder.experiences;
    }

    public void exportToPDF(String filename) {
        System.out.println("Exporting resume to PDF file: " + filename + ".pdf");
        // Simulated file generation (real implementation can use iText or Apache PDFBox)
    }

    public static class ResumeBuilder {
        private final String name;
        private final String email;

        // Optional fields
        private String phone;
        private String linkedin;
        private String github;
        private String summary;
        private List<String> skills;
        private List<String> experiences;

        public ResumeBuilder(String name, String email) {
            this.name = name;
            this.email = email;
        }

        // Setting optional fields
        public ResumeBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }
        public ResumeBuilder linkedin(String linkedin) {
            this.linkedin = linkedin;
            return this;
        }
        public ResumeBuilder github(String github) {
            this.github = github;
            return this;
        }
        public ResumeBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }
        public ResumeBuilder skills(List<String> skills) {
            this.skills = skills;
            return this;
        }

        public ResumeBuilder experiences(List<String> experiences) {
            this.experiences = experiences;
            return this;
        }


        public Resume build() {
            return new Resume(this);
        }
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(name).append("\n");
        sb.append("Email: ").append(email).append("\n");
        if (phone != null) sb.append("Phone: ").append(phone).append("\n");
        if (linkedin != null) sb.append("LinkedIn: ").append(linkedin).append("\n");
        if (github != null) sb.append("GitHub: ").append(github).append("\n");
        if (summary != null) sb.append("Summary: ").append(summary).append("\n");
        if (skills != null && !skills.isEmpty()) sb.append("Skills: ").append(String.join(", ", skills)).append("\n");
        if (experiences != null && !experiences.isEmpty()) sb.append("Experience: ").append(String.join(", ", experiences)).append("\n");
        return sb.toString();
    }
}
