package CreationPatterns.Prototype.prob1;


public interface GameCharacter extends Cloneable {
    GameCharacter clone();
    void displayStats();
}