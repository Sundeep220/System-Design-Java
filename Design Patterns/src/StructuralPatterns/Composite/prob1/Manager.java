package StructuralPatterns.Composite.prob1;

import java.util.ArrayList;
import java.util.List;

public class Manager implements EmployeeComponent {
    private String name;
    private String role;
    private List<EmployeeComponent> team = new ArrayList<>();

    public Manager(String name, String role) {
        this.name = name;
        this.role = role;
    }

    public void add(EmployeeComponent member) {
        team.add(member);
    }

    public void remove(EmployeeComponent member) {
        team.remove(member);
    }

    @Override
    public void showDetails(String indent) {
        System.out.println(indent + "+ " + role + ": " + name);
        for (EmployeeComponent member : team) {
            member.showDetails(indent + "  ");
        }
    }
}
