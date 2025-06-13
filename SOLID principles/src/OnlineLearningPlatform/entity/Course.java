package OnlineLearningPlatform.entity;

public class Course {
    private final String id;
    private final String title;
    private final String description;
    private final Teacher teacher;

    public Course(String id, String title, String description, Teacher teacher) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.teacher = teacher;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Teacher getTeacher() {
        return teacher;
    }
}
