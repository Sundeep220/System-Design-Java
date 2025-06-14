package CreationPatterns.Prototype.prob1;

public class Armor implements Cloneable {
    String material;
    int defense;

    public Armor(String material, int defense) {
        this.material = material;
        this.defense = defense;
    }

    public Armor clone() {
        try {
            return (Armor) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Armor clone failed");
        }
    }

    public String toString() {
        return material + " (" + defense + " def)";
    }
}
