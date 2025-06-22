# 🧬 Prototype Design Pattern in Java

## 🎯 Intent

> To create duplicate objects (clones) without depending on their exact class.
Specify the kinds of objects to create using a prototypical instance and create new objects by copying this prototype.

---

## 💡 Why Use It?

* Object creation is expensive (e.g., loading from database, file parsing)
* You need many similar objects with minor variations
* You want to avoid constructors with complex logic

## 🧠 Real-World Analogy

Imagine you're designing a document or game character:

* You create a base **template** or **prototype**
* Then, you clone it to produce many copies with small customizations
* This saves time and avoids rebuilding the whole object from scratch

---

## 🔄 Structure

```java
interface Prototype extends Cloneable {
    Prototype clone();
}

class ConcretePrototype implements Prototype {
    private String field;

    public ConcretePrototype(String field) {
        this.field = field;
    }

    public Prototype clone() {
        return new ConcretePrototype(this.field);
    }
}
```

---

## ✅ Use Cases

* Object creation is **expensive** (e.g., loading from database, file parsing)
* You need **many similar objects** with minor variations
* You want to avoid constructors with complex logic

---

## 🧪 Example: Game Character Cloning

```java
public interface GameCharacter extends Cloneable {
    GameCharacter clone();
    void displayStats();
}

class Weapon implements Cloneable {
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
            throw new RuntimeException();
        }
    }

    public String toString() {
        return type + " (" + damage + " dmg)";
    }
}

class Armor implements Cloneable {
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
            throw new RuntimeException();
        }
    }

    public String toString() {
        return material + " (" + defense + " def)";
    }
}

class Orc implements GameCharacter {
    private String name;
    private int hitPoints;
    private Weapon weapon;
    private Armor armor;

    public Orc(String name, int hitPoints, Weapon weapon, Armor armor) {
        this.name = name;
        this.hitPoints = hitPoints;
        this.weapon = weapon;
        this.armor = armor;
    }

    public Orc clone() {
        Weapon clonedWeapon = this.weapon.clone();
        Armor clonedArmor = this.armor.clone();
        return new Orc(this.name, this.hitPoints, clonedWeapon, clonedArmor);
    }

    public void displayStats() {
        System.out.println("[Orc] " + name + ": HP=" + hitPoints + ", Weapon=" + weapon + ", Armor=" + armor);
    }

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
```

---

## 🤔 Doubts and Clarifications

### ❓ Is Prototype Pattern only for shallow copy?

**No!** Prototype can use either:

* **Shallow copy** (default with `Object.clone()`)
* **Deep copy** (manually implemented, especially when object has nested objects)

### ❓ Is deep copy expensive?

Yes, it **can** be — but:

* It's still often cheaper than rebuilding the object from scratch
* You control which parts need deep copying
* It’s valuable when the prototype is cloned **many times**

### ❓ If Builder already returns `new` object, why use `new` inside it?

In Builder, we use `new` inside the `build()` method **once**. The Builder pattern is about **encapsulating construction logic**, not avoiding `new`. The goal is to simplify **step-wise customization**, not clone behavior.

### ❓ Prototype vs Builder?

| Aspect | Prototype                        | Builder                           |
| ------ | -------------------------------- | --------------------------------- |
| Reuse  | Clones a ready-made object       | Constructs object step-by-step    |
| Cost   | Ideal when creation is expensive | Flexible when many optional parts |
| Goal   | Fast duplication                 | Complex assembly                  |

---

## ✅ Summary

| Feature           | Description                                                         |
| ----------------- | ------------------------------------------------------------------- |
| Type              | Creational Pattern                                                  |
| Key Idea          | Clone existing objects to avoid repetitive creation                 |
| Use When          | Object construction is expensive and many similar copies are needed |
| Deep Copy Support | ✅ Yes, via custom logic                                             |
| Common Use        | Game objects, UI components, documents, configuration templates     |

---

