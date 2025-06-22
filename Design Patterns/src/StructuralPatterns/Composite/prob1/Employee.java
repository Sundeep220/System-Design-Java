package StructuralPatterns.Composite.prob1;

public class Employee implements EmployeeComponent {
    private String name;
    private String role;

    public Employee(String name, String role) {
        this.name = name;
        this.role = role;
    }

    @Override
    public void showDetails(String indent) {
        System.out.println(indent + "- " + role + ": " + name);
    }
}
