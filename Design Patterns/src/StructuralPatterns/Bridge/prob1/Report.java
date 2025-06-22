package StructuralPatterns.Bridge.prob1;

public abstract class Report {
    protected ReportFormatter formatter;

    public Report(ReportFormatter formatter) {
        this.formatter = formatter;
    }

    public abstract void display();
}

