package CreationPatterns.Builder.prob1;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Resume resume = new Resume.ResumeBuilder("Alice", "alice@example.com")
                .phone("1234567890")
                .linkedin("linkedin.com/in/alice")
                .skills(List.of("Java", "Python", "SQL"))
                .build();

        Resume resume2 = new Resume.ResumeBuilder("Alice Johnson", "alice@example.com")
                .phone("9876543210")
                .linkedin("linkedin.com/in/alice")
                .github("github.com/alice-dev")
                .summary("Experienced backend engineer specializing in Java and Spring Boot.")
                .skills(List.of("Java", "Spring", "Docker", "Kubernetes"))
                .experiences(List.of("MBRDI", "Infosys", "TCS"))
                .build();
        System.out.println("Resume: " + resume2);
        resume2.exportToPDF("Alice_Resume");
    }
}
