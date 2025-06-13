package OnlineLearningPlatform.repo;

import OnlineLearningPlatform.entity.Course;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class CourseRepository{
    private final HashMap<String, Course> courseMap = new HashMap<>(); // <id, course>
    public void save(Course course){
        courseMap.put(course.getId(), course);
    }

    public Optional<Course> findById(String id){
        return Optional.ofNullable(courseMap.get(id)); // Nullable ensures that the value can be null
    }

    public List<Course> findAll(){
        return courseMap.values().stream().toList();
    }
}
