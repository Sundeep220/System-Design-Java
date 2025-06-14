package CreationPatterns.Prototype.prob2.components;

public class MetaData {
    private String createdBy;
    private String department;

    public MetaData(String createdBy, String department) {
        this.createdBy = createdBy;
        this.department = department;
    }

    public MetaData(MetaData original) {
        this.createdBy = original.createdBy;
        this.department = original.department;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}

