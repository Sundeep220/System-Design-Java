package CreationPatterns.Prototype.prob1;

public class Main {
    public static void main(String[] args) {
        // Base prototypes
        Orc orcPrototype = new Orc(
                "Gorg", 100,
                new Weapon("Sword", 25),
                new Armor("Steel", 15)
        );

        Troll trollPrototype = new Troll(
                "Brak", 120,
                new Weapon("Club", 30),
                new Armor("Stone", 20)
        );

        // Clone and customize
        Orc orc1 = orcPrototype.clone();
        orc1.setName("Gorg Clone1");
        orc1.setWeapon(new Weapon("Axe", 20));
        orc1.setArmor(new Armor("Leather", 10));

        Orc orc2 = orcPrototype.clone();
        orc2.setName("Gorg Clone2");
        orc2.setWeapon(new Weapon("Mace", 18));
        orc2.setArmor(new Armor("Cloth", 5));

        Troll troll1 = trollPrototype.clone();
        troll1.setName("Brak Clone1");
        troll1.setWeapon(new Weapon("Spiked Club", 35));
        troll1.setArmor(new Armor("Bone", 18));

        // Display all characters
        System.out.println("=== Characters Created ===");
        orcPrototype.displayStats();
        orc1.displayStats();
        orc2.displayStats();
        trollPrototype.displayStats();
        troll1.displayStats();
    }
}
