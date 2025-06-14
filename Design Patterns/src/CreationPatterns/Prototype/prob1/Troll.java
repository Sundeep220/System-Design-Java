package CreationPatterns.Prototype.prob1;

public class Troll implements GameCharacter {
    private String name;
    private int hitPoints;
    private Weapon weapon;
    private Armor armor;

    public Troll(String name, int hitPoints, Weapon weapon, Armor armor) {
        this.name = name;
        this.hitPoints = hitPoints;
        this.weapon = weapon;
        this.armor = armor;
    }

    @Override
    public Troll clone() {
        try {
            Weapon clonedWeapon = this.weapon.clone(); // deep copy
            Armor clonedArmor = this.armor.clone();    // deep copy
            return new Troll(this.name, this.hitPoints, clonedWeapon, clonedArmor);
        } catch (Exception e) {
            throw new RuntimeException("Cloning failed");
        }
    }

    @Override
    public void displayStats() {
        System.out.println("[Troll] " + name + ": HP=" + hitPoints +
                ", Weapon=" + weapon + ", Armor=" + armor);
    }

    // Setters for customization
    public void setName(String name) {
        this.name = name;
    }

    public void setWeapon(Weapon weapon) {
        this.weapon = weapon;
    }

    public void setArmor(Armor armor) {
        this.armor = armor;
    }
}

