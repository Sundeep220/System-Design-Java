# 🧪 Prototype Pattern Assignment – Game Character Cloning

## 📘 Problem Statement

You are developing a game that requires frequent creation of similar game characters like **Orcs** and **Trolls**.  
Creating each character from scratch is expensive because each one has complex internal objects like weapons and armor.

To optimize performance, you are required to implement the **Prototype Design Pattern** so you can:

- Define base prototypes (`Orc`, `Troll`)
- Clone them on demand
- Customize the cloned objects independently

This exercise should also demonstrate the difference between **shallow** and **deep copying**.

---

## ✅ Requirements

1. Define an interface `GameCharacter` with:
    - A `clone()` method
    - A `displayStats()` method

2. Create two classes: `Orc` and `Troll` that implement `GameCharacter`.  
   Each character should have:
    - `name`
    - `hitPoints`
    - A `Weapon` object (`type`, `damage`)
    - An `Armor` object (`material`, `defense`)

3. Implement **deep copy** in the `clone()` method of each class:
    - Ensure `Weapon` and `Armor` are **independent** in cloned objects

4. Demonstrate usage in a `Main` class:
    - Create base prototypes (`Orc`, `Troll`)
    - Clone multiple characters
    - Customize each clone's weapon, armor, and name
    - Print stats using `displayStats()` to verify clones are independent

---

## 🧾 Output Example

```text
=== Characters Created ===
[Orc] Gorg: HP=100, Weapon=Sword (25 dmg), Armor=Steel (15 def)
[Orc] Gorg Clone1: HP=100, Weapon=Axe (20 dmg), Armor=Leather (10 def)
[Orc] Gorg Clone2: HP=100, Weapon=Mace (18 dmg), Armor=Cloth (5 def)
[Troll] Brak: HP=120, Weapon=Club (30 dmg), Armor=Stone (20 def)
[Troll] Brak Clone1: HP=120, Weapon=Spiked Club (35 dmg), Armor=Bone (18 def)
