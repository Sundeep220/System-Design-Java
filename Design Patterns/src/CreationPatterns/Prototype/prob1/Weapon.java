package CreationPatterns.Prototype.prob1;

public class Weapon implements Cloneable {
    String type;
    int damage;

    public Weapon(String type, int damage) {
        this.type = type;
        this.damage = damage;
    }

    public Weapon clone() {
        try {
            return (Weapon) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Weapon clone failed");
        }
    }

    public String toString() {
        return type + " (" + damage + " dmg)";
    }
}