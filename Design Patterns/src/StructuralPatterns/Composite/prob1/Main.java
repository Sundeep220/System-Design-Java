package StructuralPatterns.Composite.prob1;

public class Main {
    public static void main(String[] args) {
        // Leaf employees
        EmployeeComponent dev1 = new Employee("Alice", "Developer");
        EmployeeComponent dev2 = new Employee("Bob", "Developer");
        EmployeeComponent designer = new Employee("Charlie", "Designer");

        // Manager 1
        Manager teamLead = new Manager("Eve", "Team Lead");
        teamLead.add(dev1);
        teamLead.add(dev2);

        // Manager 2
        Manager designHead = new Manager("Grace", "Design Head");
        designHead.add(designer);

        // Top-level Manager (CTO)
        Manager cto = new Manager("Mallory", "CTO");
        cto.add(teamLead);
        cto.add(designHead);

        // Show full organization hierarchy
        cto.showDetails("");
    }
}
