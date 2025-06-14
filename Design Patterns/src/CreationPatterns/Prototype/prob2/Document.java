package CreationPatterns.Prototype.prob2;

public interface Document extends Cloneable{
    Document clone();
    void printContent();
}
